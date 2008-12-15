<%@ include file="/common/taglibs.jsp"%>
<%@ page language="java" isErrorPage="true"%>

<title><fmt:message key="errorPage.title" />
</title>
<link rel="stylesheet" type="text/css" media="all" href="<c:url value='/styles/${appConfig["theme"]}/theme.css'/>" />

<h1>
	<fmt:message key="errorPage.heading" />
</h1>

<%@ include file="/common/messages.jsp"%>

<%
    if ( request.getAttribute( "exception" ) != null ) {
%>
<Gemma:exception exception="${exception}" />
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
