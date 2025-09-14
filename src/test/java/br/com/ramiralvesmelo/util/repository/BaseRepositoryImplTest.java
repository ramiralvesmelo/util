package br.com.ramiralvesmelo.util.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

/**
 * Cobertura completa (mockada) para BaseRepositoryImpl.
 */
@ExtendWith(MockitoExtension.class)
class BaseRepositoryImplTest {

    // ==========
    // Suporte
    // ==========
    static class TestEntity {
        Long id;
        String nome;

        TestEntity() {}
        TestEntity(Long id, String nome) { this.id = id; this.nome = nome; }

        public Long getId() { return id; }
        public String getNome() { return nome; }
        public void setId(Long id) { this.id = id; }
        public void setNome(String nome) { this.nome = nome; }
    }

    static class TestRepo extends BaseRepositoryImpl<TestEntity, Long> implements BaseRepository<TestEntity, Long> {
        void setEntityManager(EntityManager em) { this.entityManager = em; }
    }

    private TestRepo repo;

    @Mock private EntityManager em;

    // Mocks para findAll
    @Mock private CriteriaBuilder cb;
    @Mock private CriteriaQuery<TestEntity> cqEntity;
    @Mock private Root<TestEntity> rootEntity;
    @Mock private TypedQuery<TestEntity> typedQueryEntity;

    // Mocks para count
    @Mock private CriteriaQuery<Long> cqLong;
    @Mock private Root<TestEntity> rootCount;
    @Mock private TypedQuery<Long> typedQueryLong;

    @BeforeEach
    void setup() {
        repo = new TestRepo();
        repo.setEntityManager(em);
    }

    @Test
    @DisplayName("save() deve delegar para entityManager.merge e retornar a entidade gerenciada")
    void save_ok() {
        TestEntity detached = new TestEntity(1L, "a");
        TestEntity managed = new TestEntity(1L, "a*managed*");

        when(em.merge(detached)).thenReturn(managed);

        TestEntity out = repo.save(detached);

        assertNotNull(out);
        assertEquals("a*managed*", out.getNome());
        verify(em).merge(detached);
    }

    @Test
    @DisplayName("findById() deve retornar Optional presente quando existir")
    void findById_exists() {
        TestEntity e = new TestEntity(10L, "x");
        when(em.find(TestEntity.class, 10L)).thenReturn(e);

        Optional<TestEntity> opt = repo.findById(10L);

        assertTrue(opt.isPresent());
        assertEquals("x", opt.get().getNome());
        verify(em).find(TestEntity.class, 10L);
    }

    @Test
    @DisplayName("findById() deve retornar Optional vazio quando não existir")
    void findById_notExists() {
        when(em.find(TestEntity.class, 99L)).thenReturn(null);

        Optional<TestEntity> opt = repo.findById(99L);

        assertTrue(opt.isEmpty());
        verify(em).find(TestEntity.class, 99L);
    }

    @Test
    @DisplayName("findAll() deve montar Criteria API e retornar lista")
    void findAll_ok() {
        // Stubs do pipeline Criteria -> Query -> Result
        when(em.getCriteriaBuilder()).thenReturn(cb);

        when(cb.createQuery(TestEntity.class)).thenReturn(cqEntity);
        when(cqEntity.from(TestEntity.class)).thenReturn(rootEntity);
        // select precisa retornar o próprio cq
        when(cqEntity.select(any())).thenReturn(cqEntity);

        when(em.createQuery(cqEntity)).thenReturn(typedQueryEntity);
        when(typedQueryEntity.getResultList()).thenReturn(List.of(
                new TestEntity(1L, "a"),
                new TestEntity(2L, "b")
        ));

        List<TestEntity> all = repo.findAll();

        assertEquals(2, all.size());
        assertEquals("a", all.get(0).getNome());
        assertEquals("b", all.get(1).getNome());

        verify(em).getCriteriaBuilder();
        verify(cb).createQuery(TestEntity.class);
        verify(cqEntity).from(TestEntity.class);
        verify(cqEntity).select(any());
        verify(em).createQuery(cqEntity);
        verify(typedQueryEntity).getResultList();
    }

    @Test
    @DisplayName("delete() deve remover entidade quando já está gerenciada (contains=true)")
    void delete_whenManaged() {
        TestEntity e = new TestEntity(5L, "managed");

        when(em.contains(e)).thenReturn(true);

        repo.delete(e);

        verify(em).contains(e);
        verify(em).remove(e);
        verify(em, never()).merge(any());
    }

    @Test
    @DisplayName("delete() deve fazer merge antes de remover quando está destacada (contains=false)")
    void delete_whenDetached() {
        TestEntity detached = new TestEntity(6L, "detached");
        TestEntity managed = new TestEntity(6L, "managed");

        when(em.contains(detached)).thenReturn(false);
        when(em.merge(detached)).thenReturn(managed);

        repo.delete(detached);

        verify(em).contains(detached);
        verify(em).merge(detached);
        verify(em).remove(managed);
    }

    @Test
    @DisplayName("deleteById() deve encontrar e remover quando existir")
    void deleteById_exists() {
        TestEntity e = new TestEntity(7L, "will-delete");

        // findById -> em.find
        when(em.find(TestEntity.class, 7L)).thenReturn(e);
        // delete -> contains true para cair no remove direto
        when(em.contains(e)).thenReturn(true);

        repo.deleteById(7L);

        verify(em).find(TestEntity.class, 7L);
        verify(em).contains(e);
        verify(em).remove(e);
    }

    @Test
    @DisplayName("deleteById() não deve fazer nada quando não existir")
    void deleteById_notExists() {
        when(em.find(TestEntity.class, 404L)).thenReturn(null);

        repo.deleteById(404L);

        verify(em).find(TestEntity.class, 404L);
        verify(em, never()).remove(any());
    }

    @Test
    @DisplayName("existsById() deve retornar true/false conforme findById()")
    void existsById_ok() {
        when(em.find(TestEntity.class, 1L)).thenReturn(new TestEntity(1L, "a"));
        when(em.find(TestEntity.class, 2L)).thenReturn(null);

        assertTrue(repo.existsById(1L));
        assertFalse(repo.existsById(2L));

        verify(em).find(TestEntity.class, 1L);
        verify(em).find(TestEntity.class, 2L);
    }

    @Test
    @DisplayName("count() deve usar Criteria API e retornar total")
    void count_ok() {
        when(em.getCriteriaBuilder()).thenReturn(cb);

        // createQuery(Long)
        when(cb.createQuery(Long.class)).thenReturn(cqLong);

        // from(entityClass) e select(count(...)) precisam retornar o próprio cqLong
        when(cqLong.from(TestEntity.class)).thenReturn(rootCount);
        when(cqLong.select(any())).thenReturn(cqLong);

        when(em.createQuery(cqLong)).thenReturn(typedQueryLong);
        when(typedQueryLong.getSingleResult()).thenReturn(5L);

        long total = repo.count();

        assertEquals(5L, total);

        verify(em).getCriteriaBuilder();
        verify(cb).createQuery(Long.class);
        verify(cqLong).from(TestEntity.class);
        verify(cqLong).select(any());
        verify(em).createQuery(cqLong);
        verify(typedQueryLong).getSingleResult();
    }
}
