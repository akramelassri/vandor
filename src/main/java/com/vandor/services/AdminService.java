package com.vandor.services;

import com.vandor.models.Category;
import com.vandor.models.Product;
import com.vandor.models.User;
import com.vandor.models.UserRole;
import com.vandor.models.UserStatus;
import com.vandor.repository.SessionRepository;
import com.vandor.util.Permissions;
import com.vandor.util.TransactionManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.LockModeType;
import java.util.List;

public class AdminService {
  private final TransactionManager db;

  public AdminService() {
    this(new TransactionManager());
  }

  public AdminService(TransactionManager db) {
    this.db = db;
  }

  public List<User> users(Long actor) {
    return db.execute(em -> {
      Permissions.require(em.find(User.class, actor), UserRole.ADMIN);
      return em.createQuery("FROM User ORDER BY id", User.class).getResultList();
    });
  }

  public List<Product> products(Long actor) {
    return db.execute(em -> {
      Permissions.require(em.find(User.class, actor), UserRole.ADMIN);
      return em.createQuery(
              "SELECT p FROM Product p JOIN FETCH p.seller s JOIN FETCH s.user LEFT JOIN FETCH"
                  + " p.category ORDER BY p.id DESC",
              Product.class)
          .getResultList();
    });
  }

  public void status(Long actor, Long target, UserStatus status) {
    if (actor.equals(target)) {
      throw new IllegalArgumentException("You cannot change your own account status.");
    }
    db.execute(em -> {
      Permissions.require(em.find(User.class, actor), UserRole.ADMIN);
      User user = em.find(User.class, target, LockModeType.PESSIMISTIC_WRITE);
      if (user == null) {
        throw new EntityNotFoundException("User not found.");
      }
      user.setStatus(status);
      if (status == UserStatus.BLOCKED) {
        new SessionRepository(em).revokeUser(target);
      }
      return null;
    });
  }

  public void saveCategory(Long actor, Integer id, String name, String description) {
    String title = name == null ? "" : name.trim();
    if (title.isEmpty() || title.length() > 50) {
      throw new IllegalArgumentException("Category name must contain 1 to 50 characters.");
    }
    db.execute(em -> {
      Permissions.require(em.find(User.class, actor), UserRole.ADMIN);
      Category category =
          id == null ? new Category() : em.find(Category.class, id, LockModeType.PESSIMISTIC_WRITE);
      if (category == null) {
        throw new EntityNotFoundException("Category not found.");
      }
      category.setName(title);
      category.setDescription(description == null ? "" : description.trim());
      if (id == null) {
        em.persist(category);
      }
      return null;
    });
  }

  public void deleteCategory(Long actor, Integer id) {
    if (id == null) {
      throw new IllegalArgumentException("Choose a category.");
    }
    db.execute(em -> {
      Permissions.require(em.find(User.class, actor), UserRole.ADMIN);
      Category category = em.find(Category.class, id, LockModeType.PESSIMISTIC_WRITE);
      if (category == null) {
        throw new EntityNotFoundException("Category not found.");
      }
      long products =
          em.createQuery("SELECT count(p) FROM Product p WHERE p.category.id = :id", Long.class)
              .setParameter("id", id)
              .getSingleResult();
      if (products > 0) {
        throw new IllegalArgumentException(
            "This category contains products and cannot be removed.");
      }
      em.remove(category);
      return null;
    });
  }
}
