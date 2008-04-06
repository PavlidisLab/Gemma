<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
    "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<%-- Decorator for the home page --%>
<%-- $Id$ -->

<%-- Include common set of tag library declarations for each layout --%>
<%@ include file="/common/taglibs.jsp"%>
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
	<head>
		<%-- Include common set of meta tags for each layout --%>
		<%@ include file="/common/meta.jsp"%>
		<title><decorator:title /> | <fmt:message key="webapp.name" />
		</title>

		<link href="<c:url value='/styles/ext-all.css'/>" media="screen" rel="stylesheet" type="text/css" />
		<link rel="stylesheet" type="text/css" media="all" href="<c:url value='/styles/${appConfig["theme"]}/theme.css'/>" />
		<link rel="stylesheet" type="text/css" media="print" href="<c:url value='/styles/${appConfig["theme"]}/print.css'/>" />
		<!--[if gte IE 6]><link rel="stylesheet" type="text/css" media="screen" href="/styles/css/ie-standards.css" /><![endif]-->
		<script type="text/javascript" src="<c:url value='/scripts/helptip.js'/>"></script>
		<script type="text/javascript" src="<c:url value='/scripts/prototype.js'/>"></script>
		<script type="text/javascript" src="<c:url value='/scripts/scriptaculous/scriptaculous.js'/>"></script>
		<script type="text/javascript" src="<c:url value='/scripts/global.js'/>"></script>

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
					<h1>
						<decorator:getProperty property="page.heading" />
					</h1>
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
	</body>
</html>

