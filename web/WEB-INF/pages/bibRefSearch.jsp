<%-- $Id  --%>
<%@ include file="/common/taglibs.jsp"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">

<html>
<head>

</head>

<body>
<h2>Search Gemma for a PubMed reference</h2>

<form action=<c:url value="/bibRef/searchBibRef.html"/> method="get"><input
    type="text" name="pubMedId" value="Enter PubMed Id" /> <input
    type="submit" /></form>
<hr />

</body>
</html>
