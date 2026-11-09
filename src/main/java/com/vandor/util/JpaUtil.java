package com.vandor.util;

import com.vandor.models.CartItem;
import com.vandor.models.Category;
import com.vandor.models.CustomerUser;
import com.vandor.models.Order;
import com.vandor.models.OrderItem;
import com.vandor.models.Product;
import com.vandor.models.SellerUser;
import com.vandor.models.Session;
import com.vandor.models.User;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.cfg.Configuration;

public class JpaUtil {
  private static EntityManagerFactory emf;

  public static synchronized EntityManagerFactory getFactory() {
    if (emf == null || !emf.isOpen()) {
      emf = createFactory();
    }
    return emf;
  }

  private static EntityManagerFactory createFactory() {
    Configuration configuration = new Configuration();

    // Jakarta
    configuration.setProperty("jakarta.persistence.jdbc.driver", "org.postgresql.Driver");
    configuration.setProperty(
        "jakarta.persistence.jdbc.url",
        env("DB_URL", "jdbc:postgresql://vandor-db:5432/vandor_db"));
    configuration.setProperty("jakarta.persistence.jdbc.user", env("DB_USER", "vandor"));
    configuration.setProperty("jakarta.persistence.jdbc.password", env("DB_PASSWORD", "vandorpw"));

    // Hibernate
    configuration.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
    configuration.setProperty("hibernate.show_sql", "false");
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

  private static String env(String name, String fallback) {
    String value = System.getenv(name);
    return value == null || value.isBlank() ? fallback : value;
  }

  public static synchronized void closeFactory() {
    if (emf != null && emf.isOpen()) {
      emf.close();
    }
    emf = null;
  }
}
