<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isELIgnored="false"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="v" tagdir="/WEB-INF/tags" %>
<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1" />
    <title>Create your account — Vandor</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/auth.css" />
  </head>
  <body>
    <header class="hdr">
      <div class="wrap hdr-in">
        <a class="logo" href="${pageContext.request.contextPath}/">Vandor</a>
        <nav class="hdr-nav" aria-label="Account navigation">
          <a class="nl" href="${pageContext.request.contextPath}/login">Sign in</a>
          <a class="btn sm" aria-current="page" href="${pageContext.request.contextPath}/register">
            Register
          </a>
        </nav>
      </div>
    </header>
    <main class="wrap authpg">
      <section class="auth-card" aria-labelledby="auth-title">
        <h1 id="auth-title">Create your account</h1>
        <c:if test="${not empty errors}">
          <div class="notice error" role="alert">
            <ul>
              <c:forEach items="${errors}" var="error">
                <li><c:out value="${error.value}"/></li>
              </c:forEach>
            </ul>
          </div>
        </c:if>
        <form method="post" action="${pageContext.request.contextPath}/register">
          <v:csrf />
          <div class="grid2">
            <div class="field">
              <label class="label" for="first-name">First name</label>
              <input
                class="input"
                id="first-name"
                name="firstName"
                value="<c:out value='${firstName}'/>"
                autocomplete="given-name"
                maxlength="50"
                placeholder="Jane"
                required
              />
            </div>
            <div class="field">
              <label class="label" for="last-name">Last name</label>
              <input
                class="input"
                id="last-name"
                name="lastName"
                value="<c:out value='${lastName}'/>"
                autocomplete="family-name"
                maxlength="100"
                placeholder="Cooper"
                required
              />
            </div>
          </div>
          <div class="field">
            <label class="label" for="email">Email</label>
            <input
              class="input"
              id="email"
              name="email"
              value="<c:out value='${email}'/>"
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
              autocomplete="new-password"
              minlength="8"
              placeholder="Your password"
              required
            />
          </div>
          <div class="rolepick" role="group" aria-label="Account type">
            <label class="rolebox">
              <input
                type="radio"
                name="role"
                value="CUSTOMER"
                ${empty role or role == 'CUSTOMER' ? 'checked' : ''}
                required
              />
              <span class="rt">I'm shopping</span>
              <span class="rdesc">Browse, buy, track orders.</span>
            </label>
            <label class="rolebox">
              <input type="radio" name="role" value="SELLER" ${role == 'SELLER' ? 'checked' : ''} />
              <span class="rt">I'm selling</span>
              <span class="rdesc">List and sell products.</span>
            </label>
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
            Create account
          </button>
          <p class="alt">
            Already have an account?
            <a class="link" href="${pageContext.request.contextPath}/login">Sign in</a>
            .
          </p>
        </form>
      </section>
    </main>
  </body>
</html>
