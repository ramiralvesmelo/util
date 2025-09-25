package br.com.ramiralvesmelo.util.mapper;

import org.modelmapper.ModelMapper;

public final class BeanMapper {

    private static final ModelMapper MAPPER = new ModelMapper();

    private BeanMapper() {
        // utilitário -> não instanciável
    }

    /**
     * Copia propriedades do objeto de origem para a classe de destino.
     *
     * @param source objeto de origem
     * @param destClass classe de destino
     * @return instância da classe de destino com os valores mapeados
     */
    public static <T> T map(Object source, Class<T> destClass) {
        return MAPPER.map(source, destClass);
    }
}
