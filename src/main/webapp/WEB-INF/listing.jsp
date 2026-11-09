<%@ include file="fragments/header.jspf" %>
<a class="link" href="${ctx}/studio">← Back to your studio</a>
<div class="section-heading"><h1>${empty product ? 'Create a listing' : 'Edit product'}</h1></div>
<form
  method="post"
  action="${ctx}/studio"
  enctype="multipart/form-data"
  class="split"
>
  <v:csrf />
  <input type="hidden" name="action" value="save" />
  <input type="hidden" name="tab" value="${empty product ? 'new' : 'edit'}" />
  <input type="hidden" name="id" value="${product.id}" />
  <section class="panel">
    <div class="field">
      <label class="label" for="name">Product title</label>
      <input
        class="input"
        id="name"
        name="name"
        value="<c:out value='${form.name}'/>"
        minlength="3"
        maxlength="50"
        required
      />
    </div>
    <div class="grid2">
      <div class="field">
        <label class="label" for="category">Category</label>
        <select class="input" id="category" name="category" required>
          <option value="">Choose a category</option>
          <c:forEach items="${categories}" var="category">
            <option value="${category.id}" ${form.category == category.id ? 'selected' : ''}>
              <c:out value="${category.name}"/>
            </option>
          </c:forEach>
        </select>
      </div>
      <div class="field">
        <label class="label" for="price">Price (€)</label>
        <input
          class="input"
          id="price"
          name="price"
          type="number"
          min="0.01"
          step="0.01"
          value="<c:out value='${form.price}'/>"
          required
        />
      </div>
    </div>
    <div class="field">
      <label class="label" for="stock">Stock quantity</label>
      <input
        class="input"
        id="stock"
        name="stock"
        type="number"
        min="0"
        max="2147483647"
        value="<c:out value='${form.stock}'/>"
        required
      />
    </div>
    <div class="field">
      <label class="label" for="description">Description</label>
      <textarea
        class="input"
        id="description"
        name="description"
        rows="7"
        minlength="10"
        maxlength="10000"
        required
      ><c:out value="${form.description}"/></textarea>
    </div>
    <div class="row">
      <button class="btn" data-submit-once>Save product</button>
      <a class="btn outline" href="${ctx}/studio">Cancel</a>
    </div>
  </section>
  <aside class="panel">
    <h2>Product photo</h2>
    <img
      id="image-preview"
      class="listing-preview"
      src="${ctx}/images/<c:out value='${empty product ? "default.jpg" : product.thumbnail}'/>"
      alt="Product photo preview"
    />
    <div class="field">
      <label class="label" for="image">Upload photo</label>
      <input
        class="input"
        id="image"
        name="image"
        type="file"
        accept="image/jpeg,image/png"
        data-preview="image-preview"
      />
    </div>
    <p class="muted small">
      JPG or PNG, up to 5 MB and 4096 pixels per side. Leave empty to keep the current
      image.
    </p>
  </aside>
</form>
<%@ include file="fragments/footer.jspf" %>
