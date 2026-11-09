package com.vandor.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vandor.dto.RegisterRequest;
import com.vandor.exceptions.EmailAlreadyRegisteredException;
import com.vandor.exceptions.ValidationException;
import com.vandor.services.AuthService;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AuthServletTest {
  private final Map<String, Object> attributes = new HashMap<>();
  private final Map<String, Object> sessionAttributes = new HashMap<>();
  private final Map<String, String> parameters =
      Map.of(
          "firstName",
          "Jane",
          "lastName",
          "Cooper",
          "email",
          "jane@example.com",
          "password",
          "secret123",
          "role",
          "CUSTOMER");
  private String route = "/register";
  private String redirect;
  private String forwarded;
  private int status = 200;

  private <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
    return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, handler));
  }

  private final HttpSession session =
      proxy(
          HttpSession.class,
          (p, m, a) ->
              switch (m.getName()) {
                case "getAttribute" -> sessionAttributes.get(a[0]);
                case "setAttribute" -> sessionAttributes.put((String) a[0], a[1]);
                case "removeAttribute" -> sessionAttributes.remove(a[0]);
                default -> null;
              });

  private HttpServletRequest request() {
    return proxy(
        HttpServletRequest.class,
        (p, m, a) ->
            switch (m.getName()) {
              case "getServletPath" -> route;
              case "getContextPath" -> "/vandor";
              case "getParameter" -> parameters.get(a[0]);
              case "getAttribute" -> attributes.get(a[0]);
              case "setAttribute" -> attributes.put((String) a[0], a[1]);
              case "getSession" -> session;
              case "getRequestDispatcher" -> {
                String path = (String) a[0];
                yield proxy(
                    RequestDispatcher.class,
                    (dp, dm, da) -> {
                      if (dm.getName().equals("forward")) {
                        forwarded = path;
                      }
                      return null;
                    });
              }
              default -> null;
            });
  }

  private HttpServletResponse response() {
    return proxy(
        HttpServletResponse.class,
        (p, m, a) -> {
          if (m.getName().equals("sendRedirect")) {
            redirect = (String) a[0];
          }
          if (m.getName().equals("setStatus")) {
            status = (Integer) a[0];
          }
          return null;
        });
  }

  @Test
  void redirectsAfterSuccessAndConsumesFlashOnce() throws Exception {
    AuthServlet servlet =
        new AuthServlet(
            new AuthService() {
              @Override
              public void registerUser(RegisterRequest data) {
                assertEquals("jane@example.com", data.email());
              }
            });
    servlet.doPost(request(), response());
    assertEquals("/vandor/login", redirect);
    assertNull(forwarded);
    assertNotNull(sessionAttributes.get("registrationSuccess"));
    route = "/login";
    servlet.doGet(request(), response());
    assertEquals("Account created. Please sign in.", attributes.get("success"));
    assertEquals("/WEB-INF/login.jsp", forwarded);
    attributes.clear();
    servlet.doGet(request(), response());
    assertNull(attributes.get("success"));
  }

  @Test
  void handlesDuplicateEmailWithoutSuccessMessage() throws Exception {
    AuthServlet servlet =
        new AuthServlet(
            new AuthService() {
              @Override
              public void registerUser(RegisterRequest data) {
                throw new EmailAlreadyRegisteredException();
              }
            });
    servlet.doPost(request(), response());
    assertEquals(400, status);
    assertEquals("/WEB-INF/register.jsp", forwarded);
    assertNotNull(attributes.get("errors"));
    assertNull(redirect);
    assertTrue(sessionAttributes.isEmpty());
  }

  @Test
  void propagatesUnexpectedErrorsForGlobalHandler() {
    IllegalStateException failure = new IllegalStateException("unavailable");
    AuthServlet servlet =
        new AuthServlet(
            new AuthService() {
              @Override
              public void registerUser(RegisterRequest data) {
                throw failure;
              }
            });
    assertSame(
        failure,
        assertThrows(IllegalStateException.class, () -> servlet.doPost(request(), response())));
    assertNull(forwarded);
    assertNull(redirect);
    assertTrue(sessionAttributes.isEmpty());
  }

  @Test
  void preservesOnlyNonPasswordFieldsOnValidationFailure() throws Exception {
    AuthServlet servlet =
        new AuthServlet(
            new AuthService() {
              @Override
              public void registerUser(RegisterRequest data) {
                throw new ValidationException(Map.of("email", "Invalid email"));
              }
            });
    servlet.doPost(request(), response());
    assertEquals(400, status);
    assertEquals("/WEB-INF/register.jsp", forwarded);
    assertEquals("Jane", attributes.get("firstName"));
    assertEquals("jane@example.com", attributes.get("email"));
    assertNull(attributes.get("password"));
    assertNull(redirect);
    assertTrue(sessionAttributes.isEmpty());
  }
}
