package com.vandor.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vandor.dto.RegisterRequest;
import com.vandor.exceptions.EmailAlreadyRegisteredException;
import com.vandor.exceptions.ValidationException;
import com.vandor.models.CustomerUser;
import com.vandor.models.SellerUser;
import com.vandor.models.User;
import com.vandor.models.UserRole;
import com.vandor.models.UserStatus;
import com.vandor.util.PasswordUtil;
import com.vandor.util.TransactionManager;
import jakarta.persistence.EntityManager;
import java.sql.SQLException;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuthServiceTest {
  private SessionFactory factory;
  private AuthService service;

  @BeforeEach
  void setUp() {
    factory =
        new Configuration()
            .setProperty("hibernate.connection.driver_class", "org.h2.Driver")
            .setProperty("hibernate.connection.url", "jdbc:h2:mem:" + UUID.randomUUID())
            .setProperty("hibernate.hbm2ddl.auto", "create-drop")
            .addAnnotatedClass(User.class)
            .addAnnotatedClass(CustomerUser.class)
            .addAnnotatedClass(SellerUser.class)
            .buildSessionFactory();
    service = new AuthService(new TransactionManager(factory));
  }

  @AfterEach
  void close() {
    factory.close();
  }

  private void transact(Consumer<EntityManager> action) {
    new TransactionManager(factory)
        .execute(em -> {
          action.accept(em);
          return null;
        });
  }

  private AuthService failingService(RuntimeException failure) {
    return new AuthService(
        new TransactionManager() {
          @Override
          public <R> R execute(Function<EntityManager, R> action) {
            throw failure;
          }
        });
  }

  private RegisterRequest data(String role) {
    return new RegisterRequest(" Jane ", " Cooper ", " Jane@Example.COM ", " secret123 ", role);
  }

  @Test
  void createsCustomerWithNormalizedFieldsAndHashedPassword() {
    service.registerUser(data("CUSTOMER"));
    transact(em -> {
      User user = em.createQuery("FROM User", User.class).getSingleResult();
      assertEquals("Jane", user.getFirstName());
      assertEquals("Cooper", user.getLastName());
      assertEquals("jane@example.com", user.getEmail());
      assertEquals(UserStatus.ACTIVE, user.getStatus());
      assertEquals(UserRole.CUSTOMER, user.getRole());
      assertEquals(60, user.getHashedPass().length());
      assertTrue(PasswordUtil.verify(" secret123 ", user.getHashedPass()));
      assertFalse(PasswordUtil.verify("secret123", user.getHashedPass()));
      assertNotNull(em.find(CustomerUser.class, user.getId()));
      assertNull(em.find(SellerUser.class, user.getId()));
    });
  }

  @Test
  void createsSeller() {
    service.registerUser(data("SELLER"));
    transact(em -> {
      User user = em.createQuery("FROM User", User.class).getSingleResult();
      assertEquals(UserRole.SELLER, user.getRole());
      assertNotNull(em.find(SellerUser.class, user.getId()));
      assertNull(em.find(CustomerUser.class, user.getId()));
    });
  }

  @Test
  void rejectsDuplicateEmail() {
    service.registerUser(data("CUSTOMER"));
    assertThrows(EmailAlreadyRegisteredException.class, () -> service.registerUser(data("SELLER")));
    transact(em ->
        assertEquals(
            1L, em.createQuery("SELECT COUNT(u) FROM User u", Long.class).getSingleResult()));
  }

  @Test
  void validatesBeforeStartingTransaction() {
    AuthService validating =
        failingService(new IllegalStateException("Transaction must not start"));
    ValidationException error =
        assertThrows(
            ValidationException.class,
            () ->
                validating.registerUser(
                    new RegisterRequest(null, "x".repeat(101), "bad", "short", "ADMIN")));
    assertEquals(5, error.getErrors().size());
    assertThrows(
        ValidationException.class,
        () ->
            validating.registerUser(
                new RegisterRequest("Jane", "Cooper", "a@b.com", "é".repeat(37), "CUSTOMER")));
    assertThrows(
        ValidationException.class,
        () ->
            validating.registerUser(
                new RegisterRequest("Jane", "Cooper", "a@b.com", null, "CUSTOMER")));
    assertFalse(data("CUSTOMER").toString().contains("secret123"));
  }

  @Test
  void rejectsControlCharactersBeforePersistence() {
    AuthService validating =
        failingService(new IllegalStateException("Transaction must not start"));
    ValidationException error =
        assertThrows(
            ValidationException.class,
            () ->
                validating.registerUser(
                    new RegisterRequest(
                        "Ja\0ne", "Coo\0per", "ja\0ne@example.com", "secret123", "CUSTOMER")));
    assertEquals(java.util.Set.of("firstName", "lastName", "email"), error.getErrors().keySet());
  }

  @Test
  void rejectsOversizedNamesAndEmail() {
    AuthService validating =
        failingService(new IllegalStateException("Transaction must not start"));
    ValidationException error =
        assertThrows(
            ValidationException.class,
            () ->
                validating.registerUser(
                    new RegisterRequest(
                        "a".repeat(51),
                        "b".repeat(101),
                        "a".repeat(250) + "@example.com",
                        "secret123",
                        "CUSTOMER")));
    assertEquals(java.util.Set.of("firstName", "lastName", "email"), error.getErrors().keySet());
  }

  @Test
  void rollsBackUserWhenRoleInsertFails() {
    transact(em ->
        em.createNativeQuery(
                "ALTER TABLE seller_users ADD CONSTRAINT reject_seller CHECK (seller_id < 0)")
            .executeUpdate());
    assertThrows(RuntimeException.class, () -> service.registerUser(data("SELLER")));
    transact(em ->
        assertEquals(
            0L, em.createQuery("SELECT COUNT(u) FROM User u", Long.class).getSingleResult()));
  }

  @Test
  void concurrentRequestsCreateOnlyOneAccount() throws Exception {
    try (var pool = java.util.concurrent.Executors.newFixedThreadPool(2)) {
      var start = new java.util.concurrent.CyclicBarrier(2);
      java.util.concurrent.Callable<Boolean> attempt =
          () -> {
            start.await();
            try {
              service.registerUser(data("CUSTOMER"));
              return true;
            } catch (EmailAlreadyRegisteredException e) {
              return false;
            } catch (ConstraintViolationException e) {
              assertEquals("23505", e.getSQLState());
              return false;
            }
          };
      var first = pool.submit(attempt);
      var second = pool.submit(attempt);
      assertNotEquals(
          first.get(30, java.util.concurrent.TimeUnit.SECONDS),
          second.get(30, java.util.concurrent.TimeUnit.SECONDS));
    }
    transact(em -> {
      assertEquals(1L, em.createQuery("SELECT COUNT(u) FROM User u", Long.class).getSingleResult());
      assertEquals(
          1L, em.createQuery("SELECT COUNT(c) FROM CustomerUser c", Long.class).getSingleResult());
    });
  }

  @Test
  void translatesOnlyEmailConstraintIncludingCommitFailure() {
    ConstraintViolationException duplicate =
        new ConstraintViolationException(
            "duplicate", new SQLException("duplicate", "23505"), "users_email_key");
    AuthService failing = failingService(new RuntimeException(duplicate));
    assertSame(
        duplicate,
        assertThrows(
                EmailAlreadyRegisteredException.class, () -> failing.registerUser(data("CUSTOMER")))
            .getCause()
            .getCause());
    ConstraintViolationException unrelated =
        new ConstraintViolationException(
            "duplicate other column", new SQLException("duplicate", "23505"), "other_unique_key");
    AuthService unrelatedFailure = failingService(unrelated);
    assertSame(
        unrelated,
        assertThrows(
            ConstraintViolationException.class,
            () -> unrelatedFailure.registerUser(data("CUSTOMER"))));
    IllegalStateException unexpected = new IllegalStateException("unavailable");
    AuthService broken = failingService(unexpected);
    assertSame(
        unexpected,
        assertThrows(IllegalStateException.class, () -> broken.registerUser(data("CUSTOMER"))));
  }
}
