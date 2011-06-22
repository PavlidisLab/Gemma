<!DOCTYPE html>
<%-- Include common set of tag library declarations for each layout --%>
<%@ include file="/common/taglibs.jsp"%>
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
	<head>
		<%-- Include common set of meta tags for each layout --%>
		<%@ include file="/common/meta.jsp"%>
		<title><decorator:title /> | <fmt:message key="webapp.name" />
		</title>

		<jwr:style src="/bundles/gemma-all.css" />
		<script type='text/javascript' src='/Gemma/dwr/engine.js'></script>
		<script type='text/javascript' src='/Gemma/dwr/util.js'></script>

		<jwr:script src="/bundles/include.js" />
		<jwr:script src="/bundles/gemma-all.js" />

		<decorator:head />
	</head>
	<body <decorator:getProperty property="body.id" writeEntireProperty="true"/>
		<decorator:getProperty property="body.class" writeEntireProperty="true"/>>


		<div id="page">

			<div id="header" class="clearfix">
				<jsp:include page="/common/header.inner.jsp" />
			</div>

			<div id="content" class="clearfix">



				<div id="main">
					<%@ include file="/common/messages.jsp"%>
					<%@ include file="/common/helpLink.jsp"%>

					<h2>
						<decorator:getProperty property="page.heading" />
					</h2>



					<decorator:body />
				</div>

				<div id="nav" style="z-index: 1">
					<!--  make sure menus are not in front of other things  -->
					<div class="wrapper">
						<h2 class="accessibility">
							Navigation
						</h2>
						<jsp:include page="/WEB-INF/pages/menu.jsp" />
					</div>
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

