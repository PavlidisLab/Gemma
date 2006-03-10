<%-- $Id$  --%>
<%@ include file="/common/taglibs.jsp"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">

<html>
<head>
<title>Search Gemma for a PubMed reference</title>
</head>

<body>
<h2>Search Gemma for a PubMed reference</h2>

<%-- This is not _really_ treated as a "proper" form: we don't use spring:bind. Instead the parameter is passed
in the URL. This makes it simple to bookmark these URLs, but it also means that some of our actions must
know how to look for the 'accession' parameter as well as deal with a bound bibliographicReference. --%>

<form action=<c:url value="/bibRef/searchBibRef.html"/> method="get"><input
    type="text" name="accession"> <input type="submit"></form>
<hr />

<%-- This link/button takes the user to the 'view all' page (pubMed.GetAll.results.view.jsp) --%>

<DIV align="left"><INPUT type="button"
    onclick="location.href='showAllBibRef.html'"
    value="View all references"></DIV>
<hr />

<%-- This link takes the user to the NCBI search flow page (pubMed.Search.criteria.view.jsp). --%>

<a href="<c:url value="/flowController.html?_flowId=pubMed.Search"/>"><fmt:message
    key="menu.flow.PubMedSearch" /></a>
</body>
</html>
