<%@ include file="/common/taglibs.jsp"%>
<%-- $Id$ --%>

<html>
	<head>
		<title>Data Access Error</title>

	</head>
	<body>
		<content tag="heading">
		Data Access Failure
		</content>
		<p>
			<c:out value="${requestScope.exception.message}" />
		</p>



		<a href="mainMenu.html" onclick="history.back();return false">&#171;
			Back</a>
	</body>
</html>

