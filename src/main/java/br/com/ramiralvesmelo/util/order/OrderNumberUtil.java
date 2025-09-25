package br.com.ramiralvesmelo.util.order;

import com.github.f4b6a3.ulid.UlidCreator;

/**
 * Classe utilitária para gerar números de pedido baseados em ULID.
 */
public final class OrderNumberUtil {

    private OrderNumberUtil() {
    }

    /**
     * Gera número de pedido no formato ORD-{ULID}
     * Exemplo: ORD-01J7ZXDW9Y7N1J5SJQ6EZX5QK4
     */
    public static String generate() {
        return "ORD-" + UlidCreator.getUlid().toString();
    }
}
