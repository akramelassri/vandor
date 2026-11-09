package com.vandor.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vandor.models.User;
import com.vandor.models.UserRole;
import com.vandor.models.UserStatus;
import com.vandor.services.SessionService;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SecurityFilterTest {
  Map<String, Object> attributes = new HashMap<>(), sessionAttributes = new HashMap<>();
  Map<String, String> parameters = new HashMap<>();
  User user;
  String path = "/shop", method = "GET", redirect;
  int status = 200;
  boolean passed;
  String contentType;
  IllegalStateException parameterFailure;
  IllegalStateException applicationFailure;

  <T> T proxy(Class<T> cls, java.lang.reflect.InvocationHandler h) {
    return cls.cast(Proxy.newProxyInstance(cls.getClassLoader(), new Class<?>[] {cls}, h));
  }

  void filter() throws Exception {
    HttpSession session =
        proxy(
            HttpSession.class,
            (p, m, a) ->
                switch (m.getName()) {
                  case "getAttribute" -> sessionAttributes.get(a[0]);
                  case "setAttribute" -> sessionAttributes.put((String) a[0], a[1]);
                  case "removeAttribute" -> sessionAttributes.remove(a[0]);
                  default -> null;
                });
    HttpServletRequest request =
        proxy(
            HttpServletRequest.class,
            (p, m, a) ->
                switch (m.getName()) {
                  case "getServletPath" -> path;
                  case "getMethod" -> method;
                  case "getContextPath" -> "/vandor";
                  case "getSession" -> session;
                  case "getCookies" -> null;
                  case "getContentType" -> contentType;
                  case "getParameter" -> {
                    if (parameterFailure != null) {
                      throw parameterFailure;
                    }
                    yield parameters.get(a[0]);
                  }
                  case "getAttribute" -> attributes.get(a[0]);
                  case "setAttribute" -> attributes.put((String) a[0], a[1]);
                  case "getRequestDispatcher" ->
                      proxy(RequestDispatcher.class, (dp, dm, da) -> null);
                  default -> null;
                });
    HttpServletResponse response =
        proxy(
            HttpServletResponse.class,
            (p, m, a) ->
                switch (m.getName()) {
                  case "setStatus" -> {
                    status = (Integer) a[0];
                    yield null;
                  }
                  case "sendRedirect" -> {
                    redirect = (String) a[0];
                    yield null;
                  }
                  case "isCommitted" -> false;
                  default -> null;
                });
    SessionService sessions =
        new SessionService() {
          @Override
          public User resolve(String token) {
            return user;
          }
        };
    new SecurityFilter(sessions)
        .doFilter(
            request,
            response,
            (r, s) -> {
              if (applicationFailure != null) {
                throw applicationFailure;
              }
              passed = true;
            });
  }

  @Test
  void guestsCanBrowseButCheckoutRequiresLoginWithReturnIntent() throws Exception {
    filter();
    assertTrue(passed);
    assertNotNull(sessionAttributes.get("csrf"));
    passed = false;
    path = "/checkout";
    filter();
    assertFalse(passed);
    assertEquals("/vandor/login", redirect);
    assertEquals("/checkout", sessionAttributes.get("afterLogin"));
  }

  @Test
  void postRequiresMatchingCsrfToken() throws Exception {
    method = "POST";
    path = "/cart";
    filter();
    assertEquals(403, status);
    assertFalse(passed);
    parameters.put("csrf", (String) sessionAttributes.get("csrf"));
    status = 200;
    filter();
    assertTrue(passed);
  }

  @Test
  void customerCannotAccessSellerOrAdminScreens() throws Exception {
    user = new User();
    user.setRole(UserRole.CUSTOMER);
    user.setStatus(UserStatus.ACTIVE);
    path = "/studio";
    filter();
    assertEquals(403, status);
    assertFalse(passed);
    path = "/admin";
    filter();
    assertEquals(403, status);
    assertFalse(passed);
  }

  @Test
  void oversizedMultipartRequestHasAUsefulError() throws Exception {
    method = "POST";
    contentType = "multipart/form-data; boundary=test";
    parameterFailure = new IllegalStateException("Multipart size limit exceeded");
    filter();
    assertEquals(400, status);
    assertFalse(passed);
    assertTrue(attributes.get("error").toString().contains("5 MB"));
  }

  @Test
  void applicationFailureIsNotMisreportedAsAnUploadProblem() {
    method = "POST";
    contentType = "multipart/form-data; boundary=test";
    sessionAttributes.put("csrf", "known-token");
    parameters.put("csrf", "known-token");
    applicationFailure = new IllegalStateException("Unrelated application failure");
    assertSame(applicationFailure, assertThrows(IllegalStateException.class, this::filter));
  }
}
