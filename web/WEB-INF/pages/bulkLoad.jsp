<%@ include file="include.jsp" %>
<%@ taglib prefix="Gemma" uri="/Gemma" %>
<%@ page contentType="text/html; charset=iso-8859-1" language="java" import="java.sql.*" errorPage="" %>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head><title><fmt:message key="title"/></title><meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1"></head>
<body bgcolor="#ffffff">
<%--<img src="bigicon.gif" width="88" height="97"> <br>--%>
<h1><fmt:message key="bulkLoad.heading"/></h1>
<form method="post">
<form name="form1" method="post" action="">
      <td alignment="right" width="31%"><strong>Filename:</strong></td>
      <spring:bind path="fileName.fileName">
        <td width="38%">
          <input name="fileName" type="text" value="<c:out value="${status.value}"/>
          ">
        </td>
        <td width="31%">
          <font color="red"><c:out value="${status.errorMessage}"/></font>
        </td>
      </spring:bind>
<br></br>
<input type="submit" alignment="center" value="Execute">       
</form>
<a href="<c:url value="home.jsp"/>">Home</a>
</body>
</html>


