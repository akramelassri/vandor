<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"
%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="v" tagdir="/WEB-INF/tags" %>
<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1" />
    <title>Sign in — Vandor</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/auth.css" />
  </head>
  <body>
    <header class="hdr">
      <div class="wrap hdr-in">
        <a class="logo" href="${pageContext.request.contextPath}/">Vandor</a>
        <nav class="hdr-nav" aria-label="Account navigation">
          <a class="nl on" aria-current="page" href="${pageContext.request.contextPath}/login">
            Sign in
          </a>
          <a class="btn sm" href="${pageContext.request.contextPath}/register">Register</a>
        </nav>
      </div>
    </header>
    <main class="wrap authpg">
      <section class="auth-card" aria-labelledby="auth-title">
        <h1 id="auth-title">Sign in</h1>
        <c:if test="${not empty error}">
          <p class="notice error" role="alert"><c:out value="${error}"/></p>
        </c:if>
        <c:if test="${not empty flash}">
          <p class="notice" role="status"><c:out value="${flash}"/></p>
        </c:if>
        <c:if test="${not empty success}">
          <p class="notice" role="status"><c:out value="${success}"/></p>
        </c:if>
        <form method="post" action="${pageContext.request.contextPath}/login">
          <v:csrf />
          <div class="field">
            <label class="label" for="email">Email</label>
            <input
              class="input"
              id="email"
              name="email"
              type="email"
              autocomplete="email"
              maxlength="255"
              placeholder="you@example.com"
              required
            />
          </div>
          <div class="field">
            <label class="label" for="password">Password</label>
            <input
              class="input"
              id="password"
              name="password"
              type="password"
              autocomplete="current-password"
              placeholder="••••••••"
              required
            />
          </div>
          <button class="btn wide" type="submit">
            <svg
              class="ic"
              width="18"
              height="18"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="1.8"
              stroke-linecap="round"
              stroke-linejoin="round"
              aria-hidden="true"
            >
              <path d="M4 12h15M13 6l6 6-6 6" />
            </svg>
            Sign in
          </button>
          <p class="alt">
            No account yet?
            <a class="link" href="${pageContext.request.contextPath}/register">Register</a>
            .
          </p>
        </form>
      </section>
    </main>
  </body>
</html>
