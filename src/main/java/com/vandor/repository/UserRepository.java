package com.vandor.repository;

import com.vandor.models.User;
import com.vandor.models.UserStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public class UserRepository extends Repository<User, Long> {

  public UserRepository(EntityManager em) {
    super(em, User.class);
  }

  public Optional<User> findByEmail(String email) {
    return em.createQuery("FROM User WHERE lower(email) = :email", User.class)
        .setParameter("email", email)
        .getResultStream()
        .findFirst();
  }

  public User lock(Long id) {
    User user = em.find(User.class, id, LockModeType.PESSIMISTIC_WRITE);
    if (user != null) {
      em.refresh(user, LockModeType.PESSIMISTIC_WRITE);
    }
    if (user == null || user.getStatus() != UserStatus.ACTIVE) {
      throw new IllegalArgumentException("Account is unavailable.");
    }
    return user;
  }

  public boolean existsByEmail(String email) {
    return em.createQuery("SELECT COUNT(u) FROM User u WHERE lower(u.email) = :email", Long.class)
            .setParameter("email", email)
            .getSingleResult()
        > 0;
  }
}
