<%@ include file="include.jsp" %>
<%@ page contentType="text/html; charset=iso-8859-1" errorPage="" %>
<html>
<head><title><fmt:message key="title"/></title><meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1"></head>
<body bgcolor="#ffffff">
<form name="form1" method="post" action="">

<td alignment="right" width="31%"><strong>PubMed Id:</strong></td>
<td width="38%">
  <input name="pubMedId" type="text" value="<c:out value="${status.value}"/>">
</td>  
<br><br>
<input type="submit" alignment="center" value="Submit">  
</form>

<a href="<c:url value="home.jsp"/>">Home</a>
</body>
</html>


