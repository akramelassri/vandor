package com.vandor.services;

import com.vandor.exceptions.AccessDeniedException;
import com.vandor.models.Category;
import com.vandor.models.Product;
import com.vandor.models.SellerUser;
import com.vandor.models.UserRole;
import com.vandor.repository.ProductRepository;
import com.vandor.repository.UserRepository;
import com.vandor.util.Permissions;
import com.vandor.util.TransactionManager;
import java.io.IOException;
import java.util.List;

public class SellerService {
  private static final System.Logger LOG = System.getLogger(SellerService.class.getName());
  private final TransactionManager db;
  private final ImageService images;

  public SellerService() {
    this(new TransactionManager());
  }

  public SellerService(TransactionManager db) {
    this(db, new ImageService());
  }

  public SellerService(TransactionManager db, ImageService images) {
    this.db = db;
    this.images = images;
  }

  public List<Product> listings(Long seller) {
    return db.execute(em -> new ProductRepository(em).forSeller(seller));
  }

  public Product listing(Long seller, Long product) {
    return db.execute(em -> {
      Product p = new ProductRepository(em).details(product);
      own(p, seller);
      return p;
    });
  }

  private void own(Product p, Long seller) {
    if (!p.getSeller().getSellerId().equals(seller)) {
      throw new AccessDeniedException();
    }
  }

  public void save(
      Long seller,
      Long id,
      String name,
      String description,
      Integer category,
      int price,
      int stock,
      String thumbnail) {
    String title = name == null ? "" : name.trim();
    String text = description == null ? "" : description.trim();
    if (title.length() < 3 || title.length() > 50) {
      throw new IllegalArgumentException("Title must contain 3 to 50 characters.");
    }
    if (text.length() < 10 || text.length() > 10000) {
      throw new IllegalArgumentException("Description must contain 10 to 10,000 characters.");
    }
    if (price <= 0 || stock < 0) {
      throw new IllegalArgumentException("Price must be positive and stock cannot be negative.");
    }
    String unusedPhoto =
        db.execute(em -> {
          Permissions.require(new UserRepository(em).lock(seller), UserRole.SELLER);
          Category cat = category == null ? null : em.find(Category.class, category);
          if (cat == null) {
            throw new IllegalArgumentException("Choose an existing category.");
          }
          Product p;
          if (id == null) {
            p = new Product();
            p.setSeller(em.getReference(SellerUser.class, seller));
          } else {
            p = new ProductRepository(em).lock(id);
            own(p, seller);
          }
          String previousPhoto = p.getThumbnail();
          p.setName(title);
          p.setDescription(text);
          p.setCategory(cat);
          p.setPrice(price);
          p.setStock(stock);
          if (thumbnail != null) {
            p.setThumbnail(thumbnail);
          }
          if (id == null) {
            em.persist(p);
          }
          if (thumbnail == null || thumbnail.equals(previousPhoto)) {
            return null;
          }
          return previousPhoto != null && !new ProductRepository(em).usesThumbnail(previousPhoto)
              ? previousPhoto
              : null;
        });
    discardUnusedPhoto(unusedPhoto);
  }

  public void delete(Long seller, Long product) {
    String unusedPhoto =
        db.execute(em -> {
          Permissions.require(new UserRepository(em).lock(seller), UserRole.SELLER);
          ProductRepository repo = new ProductRepository(em);
          Product p = repo.lock(product);
          own(p, seller);
          if (repo.ordered(product)) {
            throw new IllegalArgumentException(
                "This product appears in an order and must be kept. Set stock to zero to stop"
                    + " selling it.");
          }
          // Remove cart references explicitly as well as relying on PostgreSQL's ON DELETE CASCADE.
          em.createQuery("DELETE FROM CartItem WHERE product.id = :id")
              .setParameter("id", product)
              .executeUpdate();
          String previousPhoto = p.getThumbnail();
          em.remove(p);
          return repo.usesThumbnail(previousPhoto) ? null : previousPhoto;
        });
    discardUnusedPhoto(unusedPhoto);
  }

  private void discardUnusedPhoto(String filename) {
    if (filename == null) {
      return;
    }
    // The transaction has committed; a cleanup failure must not undo the successful response.
    try {
      images.discard(filename);
    } catch (IOException e) {
      LOG.log(System.Logger.Level.WARNING, "Could not remove unused product photo: " + filename, e);
    }
  }
}
