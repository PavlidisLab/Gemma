<%@ include file="include.jsp" %>

<html>
<head><title>taxon</title></head>
<body>
<h1><fmt:message key="heading"/></h1>
<p><fmt:message key="greeting"/> <c:out value="${model.now}"/>
</p> 
<h3>Taxons Loaded</h3>
<br>
<a href="<c:url value="welcome.jsp"/>">Home</a>
<br>
</body>
</html>