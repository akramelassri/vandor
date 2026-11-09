package com.vandor.controllers;

import com.vandor.dto.CartLine;
import com.vandor.exceptions.AccessDeniedException;
import com.vandor.models.UserRole;
import com.vandor.services.CartService;
import com.vandor.util.Money;
import com.vandor.web.Web;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/cart")
public class CartServlet extends HttpServlet {
  private final CartService carts = new CartService();

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    synchronized (request.getSession()) {
      var user = Web.user(request);
      if (user != null && user.getRole() != UserRole.CUSTOMER) {
        throw new AccessDeniedException();
      }
      var lines = carts.lines(user == null ? null : user.getId(), Web.guest(request));
      request.setAttribute("lines", lines);
      request.setAttribute("stockChanged", lines.stream().anyMatch(CartLine::isChanged));
      request.setAttribute(
          "total",
          Money.format(
              lines.stream()
                  .mapToLong(l -> (long) l.getProduct().getPrice() * l.getQuantity())
                  .sum()));
      Web.render(request, response, "cart");
    }
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    synchronized (request.getSession()) {
      var user = Web.user(request);
      if (user != null && user.getRole() != UserRole.CUSTOMER) {
        throw new AccessDeniedException();
      }
      Long id = user == null ? null : user.getId();
      String action = request.getParameter("action");
      if ("adjust".equals(action)) {
        carts.adjust(id, Web.guest(request));
      } else {
        if (!"add".equals(action)
            && !"update".equals(action)
            && !"remove".equals(action)
            && !"buy".equals(action)) {
          throw new IllegalArgumentException("Unknown cart action.");
        }
        Integer quantity =
            "remove".equals(action) ? 0 : Web.integer(request.getParameter("quantity"));
        if (quantity == null
            || quantity < 0
            || (("add".equals(action) || "buy".equals(action)) && quantity == 0)) {
          throw new IllegalArgumentException("Enter a positive quantity.");
        }
        carts.update(
            id,
            Web.guest(request),
            Web.id(request, "product"),
            quantity,
            "add".equals(action) || "buy".equals(action));
      }
      Web.redirect(
          request,
          response,
          "buy".equals(action) ? "/checkout" : "/cart",
          "Cart updated. Quantities are limited to available stock.");
    }
  }
}
