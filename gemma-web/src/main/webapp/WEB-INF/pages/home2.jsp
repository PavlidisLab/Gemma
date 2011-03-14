
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

<div id="main" style="text-align:left">
	<%@ include file="/common/messages.jsp"%>
</div>
	
<div align="center">
	<div style="width:900px">
		<%@ include file="/WEB-INF/pages/frontPageContent.jsp"%>
	</div>
</div>
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
