<%@ include file="/common/taglibs.jsp"%>
<head>
	<title><fmt:message key="mainMenu.title" />
	</title>


	<script type="text/javascript">
	Ext.BLANK_IMAGE_URL = '/Gemma/images/default/s.gif';
	Ext.onReady( function() {
		Ext.QuickTips.init();
		Ext.state.Manager.setProvider(new Ext.state.CookieProvider());

		// Coexpression form.
			var searchForm = new Gemma.CoexpressionSearchFormLite( {
				renderTo :"coexpression-form"
			});

			var feed = new Gemma.NewsDisplay( {
				renderTo :"newsfeed"
			});

		});
</script>

</head>


<div class="rightcolumn" style="width: 200px; float: right; font-size: smaller">

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


	<div id="coexpression-area">
		<div id="coexpression-messages" style="font-size: smaller; width: 200px;">
			<h3>
				Coexpression query
			</h3>
		</div>

		<div id="coexpression-form"></div>

		<div id="sampleQueries" style="padding: 4px; width: 200px; margin-bottom: 6px;">
			Examples: rat
			<a href='<c:url value="/searchCoexpression.html?g=938103&amp;a=776" />'>Ddn</a>; mouse
			<a href='<c:url value="/searchCoexpression.html?g=598735&amp;s=3&amp;a=708" />'>Mapk3</a>
		</div>
	</div>


	<security:authorize ifAnyGranted="admin">
		<div id="adminFunctions" class="roundedcornr_box_777249" style="margin-bottom: 15px;">
			<div class="roundedcornr_top_777249">
				<div></div>
			</div>
			<div class="roundedcornr_content_777249">
				<strong> More administrative functions </strong>
				<ul class="compactList" style="padding-left: 4px;">
					<li>
						<a href='<c:url value="/systemStats.html"/>'> System monitoring </a>
					</li>
					<li>
						<a href='<c:url value="/indexer.html"/>'> <fmt:message key="menu.compassIndexer" /> </a>
					</li>
					<li>
						<a href='<c:url value="/securityManager.html"/>'> <fmt:message key="menu.securityManager" /> </a>
					</li>
					<li>
						<a href='<c:url value="/expressionExperimentSetManager.html" />'>Manage gene sets</a>
					</li>
					<li>
						<a href='<c:url value="/maintenanceMode.html" />'>Manage maintenance mode</a>
					</li>
					<li>
						<a href='<c:url value="/whatsnew/generateCache.html" />'>Regenerate What's New Cache</a>
					</li>
					<li>
						<a href='<c:url value="/arrayDesign/associateSequences.html"/>'> <fmt:message
								key="menu.arrayDesignSequenceAdd" /> </a>
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
			<div class="roundedcornr_bottom_777249">
				<div></div>
			</div>
		</div>



	</security:authorize>

</div>


<div id="news-wrapper" style="width: 440; margin-top: 10px">
	<span style="font-size: 1.3em">News</span>
	<div style="padding: 0 4 2 0px; margin: 0px;" id="newsfeed">
	</div>
	<a style="font-size: 0.90em" href="http://bioinformatics.ubc.ca/confluence/display/gemma/Gemma+blog">More news</a>
</div>
