<%@ include file="/common/taglibs.jsp"%>

<div id="divider">
	<div></div>
</div>
<div class="footer">

	<span class="left"><a href="http://www.ubc.ca"><img
				style="padding: 0 20px"
				src="<c:url value="/images/logo/logo_ubc.jpg"/>" /> </a> </span>
	<span class="left">Centre for High-Throughput
		Biology&nbsp;|&nbsp;Copyright &copy; 2007-2009 &nbsp;</span>

	<c:if test="${pageContext.request.remoteUser != null}">
		<span class="right"> | <fmt:message key="user.status" /> <security:authentication
				property="principal.username" /> | <a
			href="<c:url value="/logout.html"/>"> <fmt:message
					key="user.logout" /> </a> </span>

	</c:if>
	<span class="left"><a
		href='<c:url value="/static/termsAndConditions.html" />'>Terms and
			conditions</a>
	</span>
	<c:if test="${empty pageContext.request.remoteUser}">
		<span class="right"> | <a
			title="Login is not needed for use of Gemma"
			href="<c:url value="/login.jsp"/>" class="current"><fmt:message
					key="login.title" /> </a> </span>
	</c:if>

	<c:if test="${applicationScope.userCounter != null}">
		<span class="right"> <security:authorize ifAllGranted="admin">
				<a href="<c:url value="/activeUsers.html"/>"><fmt:message
						key="mainMenu.activeUsers" /> </a>:
    </security:authorize> <security:authorize ifNotGranted="admin">
				<fmt:message key="mainMenu.activeUsers" />:
    </security:authorize> <c:if test="${userCounter >= 0}">
				<c:out value="${userCounter}" />
			</c:if> &nbsp; </span>
	</c:if>



	<br />
	<security:authorize ifAllGranted="admin">
		Gemma version ${appConfig['version']}&nbsp;|
		<script type="text/javascript"> document.writeln("Page Loaded: "+document.lastModified); </script>
		<Gemma:lastModified refFile="/WEB-INF/action-servlet.xml"></Gemma:lastModified>
	</security:authorize>
</div>

<c:if test='${ appConfig["ga.tracker"] != null}'>
	<script type="text/javascript">
	var gaJsHost = (("https:" == document.location.protocol) ? "https://ssl." : "http://www.");
	document.write(unescape("%3Cscript src='" + gaJsHost
			+ "google-analytics.com/ga.js' type='text/javascript'%3E%3C/script%3E"));
</script>
	<script type="text/javascript">
	try {
		var pageTracker = _gat._getTracker('${appConfig["ga.tracker"]}');
		pageTracker._trackPageview();
	} catch (err) {
	}
</script>


</c:if>

<%-- Google chrome frame check -- pops up a window asking the user to install it. See http://code.google.com/chrome/chromeframe/developers_guide.html --%>
<%-- <c:if test="${fn:contains(header['User-Agent'], 'MSIE')}">
			<script type="text/javascript" src="http://ajax.googleapis.com/ajax/libs/chrome-frame/1/CFInstall.min.js"></script>
<div id='placeholder'></div>
			<script>
	CFInstall.check( {
		node : "placeholder",
		destination : "http://www.google.com"
	});
</script>
		</c:if> --%>