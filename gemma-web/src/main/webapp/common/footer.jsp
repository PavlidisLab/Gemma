<%@ include file="/common/taglibs.jsp"%>

<div id="divider">
	<div></div>
</div>
<div class="footer">

	<span class="left"><a href="http://www.ubc.ca"><img style="padding: 0 20px"
				src="<c:url value="/styles/antisense/images/logo_ubc.jpg"/>" /> </a> </span>
	<span class="left">Centre for High-Throughput Biology&nbsp;|&nbsp;Copyright &copy; 2007-2008 &nbsp;|&nbsp;Gemma version ${appConfig["version"]} </span>
	
	<c:if test="${pageContext.request.remoteUser != null}">
		<span class="right"> | <fmt:message key="user.status" /> <authz:authentication operation="username" /> | <a
			href="<c:url value="/logout.html"/>"> <fmt:message key="user.logout" /> </a> </span>

	</c:if>
	<c:if test="${empty pageContext.request.remoteUser}">
		<span class="right"> | <a title="Login is not needed for use of Gemma" href="<c:url value="/login.jsp"/>"
			class="current"><fmt:message key="login.title" /> </a> </span>
	</c:if>

	<c:if test="${applicationScope.userCounter != null}">
		<span class="right"> <authz:authorize ifAllGranted="admin">
				<a href="<c:url value="/activeUsers.html"/>"><fmt:message key="mainMenu.activeUsers" /> </a>:
    </authz:authorize> <authz:authorize ifNotGranted="admin">
				<fmt:message key="mainMenu.activeUsers" />:
    </authz:authorize> <c:if test="${userCounter >= 0}">
				<c:out value="${userCounter}" />
			</c:if> &nbsp; </span>
	</c:if>

	<script src="<c:url value='/scripts/urchin.js' />" type="text/javascript">
</script>
	<script type="text/javascript">
_uacct = "UA-255601-1";
urchinTracker();
</script>
	<br />
	<authz:authorize ifAllGranted="admin">
		<script type="text/javascript"> document.writeln("Page Loaded: "+document.lastModified); </script>
		<Gemma:lastModified refFile="/WEB-INF/action-servlet.xml"></Gemma:lastModified>
	</authz:authorize>
</div>