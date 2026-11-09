package com.vandor.repository;

import com.vandor.models.CartItem;
import com.vandor.models.CartItemId;
import jakarta.persistence.EntityManager;
import java.util.List;

public class CartItemRepository extends Repository<CartItem, CartItemId> {
  public CartItemRepository(EntityManager em) {
    super(em, CartItem.class);
  }

  public List<CartItem> forUser(Long user) {
    return em.createQuery(
            "SELECT c FROM CartItem c JOIN FETCH c.product p JOIN FETCH p.seller s JOIN FETCH"
                + " s.user LEFT JOIN FETCH p.category WHERE c.user.id = :user ORDER BY p.id",
            CartItem.class)
        .setParameter("user", user)
        .getResultList();
  }

  public void clear(Long user) {
    em.createQuery("DELETE FROM CartItem WHERE user.id = :user")
        .setParameter("user", user)
        .executeUpdate();
  }
}
