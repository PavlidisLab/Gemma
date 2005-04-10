<%@ include file="include.jsp" %>
<%@ page contentType="text/html; charset=iso-8859-1" language="java" import="java.sql.*" errorPage="" %>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head><title>Search By PubMed Id</title><meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1"></head>
<body bgcolor="#ffffff">
<form name="form1" method="post" action="">

<td alignment="right" width="31%"><strong>Current Bibliographic References:</strong></td>

<table BORDER = 1 CELLPADDING = 10>
<tr><td><h4>Title</h4></td><td><h4>Author List</h4></td></tr>
<c:forEach items="${model.bibRefs}" var="br">
<tr><td><c:out value="${br.title}"/></td><td><c:out value="${br.authorList}"/></td></tr>
</c:forEach>
</table>
</form>
<a href="<c:url value="home.jsp"/>">Home</a>
</body>
</html>


