<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<% application.log("Unhandled request failure", exception); %>
<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1" />
    <title>Something went wrong — Vandor</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/auth.css" />
  </head>
  <body>
    <main class="wrap authpg">
      <section class="auth-card">
        <h1>Something went wrong</h1>
        <p>We couldn't complete your request. Please try again later.</p>
        <p class="alt">
          <a class="link" href="${pageContext.request.contextPath}/register">Back to registration</a>
        </p>
      </section>
    </main>
  </body>
</html>
