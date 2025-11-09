<%@ include file="/WEB-INF/common/taglibs.jsp" %>
<jsp:useBean id="appConfig" scope="application" type="java.util.Map" />
<head>
<title><fmt:message key="activeUsers.title" /></title>
<Gemma:script src='/scripts/app/jobmonitoring.js' />
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
           SecurityController.getAuthenticatedUserCount( function( count ) {
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
        Gemma version ${appConfig["gemma.version"] != null ? appConfig["gemma.version"] : "?"}
        <c:if test="${appConfig['gemma.build.timestamp'] != null or appConfig['gemma.build.gitHash'] != null}">
            built
        </c:if>
        <c:if test="${appConfig['gemma.build.timestamp'] != null}">
            on ${appConfig["gemma.build.timestamp"]}
        </c:if>
        <c:if test="${appConfig['gemma.build.gitHash'] != null}">
            from <a href="https://github.com/PavlidisLab/Gemma/commits/${appConfig['gemma.build.gitHash']}"
                target="_blank" rel="noopener noreferrer">${appConfig["gemma.build.gitHash"]}</a>
        </c:if>
        <script type="text/javascript">
        document.writeln( "Page Loaded: " + document.lastModified );
        </script>

    </security:authorize>
</div>