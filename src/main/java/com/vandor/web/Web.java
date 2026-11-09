package com.vandor.web;

import com.vandor.models.OrderStatus;
import com.vandor.models.User;
import com.vandor.models.UserStatus;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public final class Web {
  private Web() {}

  public static User user(HttpServletRequest request) {
    return (User) request.getAttribute("currentUser");
  }

  public static long id(HttpServletRequest request, String name) {
    try {
      long id = Long.parseLong(request.getParameter(name));
      if (id <= 0) {
        throw new NumberFormatException();
      }
      return id;
    } catch (RuntimeException e) {
      throw new IllegalArgumentException("Invalid " + name + ".");
    }
  }

  public static OrderStatus orderStatus(HttpServletRequest request) {
    try {
      return OrderStatus.valueOf(request.getParameter("status"));
    } catch (RuntimeException e) {
      throw new IllegalArgumentException("Choose a valid order status.");
    }
  }

  public static UserStatus userStatus(HttpServletRequest request) {
    try {
      return UserStatus.valueOf(request.getParameter("status"));
    } catch (RuntimeException e) {
      throw new IllegalArgumentException("Choose a valid account status.");
    }
  }

  public static Integer integer(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return Integer.valueOf(value);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Enter a valid whole number.");
    }
  }

  @SuppressWarnings("unchecked")
  public static Map<Long, Integer> guest(HttpServletRequest request) {
    HttpSession session = request.getSession();
    synchronized (session) {
      Map<Long, Integer> cart = (Map<Long, Integer>) session.getAttribute("guestCart");
      if (cart == null) {
        cart = new LinkedHashMap<>();
        session.setAttribute("guestCart", cart);
      }
      return cart;
    }
  }

  public static void render(HttpServletRequest request, HttpServletResponse response, String page)
      throws ServletException, IOException {
    request.getRequestDispatcher("/WEB-INF/" + page + ".jsp").forward(request, response);
  }

  public static void redirect(
      HttpServletRequest request, HttpServletResponse response, String path, String message)
      throws IOException {
    if (message != null) {
      request.getSession().setAttribute("flash", message);
    }
    response.sendRedirect(request.getContextPath() + path);
  }

  public static String home(User user) {
    return switch (user.getRole()) {
      case ADMIN -> "/admin";
      case SELLER -> "/studio";
      default -> "/shop";
    };
  }
}
