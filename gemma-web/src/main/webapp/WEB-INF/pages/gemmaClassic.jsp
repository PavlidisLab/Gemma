<%@ include file="/common/taglibs.jsp"%>
<title><fmt:message key="mainMenu.title" /></title>


<script type="text/javascript">
	Ext.BLANK_IMAGE_URL = '/Gemma/images/default/s.gif';
	Ext.onReady( function() {
		Ext.QuickTips.init();
		Ext.state.Manager.setProvider(new Ext.state.CookieProvider());

		// gene form.
			var searchForm = new Gemma.GeneSearch( {
				renderTo : "gene-query-form",
				stickyTaxon: false
			});

			var feed = new Gemma.NewsDisplay( {
				renderTo : "newsfeed"
			});

		});
</script>

<div class="rightcolumn" style="width: 265px; float: right; font-size: smaller">

	<div class="roundedcornr_box_777249" style="margin-bottom: 15px;">
		<div class="roundedcornr_top_777249">
			<div></div>
		</div>
		<div class="roundedcornr_content_777249">
			<div id="dataSummary">
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
								<a
									href='/Gemma/expressionExperiment/showAllExpressionExperiments.html?taxonId=<c:out value="${ taxon.key.id}" />'>
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
		</div>
		<div class="roundedcornr_bottom_777249">
			<div></div>
		</div>
	</div>


	<div id="news-wrapper" style="margin-top: 10px; font-size: 11px">
		<div style="padding: 0 4 2 0px; margin: 0px;" id="newsfeed">
		</div>
		<a style="font-size: 0.90em" href="http://www.chibi.ubc.ca/faculty/pavlidis/wiki/display/gemma/All+news">More news</a>
	</div>

	<security:authorize access="hasRole('GROUP_ADMIN')">

		<div id="adminFunctions" class="roundedcornr_box_777249" style="margin-bottom: 15px;">
			<div class="roundedcornr_top_777249">
				<div></div>
			</div>
			<div class="roundedcornr_content_777249">
				<strong> More administrative functions </strong>
				<ul class="compactList" style="padding-left: 4px;">
					<li>
						<a href='<c:url value="/admin/systemStats.html"/>'> System monitoring </a>
					</li>
					<li>
						<a href='<c:url value="/admin/indexer.html"/>'> <fmt:message key="menu.compassIndexer" /> </a>
					</li>
					<li>
						<a href='<c:url value="/admin/maintenanceMode.html" />'>Manage maintenance mode</a>
					</li>
					<li>
						<a href='<c:url value="/whatsnew/generateCache.html" />'>Update "What's New"</a>
					</li>
					<li>
						<a href='<c:url value="/admin/widgetTest.html"/>'>Widget test</a>
					</li>

				</ul>

			</div>
			<div class="roundedcornr_bottom_777249">
				<div></div>
			</div>
		</div>

	</security:authorize>


</div>

<div id="query-gemma">
	<div id="geneSearchMessages" style="font-size: smaller; width: 300px;">
	</div>
	<br>
	<br>
	<h3>
		Gene query
	</h3>
	<div id="gene-query-form"></div>

	<div id="sampleQueries" style="padding: 4px; width: 300px; margin-bottom: 6px;">
		Examples:
		<a href='<c:url value="/gene/showGene.html?ncbiid=14810"/>'>Grin1</a> (Mouse)
		<a href='<c:url value="gene/showGene.html?ncbiid=50689" />'>Mapk3</a> (Rat)
	</div>
</div>