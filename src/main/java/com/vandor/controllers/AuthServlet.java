package com.vandor.controllers;

import com.vandor.dto.RegisterRequest;
import com.vandor.exceptions.EmailAlreadyRegisteredException;
import com.vandor.exceptions.ValidationException;
import com.vandor.models.Session;
import com.vandor.models.UserRole;
import com.vandor.services.AuthService;
import com.vandor.services.CartService;
import com.vandor.services.SessionService;
import com.vandor.web.SecurityFilter;
import com.vandor.web.Web;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@WebServlet(urlPatterns = { "/login", "/register", "/logout" })
public class AuthServlet extends HttpServlet {
  private final AuthService authService;

  public AuthServlet() {
    this(new AuthService());
  }

  AuthServlet(AuthService authService) {
    this.authService = authService;
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    if (Web.user(request) != null) {
      Web.redirect(request, response, Web.home(Web.user(request)), null);
      return;
    }
    if ("/login".equals(request.getServletPath())) {
      HttpSession session = request.getSession(false);
      if (session != null) {
        synchronized (session) {
          request.setAttribute("success", session.getAttribute("registrationSuccess"));
          session.removeAttribute("registrationSuccess");
        }
      }
    }
    forwardAuthPage(request, response);
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    request.setCharacterEncoding("UTF-8");
    if (Web.user(request) != null && !"/logout".equals(request.getServletPath())) {
      Web.redirect(request, response, Web.home(Web.user(request)), null);
      return;
    }
    if ("/logout".equals(request.getServletPath())) {
      new SessionService().logout(SecurityFilter.token(request));
      SecurityFilter.cookie(request, response, "", 0);
      request.getSession().invalidate();
      Web.redirect(request, response, "/shop", null);
      return;
    }
    if ("/login".equals(request.getServletPath())) {
      login(request, response);
      return;
    }
    RegisterRequest data = new RegisterRequest(
        request.getParameter("firstName"),
        request.getParameter("lastName"),
        request.getParameter("email"),
        request.getParameter("password"),
        request.getParameter("role"));
    try {
      authService.registerUser(data);
    } catch (ValidationException e) {
      request.setAttribute("errors", e.getErrors());
      forwardRegistrationError(request, response);
      return;
    } catch (EmailAlreadyRegisteredException e) {
      request.setAttribute("errors", Map.of("email", e.getMessage()));
      forwardRegistrationError(request, response);
      return;
    }
    request.getSession().setAttribute("registrationSuccess", "Account created. Please sign in.");
    response.sendRedirect(request.getContextPath() + "/login");
  }

  private void login(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    SessionService sessions = new SessionService();
    Session authenticated;
    try {
      authenticated = sessions.login(request.getParameter("email"), request.getParameter("password"));
    } catch (IllegalArgumentException e) {
      response.setStatus(400);
      request.setAttribute("error", e.getMessage());
      forwardAuthPage(request, response);
      return;
    }
    HttpSession httpSession = request.getSession();
    synchronized (httpSession) {
      try {
        if (authenticated.getUser().getRole() == UserRole.CUSTOMER) {
          new CartService().merge(authenticated.getUser().getId(), Web.guest(request));
          httpSession.removeAttribute("guestCart");
        }
      } catch (RuntimeException e) {
        sessions.logout(authenticated.getId().toString());
        throw e;
      }
      sessions.logout(SecurityFilter.token(request));
      request.changeSessionId();
      httpSession.setAttribute("csrf", UUID.randomUUID().toString());
      SecurityFilter.cookie(
          request, response, authenticated.getId().toString(), SessionService.LIFETIME_SECONDS);
      String destination = (String) httpSession.getAttribute("afterLogin");
      httpSession.removeAttribute("afterLogin");
      boolean resume = authenticated.getUser().getRole() == UserRole.CUSTOMER
          && ("/checkout".equals(destination) || "/orders".equals(destination));
      Web.redirect(
          request,
          response,
          resume ? destination : Web.home(authenticated.getUser()),
          "Welcome back.");
    }
  }

  private void forwardRegistrationError(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    request.setAttribute("firstName", request.getParameter("firstName"));
    request.setAttribute("lastName", request.getParameter("lastName"));
    request.setAttribute("email", request.getParameter("email"));
    request.setAttribute("role", request.getParameter("role"));
    forwardAuthPage(request, response);
  }

  private void forwardAuthPage(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    switch (request.getServletPath()) {
      case "/login" ->
        request.getRequestDispatcher("/WEB-INF/login.jsp").forward(request, response);
      case "/register" ->
        request.getRequestDispatcher("/WEB-INF/register.jsp").forward(request, response);
      default -> response.sendError(HttpServletResponse.SC_NOT_FOUND);
    }
  }
}
