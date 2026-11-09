package com.vandor.services;

import com.vandor.models.Session;
import com.vandor.models.User;
import com.vandor.models.UserStatus;
import com.vandor.repository.SessionRepository;
import com.vandor.repository.UserRepository;
import com.vandor.util.PasswordUtil;
import com.vandor.util.TransactionManager;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

public class SessionService {
  public static final int LIFETIME_SECONDS = 86400;
  private final TransactionManager db;

  public SessionService() {
    this(new TransactionManager());
  }

  public SessionService(TransactionManager db) {
    this.db = db;
  }

  public Session login(String email, String password) {
    String normalized = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    return db.execute(em -> {
      User user = new UserRepository(em).findByEmail(normalized).orElse(null);
      if (user == null || password == null || !validPassword(password, user.getHashedPass())) {
        throw new IllegalArgumentException("Email and password do not match our records.");
      }
      user = new UserRepository(em).lock(user.getId());
      SessionRepository sessions = new SessionRepository(em);
      sessions.expire(Instant.now());
      Session session = new Session();
      session.setUser(user);
      session.setCreatedAt(Instant.now());
      session.setExpiresAt(session.getCreatedAt().plusSeconds(LIFETIME_SECONDS));
      return sessions.create(session);
    });
  }

  private boolean validPassword(String password, String hash) {
    if (hash == null
        || password.getBytes(StandardCharsets.UTF_8).length > 72
        || password.indexOf(0) >= 0) {
      return false;
    }
    try {
      return PasswordUtil.verify(password, hash);
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  public User resolve(String token) {
    UUID id = parse(token);
    if (id == null) {
      return null;
    }
    return db.execute(em ->
        new SessionRepository(em)
            .active(id, Instant.now())
            .map(Session::getUser)
            .filter(u -> u.getStatus() == UserStatus.ACTIVE)
            .orElse(null));
  }

  public void logout(String token) {
    UUID id = parse(token);
    if (id != null) {
      db.execute(em -> {
        new SessionRepository(em).revoke(id);
        return null;
      });
    }
  }

  private UUID parse(String token) {
    try {
      return UUID.fromString(token);
    } catch (RuntimeException e) {
      return null;
    }
  }
}
