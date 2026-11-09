package com.vandor.controllers;

import com.vandor.services.AdminService;
import com.vandor.services.CatalogService;
import com.vandor.services.OrderService;
import com.vandor.web.Web;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/admin")
public class AdminServlet extends HttpServlet {
  private final AdminService admin = new AdminService();
  private final CatalogService catalog = new CatalogService();
  private final OrderService orders = new OrderService();

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    Long actor = Web.user(request).getId();
    request.setAttribute("users", admin.users(actor));
    request.setAttribute("products", admin.products(actor));
    request.setAttribute("categories", catalog.categories());
    request.setAttribute("orders", orders.list(Web.user(request)));
    Web.render(request, response, "admin");
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    Long actor = Web.user(request).getId();
    switch (request.getParameter("action") == null ? "" : request.getParameter("action")) {
      case "user" -> admin.status(actor, Web.id(request, "id"), Web.userStatus(request));
      case "category" ->
        admin.saveCategory(
            actor,
            Web.integer(request.getParameter("id")),
            request.getParameter("name"),
            request.getParameter("description"));
      case "delete-category" ->
        admin.deleteCategory(actor, Web.integer(request.getParameter("id")));
      case "status" -> orders.change(actor, Web.id(request, "id"), Web.orderStatus(request));
      default -> throw new IllegalArgumentException("Unknown admin action.");
    }
    Web.redirect(request, response, "/admin", "Changes saved.");
  }
}
