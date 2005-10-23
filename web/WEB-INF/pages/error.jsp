<%@ page language="java" isErrorPage="true" %>
<%@ include file="/common/taglibs.jsp"%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">

<html>
<head>
    <title><fmt:message key="errorPage.title"/></title>
    <link rel="stylesheet" type="text/css" media="all" 
        href="<c:url value="/styles/default.css"/>" /> 
</head>

<body>

<div id="screen">
    <div id="content">
    <h2><fmt:message key="errorPage.heading"/></h2>
    <%@ include file="/common/messages.jsp" %>
 <% if (request.getAttribute("exception") != null) { %>
    <p><%=((Exception)request.getAttribute("exception")).getLocalizedMessage() %></p>
    <h3>Stack Trace:</h3>
    <pre><% ((Exception)request.getAttribute("exception")).printStackTrace(new java.io.PrintWriter(out)); %></pre>
 <% } else if (exception != null) { %> <%-- fixme: this is always null --%>
    <pre><% exception.printStackTrace(new java.io.PrintWriter(out)); %></pre>
 <% } else if ((Exception)request.getAttribute("javax.servlet.error.exception") != null) { %>
    <pre><% ((Exception)request.getAttribute("javax.servlet.error.exception"))
                           .printStackTrace(new java.io.PrintWriter(out)); %></pre>
 <% } else { %>
    <p>The error information could not be obtained.</p>
 <% } %>
    </div>
</body>
</html>
