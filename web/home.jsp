<%@ include file="/common/taglibs.jsp"%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<page:applyDecorator name="default">
<html>
<head><title>Gemma</title></head>
<body>
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
<a href="<c:url value="pubMedSearch.htm"/>">PubMed Search</a><br><br>
<%--<a href="<c:url value="search.htm?&_flowId=bibRef.GetAll"/>">PubMed Search</a><br><br>--%>
</body>
</html>
</page:applyDecorator>