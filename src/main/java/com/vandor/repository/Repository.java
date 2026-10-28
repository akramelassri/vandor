package com.vandor.repository;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.*;

public abstract class Repository<T, ID> {

    private final EntityManager em;
    private final Class<T> entityClass;

    public Repository(EntityManager em, Class<T> entityClass) {
        this.em = em;
        this.entityClass = entityClass;
    }

    public T create(T entity) {
        em.persist(entity);
        return entity;
    }

    public T update(T entity) {
        return em.merge(entity);
    }

    public Optional<T> findById(ID id) {
        T entity = em.find(entityClass, id);

        return Optional.ofNullable(entity);
    }

    public List<T> findAll() {
        String entityName = em.getMetamodel()
                .entity(entityClass)
                .getName();
        return em.createQuery("FROM " + entityName, entityClass)
                .getResultList();
    }

    public void delete(ID id) {
        T entity = findById(id).orElseThrow(() -> {
            throw new EntityNotFoundException(entityClass.getSimpleName() + " not found with id " + id);
        });
        em.remove(entity);
    }
}
