<%@ include file="fragments/header.jspf" %>
<a class="link" href="${ctx}/shop">← Back to the collection</a>
<section class="product-detail">
  <img
    class="detail-image"
    src="${ctx}/images/<c:out value='${product.thumbnail}'/>"
    alt="<c:out value='${product.name}'/>"
  />
  <div>
    <p class="eyebrow"><c:out value="${product.category.name}"/></p>
    <h1><c:out value="${product.name}"/></h1>
    <p class="muted">Made by <c:out value="${product.seller.user.firstName}"/> <c:out value="${product.seller.user.lastName}"/></p>
    <p class="detail-price">${product.formattedPrice}</p>
    <p class="description"><c:out value="${product.description}"/></p>
    <p class="stock-note">
      <c:choose>
        <c:when test="${product.stock == 0}">Sold out</c:when>
        <c:when test="${product.stock <= 3}">
          Only ${product.stock} left
        </c:when>
        <c:otherwise>In stock · ready for your next order</c:otherwise>
      </c:choose>
    </p>
    <c:if test="${product.stock > 0 && (empty currentUser || currentUser.role == 'CUSTOMER')}">
      <form method="post" action="${ctx}/cart" class="purchase">
        <v:csrf />
        <input type="hidden" name="product" value="${product.id}" />
        <div class="field">
          <label class="label" for="quantity">Quantity</label>
          <input
            class="input quantity"
            id="quantity"
            name="quantity"
            type="number"
            min="1"
            max="${product.stock}"
            value="1"
            required
          />
        </div>
        <div class="row">
          <button class="btn" name="action" value="add">Add to cart</button>
          <button class="btn outline" name="action" value="buy">Buy now</button>
        </div>
      </form>
    </c:if>
  </div>
</section>
<%@ include file="fragments/footer.jspf" %>
