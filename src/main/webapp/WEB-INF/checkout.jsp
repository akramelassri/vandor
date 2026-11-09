<%@ include file="fragments/header.jspf" %>
<div class="section-heading">
  <div>
    <p class="eyebrow">ONE LAST LOOK</p>
    <h1>Checkout</h1>
  </div>
  <a class="link" href="${ctx}/cart">← Edit cart</a>
</div>
<c:choose>
  <c:when test="${empty lines}">
    <div class="empty">
      <h2>Your cart is empty</h2>
      <p>Already placed an order? You can find it in your orders.</p>
      <a class="btn" href="${ctx}/orders">View orders</a>
    </div>
  </c:when>
  <c:otherwise>
    <div class="split">
      <section class="panel">
        <h2>Review your items</h2>
        <p class="muted">
          Your purchase creates ${sellerCount} order(s), one for each maker.
        </p>
        <c:forEach items="${lines}" var="line">
          <div class="cart-line">
            <img
              class="thumb"
              src="${ctx}/images/<c:out value='${line.product.thumbnail}'/>"
              alt=""
            />
            <div class="grow">
              <strong><c:out value="${line.product.name}"/></strong>
              <p class="small muted">
                By <c:out value="${line.product.seller.user.firstName}"/> <c:out value="${line.product.seller.user.lastName}"/> ·
                ${line.quantity} × ${line.product.formattedPrice}
              </p>
            </div>
            <strong>${line.formattedSubtotal}</strong>
          </div>
        </c:forEach>
      </section>
      <aside class="panel summary">
        <h2>Order summary</h2>
        <div class="row spread">
          <span>Total</span>
          <strong>${total}</strong>
        </div>
        <p class="muted small">
          Shipping is free. No payment is collected. Stock is checked when you place your
          order.
        </p>
        <c:choose>
          <c:when test="${stockChanged}">
            <form method="post" action="${ctx}/cart">
              <v:csrf />
              <button class="btn wide" name="action" value="adjust">
                Adjust cart to available stock
              </button>
            </form>
          </c:when>
          <c:otherwise>
            <form method="post" action="${ctx}/checkout">
              <v:csrf />
              <input type="hidden" name="checkoutToken" value="<c:out value='${sessionScope.checkoutToken}'/>" />
              <button class="btn wide" data-submit-once>
                Place order · ${total}
              </button>
            </form>
          </c:otherwise>
        </c:choose>
      </aside>
    </div>
  </c:otherwise>
</c:choose>
<%@ include file="fragments/footer.jspf" %>
