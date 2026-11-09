package com.vandor.controllers;

import com.vandor.dto.CartLine;
import com.vandor.models.OrderStatus;
import com.vandor.services.CartService;
import com.vandor.services.OrderService;
import com.vandor.util.Money;
import com.vandor.web.Web;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@WebServlet(urlPatterns = {"/orders", "/checkout"})
public class OrderServlet extends HttpServlet {
  private final OrderService orders = new OrderService();
  private final CartService carts = new CartService();

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    if ("/checkout".equals(request.getServletPath())) {
      var lines = carts.lines(Web.user(request).getId(), Map.of());
      request.setAttribute("lines", lines);
      request.setAttribute("stockChanged", lines.stream().anyMatch(CartLine::isChanged));
      request.setAttribute(
          "sellerCount",
          lines.stream().map(l -> l.getProduct().getSeller().getSellerId()).distinct().count());
      request.setAttribute(
          "total",
          Money.format(
              lines.stream()
                  .mapToLong(l -> (long) l.getProduct().getPrice() * l.getQuantity())
                  .sum()));
      synchronized (request.getSession()) {
        if (request.getSession().getAttribute("checkoutToken") == null) {
          request.getSession().setAttribute("checkoutToken", UUID.randomUUID().toString());
        }
      }
      Web.render(request, response, "checkout");
    } else {
      request.setAttribute("orders", orders.list(Web.user(request)));
      Web.render(request, response, "orders");
    }
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    if ("/checkout".equals(request.getServletPath())) {
      synchronized (request.getSession()) {
        String token = request.getParameter("checkoutToken");
        if (token == null || !token.equals(request.getSession().getAttribute("checkoutToken"))) {
          throw new IllegalArgumentException(
              "This checkout has already been submitted. Open your orders to check the result.");
        }
        try {
          var ids = orders.checkout(Web.user(request).getId());
          request.getSession().removeAttribute("checkoutToken");
          Web.redirect(
              request,
              response,
              "/orders",
              "Order placed. Created " + ids.size() + " order(s), one per seller.");
        } catch (IllegalArgumentException e) {
          request.setAttribute("error", e.getMessage());
          response.setStatus(400);
          doGet(request, response);
        }
      }
    } else {
      orders.change(Web.user(request).getId(), Web.id(request, "id"), OrderStatus.CANCELLED);
      Web.redirect(request, response, "/orders", "Order cancelled and stock restored.");
    }
  }
}
