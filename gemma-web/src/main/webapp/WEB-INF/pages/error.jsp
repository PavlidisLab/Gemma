<%@ include file="/common/taglibs.jsp"%>
<%@ page language="java" isErrorPage="true"%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
    "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<html>
	<head>
		<title><fmt:message key="errorPage.title" /></title>
		<link rel="stylesheet" type="text/css" media="all" href="<c:url value='/styles/${appConfig["theme"]}/theme.css'/>" />
	</head>

	<body id="error">
		<div id="page">
			<div id="content" class="clearfix">
				<div id="main">
					<h1>
						<fmt:message key="errorPage.heading" />
					</h1>

					<a href="mainMenu.html" onclick="history.back();return false">&#171; Back</a>

					<%@ include file="/common/messages.jsp"%>

					<%
					if ( request.getAttribute( "exception" ) != null ) {
					%>
					<Gemma:exception exception="${pageContext.request.exception}" />
					<%
					} else if ( ( Exception ) request.getAttribute( "javax.servlet.error.exception" ) != null ) {
					%>
					<Gemma:exception exception="${pageContext.request.attribute['javax.servlet.error.exception']}" />
					<%
					} else if ( ( Exception ) request.getAttribute( "exception" ) != null ) {
					%>
					<Gemma:exception exception="${pageContext.request.attribute['exception']}" />
					<%
					} else {
					%>
					<p>
						<fmt:message key="errorPage.info.missing" />
					</p>
					<%
					}
					%>
				</div>
			</div>
		</div>
	</body>
</html>

