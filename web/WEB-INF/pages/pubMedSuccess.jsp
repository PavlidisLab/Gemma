<%@ include file="include.jsp" %>
<%@ page contentType="text/html; charset=iso-8859-1" language="java" import="java.sql.*" errorPage="" %>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head><title><fmt:message key="title"/></title><meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1"></head>
<body bgcolor="#ffffff">
<form name="form1" method="post" action="">

<td alignment="right" width="31%"><strong>Bibliographic Reference Results:</strong></td>

<table BORDER = 1 CELLPADDING = 10>
<tr><td>Title</td><td><c:out value="${model.bibRef.title}"/></td></tr>
<tr><td>Publication</td><td><c:out value="${model.bibRef.publication}"/></td></tr>
<tr><td>Author List</td><td><c:out value="${model.bibRef.authorList}"/></td></tr>
<tr><td>Abstract</td><td width=400><c:out value="${model.bibRef.abstractText}"/></td></tr>
</table>
<br><br>
<td alignment="right" width="31%"><strong>Would You Like To Submit This Information?</strong></td>

<br><br>
  <input type="submit" alignment="center" value="Submit">
<br><br>
</form>
<a href="<c:url value="home.jsp"/>">Home</a>
</body>
</html>


