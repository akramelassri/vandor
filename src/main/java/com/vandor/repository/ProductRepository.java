package com.vandor.repository;

import com.vandor.models.Product;
import com.vandor.models.UserStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Locale;

public class ProductRepository extends Repository<Product, Long> {
  private static final String DETAILS =
      "SELECT p FROM Product p JOIN FETCH p.seller s JOIN FETCH s.user LEFT JOIN FETCH p.category";

  public ProductRepository(EntityManager em) {
    super(em, Product.class);
  }

  public List<Product> search(
      String term,
      Integer category,
      Integer minimum,
      Integer maximum,
      boolean inStock,
      String sort) {
    String order =
        switch (sort == null ? "" : sort) {
          case "price-asc" -> "p.price ASC, p.id DESC";
          case "price-desc" -> "p.price DESC, p.id DESC";
          case "name" -> "lower(p.name), p.id DESC";
          default -> "p.id DESC";
        };
    var query =
        em.createQuery(
            DETAILS
                + " WHERE s.user.status = :active AND (:category IS NULL OR p.category.id ="
                + " :category) AND (:minimum IS NULL OR p.price >= :minimum) AND (:maximum IS NULL"
                + " OR p.price <= :maximum)"
                + (inStock ? " AND p.stock > 0" : "")
                + " AND (lower(p.name) LIKE :term OR lower(coalesce(p.description,'')) LIKE :term"
                + " OR lower(s.user.firstName) LIKE :term OR lower(s.user.lastName) LIKE :term"
                + " OR lower(concat(s.user.firstName, concat(' ', s.user.lastName))) LIKE :term"
                + " OR lower(coalesce(p.category.name,'')) LIKE :term) ORDER BY "
                + order,
            Product.class);
    return query
        .setParameter("active", UserStatus.ACTIVE)
        .setParameter("category", category)
        .setParameter("minimum", minimum)
        .setParameter("maximum", maximum)
        .setParameter(
            "term", "%" + (term == null ? "" : term.trim().toLowerCase(Locale.ROOT)) + "%")
        .getResultList();
  }

  public Product details(Long id) {
    return em.createQuery(DETAILS + " WHERE p.id = :id", Product.class)
        .setParameter("id", id)
        .getResultStream()
        .findFirst()
        .orElseThrow(() -> new EntityNotFoundException("Product not found."));
  }

  public Product lock(Long id) {
    Product p = em.find(Product.class, id, LockModeType.PESSIMISTIC_WRITE);
    if (p == null) {
      throw new EntityNotFoundException("Product not found.");
    }
    em.refresh(p, LockModeType.PESSIMISTIC_WRITE);
    return p;
  }

  public List<Product> forSeller(Long seller) {
    return em.createQuery(DETAILS + " WHERE s.sellerId = :seller ORDER BY p.id DESC", Product.class)
        .setParameter("seller", seller)
        .getResultList();
  }

  public boolean ordered(Long id) {
    return em.createQuery("SELECT count(i) FROM OrderItem i WHERE i.product.id = :id", Long.class)
            .setParameter("id", id)
            .getSingleResult()
        > 0;
  }
}
