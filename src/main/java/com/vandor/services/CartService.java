package com.vandor.services;

import com.vandor.dto.CartLine;
import com.vandor.models.CartItem;
import com.vandor.models.CartItemId;
import com.vandor.models.Product;
import com.vandor.models.User;
import com.vandor.models.UserRole;
import com.vandor.models.UserStatus;
import com.vandor.repository.CartItemRepository;
import com.vandor.repository.ProductRepository;
import com.vandor.repository.UserRepository;
import com.vandor.util.Permissions;
import com.vandor.util.TransactionManager;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CartService {
  private final TransactionManager db;

  public CartService() {
    this(new TransactionManager());
  }

  public CartService(TransactionManager db) {
    this.db = db;
  }

  public List<CartLine> lines(Long user, Map<Long, Integer> guest) {
    return db.execute(em -> {
      if (user != null) {
        return new CartItemRepository(em)
            .forUser(user).stream()
                .map(c -> new CartLine(c.getProduct(), c.getQuantity()))
                .toList();
      }
      List<CartLine> result = new ArrayList<>();
      for (var entry : guest.entrySet()) {
        if (em.find(Product.class, entry.getKey()) != null) {
          result.add(
              new CartLine(new ProductRepository(em).details(entry.getKey()), entry.getValue()));
        }
      }
      return result;
    });
  }

  public void update(Long user, Map<Long, Integer> guest, Long product, int quantity, boolean add) {
    if (quantity < 0) {
      throw new IllegalArgumentException("Quantity cannot be negative.");
    }
    db.execute(em -> {
      User buyer = user == null ? null : new UserRepository(em).lock(user);
      if (buyer != null) {
        Permissions.require(buyer, UserRole.CUSTOMER);
      }
      Product p = new ProductRepository(em).details(product);
      if (quantity > 0 && p.getSeller().getUser().getStatus() != UserStatus.ACTIVE) {
        throw new IllegalArgumentException("Product unavailable.");
      }
      CartItem old = user == null ? null : em.find(CartItem.class, new CartItemId(user, product));
      int previous =
          user == null ? guest.getOrDefault(product, 0) : old == null ? 0 : old.getQuantity();
      int wanted = (int) Math.min(Integer.MAX_VALUE, (long) quantity + (add ? previous : 0));
      int value = Math.min(wanted, p.getStock());
      if (user == null) {
        if (value == 0) {
          guest.remove(product);
        } else {
          guest.put(product, value);
        }
      } else {
        save(em, buyer, p, old, value);
      }
      return null;
    });
  }

  private void save(EntityManager em, User buyer, Product p, CartItem old, int quantity) {
    if (quantity <= 0) {
      if (old != null) {
        em.remove(old);
      }
      return;
    }
    if (old != null) {
      old.setQuantity(quantity);
      return;
    }
    CartItem item = new CartItem();
    item.setId(new CartItemId(buyer.getId(), p.getId()));
    item.setUser(buyer);
    item.setProduct(p);
    item.setQuantity(quantity);
    em.persist(item);
  }

  public void merge(Long user, Map<Long, Integer> guest) {
    db.execute(em -> {
      User buyer = new UserRepository(em).lock(user);
      Permissions.require(buyer, UserRole.CUSTOMER);
      for (var entry : guest.entrySet()) {
        Product p = em.find(Product.class, entry.getKey());
        if (p == null || p.getSeller().getUser().getStatus() != UserStatus.ACTIVE) {
          continue;
        }
        CartItem old = em.find(CartItem.class, new CartItemId(user, p.getId()));
        int total =
            (int)
                Math.min(
                    p.getStock(), (long) entry.getValue() + (old == null ? 0 : old.getQuantity()));
        save(em, buyer, p, old, total);
      }
      return null;
    });
  }

  public void adjust(Long user, Map<Long, Integer> guest) {
    if (user == null) {
      List<CartLine> available = lines(null, guest);
      guest.clear();
      for (CartLine line : available) {
        Product p = line.getProduct();
        int q = Math.min(line.getQuantity(), p.getStock());
        if (q > 0 && p.getSeller().getUser().getStatus() == UserStatus.ACTIVE) {
          guest.put(p.getId(), q);
        }
      }
      return;
    }
    db.execute(em -> {
      Permissions.require(new UserRepository(em).lock(user), UserRole.CUSTOMER);
      for (CartItem item : new CartItemRepository(em).forUser(user)) {
        int quantity =
            item.getProduct().getSeller().getUser().getStatus() == UserStatus.ACTIVE
                ? Math.min(item.getQuantity(), item.getProduct().getStock())
                : 0;
        if (quantity == 0) {
          em.remove(item);
        } else {
          item.setQuantity(quantity);
        }
      }
      return null;
    });
  }
}
