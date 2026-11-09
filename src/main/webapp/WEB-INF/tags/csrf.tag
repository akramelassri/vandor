<%@ tag pageEncoding="UTF-8" body-content="empty" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<input type="hidden" name="csrf" value="<c:out value='${sessionScope.csrf}'/>" />
