package br.com.ramiralvesmelo.util.data.jpa;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.com.ramiralvesmelo.util.commons.dto.OrderItemDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

class BaseRepositoryImplTest {

    private EntityManager em;
    private CriteriaBuilder cb;

    @SuppressWarnings({"rawtypes", "unchecked"})
    private BaseRepositoryImpl<OrderItemDto, Long> repo;

    @BeforeEach
    @SuppressWarnings({ "rawtypes", "unchecked" })
    void setUp() throws Exception {
        em = mock(EntityManager.class);
        cb = mock(CriteriaBuilder.class);

        // Cria subclasse anônima só no teste para fixar o tipo genérico T=OrderItemDto
        repo = new BaseRepositoryImpl<OrderItemDto, Long>() {};

        // Injeta o EntityManager no campo @PersistenceContext via reflexão
        Field f = BaseRepositoryImpl.class.getDeclaredField("entityManager");
        f.setAccessible(true);
        f.set(repo, em);
    }

    @Test
    void save_deveChamarMergeERetornarEntidade() {
        OrderItemDto in = OrderItemDto.builder().id(1L).productName("A").build();
        OrderItemDto merged = in.toBuilder().productName("B").build();

        when(em.merge(in)).thenReturn(merged);

        OrderItemDto out = repo.save(in);

        assertNotNull(out);
        assertEquals("B", out.getProductName());
        verify(em).merge(in);
        verifyNoMoreInteractions(em);
    }

    @Test
    void findById_quandoExiste_deveRetornarOptionalComValor() {
        OrderItemDto obj = OrderItemDto.builder().id(10L).build();
        when(em.find(OrderItemDto.class, 10L)).thenReturn(obj);

        Optional<OrderItemDto> opt = repo.findById(10L);

        assertTrue(opt.isPresent());
        assertEquals(10L, opt.get().getId());
        verify(em).find(OrderItemDto.class, 10L);
        verifyNoMoreInteractions(em);
    }

    @Test
    void findById_quandoNaoExiste_deveRetornarOptionalVazio() {
        when(em.find(OrderItemDto.class, 999L)).thenReturn(null);

        Optional<OrderItemDto> opt = repo.findById(999L);

        assertTrue(opt.isEmpty());
        verify(em).find(OrderItemDto.class, 999L);
        verifyNoMoreInteractions(em);
    }

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    void findAll_deveUsarCriteriaApiEListar() {
        CriteriaQuery cq = mock(CriteriaQuery.class);          // raw para simplificar
        Root root = mock(Root.class);
        TypedQuery typedQuery = mock(TypedQuery.class);

        when(em.getCriteriaBuilder()).thenReturn(cb);
        when(cb.createQuery(OrderItemDto.class)).thenReturn(cq);
        when(cq.from(OrderItemDto.class)).thenReturn(root);
        when(cq.select(root)).thenReturn(cq);
        when(em.createQuery(cq)).thenReturn(typedQuery);

        OrderItemDto a = OrderItemDto.builder().id(1L).build();
        OrderItemDto b = OrderItemDto.builder().id(2L).build();
        when(typedQuery.getResultList()).thenReturn(List.of(a, b));

        List<OrderItemDto> res = repo.findAll();

        assertEquals(2, res.size());
        assertEquals(1L, res.get(0).getId());
        assertEquals(2L, res.get(1).getId());

        verify(em).getCriteriaBuilder();
        verify(cb).createQuery(OrderItemDto.class);
        verify(cq).from(OrderItemDto.class);
        verify(cq).select(root);
        verify(em).createQuery(cq);
        verify(typedQuery).getResultList();
        verifyNoMoreInteractions(em, cb, cq, root, typedQuery);
    }

    @Test
    void delete_quandoEntityManagerContemEntidade_deveRemoverDireto() {
        OrderItemDto e = OrderItemDto.builder().id(3L).build();

        when(em.contains(e)).thenReturn(true);

        repo.delete(e);

        verify(em).contains(e);
        verify(em).remove(e);
        verifyNoMoreInteractions(em);
    }

    @Test
    void delete_quandoEntityManagerNaoContem_deveMergeAntesDeRemover() {
        OrderItemDto e = OrderItemDto.builder().id(4L).productName("X").build();
        OrderItemDto merged = e.toBuilder().productName("X-merged").build();

        when(em.contains(e)).thenReturn(false);
        when(em.merge(e)).thenReturn(merged);

        repo.delete(e);

        verify(em).contains(e);
        verify(em).merge(e);
        verify(em).remove(merged);
        verifyNoMoreInteractions(em);
    }

    @Test
    void deleteById_quandoExiste_deveRemover() {
        OrderItemDto found = OrderItemDto.builder().id(5L).build();

        when(em.find(OrderItemDto.class, 5L)).thenReturn(found);
        when(em.contains(found)).thenReturn(true);

        repo.deleteById(5L);

        verify(em).find(OrderItemDto.class, 5L);
        verify(em).contains(found);
        verify(em).remove(found);
        verifyNoMoreInteractions(em);
    }

    @Test
    void deleteById_quandoNaoExiste_naoChamaRemove() {
        when(em.find(OrderItemDto.class, 777L)).thenReturn(null);

        repo.deleteById(777L);

        verify(em).find(OrderItemDto.class, 777L);
        verifyNoMoreInteractions(em);
    }

    @Test
    void existsById_trueQuandoEncontrado_falseQuandoNao() {
        OrderItemDto obj = OrderItemDto.builder().id(9L).build();

        when(em.find(OrderItemDto.class, 9L)).thenReturn(obj);
        assertTrue(repo.existsById(9L));

        // reset para cenário false
        reset(em);
        when(em.find(OrderItemDto.class, 9L)).thenReturn(null);
        assertFalse(repo.existsById(9L));
    }

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    void count_deveUsarCriteriaApiEretornarValor() {
        CriteriaQuery<Long> cqLong = mock(CriteriaQuery.class);
        Root root = mock(Root.class);
        // ✅ count() retorna Expression<Long>
        jakarta.persistence.criteria.Expression<Long> exprCount = mock(jakarta.persistence.criteria.Expression.class);
        TypedQuery<Long> typedQuery = mock(TypedQuery.class);

        when(em.getCriteriaBuilder()).thenReturn(cb);
        when(cb.createQuery(Long.class)).thenReturn(cqLong);
        when(cqLong.from(OrderItemDto.class)).thenReturn(root);

        // ✅ ajustar para Expression<Long>
        when(cb.count(any())).thenReturn(exprCount);
        when(cqLong.select(exprCount)).thenReturn(cqLong);

        when(em.createQuery(cqLong)).thenReturn(typedQuery);
        when(typedQuery.getSingleResult()).thenReturn(5L);

        long total = repo.count();

        assertEquals(5L, total);

        verify(em).getCriteriaBuilder();
        verify(cb).createQuery(Long.class);
        verify(cqLong).from(OrderItemDto.class);
        verify(cb).count(any());
        verify(cqLong).select(exprCount);
        verify(em).createQuery(cqLong);
        verify(typedQuery).getSingleResult();
        verifyNoMoreInteractions(em, cb, cqLong, root, typedQuery, exprCount);
    }

}
