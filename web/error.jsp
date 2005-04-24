<%@ page language="java" isErrorPage="true" %>
<%@ include file="/common/taglibs.jsp"%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
    <title><fmt:message key="errorPage.title"/></title>
    <link rel="stylesheet" type="text/css" media="all" 
        href="<c:url value="/styles/default.css"/>" /> 
</head>

<body>
<div id="screen">
    <div id="content">
    <h1><fmt:message key="errorPage.heading"/></h1>
    <%@ include file="/common/messages.jsp" %>
 <% if (exception != null) { %>
    <pre><% exception.printStackTrace(new java.io.PrintWriter(out)); %></pre>
 <% } else if ((Exception)request.getAttribute("javax.servlet.error.exception") != null) { %>
    <pre><% ((Exception)request.getAttribute("javax.servlet.error.exception"))
                           .printStackTrace(new java.io.PrintWriter(out)); %></pre>
 <% } %>
    </div>
</body>
<a href="<c:url value="home.jsp"/>">Home</a>
</html>
