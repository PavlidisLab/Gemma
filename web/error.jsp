<%@ page language="java" isErrorPage="true" %>
<%@ include file="/common/taglibs.jsp"%>
<page:applyDecorator name="default">
<head><title>Error</title></head>
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
</page:applyDecorator>
