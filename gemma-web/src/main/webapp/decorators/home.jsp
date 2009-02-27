
<%-- Decorator for the home page --%>
<%-- $Id$ --%>

<%-- Include common set of tag library declarations for each layout --%>
<%@ include file="/common/taglibs.jsp"%>
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
	<head>
		<%-- Include common set of meta tags for each layout --%>
		<%@ include file="/common/meta.jsp"%>
		<title><decorator:title /> | <fmt:message key="webapp.name" /></title>

		<script type='text/javascript' src='/Gemma/dwr/engine.js'></script>
		<script type='text/javascript' src='/Gemma/dwr/util.js'></script>

		<jwr:style src="/bundles/gemma-all.css" />
		<jwr:script src="/bundles/include.js" />
		<jwr:script src="/bundles/gemma-lib.js" />

		<decorator:head />
	</head>
	<body <decorator:getProperty property="body.id" writeEntireProperty="true"/>
		<decorator:getProperty property="body.class" writeEntireProperty="true"/>>

		<div id="page">

			<div id="homeheader" class="clearfix">
				<jsp:include page="/common/header.jsp" />
			</div>

			<div id="content" class="clearfix">

				<div id="main" class="home">
					<%@ include file="/common/messages.jsp"%>
					<!-- 
					<h1>
						<decorator:getProperty property="page.heading" />
					</h1>-->
					<decorator:body />
				</div>

				<div id="sub" class="home">
					<%@ include file="/common/subarea.jsp"%>
				</div>


				<c:set var="currentMenu" scope="request">
					<decorator:getProperty property="meta.menu" />
				</c:set>
				<c:if test="${currentMenu == 'AdminMenu'}">
					<menu:useMenuDisplayer name="Velocity" config="WEB-INF/classes/cssVerticalMenu.vm" permissions="rolesAdapter">
						<menu:displayMenu name="AdminMenu" />
					</menu:useMenuDisplayer>
				</c:if>
				<div id="nav">
					<div class="wrapper">
						<h2 class="accessibility">
							Navigation
						</h2>
						<jsp:include page="/WEB-INF/pages/menu.jsp" />
					</div>
					<hr />
				</div>


				<%-- end nav --%>



			</div>
			<%-- end content --%>

			<div id="footer" class="clearfix">
				<jsp:include page="/common/footer.jsp" />
			</div>
		</div>
<c:if test='${ appConfig["ga.tracker"] != null}'>
	<script type="text/javascript">
	var gaJsHost = (("https:" == document.location.protocol) ? "https://ssl." : "http://www.");
		document.write(unescape("%3Cscript src='"
					+ gaJsHost
					+ "google-analytics.com/ga.js' type='text/javascript'%3E%3C/script%3E"));

	var pageTracker = _gat._getTracker('${appConfig["ga.tracker"]}');
	pageTracker._initData();
	pageTracker._trackPageview();
	</script>
</c:if>
	</body>
</html>

