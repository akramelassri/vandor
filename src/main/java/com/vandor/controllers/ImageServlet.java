package com.vandor.controllers;

import com.vandor.services.ImageService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;

@WebServlet("/images/*")
public class ImageServlet extends HttpServlet {
  private final ImageService images = new ImageService();

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    String name = request.getPathInfo() == null ? "" : request.getPathInfo().substring(1);
    var file = images.file(name);
    if (file == null) {
      response.sendRedirect(request.getContextPath() + "/assets/default.svg");
      return;
    }
    response.setContentType(name.endsWith("png") ? "image/png" : "image/jpeg");
    response.setHeader("Cache-Control", "public, max-age=86400");
    response.setContentLengthLong(Files.size(file));
    Files.copy(file, response.getOutputStream());
  }
}
