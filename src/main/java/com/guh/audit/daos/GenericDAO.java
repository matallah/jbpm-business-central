package com.guh.audit.daos;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

public abstract class GenericDAO<T> {

    @PersistenceContext(unitName = "auditPU")
    protected EntityManager em;

    private final Class<T> entityClass;

    protected GenericDAO(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    public T save(T entity) {
        if (em.contains(entity)) {
            return em.merge(entity);
        } else {
            em.persist(entity);
            return entity;
        }
    }

    public T findById(Long id) {
        return em.find(entityClass, id);
    }

    public void delete(T entity) {
        em.remove(em.contains(entity) ? entity : em.merge(entity));
    }

    public List<T> findAll() {
        return em.createQuery("SELECT e FROM " + entityClass.getSimpleName() + " e", entityClass)
                .getResultList();
    }
}