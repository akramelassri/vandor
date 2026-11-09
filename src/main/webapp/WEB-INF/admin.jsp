<%@ include file="fragments/header.jspf" %>
<div class="section-heading">
  <div>
    <p class="eyebrow">KEEP THE MARKET RUNNING</p>
    <h1>Administration</h1>
  </div>
</div>
<nav class="tabs">
  <a href="#users">Users (${users.size()})</a>
  <a href="#categories">Categories (${categories.size()})</a>
  <a href="#products">Products (${products.size()})</a>
  <a href="#orders">Orders (${orders.size()})</a>
</nav>
<h2 id="users">Users</h2>
<div class="table-wrap">
  <table>
    <thead>
      <tr>
        <th>Name</th>
        <th>Email</th>
        <th>Role</th>
        <th>Status</th>
        <th>Action</th>
      </tr>
    </thead>
    <tbody>
      <c:forEach items="${users}" var="user">
        <tr>
          <td><c:out value="${user.firstName}"/> <c:out value="${user.lastName}"/></td>
          <td><c:out value="${user.email}"/></td>
          <td>${user.role}</td>
          <td><span class="badge">${user.status}</span></td>
          <td>
            <c:if test="${user.id != currentUser.id}">
              <form method="post" action="${ctx}/admin">
                <v:csrf />
                <input type="hidden" name="action" value="user" />
                <input type="hidden" name="id" value="${user.id}" />
                <button
                  class="btn outline sm"
                  name="status"
                  value="${user.status == 'ACTIVE' ? 'BLOCKED' : 'ACTIVE'}"
                >
                  ${user.status == 'ACTIVE' ? 'Suspend' : 'Restore'}
                </button>
              </form>
            </c:if>
          </td>
        </tr>
      </c:forEach>
    </tbody>
  </table>
</div>
<div class="section-heading" id="categories"><h2>Categories</h2></div>
<div class="panel">
  <c:forEach items="${categories}" var="category">
    <form class="category-row" method="post" action="${ctx}/admin">
      <v:csrf />
      <input type="hidden" name="id" value="${category.id}" />
      <label class="sr-only" for="cat-${category.id}">Category name</label>
      <input
        class="input"
        id="cat-${category.id}"
        name="name"
        maxlength="50"
        value="<c:out value='${category.name}'/>"
        required
      />
      <label class="sr-only" for="desc-${category.id}">
        Category description
      </label>
      <input
        class="input"
        id="desc-${category.id}"
        name="description"
        value="<c:out value='${category.description}'/>"
        placeholder="Description"
      />
      <button class="btn outline sm" name="action" value="category">Save</button>
      <button
        class="link small"
        name="action"
        value="delete-category"
        data-confirm-button="Delete this category?"
      >
        Remove
      </button>
    </form>
  </c:forEach>
  <form method="post" action="${ctx}/admin" class="category-row">
    <v:csrf />
    <label class="sr-only" for="new-category">New category name</label>
    <input
      class="input"
      id="new-category"
      name="name"
      placeholder="New category name"
      maxlength="50"
      required
    />
    <label class="sr-only" for="new-description">Description</label>
    <input
      class="input"
      id="new-description"
      name="description"
      placeholder="Description"
    />
    <button class="btn sm" name="action" value="category">Add category</button>
  </form>
</div>
<div class="section-heading" id="products"><h2>Products</h2></div>
<div class="table-wrap">
  <table>
    <thead>
      <tr>
        <th>Title</th>
        <th>Seller</th>
        <th>Price</th>
        <th>Stock</th>
      </tr>
    </thead>
    <tbody>
      <c:forEach items="${products}" var="p">
        <tr>
          <td><c:out value="${p.name}"/></td>
          <td><c:out value="${p.seller.user.firstName}"/> <c:out value="${p.seller.user.lastName}"/></td>
          <td>${p.formattedPrice}</td>
          <td>${p.stock}</td>
        </tr>
      </c:forEach>
    </tbody>
  </table>
</div>
<div class="section-heading" id="orders"><h2>Orders</h2></div>
<%@ include file="fragments/order-list.jspf" %>
<%@ include file="fragments/footer.jspf" %>
