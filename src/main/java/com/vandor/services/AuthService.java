package com.vandor.services;

import com.vandor.dto.RegisterRequest;
import com.vandor.exceptions.EmailAlreadyRegisteredException;
import com.vandor.exceptions.ValidationException;
import com.vandor.models.CustomerUser;
import com.vandor.models.SellerUser;
import com.vandor.models.User;
import com.vandor.models.UserRole;
import com.vandor.models.UserStatus;
import com.vandor.repository.CustomerUserRepository;
import com.vandor.repository.SellerUserRepository;
import com.vandor.repository.UserRepository;
import com.vandor.util.PasswordUtil;
import com.vandor.util.TransactionManager;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.hibernate.exception.ConstraintViolationException;

public class AuthService {
  private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
  private final TransactionManager db;

  public AuthService() {
    this(new TransactionManager());
  }

  public AuthService(TransactionManager db) {
    this.db = db;
  }

  public void registerUser(RegisterRequest data) {
    String firstName = clean(data.firstName());
    String lastName = clean(data.lastName());
    String email = clean(data.email()).toLowerCase(Locale.ROOT);
    String password = data.password();
    Map<String, String> errors = new LinkedHashMap<>();
    if (firstName.isEmpty() || firstName.length() > 50 || hasControlCharacters(firstName)) {
      errors.put(
          "firstName",
          "First name is required, must be at most 50 characters, and cannot contain control"
              + " characters.");
    }
    if (lastName.isEmpty() || lastName.length() > 100 || hasControlCharacters(lastName)) {
      errors.put(
          "lastName",
          "Last name is required, must be at most 100 characters, and cannot contain control"
              + " characters.");
    }
    if (email.length() > 255 || hasControlCharacters(email) || !EMAIL.matcher(email).matches()) {
      errors.put("email", "Enter a valid email address of at most 255 characters.");
    }
    if (password == null
        || password.codePointCount(0, password.length()) < 8
        || password.getBytes(StandardCharsets.UTF_8).length > 72
        || password.indexOf(0) >= 0) {
      errors.put(
          "password",
          "Password must contain at least 8 characters and at most 72 UTF-8 bytes, without null"
              + " characters.");
    }
    if (!"CUSTOMER".equals(data.role()) && !"SELLER".equals(data.role())) {
      errors.put("role", "Choose a customer or seller account.");
    }
    if (!errors.isEmpty()) {
      throw new ValidationException(errors);
    }
    String hash = PasswordUtil.hash(password);
    try {
      db.execute(em -> {
        UserRepository users = new UserRepository(em);
        if (users.existsByEmail(email)) {
          throw new EmailAlreadyRegisteredException();
        }
        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setHashedPass(hash);
        user.setRole(UserRole.valueOf(data.role()));
        user.setStatus(UserStatus.ACTIVE);
        users.create(user);
        if (user.getRole() == UserRole.SELLER) {
          new SellerUserRepository(em).create(new SellerUser(user));
        } else {
          new CustomerUserRepository(em).create(new CustomerUser(user));
        }
        return null;
      });
    } catch (RuntimeException e) {
      for (Throwable cause = e; cause != null; cause = cause.getCause()) {
        if (cause instanceof ConstraintViolationException violation
            && "23505".equals(violation.getSQLState())
            && "users_email_key".equals(violation.getConstraintName())) {
          throw new EmailAlreadyRegisteredException(e);
        }
      }
      throw e;
    }
  }

  private static boolean hasControlCharacters(String value) {
    return value.codePoints().anyMatch(Character::isISOControl);
  }

  private static String clean(String value) {
    return value == null ? "" : value.trim();
  }
}
