<%@ include file="include.jsp" %>

<html>
<head><title>qtl.jsp</title></head>
<body>
<h1><fmt:message key="heading"/></h1>
<p><fmt:message key="greeting"/> <c:out value="${model.now}"/>
</p>
<h3>QTL</h3>
<c:forEach items="${model.qtls}" var="qtl">
  <%--<c:out value="${bs.biosequence_id}"/> <c:out value="${bs.length}"/> <i><c:out value="${bs.sequence}"/></i><br><br>--%>
  <c:out value="${qtl.name}"/> <c:out value="${qtl.start}"/> <i><c:out value="${qtl.end}"/></i> <br><br>
</c:forEach>
<br>
<a href="<c:url value="welcome.jsp"/>">Home</a>
<br>
</body>
</html>