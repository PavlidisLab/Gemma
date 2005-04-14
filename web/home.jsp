<%@ include file="WEB-INF/pages/include.jsp" %>

<%@ page contentType="text/html; charset=iso-8859-1" language="java" import="java.sql.*" errorPage="" %>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head><title>Gemma</title><meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1"></head>
<body bgcolor="#CCCC99">
<%--<img src="bigicon.gif" width="88" height="97"> <br>--%>
<h1>Options</h1>
<form method="post">
<form name="form1" method="post" action="">
<%--
<a href="<c:url value="bulkLoadForm.htm"/>">Load Database</a><br><br>
<a href="<c:url value="pubMedForm.htm"/>">Search PubMed By Id</a><br><br>
<a href="<c:url value="pubMedArticleListForm.htm"/>">List Articles</a><br><br>
--%>
<a href="<c:url value=""/>">Load Database</a><br><br>
<a href="<c:url value=""/>">Search PubMed By Id</a><br><br>
<a href="<c:url value=""/>">List Articles</a><br><br>

<a href="<c:url value="pubMedSearch.htm"/>">PubMed Search</a><br><br>
</body>
</html>