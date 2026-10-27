package com.vandor.util;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.cfg.Configuration;

import com.vandor.models.*;

public class JpaUtil {
    static private EntityManagerFactory emf;

    synchronized public static EntityManagerFactory getFactory() {
        if (emf == null || !emf.isOpen()) {
            emf = createFactory();
        }
        return emf;
    }

    private static EntityManagerFactory createFactory() {
        Configuration configuration = new Configuration();

        // Jakarta
        configuration.setProperty("jakarta.persistence.jdbc.driver", "org.postgresql.Driver");
        configuration.setProperty("jakarta.persistence.jdbc.url", "jdbc:postgresql://localhost:5432/vandor_db");
        configuration.setProperty("jakarta.persistence.jdbc.user", "vandor");
        configuration.setProperty("jakarta.persistence.jdbc.password", "vandorpw");

        // Hibernate
        configuration.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        configuration.setProperty("hibernate.show_sql", "true");
        configuration.setProperty("hibernate.format_sql", "true");
        configuration.setProperty("hibernate.hbm2ddl.auto", "validate");

        // Entities
        configuration.addAnnotatedClass(User.class);
        configuration.addAnnotatedClass(CustomerUser.class);
        configuration.addAnnotatedClass(SellerUser.class);

        configuration.addAnnotatedClass(Product.class);
        configuration.addAnnotatedClass(Category.class);

        configuration.addAnnotatedClass(CartItem.class);

        configuration.addAnnotatedClass(Order.class);
        configuration.addAnnotatedClass(OrderItem.class);

        configuration.addAnnotatedClass(Session.class);

        return configuration.buildSessionFactory();
    }

    public static synchronized void closeFactory() {
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
        emf = null;
    }
}
