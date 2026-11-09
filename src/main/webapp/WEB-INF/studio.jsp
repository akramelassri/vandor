<%@ include file="fragments/header.jspf" %>
<div class="section-heading">
  <div>
    <p class="eyebrow">YOUR MAKER SPACE</p>
    <h1>Seller studio</h1>
    <p class="muted">
      Welcome, <c:out value="${currentUser.firstName}"/>. Make room for your next good thing.
    </p>
  </div>
  <a class="btn" href="${ctx}/studio?tab=new">+ New product</a>
</div>
<nav class="tabs">
  <a href="#products">Your products (${products.size()})</a>
  <a href="#orders">Orders (${orders.size()})</a>
</nav>
<h2 id="products">Your products</h2>
<c:if test="${empty products}">
  <div class="empty">
    <h3>Start your collection</h3>
    <p>Create your first listing to open for business.</p>
    <a class="btn" href="${ctx}/studio?tab=new">Create a listing</a>
  </div>
</c:if>
<div class="table-wrap">
  <table>
    <thead>
      <tr>
        <th>Product</th>
        <th>Price</th>
        <th>Stock</th>
        <th>Actions</th>
      </tr>
    </thead>
    <tbody>
      <c:forEach items="${products}" var="p">
        <tr>
          <td>
            <div class="row">
              <img
                class="thumb"
                src="${ctx}/images/<c:out value='${p.thumbnail}'/>"
                alt=""
              />
              <div>
                <strong><c:out value="${p.name}"/></strong>
                <p class="small muted"><c:out value="${p.category.name}"/></p>
              </div>
            </div>
          </td>
          <td>${p.formattedPrice}</td>
          <td><span class="${p.stock <= 3 ? 'stock-low' : ''}">${p.stock == 0 ? 'Sold out' : p.stock}</span></td>
          <td>
            <div class="row">
              <a
                class="btn outline sm"
                href="${ctx}/studio?tab=edit&amp;id=${p.id}"
              >
                Edit / restock
              </a>
              <form
                method="post"
                action="${ctx}/studio"
                data-confirm="Remove this listing? Products referenced by orders must be kept."
              >
                <v:csrf />
                <input type="hidden" name="id" value="${p.id}" />
                <button class="link small" name="action" value="delete">Remove</button>
              </form>
            </div>
          </td>
        </tr>
      </c:forEach>
    </tbody>
  </table>
</div>
<div class="section-heading" id="orders"><h2>Orders with your products</h2></div>
<%@ include file="fragments/order-list.jspf" %>
<%@ include file="fragments/footer.jspf" %>
