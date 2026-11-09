package com.vandor.controllers;

import com.vandor.services.CatalogService;
import com.vandor.services.ImageService;
import com.vandor.services.OrderService;
import com.vandor.services.SellerService;
import com.vandor.util.Money;
import com.vandor.web.Web;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@WebServlet("/studio")
@MultipartConfig(
    maxFileSize = ImageService.MAX_SIZE,
    maxRequestSize = ImageService.MAX_SIZE + 65536)
public class SellerServlet extends HttpServlet {
  private final SellerService sellers = new SellerService();
  private final OrderService orders = new OrderService();
  private final CatalogService catalog = new CatalogService();
  private final ImageService images = new ImageService();

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    Long seller = Web.user(request).getId();
    String tab = request.getParameter("tab");
    if ("edit".equals(tab) || "new".equals(tab)) {
      showListing(request, response, "edit".equals(tab) ? Web.id(request, "id") : null, false);
      return;
    }
    request.setAttribute("products", sellers.listings(seller));
    request.setAttribute("orders", orders.list(Web.user(request)));
    Web.render(request, response, "studio");
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    Long seller = Web.user(request).getId();
    String action = request.getParameter("action");
    if ("delete".equals(action)) {
      sellers.delete(seller, Web.id(request, "id"));
    } else if ("status".equals(action)) {
      orders.change(seller, Web.id(request, "id"), Web.orderStatus(request));
    } else if ("save".equals(action)) {
      Long id =
          request.getParameter("id") == null || request.getParameter("id").isBlank()
              ? null
              : Web.id(request, "id");
      String filename = null;
      boolean saved = false;
      try {
        filename = images.store(request.getPart("image"));
        Integer stock = Web.integer(request.getParameter("stock"));
        if (stock == null) {
          throw new IllegalArgumentException("Enter a stock quantity.");
        }
        sellers.save(
            seller,
            id,
            request.getParameter("name"),
            request.getParameter("description"),
            Web.integer(request.getParameter("category")),
            Money.parse(request.getParameter("price")),
            stock,
            filename);
        saved = true;
      } catch (IllegalArgumentException e) {
        response.setStatus(400);
        request.setAttribute("error", e.getMessage());
        showListing(request, response, id, true);
        return;
      } finally {
        if (!saved && filename != null) {
          images.discard(filename);
        }
      }
    } else {
      throw new IllegalArgumentException("Unknown seller action.");
    }
    Web.redirect(request, response, "/studio", "Changes saved.");
  }

  private void showListing(
      HttpServletRequest request, HttpServletResponse response, Long id, boolean submitted)
      throws ServletException, IOException {
    var product = id == null ? null : sellers.listing(Web.user(request).getId(), id);
    Map<String, String> form = new LinkedHashMap<>();
    form.put("name", product == null ? "" : product.getName());
    form.put("description", product == null ? "" : product.getDescription());
    form.put(
        "category",
        product == null || product.getCategory() == null
            ? ""
            : product.getCategory().getId().toString());
    form.put(
        "price", product == null ? "" : BigDecimal.valueOf(product.getPrice(), 2).toPlainString());
    form.put("stock", product == null ? "1" : product.getStock().toString());
    if (submitted) {
      form.replaceAll(
          (name, value) -> request.getParameter(name) == null ? "" : request.getParameter(name));
    }
    request.setAttribute("product", product);
    request.setAttribute("form", form);
    request.setAttribute("categories", catalog.categories());
    Web.render(request, response, "listing");
  }
}
