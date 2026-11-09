package com.vandor.web;

import com.vandor.util.JpaUtil;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

@WebListener
public class AppLifecycle implements ServletContextListener {
  @Override
  public void contextInitialized(ServletContextEvent event) {
    event.getServletContext().getSessionCookieConfig().setAttribute("SameSite", "Lax");
  }

  @Override
  public void contextDestroyed(ServletContextEvent event) {
    JpaUtil.closeFactory();
  }
}
