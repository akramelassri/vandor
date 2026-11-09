package com.vandor.services;

import com.vandor.models.Category;
import com.vandor.models.Product;
import com.vandor.models.UserStatus;
import com.vandor.repository.ProductRepository;
import com.vandor.util.TransactionManager;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;

public class CatalogService {
  private final TransactionManager db;

  public CatalogService() {
    this(new TransactionManager());
  }

  public CatalogService(TransactionManager db) {
    this.db = db;
  }

  public List<Product> search(
      String term, Integer category, Integer minimum, Integer maximum, boolean stock, String sort) {
    if ((minimum != null && minimum < 0)
        || (maximum != null && maximum < 0)
        || (minimum != null && maximum != null && minimum > maximum)) {
      throw new IllegalArgumentException("Enter a valid price range.");
    }
    return db.execute(
        em -> new ProductRepository(em).search(term, category, minimum, maximum, stock, sort));
  }

  public Product product(Long id) {
    return db.execute(em -> {
      Product p = new ProductRepository(em).details(id);
      if (p.getSeller().getUser().getStatus() != UserStatus.ACTIVE) {
        throw new EntityNotFoundException("Product unavailable.");
      }
      return p;
    });
  }

  public List<Category> categories() {
    return db.execute(
        em -> em.createQuery("FROM Category ORDER BY name", Category.class).getResultList());
  }
}
