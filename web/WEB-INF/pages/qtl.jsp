<%@ include file="/common/taglibs.jsp"%>

<html>
<head><title>qtl</title></head>
<body>
</p>
<h3>QTLs Found</h3>
<c:forEach items="${model.qtls}" var="qtl">
  <%--<c:out value="${bs.biosequence_id}"/> <c:out value="${bs.length}"/> <i><c:out value="${bs.sequence}"/></i><br><br>--%>
  <c:out value="${qtl.name}"/> <c:out value="${qtl.start}"/> <i><c:out value="${qtl.end}"/></i> <br><br>
</c:forEach>
<br>
<a href="<c:url value="welcome.jsp"/>">Home</a>
<br>
</body>
</html>