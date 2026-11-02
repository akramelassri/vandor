package com.vandor.util;

import java.util.function.Consumer;
import java.util.function.Function;

import jakarta.persistence.EntityManager;

public class TransactionManager {

    public static <R> R runFunction(Function<EntityManager, R> action) {
        try (EntityManager em = JpaUtil.getFactory().createEntityManager()) {
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
                } catch (RuntimeException rollbackException) {
                    e.addSuppressed(rollbackException);
                }
                throw e;
            }
        }
    }

    public static void runAction(Consumer<EntityManager> action) {
        try (EntityManager em = JpaUtil.getFactory().createEntityManager()) {
            try {
                em.getTransaction().begin();
                action.accept(em);
                em.getTransaction().commit();
            } catch (RuntimeException e) {
                try {
                    if (em.getTransaction().isActive()) {
                        em.getTransaction().rollback();
                    }
                } catch (RuntimeException rollbackException) {
                    e.addSuppressed(rollbackException);
                }
                throw e;
            }
        }
    }
}
