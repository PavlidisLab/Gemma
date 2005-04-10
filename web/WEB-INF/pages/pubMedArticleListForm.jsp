<%@ include file="include.jsp" %>

<%@ page contentType="text/html; charset=iso-8859-1" language="java" import="java.sql.*" errorPage="" %>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head><title><fmt:message key="title"/></title><meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1"></head>
<body bgcolor="#ffffff">
<form method="post">
<form name="form1" method="post" action="">
<SELECT NAME="maxResults">
<OPTION VALUE="10">10
<%--<OPTION VALUE="50">50
<OPTION VALUE="100">100--%>
</SELECT>


<br></br>
<%--
<spring:hasBindErrors name="fileName">
    <b>Please fix all errors!</b>
</spring:hasBindErrors>
--%>
<br>
<input type="submit" alignment="center" value="Submit">       
</form>

<a href="<c:url value="home.jsp"/>">Home</a>
</body>
</html>


