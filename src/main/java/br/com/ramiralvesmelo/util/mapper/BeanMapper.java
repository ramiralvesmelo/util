package br.com.ramiralvesmelo.util.mapper;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/** Marca um campo para ser ignorado no mapeamento. */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
@interface IgnoreMapping {}

/** Alias de nome do campo de origem (quando o nome não bate 1:1). */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
@interface Alias {
    String value();
}

/**
 * BeanMapper (reflection-based)
 * -----------------------------
 * - map(S, Class<D>) -> D novo
 * - mapInto(S, D) -> atualiza destino existente (somente não-nulos)
 * - mapInto(S, D, resolvers) -> com resoluções customizadas por nome do campo destino
 *
 * Observações:
 * - Usa cache de Fields por classe.
 * - Copia somente valores != null (não sobrescreve com null).
 * - Suporta Enum<->String, Number<->Number, CharSequence<->String, coleções compatíveis.
 * - Campos anotados com @IgnoreMapping são ignorados.
 * - Campos com @Alias usam o nome indicado para buscar no fonte.
 * - ✅ Suporte a RECORDS: instancia via construtor canônico (RecordComponents por nome).
 */
public final class BeanMapper {

    private BeanMapper() {}

    // Cache: classe -> (nomeCampo -> Field)
    private static final Map<Class<?>, Map<String, Field>> FIELDS_CACHE = new ConcurrentHashMap<>();

    /** Cria e popula um novo destino a partir do fonte (sem resolvers). */
    public static <S, D> D map(S src, Class<D> destType) {
        return map(src, destType, Collections.emptyMap());
    }

    /** Cria e popula um novo destino a partir do fonte com resolvers. */
    public static <S, D> D map(S src, Class<D> destType, Map<String, Function<S, Object>> resolvers) {
        if (src == null) return null;
        try {
            // ✅ Se for RECORD, constrói via construtor canônico
            if (isRecord(destType)) {
                return constructRecord(src, destType, resolvers);
            }

            // Caminho padrão: precisa de no-arg
            D dest = newInstance(destType);
            mapInto(src, dest, resolvers);
            return dest;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao instanciar " + destType.getName(), e);
        }
    }

    /** Atualiza destino (somente não-nulos) sem resolvers. */
    public static <S, D> void mapInto(S src, D dest) {
        mapInto(src, dest, Collections.emptyMap());
    }

    /**
     * Atualiza destino (somente não-nulos) com resolvers customizados por nome de campo de destino.
     * Ex.: resolver "order" (Order entity) a partir de "orderId" no DTO.
     */
    public static <S, D> void mapInto(S src, D dest, Map<String, Function<S, Object>> resolvers) {
        if (src == null || dest == null) return;

        Map<String, Field> srcFields  = getAllFields(src.getClass());
        Map<String, Field> destFields = getAllFields(dest.getClass());

        for (Map.Entry<String, Field> e : destFields.entrySet()) {
            Field destField = e.getValue();
            if (destField.isAnnotationPresent(IgnoreMapping.class)) continue;

            String destName = destField.getName();
            Object value = null;

            // 1) Resolver customizado tem prioridade
            Function<S, Object> resolver = resolvers.get(destName);
            if (resolver != null) {
                value = resolver.apply(src);
            } else {
                // 2) Nome do campo de origem: alias ou o mesmo nome
                String srcName = destField.isAnnotationPresent(Alias.class)
                        ? destField.getAnnotation(Alias.class).value()
                        : destName;

                Field srcField = srcFields.get(srcName);
                if (srcField == null) {
                    // tenta "id" para associações, ex.: order (dest) ← orderId (src)
                    if (destName.endsWith("Id")) {
                        continue; // caso especial resolvido por resolver
                    } else if (destName.equalsIgnoreCase("id")) {
                        srcField = firstNonNull(
                                srcFields.get("id"),
                                srcFields.get("ID"),
                                srcFields.get("Id")
                        );
                    }
                }
                if (srcField != null) {
                    value = read(src, srcField);
                }
            }

            if (value == null) continue; // não sobrescreve com null

            // 3) Conversão de tipo quando necessário
            Object converted = convertIfNeeded(value, destField.getType(), destField.getGenericType());
            if (converted == Skip.INSTANCE) continue;

            write(dest, destField, converted);
        }
    }

    // ==== Suporte a RECORD ====

    private static boolean isRecord(Class<?> type) {
        try {
            return type.isRecord();
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Constrói um record chamando o construtor canônico com os argumentos na ordem dos RecordComponents.
     * Cada argumento é resolvido por:
     *   1) resolver por nome do componente (se fornecido),
     *   2) campo homônimo na origem (com fallback para "id"),
     *   3) conversão de tipo (Enum/String, Number, etc).
     */
    private static <S, D> D constructRecord(S src, Class<D> destType, Map<String, Function<S, Object>> resolvers) throws Exception {
        RecordComponent[] comps = destType.getRecordComponents();
        Class<?>[] paramTypes = new Class<?>[comps.length];
        Object[] args = new Object[comps.length];

        Map<String, Field> srcFields = getAllFields(src.getClass());

        for (int i = 0; i < comps.length; i++) {
            RecordComponent rc = comps[i];
            String name = rc.getName();
            Class<?> type = rc.getType();
            paramTypes[i] = type;

            Object value = null;

            // 1) resolver por nome do destino
            if (resolvers != null) {
                @SuppressWarnings("unchecked")
                Function<S, Object> r = (Function<S, Object>) resolvers.get(name);
                if (r != null) value = r.apply(src);
            }

            // 2) se não veio de resolver, tenta campo homônimo na origem
            if (value == null) {
                Field f = srcFields.get(name);
                if (f == null && "id".equalsIgnoreCase(name)) {
                    f = firstNonNull(srcFields.get("id"), srcFields.get("ID"), srcFields.get("Id"));
                }
                if (f != null) value = read(src, f);
            }

            // 3) conversão
            Object converted = convertIfNeeded(value, type, type);
            args[i] = (converted == Skip.INSTANCE) ? null : converted;
        }

        Constructor<D> ctor = destType.getDeclaredConstructor(paramTypes);
        ctor.setAccessible(true);
        return ctor.newInstance(args);
    }

    // ==== Helpers de conversão ====

    private static Object convertIfNeeded(Object value, Class<?> targetType, Type genericType) {
        if (value == null) return null;

        Class<?> srcType = value.getClass();
        if (targetType.isAssignableFrom(srcType)) {
            return value;
        }

        // String <-> Enum
        if (targetType.isEnum() && value instanceof CharSequence) {
            @SuppressWarnings({"unchecked","rawtypes"})
            Object e = enumFromString((Class<? extends Enum>) targetType, value.toString());
            return e;
        }
        if (srcType.isEnum() && targetType.equals(String.class)) {
            return ((Enum<?>) value).name();
        }

        // Number <-> Number
        if (Number.class.isAssignableFrom(targetType) && Number.class.isAssignableFrom(srcType)) {
            return numberToTarget((Number) value, targetType);
        }

        // CharSequence -> String
        if (targetType.equals(String.class) && CharSequence.class.isAssignableFrom(srcType)) {
            return value.toString();
        }

        // Coleções simples: tenta copiar quando elementos são atribuíveis
        if (value instanceof Collection && Collection.class.isAssignableFrom(targetType)) {
            return copyCollection((Collection<?>) value, targetType, genericType);
        }

        // Tipos temporais / BigDecimal → deixa passar apenas se atribuível (evita conversões perigosas)
        if (value instanceof BigDecimal || value instanceof Temporal) {
            return (targetType.isAssignableFrom(srcType)) ? value : Skip.INSTANCE;
        }

        // Como fallback: não converte
        return Skip.INSTANCE;
    }

    private static Object copyCollection(Collection<?> src, Class<?> targetType, Type genericType) {
        Collection<Object> dest;
        if (targetType.isInterface()) {
            if (Set.class.isAssignableFrom(targetType)) dest = new LinkedHashSet<>();
            else dest = new ArrayList<>();
        } else {
            try {
                dest = (Collection<Object>) newInstance(targetType);
            } catch (Exception e) {
                dest = new ArrayList<>();
            }
        }
        dest.addAll(src); // elementos devem ser atribuíveis; se precisar mapear elementos, use resolvers específicos
        return dest;
    }

    private static Number numberToTarget(Number n, Class<?> target) {
        if (target.equals(Byte.class))   return n.byteValue();
        if (target.equals(Short.class))  return n.shortValue();
        if (target.equals(Integer.class))return n.intValue();
        if (target.equals(Long.class))   return n.longValue();
        if (target.equals(Float.class))  return n.floatValue();
        if (target.equals(Double.class)) return n.doubleValue();
        if (target.equals(BigDecimal.class)) return new BigDecimal(n.toString());
        return n;
    }

    private static <E extends Enum<E>> E enumFromString(Class<E> enumType, String name) {
        try {
            return Enum.valueOf(enumType, name);
        } catch (IllegalArgumentException ex) {
            for (E c : enumType.getEnumConstants()) {
                if (c.name().equalsIgnoreCase(name)) return c;
            }
            throw ex;
        }
    }

    // ==== Reflection util ====

    private static Map<String, Field> getAllFields(Class<?> type) {
        return FIELDS_CACHE.computeIfAbsent(type, BeanMapper::scanFields);
    }

    private static Map<String, Field> scanFields(Class<?> type) {
        Map<String, Field> map = new LinkedHashMap<>();
        Class<?> t = type;
        while (t != null && t != Object.class) {
            for (Field f : t.getDeclaredFields()) {
                f.setAccessible(true);
                map.putIfAbsent(f.getName(), f);
            }
            t = t.getSuperclass();
        }
        return Collections.unmodifiableMap(map);
    }

    private static Object read(Object obj, Field f) {
        try {
            return f.get(obj);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Não foi possível ler campo " + f.getName(), e);
        }
    }

    private static void write(Object obj, Field f, Object value) {
        try {
            f.setAccessible(true);
            f.set(obj, value);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Não foi possível escrever campo " + f.getName(), e);
        }
    }

    private static <T> T newInstance(Class<T> type) {
        try {
            Constructor<T> c = type.getDeclaredConstructor();
            c.setAccessible(true);
            return c.newInstance();
        } catch (Exception e) {
            throw new IllegalStateException("Classe precisa de construtor padrão: " + type.getName(), e);
        }
    }

    @SafeVarargs
    private static <T> T firstNonNull(T... vals) {
        for (T v : vals) if (v != null) return v;
        return null;
    }

    /** Sinalizador para “não mapear”. */
    private enum Skip { INSTANCE }
}
