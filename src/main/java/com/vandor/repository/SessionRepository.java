package com.vandor.repository;

import com.vandor.models.Session;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public class SessionRepository extends Repository<Session, UUID> {
  public SessionRepository(EntityManager em) {
    super(em, Session.class);
  }

  public Optional<Session> active(UUID id, Instant now) {
    return em.createQuery(
            "SELECT s FROM Session s JOIN FETCH s.user WHERE s.id = :id AND s.expiresAt > :now",
            Session.class)
        .setParameter("id", id)
        .setParameter("now", now)
        .getResultStream()
        .findFirst();
  }

  public void revoke(UUID id) {
    em.createQuery("DELETE FROM Session WHERE id = :id").setParameter("id", id).executeUpdate();
  }

  public void revokeUser(Long user) {
    em.createQuery("DELETE FROM Session WHERE user.id = :id")
        .setParameter("id", user)
        .executeUpdate();
  }

  public void expire(Instant now) {
    em.createQuery("DELETE FROM Session WHERE expiresAt <= :now")
        .setParameter("now", now)
        .executeUpdate();
  }
}
