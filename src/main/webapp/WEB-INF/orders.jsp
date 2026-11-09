<%@ include file="fragments/header.jspf" %>
<div class="section-heading">
  <div>
    <p class="eyebrow">FROM THE MAKERS TO YOU</p>
    <h1>Your orders</h1>
  </div>
  <a class="link" href="${ctx}/shop">Explore the shop →</a>
</div>
<p class="muted history-note">
  Product names and prices below reflect the current listings.
</p>
<%@ include file="fragments/order-list.jspf" %>
<%@ include file="fragments/footer.jspf" %>
