<%@ include file="/common/taglibs.jsp"%>
<%@ include file="include.jsp" %>
<html>
<body>
<title><fmt:message key="error.heading"/></title>
<content tag="heading"><h1><fmt:message key="numberFormatError.title"/></h1></content>

<h4><i><fmt:message key="numberFormatError.message"></i></h4>
       		<fmt:param><c:url value="/"/></fmt:param>
	   </fmt:message>
<br></br>


<div id="screen">
    <div id="content">
 <% Exception exception = null; %>
      <pre><% exception.printStackTrace(new java.io.PrintWriter(out)); %></pre>
    </div>  

<a href="<c:url value="home.jsp"/>">Home</a>
</body>
</html>