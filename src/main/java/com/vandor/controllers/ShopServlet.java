package com.vandor.controllers;

import com.vandor.services.CatalogService;
import com.vandor.util.Money;
import com.vandor.web.Web;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(urlPatterns = {"", "/shop", "/product"})
public class ShopServlet extends HttpServlet {
  private final CatalogService catalog = new CatalogService();

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    if ("/product".equals(request.getServletPath())) {
      request.setAttribute("product", catalog.product(Web.id(request, "id")));
      Web.render(request, response, "product");
      return;
    }
    Integer min = price(request.getParameter("min")), max = price(request.getParameter("max"));
    request.setAttribute(
        "products",
        catalog.search(
            request.getParameter("q"),
            Web.integer(request.getParameter("category")),
            min,
            max,
            "true".equals(request.getParameter("stock")),
            request.getParameter("sort")));
    request.setAttribute("categories", catalog.categories());
    Web.render(request, response, "shop");
  }

  private Integer price(String value) {
    return value == null || value.isBlank() ? null : Money.parse(value);
  }
}
