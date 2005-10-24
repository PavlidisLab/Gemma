<%-- $Id$  --%>
<%@ include file="/common/taglibs.jsp"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">

<html>
<head>
<title>Search Gemma for a PubMed reference</title>
</head>

<body>
<h2>Search Gemma for a PubMed reference</h2>

<form action=<c:url value="/bibRef/searchBibRef.html"/> method="get"><input
    type="text" name="accession"> <input type="submit"></form>
<hr />
<DIV align="left"><INPUT type="button"
    onclick="location.href='showAllBibRef.html'"
    value="View all references"></DIV>
<hr />
<a href="<c:url value="/flowController.htm?_flowId=pubMed.Search"/>"><fmt:message
    key="menu.flow.PubMedSearch" /></a>
</body>
</html>
