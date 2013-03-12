<%@ include file="/common/taglibs.jsp"%>

<div id="divider">
	<div></div>
</div>
<div class="footer">

	<span class="left"><a href="http://www.ubc.ca"><img style="padding: 0 20px"
				src="<c:url value="/images/logo/logo_ubc.jpg"/>" /> </a> </span>
	<span class="left">Centre for High-Throughput Biology&nbsp;|&nbsp;Copyright &copy; 2007-2011 &nbsp;</span>

	<c:if test="${not empty pageContext.request.remoteUser}">
		<span class="right"> <div id="footer-login-status"><fmt:message key="user.status" /> <security:authentication property="principal.username" /></div>
			| <a id="footer-login-link" href="<c:url value="/j_spring_security_logout"/>"> <fmt:message key="user.logout" /> </a> </span>

	</c:if>
	<span class="left"><a href='<c:url value="/static/termsAndConditions.html" />'>Terms and conditions</a> </span>
	<c:if test="${empty pageContext.request.remoteUser}">
		<span class="right"><div id="footer-login-status"></div> | <a id="footer-login-link" title="Login is not needed for use of Gemma" href="<c:url value="/login.jsp"/>"
			class="current"><fmt:message key="login.title" /> </a> </span>
	</c:if>

	<security:authorize access="hasRole('GROUP_ADMIN')">
		<span class="right"> <a href="<c:url value="/admin/activeUsers.html"/>"><fmt:message
					key="mainMenu.activeUsers" /> </a>:&nbsp;<c:out value="${applicationScope.activeUsers}" /> &nbsp;Signed in:&nbsp;<span
			id="auth-user-count">?</span>&nbsp;|&nbsp;</span>
		<script type="text/javascript">
			Ext.onReady(function() {
				SecurityController.getAuthenticatedUserCount( function(count) {
					Ext.DomHelper.overwrite('auth-user-count', '' + count);
					} );
				 });
			</script>
	</security:authorize>

	<br />
	<security:authorize access="hasRole('GROUP_ADMIN')">
		Gemma version ${appConfig['version']}&nbsp;|
		<script type="text/javascript">
	document.writeln("Page Loaded: " + document.lastModified);
</script>
		<Gemma:lastModified refFile="/WEB-INF/gemma-servlet.xml"></Gemma:lastModified>
	</security:authorize>
</div>
<%-- Security fields used in Java script calls to hide or display information on pages --%>
<security:authorize access="hasRole('GROUP_ADMIN')">
	<input type="hidden" name="hasAdmin" id="hasAdmin" value="true" />
</security:authorize>
<security:authorize access="!hasRole('GROUP_ADMIN')">
	<input type="hidden" name="hasAdmin" id="hasAdmin" value="" />
</security:authorize>
<security:authorize access="hasRole('GROUP_USER')">
	<input type="hidden" name="hasUser" id="hasUser" value="true" />
</security:authorize>
<security:authorize access="!hasRole('GROUP_USER')">
	<input type="hidden" name="hasUser" id="hasUser" value="" />
</security:authorize>
<security:authorize ifAnyGranted="GROUP_USER,GROUP_ADMIN">
	<input type="hidden" name="loggedIn" id="loggedIn" value="true" />
</security:authorize>
<security:authorize ifNotGranted="GROUP_USER,GROUP_ADMIN">
	<input type="hidden" name="loggedIn" id="loggedIn" value="" />
</security:authorize>

<c:if test='${ appConfig["ga.tracker"] != null}'>
	 
	<script type="text/javascript">
	  var _gaq = _gaq || [];
	  _gaq.push(['_setAccount', '${appConfig["ga.tracker"]}']);
	  _gaq.push(['_setDomainName', '${appConfig["ga.domain"]}']);
	  _gaq.push(['_trackPageview']);

	  (function() {
	    var ga = document.createElement('script'); ga.type = 'text/javascript'; ga.async = true;
	    ga.src = ('https:' == document.location.protocol ? 'https://ssl' : 'http://www') + '.google-analytics.com/ga.js';
	    var s = document.getElementsByTagName('script')[0]; s.parentNode.insertBefore(ga, s);
	  })();
</script>
</c:if>
