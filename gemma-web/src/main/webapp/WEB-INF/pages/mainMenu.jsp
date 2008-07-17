<%@ include file="/common/taglibs.jsp"%>
<head>
	<title><fmt:message key="mainMenu.title" />
	</title>


	<script type="text/javascript">
	Ext.BLANK_IMAGE_URL = '/Gemma/images/default/s.gif';
   	Ext.onReady( function() {
   	Ext.QuickTips.init();
	Ext.state.Manager.setProvider( new Ext.state.CookieProvider() );
	
	// Coexpression form.
	var searchForm = new Gemma.CoexpressionSearchFormLite ( {
		renderTo : "coexpression-form"
	} ); 
   
  
  } );
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
		</div>
		<div class="roundedcornr_bottom_777249">
			<div></div>
		</div>
	</div>




	<c:if test="${whatsNew != null}">

		<div class="roundedcornr_box_777249" style="margin-bottom: 15px;">
			<div class="roundedcornr_top_777249">
				<div></div>
			</div>
			<div class="roundedcornr_content_777249">
				<div id="whatsNew">
					<Gemma:whatsNew whatsNew="${whatsNew}" />
				</div>
			</div>
			<div class="roundedcornr_bottom_777249">
				<div></div>
			</div>
		</div>



	</c:if>



	<authz:authorize ifAnyGranted="admin">
		<div id="adminFunctions" class="roundedcornr_box_777249" style="margin-bottom: 15px;">
			<div class="roundedcornr_top_777249">
				<div></div>
			</div>
			<div class="roundedcornr_content_777249">
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
			<div class="roundedcornr_bottom_777249">
				<div></div>
			</div>
		</div>



	</authz:authorize>

	<div class="roundedcornr_box_777249" style="margin-bottom: 10px;">
		<div class="roundedcornr_top_777249">
			<div></div>
		</div>
		<div class="roundedcornr_content_777249" id="contact">
			<div>
				<strong>Contacting us</strong>
				<p class="emphasized">
					To get emails about updates to the Gemma software, subscribe to the
					<a href="http://perutz.cmmt.ubc.ca/mailman/bioinformatics.ubc.ca/listinfo/gemma-announce">Gemma-announce mailing
						list</a>. Please send bug reports or feature requests
					<a href="mailto:gemma@bioinformatics.ubc.ca">here</a>.
				</p>
			</div>
		</div>
		<div class="roundedcornr_bottom_777249">
			<div></div>
		</div>
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
	<a href='<c:url value="/searchCoexpression.html?g=938103&amp;a=776" />'>Ddn</a>; mouse
	<a href='<c:url value="/searchCoexpression.html?g=598735&amp;s=3&amp;a=708" />'>Mapk3</a>
</div>


