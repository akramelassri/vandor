<%@ include file="fragments/header.jspf" %>
<section class="hero">
  <div>
    <p class="eyebrow">THE INDEPENDENT MARKETPLACE</p>
    <h1>
      Objects with a story.
      <br />
      Made for your everyday.
    </h1>
    <p>
      Discover thoughtful goods from independent makers. Find something that feels like
      you.
    </p>
    <a class="btn" href="#collection">Explore the collection ↗</a>
  </div>
  <c:choose>
    <c:when test="${not empty products}">
      <c:set var="featured" value="${products[0]}" />
      <a
        class="hero-image"
        href="${ctx}/product?id=${featured.id}"
      >
        <img
          src="${ctx}/images/<c:out value='${featured.thumbnail}'/>"
          alt="<c:out value='${featured.name}'/>"
        />
        <span class="hero-caption">
          <span>
            IN THE SPOTLIGHT
            <br />
            <strong><c:out value="${featured.name}"/></strong>
          </span>
          <strong>${featured.formattedPrice}</strong>
        </span>
      </a>
    </c:when>
    <c:otherwise>
      <div class="hero-placeholder">
        <span>
          Crafted with care.
          <br />
          Found on Vandor.
        </span>
      </div>
    </c:otherwise>
  </c:choose>
</section>
<div class="section-heading" id="collection">
  <div>
    <p class="eyebrow">GOOD THINGS, SMALL MAKERS</p>
    <h2>The collection</h2>
  </div>
  <span class="muted">${products.size()} products</span>
</div>
<form class="filters panel" method="get" action="${ctx}/shop">
  <div class="field search-field">
    <label class="label" for="search">Search</label>
    <input
      class="input"
      id="search"
      name="q"
      value="<c:out value='${param.q}'/>"
      placeholder="Products, makers, categories…"
      maxlength="200"
    />
  </div>
  <div class="field">
    <label class="label" for="category">Category</label>
    <select class="input" id="category" name="category">
      <option value="">All categories</option>
      <c:forEach items="${categories}" var="category">
        <option value="${category.id}" ${param.category == category.id ? 'selected' : ''}>
          <c:out value="${category.name}"/>
        </option>
      </c:forEach>
    </select>
  </div>
  <div class="field">
    <label class="label" for="min">Min €</label>
    <input
      class="input price-input"
      id="min"
      name="min"
      type="number"
      min="0"
      step="0.01"
      value="<c:out value='${param.min}'/>"
    />
  </div>
  <div class="field">
    <label class="label" for="max">Max €</label>
    <input
      class="input price-input"
      id="max"
      name="max"
      type="number"
      min="0"
      step="0.01"
      value="<c:out value='${param.max}'/>"
    />
  </div>
  <div class="field">
    <label class="label" for="sort">Sort by</label>
    <select class="input" id="sort" name="sort">
      <option value="newest">Newest</option>
      <option value="price-asc" ${param.sort == 'price-asc' ? 'selected' : ''}>Price: low to high</option>
      <option value="price-desc" ${param.sort == 'price-desc' ? 'selected' : ''}>Price: high to low</option>
      <option value="name" ${param.sort == 'name' ? 'selected' : ''}>Name</option>
    </select>
  </div>
  <label class="check">
    <input type="checkbox" name="stock" value="true" ${param.stock == 'true' ? 'checked' : ''} />
    In stock only
  </label>
  <button class="btn">Apply</button>
  <a class="link small" href="${ctx}/shop">Reset</a>
</form>
<div class="product-grid">
  <c:forEach items="${products}" var="p">
    <%@ include file="fragments/product-card.jspf" %>
  </c:forEach>
</div>
<c:if test="${empty products}">
  <div class="empty">
    <h3>No products found</h3>
    <p>Try another search, or check back as makers add new products.</p>
    <a class="btn outline" href="${ctx}/shop">Browse all</a>
  </div>
</c:if>
<%@ include file="fragments/footer.jspf" %>
