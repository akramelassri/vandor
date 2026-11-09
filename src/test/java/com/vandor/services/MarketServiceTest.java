package com.vandor.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vandor.exceptions.AccessDeniedException;
import com.vandor.models.CartItem;
import com.vandor.models.Category;
import com.vandor.models.CustomerUser;
import com.vandor.models.Order;
import com.vandor.models.OrderItem;
import com.vandor.models.OrderItemId;
import com.vandor.models.OrderStatus;
import com.vandor.models.Product;
import com.vandor.models.SellerUser;
import com.vandor.models.Session;
import com.vandor.models.User;
import com.vandor.models.UserRole;
import com.vandor.models.UserStatus;
import com.vandor.util.PasswordUtil;
import com.vandor.util.TransactionManager;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.h2.tools.RunScript;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MarketServiceTest {
  static EntityManagerFactory factory;
  static TransactionManager db;
  CartService carts;
  OrderService orders;
  SellerService sellers;
  AdminService admins;
  SessionService sessions;
  long customer, buyer2, seller, seller2, admin, product, otherProduct;
  int category;

  @BeforeAll
  static void database() throws Exception {
    String url = "jdbc:h2:mem:market;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000";
    try (Connection c = DriverManager.getConnection(url)) {
      // H2 needs this PostgreSQL type alias; the repository DDL itself is executed unchanged.
      c.createStatement()
          .execute("CREATE DOMAIN IF NOT EXISTS TIMESTAMPTZ AS TIMESTAMP WITH TIME ZONE");
      RunScript.execute(c, Files.newBufferedReader(Path.of("db/init.sql")));
    }
    Configuration cfg =
        new Configuration()
            .setProperty("hibernate.connection.driver_class", "org.h2.Driver")
            .setProperty("hibernate.connection.url", url)
            .setProperty("hibernate.hbm2ddl.auto", "validate");
    for (Class<?> type :
        List.of(
            User.class,
            CustomerUser.class,
            SellerUser.class,
            Session.class,
            Category.class,
            Product.class,
            CartItem.class,
            Order.class,
            OrderItem.class)) {
      cfg.addAnnotatedClass(type);
    }
    factory = cfg.buildSessionFactory();
    db = new TransactionManager(factory);
  }

  @AfterAll
  static void close() {
    if (factory != null) {
      factory.close();
    }
  }

  @BeforeEach
  void setup() {
    carts = new CartService(db);
    orders = new OrderService(db);
    sellers = new SellerService(db);
    admins = new AdminService(db);
    sessions = new SessionService(db);
    db.execute(em -> {
      for (String table :
          List.of(
              "order_items",
              "cart_items",
              "orders",
              "products",
              "categories",
              "sessions",
              "seller_users",
              "customer_users",
              "users")) {
        em.createNativeQuery("DELETE FROM " + table).executeUpdate();
      }
      customer = user(em, "buyer@example.com", UserRole.CUSTOMER);
      buyer2 = user(em, "buyer2@example.com", UserRole.CUSTOMER);
      seller = user(em, "maker@example.com", UserRole.SELLER);
      seller2 = user(em, "maker2@example.com", UserRole.SELLER);
      admin = user(em, "admin@example.com", UserRole.ADMIN);
      Category c = new Category();
      c.setName("Ceramics");
      em.persist(c);
      category = c.getId();
      product = product(em, seller, "Handmade cup", 5);
      otherProduct = product(em, seller2, "Small vase", 3);
      return null;
    });
  }

  static String hash = PasswordUtil.hash("secret123");

  long user(EntityManager em, String email, UserRole role) {
    User u = new User();
    u.setFirstName("Jane");
    u.setLastName("Maker");
    u.setEmail(email);
    u.setHashedPass(hash);
    u.setRole(role);
    em.persist(u);
    if (role == UserRole.CUSTOMER) {
      em.persist(new CustomerUser(u));
    }
    if (role == UserRole.SELLER) {
      em.persist(new SellerUser(u));
    }
    return u.getId();
  }

  long product(EntityManager em, long owner, String name, int stock) {
    Product p = new Product();
    p.setName(name);
    p.setDescription("Thoughtfully made by hand.");
    p.setPrice(1234);
    p.setStock(stock);
    p.setCategory(em.getReference(Category.class, category));
    p.setSeller(em.getReference(SellerUser.class, owner));
    em.persist(p);
    return p.getId();
  }

  User actor(long id) {
    return db.execute(em -> em.find(User.class, id));
  }

  int stock(long id) {
    return db.execute(em -> em.find(Product.class, id).getStock());
  }

  void add(long user, long pid, int quantity) {
    carts.update(user, Map.of(), pid, quantity, true);
  }

  @Test
  void checkoutSplitsBySellerAndKeepsSchemaMappingsValid() {
    add(customer, product, 2);
    add(customer, otherProduct, 1);
    var ids = orders.checkout(customer);
    assertEquals(2, ids.size());
    assertEquals(3, stock(product));
    assertEquals(2, stock(otherProduct));
    assertTrue(carts.lines(customer, Map.of()).isEmpty());
    var list = orders.list(actor(customer));
    assertEquals(2, list.size());
    assertTrue(
        list.stream().allMatch(o -> !o.isMixedSeller() && o.getStatus() == OrderStatus.PENDING));
    assertEquals(1, orders.list(actor(seller)).size());
    assertEquals(
        "€24.68",
        list.stream()
            .filter(o -> o.getItems().getFirst().getProduct().getId().equals(product))
            .findFirst()
            .orElseThrow()
            .getFormattedTotal());
    assertThrows(IllegalArgumentException.class, () -> orders.checkout(customer));
  }

  @Test
  void changedStockRollsBackEntireCheckoutAndAdjustIsExplicit() {
    add(customer, product, 2);
    add(customer, otherProduct, 2);
    db.execute(em -> {
      em.find(Product.class, otherProduct).setStock(1);
      return null;
    });
    assertThrows(IllegalArgumentException.class, () -> orders.checkout(customer));
    assertEquals(5, stock(product));
    assertTrue(orders.list(actor(customer)).isEmpty());
    assertEquals(2, carts.lines(customer, Map.of()).size());
    assertTrue(
        carts.lines(customer, Map.of()).stream().anyMatch(com.vandor.dto.CartLine::isChanged));
    carts.adjust(customer, Map.of());
    assertEquals(2, orders.checkout(customer).size());
  }

  @Test
  void cancellationRestoresStockOnlyOnceAndChecksOwnership() {
    add(customer, product, 2);
    long order = orders.checkout(customer).getFirst();
    assertThrows(
        AccessDeniedException.class, () -> orders.change(buyer2, order, OrderStatus.CANCELLED));
    assertThrows(
        AccessDeniedException.class, () -> orders.change(seller2, order, OrderStatus.CONFIRMED));
    orders.change(customer, order, OrderStatus.CANCELLED);
    assertEquals(5, stock(product));
    assertThrows(
        IllegalArgumentException.class,
        () -> orders.change(customer, order, OrderStatus.CANCELLED));
    assertEquals(5, stock(product));
  }

  @Test
  void fulfillmentEnforcesTransitionsAndCustomerCancelBoundary() {
    add(customer, product, 1);
    long order = orders.checkout(customer).getFirst();
    assertThrows(
        IllegalArgumentException.class, () -> orders.change(seller, order, OrderStatus.COMPLETED));
    orders.change(seller, order, OrderStatus.CONFIRMED);
    assertThrows(
        IllegalArgumentException.class,
        () -> orders.change(customer, order, OrderStatus.CANCELLED));
    orders.change(seller, order, OrderStatus.COMPLETED);
    assertThrows(
        IllegalArgumentException.class, () -> orders.change(seller, order, OrderStatus.CANCELLED));
    assertEquals(4, stock(product));
  }

  @Test
  void twoBuyersCannotPurchaseTheLastUnit() throws Exception {
    db.execute(em -> {
      em.find(Product.class, product).setStock(1);
      return null;
    });
    add(customer, product, 1);
    add(buyer2, product, 1);
    var barrier = new CyclicBarrier(2);
    try (var pool = Executors.newFixedThreadPool(2)) {
      var results = new ArrayList<Future<Boolean>>();
      for (long buyer : List.of(customer, buyer2)) {
        results.add(
            pool.submit(() -> {
              barrier.await(10, TimeUnit.SECONDS);
              try {
                orders.checkout(buyer);
                return true;
              } catch (IllegalArgumentException e) {
                return false;
              }
            }));
      }
      int success = 0;
      for (var result : results) {
        if (result.get(20, TimeUnit.SECONDS)) {
          success++;
        }
      }
      assertEquals(1, success);
      assertEquals(0, stock(product));
    }
  }

  @Test
  void guestCartMergesCapsQuantitiesAndDropsRemovedProducts() {
    Map<Long, Integer> guest = new LinkedHashMap<>();
    carts.update(null, guest, product, 50, true);
    assertEquals(5, guest.get(product));
    add(customer, product, 2);
    carts.merge(customer, guest);
    assertEquals(5, carts.lines(customer, Map.of()).getFirst().getQuantity());
    sellers.delete(seller2, otherProduct);
    guest.put(otherProduct, 1);
    carts.adjust(null, guest);
    assertFalse(guest.containsKey(otherProduct));
  }

  @Test
  void loginExpiryLogoutAndSuspensionRevokeAccess() {
    assertThrows(IllegalArgumentException.class, () -> sessions.login("buyer@example.com", "bad"));
    Session session = sessions.login(" BUYER@EXAMPLE.COM ", "secret123");
    assertEquals(customer, sessions.resolve(session.getId().toString()).getId());
    assertNull(sessions.resolve("invalid"));
    sessions.logout(session.getId().toString());
    assertNull(sessions.resolve(session.getId().toString()));
    Session expired = sessions.login("buyer@example.com", "secret123");
    db.execute(em -> {
      Session s = em.find(Session.class, expired.getId());
      s.setCreatedAt(Instant.now().minusSeconds(120));
      s.setExpiresAt(Instant.now().minusSeconds(1));
      return null;
    });
    assertNull(sessions.resolve(expired.getId().toString()));
    Session active = sessions.login("buyer@example.com", "secret123");
    admins.status(admin, customer, UserStatus.BLOCKED);
    assertNull(sessions.resolve(active.getId().toString()));
    assertThrows(
        IllegalArgumentException.class, () -> sessions.login("buyer@example.com", "secret123"));
    admins.status(admin, customer, UserStatus.ACTIVE);
    assertNotNull(sessions.login("buyer@example.com", "secret123"));
    assertThrows(
        IllegalArgumentException.class, () -> admins.status(admin, admin, UserStatus.BLOCKED));
  }

  @Test
  void listingValidationOwnershipAndReferencedDeletionAreEnforced() {
    assertThrows(
        AccessDeniedException.class,
        () ->
            sellers.save(
                seller2, product, "New cup", "A lovely handmade cup", category, 500, 1, null));
    assertThrows(
        IllegalArgumentException.class,
        () -> sellers.save(seller, null, "x", "short", category, 0, -1, null));
    add(customer, product, 1);
    orders.checkout(customer);
    assertThrows(IllegalArgumentException.class, () -> sellers.delete(seller, product));
    assertThrows(IllegalArgumentException.class, () -> admins.deleteCategory(admin, category));
    sellers.save(
        seller, product, "New cup title", "A lovely handmade cup", category, 1350, 0, null);
    assertEquals(
        "New cup title",
        orders.list(actor(customer)).getFirst().getItems().getFirst().getProduct().getName());
  }

  @Test
  void catalogFindsProductsByCategoryMakerAndPrice() {
    CatalogService catalog = new CatalogService(db);
    assertEquals(2, catalog.search("ceramics", category, 1000, 2000, true, "price-asc").size());
    assertEquals(2, catalog.search("Jane Maker", null, null, null, false, "name").size());
    assertTrue(catalog.search("", null, 1500, null, false, null).isEmpty());
    admins.status(admin, seller, UserStatus.BLOCKED);
    assertEquals(1, catalog.search("", null, null, null, false, null).size());
    assertThrows(EntityNotFoundException.class, () -> catalog.product(product));
  }

  @Test
  void mixedLegacyOrdersRequireAdminForFulfillment() {
    db.execute(em -> {
      Order order = new Order();
      order.setCustomer(em.getReference(CustomerUser.class, customer));
      em.persist(order);
      for (long pid : List.of(product, otherProduct)) {
        OrderItem line = new OrderItem();
        OrderItemId id = new OrderItemId();
        id.setOrderId(order.getId());
        id.setProductId(pid);
        line.setId(id);
        line.setOrder(order);
        line.setProduct(em.getReference(Product.class, pid));
        line.setQuantity(1);
        em.persist(line);
      }
      return null;
    });
    var order = orders.list(actor(seller)).getFirst();
    assertTrue(order.isMixedSeller());
    assertThrows(
        AccessDeniedException.class,
        () -> orders.change(seller, order.getId(), OrderStatus.CONFIRMED));
    orders.change(admin, order.getId(), OrderStatus.CONFIRMED);
  }

  @Test
  void categoryCreationRenameAndDeletionRespectPermissions() {
    assertThrows(
        AccessDeniedException.class,
        () -> admins.saveCategory(customer, null, "Textiles", "Woven by hand"));
    admins.saveCategory(admin, null, "Textiles", "Woven by hand");
    CatalogService catalog = new CatalogService(db);
    var category =
        catalog.categories().stream()
            .filter(c -> c.getName().equals("Textiles"))
            .findFirst()
            .orElseThrow();
    admins.saveCategory(admin, category.getId(), "Linen", "Everyday linen");
    assertTrue(catalog.categories().stream().anyMatch(c -> c.getName().equals("Linen")));
    admins.deleteCategory(admin, category.getId());
    assertTrue(catalog.categories().stream().noneMatch(c -> c.getId().equals(category.getId())));
  }
}
