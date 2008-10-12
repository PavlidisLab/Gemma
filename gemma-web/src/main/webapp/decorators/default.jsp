<%-- <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
    "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd"> --%>

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

		<script type='text/javascript' src='/Gemma/dwr/interface/AuditController.js'></script>
		<script type='text/javascript' src='/Gemma/dwr/interface/ArrayDesignController.js'></script>
		<script type='text/javascript' src='/Gemma/dwr/interface/BibliographicReferenceController.js'></script>
		<script type='text/javascript' src='/Gemma/dwr/interface/BioMaterialController.js'></script>
		<script type='text/javascript' src='/Gemma/dwr/interface/CharacteristicBrowserController.js'></script>
		<script type='text/javascript' src='/Gemma/dwr/interface/CompositeSequenceController.js'></script>
		<script type='text/javascript' src='/Gemma/dwr/interface/CustomCompassIndexController.js'></script>
		<script type='text/javascript' src='/Gemma/dwr/interface/DEDVController.js'></script>
		<script type='text/javascript' src='/Gemma/dwr/interface/DifferentialExpressionAnalysisController.js'></script>	
		<script type='text/javascript' src='/Gemma/dwr/interface/DifferentialExpressionSearchController.js'></script>
		<script type='text/javascript' src='/Gemma/dwr/interface/ProcessedExpressionDataVectorCreateController.js'></script>
		<script type='text/javascript' src='/Gemma/dwr/interface/ArrayDesignRepeatScanController.js'></script>
		<script type='text/javascript' src='/Gemma/dwr/interface/ExtCoexpressionSearchController.js'></script>
		<script type='text/javascript' src='/Gemma/dwr/interface/ExpressionDataFileUploadController.js'></script>
		<script type='text/javascript' src='/Gemma/dwr/interface/ExperimentalDesignController.js'></script>
		<script type='text/javascript' src='/Gemma/dwr/interface/ExpressionExperimentController.js'></script>
		<script type='text/javascript' src='/Gemma/dwr/interface/ExpressionExperimentDataFetchController.js'></script>
		<script type='text/javascript' src='/Gemma/dwr/interface/ExpressionExperimentLoadController.js'></script>
		<script type='text/javascript' src='/Gemma/dwr/interface/ExpressionExperimentSetController.js'></script>
		<script type='text/javascript' src='/Gemma/dwr/interface/FileUploadController.js'></script>
		<script type='text/javascript' src='/Gemma/dwr/interface/GeneController.js'></script>
		<script type='text/javascript' src='/Gemma/dwr/interface/GenePickerController.js'></script>
		<script type='text/javascript' src='/Gemma/dwr/interface/GeoBrowserService.js'></script>
		<script type='text/javascript' src='/Gemma/dwr/interface/HibernateMonitorController.js'></script>
		<script type='text/javascript' src='/Gemma/dwr/interface/MgedOntologyService.js'></script>
		<script type="text/javascript" src='/Gemma/dwr/interface/OntologyService.js'></script>
		<script type='text/javascript' src='/Gemma/dwr/interface/ProgressStatusService.js'></script>
		<script type='text/javascript' src='/Gemma/dwr/interface/SearchService.js'></script>
		<script type='text/javascript' src='/Gemma/dwr/interface/TaskCompletionController.js'></script>

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

					<%
					    //Adds the page help link if not a help page already
					    String pageUri = request.getRequestURI();
					    if ( pageUri != null && !pageUri.toLowerCase().contains( "_help" )
					            && !pageUri.toLowerCase().contains( "static" ) ) {
					%>
					<div id="help" style="font-size: smaller; float: right;">
						<a target="_blank"
							href="
	<%
		String helpuri = pageUri.substring(0,pageUri.length() - 5) + "_help.html";
		helpuri = helpuri.replace("Gemma/", "Gemma/static/");
		out.print(helpuri );
 	%>
		">page
							help</a>
					</div>
					<%
					    }
					%>

					<h2>
						<decorator:getProperty property="page.heading" />
					</h2>



					<decorator:body />
				</div>


				<c:set var="currentMenu" scope="request">
					<decorator:getProperty property="meta.menu" />
				</c:set>
				<c:if test="${currentMenu == 'AdminMenu'}">
					<div id="sub">
						<menu:useMenuDisplayer name="Velocity" config="WEB-INF/classes/cssVerticalMenu.vm" permissions="rolesAdapter">
							<menu:displayMenu name="AdminMenu" />
						</menu:useMenuDisplayer>
					</div>
				</c:if>


				<div id="nav">
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
		<script type="text/javascript">
var gaJsHost = (("https:" == document.location.protocol) ? "https://ssl." : "http://www.");
document.write(unescape("%3Cscript src='" + gaJsHost + "google-analytics.com/ga.js' type='text/javascript'%3E%3C/script%3E"));
</script>
		<script type="text/javascript">
var pageTracker = _gat._getTracker('${appConfig["ga.tracker"]}');
pageTracker._initData();
pageTracker._trackPageview();
</script>
	</body>
</html>

