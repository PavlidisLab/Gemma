<%@ include file="/common/taglibs.jsp"%>
<head>
<title><fmt:message key="activeUsers.title" /></title>


<jwr:script src='/scripts/api/ext/data/DwrProxy.js' />
<jwr:script src='/scripts/app/jobmonitoring.js' />

</head>

<div class="padded">

	<h2>Tasks</h2>
	<div id="submittedTasks"></div>


	<hr />


	<h2>
		<fmt:message key="activeUsers.heading" />
	</h2>
	<security:authorize access="hasAuthority('GROUP_ADMIN')">
		<span class="right"><fmt:message key="mainMenu.activeUsers" />
			:&nbsp;<c:out value="${applicationScope.activeUsers}" />
			<br> Signed in:&nbsp;<span id="auth-user-count">?</span></span>
		<script type="text/javascript">
         Ext.onReady( function() {
            SecurityController.getAuthenticatedUserCount( function(count) {
               if ( Ext.get( 'auth-user-count' ) ) {
                  Ext.DomHelper.overwrite( 'auth-user-count', '' + count );
               }
            } );
         } );
      </script>
	</security:authorize>
	<br>
	<p>FIXME table of authenticated users should go here.</p>

	<h2>System Stats</h2>
	<security:authorize access="hasAuthority('GROUP_ADMIN')">
		Gemma version ${appConfig['gemma.version']} built on ${appConfig['gemma.build.timestamp']} from ${appConfig['gemma.build.gitHash']}&nbsp;<br>
		<script type="text/javascript">
         document.writeln( "Page Loaded: " + document.lastModified );
      </script>

	</security:authorize>
	<%-- Security fields used in Java script calls to hide or display information on pages: MOVED TO default.jsp --%>

	<c:if test='${ appConfig["ga.tracker"] != null}'>
		<script type="text/javascript">
         var gaJsHost = (("https:" == document.location.protocol) ? "https://ssl." : "http://www.");
         document.write( unescape( "%3Cscript src='" + gaJsHost
            + "google-analytics.com/ga.js' type='text/javascript'%3E%3C/script%3E" ) );
      </script>
		<script type="text/javascript">
         try {
            var pageTracker = _gat._getTracker( '${appConfig["ga.tracker"]}' );
            pageTracker._trackPageview();
         } catch (err) {
         }
      </script>
	</c:if>

</div>