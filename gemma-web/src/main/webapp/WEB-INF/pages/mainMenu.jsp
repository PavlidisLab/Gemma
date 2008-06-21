<%@ include file="/common/taglibs.jsp"%>
<head>
	<title><fmt:message key="mainMenu.title" /></title>


	<script type="text/javascript">
	Ext.BLANK_IMAGE_URL = '/Gemma/images/default/s.gif';
   	Ext.onReady( function() {
   	Ext.QuickTips.init();
	Ext.state.Manager.setProvider( new Ext.state.CookieProvider() );
	
	// Coexpression form.
	var searchForm = new Gemma.CoexpressionSearchFormLite ( {
		renderTo : "coexpression-form"
	} ); 
   
   // rounded corners
      settings = {
          tl: { radius: 8 },
          tr: { radius: 8 },
          bl: { radius: 8 },
          br: { radius: 8 },
          antiAlias: true,
          autoPad: true,
          validTags: ["div"]
      }
     var datasumbox = new curvyCorners(settings, document.getElementById("dataSummary"));
     datasumbox.applyCornersToAll();
     
     if (Ext.get("whatsNew")) {
     	var box = new curvyCorners(settings, document.getElementById("whatsNew"));
     	box.applyCornersToAll();
     }
     
     if (Ext.get("adminFunctions")) {
     	var box = new curvyCorners(settings, document.getElementById("adminFunctions"));
     	box.applyCornersToAll();
     }
     
     if (Ext.get("contact")) {
     	var box = new curvyCorners(settings, document.getElementById("contact"));
     	box.applyCornersToAll();
     }
  } );
	</script>

</head>

<div class="rightcolumn" style="width: 200px; float: right; font-size: smaller">
	<div id="dataSummary" style="background: #D1D8F8; margin-bottom: 25px; padding: 8px;">
		<strong>Data Summary</strong>
		<table>
			<tr>
				<td>
					<a href='<c:url value="/expressionExperiment/showAllExpressionExperiments.html"/>'> Expression Experiments: </a>
				</td>
				<td align="right">
					<b><c:out value="${ expressionExperimentCount}" /> </b>
				</td>
			</tr>
			<c:forEach var="taxon" items="${ taxonCount }">
				<tr>
					<td>
						&emsp;
						<a href='/Gemma/expressionExperiment/showAllExpressionExperiments.html?taxonId=<c:out value="${ taxon.key.id}" />'>
							<c:out value="${ taxon.key.scientificName}" /> </a>

					</td>
					<td align="right">
						<c:out value="${ taxon.value}" />
					</td>
				</tr>
			</c:forEach>
			<tr>
				<td>
					<a href='<c:url value="/arrays/showAllArrayDesigns.html"/>'> Array Designs: </a>
				</td>
				<td align="right">
					<b><c:out value="${ stats.arrayDesignCount }" /> </b>
				</td>
			</tr>
			<tr>
				<td>
					Assays:
				</td>
				<td align="right">
					<b><c:out value="${ stats.bioAssayCount }" /> </b>
				</td>
			</tr>
		</table>
	</div>

	<c:if test="${whatsNew != null}">
		<div id="whatsNew" style="padding: 10px; margin-bottom: 25px; background: #D1D8F8">
			<Gemma:whatsNew whatsNew="${whatsNew}" />
		</div>
	</c:if>



	<authz:authorize ifAnyGranted="admin">
		<div id="adminFunctions" style="padding: 10px; background: #D1D8F8;">
			<strong> More administrative functions </strong>
			<ul class="compactList" style="padding-left: 3px;">

				<li>
					<a href='<c:url value="/indexer.html"/>'> <fmt:message key="menu.compassIndexer" /> </a>
				</li>
				<li>
					<a href='<c:url value="/securityManager.html"/>'> <fmt:message key="menu.securityManager" /> </a>
				</li>
				<li>
					<a href='<c:url value="/geneLinkAnalysisManager.html" />'>Manage gene link analyses</a> ("Canned" analyses)
				</li>
				<li>
					<a href='<c:url value="/maintenanceMode.html" />'>Manage maintenance mode</a>
				</li>
				<li>
					<a href='<c:url value="/whatsnew/generateCache.html" />'>Regenerate What's New Cache</a>
				</li>
				<li>
					<a href='<c:url value="loadSimpleExpressionExperiment.html"/>'> Load expression data from a tabbed file</a>
				</li>
				<li>
					<a href='<c:url value="/arrayDesign/associateSequences.html"/>'> <fmt:message key="menu.arrayDesignSequenceAdd" />
					</a>
				</li>
			</ul>
			<strong> Inactive, deprecated, or not ready for prime time </strong>
			<ul class="compactList" style="padding-left: 3px;">
				<li>
					<a href='<c:url value="/uploadFile.html"/>'> <fmt:message key="menu.selectFile" /> </a>
				</li>
				<li>
					<a href='<c:url value="/bibRefSearch.html"/>'> <fmt:message key="menu.flow.PubMedSearch" /> </a>
				</li>
			</ul>
		</div>
	</authz:authorize>


	<div id="contact" style="padding: 10px; background: #D1D8F8;">
		<h4 style="margin-left: 10px">
			Contacting us
		</h4>
		<p>
			To get emails about updates to the Gemma software, subscribe to the
			<a href="http://perutz.cmmt.ubc.ca/mailman/bioinformatics.ubc.ca/listinfo/gemma-announce">Gemma-announce mailing list</a>.
			Please send bug reports or feature requests
			<a href="mailto:gemma@bioinformatics.ubc.ca">here</a>.
		</p>
	</div>

</div>



<div id="coexpression-messages">
	<h3>
		Coexpression query
	</h3>
</div>

<div id="coexpression-form"></div>

<div id="sampleQueries" style="padding: 4px; width: 250px;">
	Examples: rat
	<a href='<c:url value="/searchCoexpression.html?g=938103&amp;a=706" />'>Ddn</a>; mouse
	<a href='<c:url value="/searchCoexpression.html?g=598735&amp;s=3&amp;a=708" />'>Mapk3</a>
</div>


