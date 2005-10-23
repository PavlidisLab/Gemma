<%@ include file="/common/taglibs.jsp"%>
<html>
<head>
<title>Data Access Error</title>

</head>
<body>
<content tag="heading">
Data Access Failure
</content>
<p><c:out value="${requestScope.exception.message}" /></p>

<!--
<% 
Exception ex = (Exception) request.getAttribute("exception");
ex.printStackTrace(new java.io.PrintWriter(out)); 
%>
-->

<a href="mainMenu.html" onclick="history.back();return false">&#171;
Back</a>
</body>
</html>
