package com.vandor.web;

import com.vandor.exceptions.AccessDeniedException;
import com.vandor.models.User;
import com.vandor.models.UserRole;
import com.vandor.services.SessionService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

@WebFilter("/*")
public class SecurityFilter implements Filter {
  public static final String COOKIE = "vandor_session";
  private final SessionService sessions;

  public SecurityFilter() {
    this(new SessionService());
  }

  public SecurityFilter(SessionService sessions) {
    this.sessions = sessions;
  }

  public static String token(HttpServletRequest request) {
    if (request.getCookies() != null) {
      for (Cookie cookie : request.getCookies()) {
        if (COOKIE.equals(cookie.getName())) {
          return cookie.getValue();
        }
      }
    }
    return null;
  }

  public static void cookie(
      HttpServletRequest request, HttpServletResponse response, String value, int age) {
    Cookie cookie = new Cookie(COOKIE, value);
    cookie.setHttpOnly(true);
    cookie.setSecure(request.isSecure());
    cookie.setAttribute("SameSite", "Lax");
    cookie.setPath(request.getContextPath().isEmpty() ? "/" : request.getContextPath());
    cookie.setMaxAge(age);
    response.addCookie(cookie);
  }

  @Override
  public void doFilter(
      ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
      throws IOException, ServletException {
    HttpServletRequest request = (HttpServletRequest) servletRequest;
    HttpServletResponse response = (HttpServletResponse) servletResponse;
    request.setCharacterEncoding("UTF-8");
    response.setCharacterEncoding("UTF-8");
    response.setHeader("X-Content-Type-Options", "nosniff");
    String path = request.getServletPath();
    if (path.startsWith("/assets/") || path.equals("/images") || path.startsWith("/images/")) {
      chain.doFilter(request, response);
      return;
    }
    response.setHeader("Cache-Control", "no-store");
    try {
      User user = sessions.resolve(token(request));
      request.setAttribute("currentUser", user);
      HttpSession session = request.getSession();
      synchronized (session) {
        if (session.getAttribute("csrf") == null) {
          session.setAttribute("csrf", UUID.randomUUID().toString());
        }
        Object flash = session.getAttribute("flash");
        request.setAttribute("flash", flash);
        session.removeAttribute("flash");
      }
      if ("POST".equals(request.getMethod())) {
        String supplied;
        try {
          supplied = request.getParameter("csrf");
        } catch (IllegalStateException e) {
          if (request.getContentType() == null
              || !request.getContentType().startsWith("multipart/")) {
            throw e;
          }
          problem(request, response, 400, "The upload is too large. Choose an image under 5 MB.");
          return;
        }
        String expected = (String) session.getAttribute("csrf");
        if (supplied == null
            || !MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                supplied.getBytes(StandardCharsets.UTF_8))) {
          response.setStatus(403);
          request.setAttribute("error", "Your form expired. Reload the page and try again.");
          Web.render(request, response, "message");
          return;
        }
      }
      UserRole required =
          switch (path) {
            case "/admin" -> UserRole.ADMIN;
            case "/studio" -> UserRole.SELLER;
            case "/orders", "/checkout" -> UserRole.CUSTOMER;
            default -> null;
          };
      if (required != null && user == null) {
        if (required == UserRole.CUSTOMER) {
          session.setAttribute("afterLogin", path);
        }
        Web.redirect(request, response, "/login", "Please sign in to continue.");
        return;
      }
      if (required != null && user.getRole() != required) {
        throw new AccessDeniedException();
      }
      chain.doFilter(request, response);
    } catch (AccessDeniedException e) {
      problem(request, response, 403, e.getMessage());
    } catch (EntityNotFoundException e) {
      problem(request, response, 404, e.getMessage());
    } catch (IllegalArgumentException e) {
      problem(request, response, 400, e.getMessage());
    }
  }

  private void problem(
      HttpServletRequest request, HttpServletResponse response, int code, String message)
      throws IOException, ServletException {
    if (response.isCommitted()) {
      return;
    }
    response.setStatus(code);
    request.setAttribute("error", message);
    Web.render(request, response, "message");
  }
}
