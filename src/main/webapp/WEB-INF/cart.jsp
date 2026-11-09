<%@ include file="fragments/header.jspf" %>
<div class="section-heading">
  <div>
    <p class="eyebrow">YOUR FINDS</p>
    <h1>Shopping cart</h1>
  </div>
  <a class="link" href="${ctx}/shop">Continue shopping →</a>
</div>
<c:choose>
  <c:when test="${empty lines}">
    <div class="empty">
      <h2>Your cart is waiting</h2>
      <p>Find something you love in the collection.</p>
      <a class="btn" href="${ctx}/shop">Explore the shop</a>
    </div>
  </c:when>
  <c:otherwise>
    <c:if test="${stockChanged}">
      <div class="notice error">
        <p>Availability has changed. Adjust your cart before checking out.</p>
        <form method="post" action="${ctx}/cart">
          <v:csrf />
          <button class="btn outline sm" name="action" value="adjust">
            Adjust cart to available stock
          </button>
        </form>
      </div>
    </c:if>
    <div class="split">
      <section class="panel">
        <c:forEach items="${lines}" var="line">
          <div class="cart-line">
            <img
              class="thumb"
              src="${ctx}/images/<c:out value='${line.product.thumbnail}'/>"
              alt=""
            />
            <div class="grow">
              <a
                class="link"
                href="${ctx}/product?id=${line.product.id}"
              >
                <c:out value="${line.product.name}"/>
              </a>
              <p class="small muted">${line.product.formattedPrice} each</p>
              <c:if test="${line.changed}">
                <p class="stock-low">Availability changed</p>
              </c:if>
            </div>
            <form class="row" method="post" action="${ctx}/cart">
              <v:csrf />
              <input type="hidden" name="product" value="${line.product.id}" />
              <label class="sr-only" for="q-${line.product.id}">
                Quantity for <c:out value="${line.product.name}"/>
              </label>
              <input
                class="input quantity"
                id="q-${line.product.id}"
                name="quantity"
                type="number"
                min="0"
                value="${line.quantity}"
                required
              />
              <button class="btn outline sm" name="action" value="update">Update</button>
              <button class="link small" name="action" value="remove">Remove</button>
            </form>
            <strong>${line.formattedSubtotal}</strong>
          </div>
        </c:forEach>
      </section>
      <aside class="panel summary">
        <h2>Your total</h2>
        <div class="row spread">
          <span>Subtotal</span>
          <strong>${total}</strong>
        </div>
        <div class="row spread">
          <span>Shipping</span>
          <span>Free</span>
        </div>
        <hr />
        <div class="row spread">
          <strong>Total</strong>
          <strong>${total}</strong>
        </div>
        <a class="btn wide" href="${ctx}/checkout">
          Continue to checkout →
        </a>
        <p class="muted small">Your cart stays with you when you sign in.</p>
      </aside>
    </div>
  </c:otherwise>
</c:choose>
<%@ include file="fragments/footer.jspf" %>
