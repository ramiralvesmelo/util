package br.com.ramiralvesmelo.util.data.jpa;

import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Optional;

import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

/**
 * Implementação base para repositórios.
 * @param <T> Tipo da entidade
 * @param <ID> Tipo do identificador da entidade
 */
public abstract class BaseRepositoryImpl<T, ID> implements BaseRepository<T, ID> {
    
    @PersistenceContext
    protected EntityManager entityManager;
    
    private final Class<T> entityClass;
    
    @SuppressWarnings("unchecked")
    public BaseRepositoryImpl() {
        this.entityClass = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass())
                .getActualTypeArguments()[0];
    }
    
    @Override
    @Transactional
    public T save(T entity) {
        return entityManager.merge(entity);
    }
    
    @Override    
    public Optional<T> findById(ID id) {
        return Optional.ofNullable(entityManager.find(entityClass, id));
    }
    
    @Override
    public List<T> findAll() {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> cq = cb.createQuery(entityClass);
        Root<T> root = cq.from(entityClass);
        cq.select(root);
        TypedQuery<T> query = entityManager.createQuery(cq);
        return query.getResultList();
    }
    
    @Override
    @Transactional
    public void delete(T entity) {
        entityManager.remove(entityManager.contains(entity) ? entity : entityManager.merge(entity));
    }
    
    @Override
    @Transactional
    public void deleteById(ID id) {
        findById(id).ifPresent(this::delete);
    }
    
    @Override
    public boolean existsById(ID id) {
        return findById(id).isPresent();
    }
    
    @Override
    public long count() {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        cq.select(cb.count(cq.from(entityClass)));
        return entityManager.createQuery(cq).getSingleResult();
    }
}