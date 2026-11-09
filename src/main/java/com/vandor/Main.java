package com.vandor;

import com.vandor.models.Category;
import com.vandor.models.CustomerUser;
import com.vandor.models.Product;
import com.vandor.models.SellerUser;
import com.vandor.models.User;
import com.vandor.models.UserRole;
import com.vandor.repository.UserRepository;
import com.vandor.util.JpaUtil;
import com.vandor.util.PasswordUtil;
import com.vandor.util.TransactionManager;
import jakarta.persistence.EntityManager;
import java.nio.charset.StandardCharsets;

/** Optional, repeatable development seed. Never invoked by application startup. */
public class Main {
  public static void main(String[] args) {
    if (args.length != 1 || !"seed".equals(args[0])) {
      throw new IllegalArgumentException("Usage: com.vandor.Main seed");
    }
    String password = System.getenv("SEED_PASSWORD");
    if (password == null
        || password.codePointCount(0, password.length()) < 8
        || password.getBytes(StandardCharsets.UTF_8).length > 72
        || password.indexOf(0) >= 0) {
      throw new IllegalArgumentException(
          "Set SEED_PASSWORD to an 8-character minimum password of at most 72 UTF-8 bytes.");
    }
    String hash = PasswordUtil.hash(password);
    try {
      new TransactionManager()
          .execute(em -> {
            account(em, "admin@vandor.test", "Alex", "Admin", UserRole.ADMIN, hash);
            account(em, "buyer@vandor.test", "Jane", "Cooper", UserRole.CUSTOMER, hash);
            User maker = account(em, "maker@vandor.test", "Mara", "Clay", UserRole.SELLER, hash);
            User maker2 = account(em, "maker2@vandor.test", "Theo", "Wood", UserRole.SELLER, hash);
            Category ceramics = category(em, "Ceramics", "Everyday objects, made from earth.");
            Category home = category(em, "Home & living", "Thoughtful pieces for your space.");
            product(
                em,
                maker,
                ceramics,
                "Stoneware breakfast cup",
                2400,
                8,
                "A hand-thrown stoneware cup with a warm, matte glaze.");
            product(
                em,
                maker,
                ceramics,
                "Small bud vase",
                3200,
                3,
                "A simple ceramic vase for a stem or two from the garden.");
            product(
                em,
                maker2,
                home,
                "Oak serving board",
                4800,
                6,
                "Solid oak, carefully shaped and finished with food-safe oil.");
            return null;
          });
      System.out.println(
          "Demo accounts, categories and listings are ready. Existing records were kept.");
    } finally {
      JpaUtil.closeFactory();
    }
  }

  private static User account(
      EntityManager em, String email, String first, String last, UserRole role, String hash) {
    return new UserRepository(em)
        .findByEmail(email)
        .orElseGet(() -> {
          User u = new User();
          u.setEmail(email);
          u.setFirstName(first);
          u.setLastName(last);
          u.setRole(role);
          u.setHashedPass(hash);
          em.persist(u);
          if (role == UserRole.SELLER) {
            em.persist(new SellerUser(u));
          }
          if (role == UserRole.CUSTOMER) {
            em.persist(new CustomerUser(u));
          }
          return u;
        });
  }

  private static Category category(EntityManager em, String name, String description) {
    return em.createQuery("FROM Category WHERE name = :name", Category.class)
        .setParameter("name", name)
        .getResultStream()
        .findFirst()
        .orElseGet(() -> {
          Category c = new Category();
          c.setName(name);
          c.setDescription(description);
          em.persist(c);
          return c;
        });
  }

  private static void product(
      EntityManager em,
      User seller,
      Category category,
      String name,
      int price,
      int stock,
      String description) {
    if (seller.getRole() != UserRole.SELLER || em.find(SellerUser.class, seller.getId()) == null) {
      return;
    }
    if (em.createQuery(
                "SELECT count(p) FROM Product p WHERE p.seller.sellerId = :seller AND p.name ="
                    + " :name",
                Long.class)
            .setParameter("seller", seller.getId())
            .setParameter("name", name)
            .getSingleResult()
        > 0) {
      return;
    }
    Product p = new Product();
    p.setSeller(em.getReference(SellerUser.class, seller.getId()));
    p.setCategory(category);
    p.setName(name);
    p.setPrice(price);
    p.setStock(stock);
    p.setDescription(description);
    em.persist(p);
  }
}
