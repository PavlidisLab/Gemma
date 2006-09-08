<%@ include file="/common/taglibs.jsp"%>
<%@ page language="java" isErrorPage="true"%>
<%-- $Id$ --%>

<html>
	<head>
		<title>Data Access Error</title>

	</head>
	<body>
		<content tag="heading">
		Data Access Failure
		</content>

		<a href="mainMenu.html" onclick="history.back();return false">&#171; Back</a>

		<Gemma:exception exception="${requestScope.exception}" />


	</body>
</html>

