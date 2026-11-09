package com.vandor.util;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.util.function.Function;

public class TransactionManager {
  private final EntityManagerFactory factory;

  public TransactionManager() {
    factory = null;
  }

  public TransactionManager(EntityManagerFactory factory) {
    this.factory = factory;
  }

  public <R> R execute(Function<EntityManager, R> action) {
    try (EntityManager em =
        (factory == null ? JpaUtil.getFactory() : factory).createEntityManager()) {
      try {
        em.getTransaction().begin();
        R result = action.apply(em);
        em.getTransaction().commit();
        return result;
      } catch (RuntimeException e) {
        try {
          if (em.getTransaction().isActive()) {
            em.getTransaction().rollback();
          }
        } catch (RuntimeException rollback) {
          e.addSuppressed(rollback);
        }
        throw e;
      }
    }
  }
}
