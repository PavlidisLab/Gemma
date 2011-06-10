<%@ include file="/common/taglibs.jsp"%>

<jwr:script src='/scripts/ajax/ext/data/DwrProxy.js' />
<jwr:script src='/scripts/app/AnalysisResultsSearch.js' />

<script type="text/javascript">
Ext.BLANK_IMAGE_URL = '/Gemma/images/default/s.gif';
Ext.onReady(function() {
	Ext.QuickTips.init();
	Ext.state.Manager.setProvider(new Ext.state.CookieProvider());
});
function hideTopBar(divID) {
  refID = document.getElementById(divID);
	refID.style.display = "none";
	// adjust other elements on the page
	// leaving this hacky because we'll probably get rid of it soon
	// resize header
	document.getElementById('homeheaderclear').style.height = '160px';
	// move menu bar
	document.getElementById('nav').style.top = '-85px';
	
	}
</script>

		<div id="topBarUpdates" style="
		background-color: #FFFFCC;
border: 1px solid #FFCC66;
float: left;
margin-left: 150px;
margin-top: -70px;
padding: 5px;
width: 84%;
text-align:center;">
			Welcome to Gemma 2.0!
			<a style="float:right" href="JavaScript:void(0)" onclick="hideTopBar('topBarUpdates');">
			<img src="/Gemma/images/icons/cross.png"/></a>
			<br/>
			<span style="font-size: 0.9em"></>Check out our <u><a target="_blank"
			href="http://www.chibi.ubc.ca/faculty/pavlidis/wiki/display/gemma/All+news">new features</a></u>, including 
			a differential expression visualizer, experiment batch effect analysis and a new search interface.</span>
		</div>	
<div align="center">
	<div style="width: 900px">

		<div align="center">
			<div id="analysis-results-search-form-warnings" align="left"></div>
			<div id="analysis-results-search-form" align="center"></div>
			<br>
			<div id="analysis-results-search-form-messages" align="left"></div>
			<div id="analysis-results-search-form-results" align="left"></div>
		</div>
	</div>


	<div id="meta-heatmap-div" align="left"></div>


	<div style="width: 900px">
		<br>

		<table id="frontPageContent" align="left" style="text-align: left">
			<tr>
				<td
					style="font-size: 0.9em; vertical-align: top; padding-right: 10px">


					<div id="left-bar-messages">
						<h2>
							Welcome!
						</h2>
						<p style="font-size: 0.90em">
							Gemma is a database and software system for the
							<strong>meta-analysis of gene expression data</strong>. Gemma
							contains data from hundreds of public
							<a
								href="<c:url value="/expressionExperiment/showAllExpressionExperiments.html"/>">microarray
								data sets</a>, referencing hundreds of
							<a href="<c:url value="/bibRef/showAllEeBibRefs.html"/>">published
								papers</a>. Users can search, access and visualize
							<a href="<c:url value="/searchCoexpression.html"/>">coexpression</a>
							and
							<a href="<c:url value="/diff/diffExpressionSearch.html"/>">differential
								expression</a> results.
						</p>
						<p style="font-size: 0.9em">
							More information about the project is
							<a href="<c:url value="/static/about.html"/>">here</a>. Gemma
							also has a
							<a href="http://chibi.ubc.ca/faculty/pavlidis/wiki/display/gemma">Wiki</a>
							where you can read additional documentation, in addition to the
							in-line help.
						</p>
					</div>


					<%--Don't show this area if the user is logged in. --%>
					<security:authorize access="isAnonymous()">
						<div style="margin-bottom: 10px;">
							<div id="contact">
								<div>
									<strong>Get an account</strong>
									<p class="emphasized" style="font-size: 0.90em">
										Most features of Gemma are open to guests. However, to access
										some functionality, such as data upload, you'll need an
										account.
										<strong><a href="<c:url value="/register.html"/>">Sign
												up</a> </strong>, or
										<strong><a href="<c:url value="/login.jsp" />">log
												in</a> </strong> if you already have an account.
									</p>
								</div>
							</div>
						</div>
					</security:authorize>

					<security:authorize access="hasRole('GROUP_ADMIN')">
						<div id="adminFunctions"
							style="margin-bottom: 10px; font-size: 0.90em;">
							<strong> More administrative functions </strong>
							<ul class="compactList" style="padding-left: 15px;">
								<li>
									<a href='<c:url value="/admin/systemStats.html"/>'> System
										monitoring </a>
								</li>
								<li>
									<a href='<c:url value="/admin/indexer.html"/>'> <fmt:message
											key="menu.compassIndexer" /> </a>
								</li>
								<li>
									<a href='<c:url value="/admin/maintenanceMode.html" />'>Manage
										maintenance mode</a>
								</li>
								<li>
									<a href='<c:url value="/whatsnew/generateCache.html" />'>Update
										"What's New"</a>
								</li>
								<li>
									<a href='<c:url value="/admin/widgetTest.html"/>'>Widget
										test</a>
								</li>

							</ul>
						</div>
					</security:authorize>


				</td>
				<td>



					<!-- Box for summary of data in Gemma and summery of updates and new additions -->
					<div class="rightcolumn" style="font-size: smaller">

						<div class="roundedcornr_box_777249" style="margin-bottom: 15px;">
							<div class="roundedcornr_top_777249">
								<div></div>
							</div>
							<div class="roundedcornr_content_777249">
								<div style="font-size: small; padding-bottom: 5px;">
									<b> <a target="_blank"
										href="http://www.chibi.ubc.ca/faculty/pavlidis/wiki/display/gemma/All+news">Updates since 
											<c:out value="${ updateDate}" /> </a> </b>
								</div>
								<div id="dataSummary"
									style="margin-left: 15px; margin-right: 15px">
									<table style="white-space: nowrap">
										<tr>
											<td style="padding-right: 10px">
												<strong>Data Summary</strong>
											</td>
											<td style="padding-right: 10px" align="right">
												Total
											</td>
											<c:if test="${ drawUpdatedColumn}">
												<td align="right" style="padding-right: 10px">
													Updated
												</td>
											</c:if>
											<c:if test="${ drawNewColumn}">
												<td align="right">
													New
												</td>
											</c:if>
										</tr>
										<tr>
											<td style="padding-right: 10px">
												<a
													href='<c:url value="/expressionExperiment/showAllExpressionExperiments.html"/>'>
													Expression Experiments: </a>
											</td>
											<td align="right" style="padding-right: 10px">
												<b><c:out value="${ expressionExperimentCount}" /> </b>
											</td>
											<td align="right" style="padding-right: 10px">
												<b><c:out value="${ updatedExpressionExperimentCount}" />
												</b>&nbsp;&nbsp;
											</td>
											<td align="right">
												<b><c:out value="${ newExpressionExperimentCount}" /> </b>&nbsp;
											</td>
										</tr>
										<c:forEach var="taxon" items="${ taxonCount }">
											<tr>
												<td style="padding-right: 10px">
													&emsp;
													<a
														href='/Gemma/expressionExperiment/showAllExpressionExperiments.html?taxonId=<c:out value="${ taxon.key.id}" />'>
														<c:out value="${ taxon.key.scientificName}" /> </a>

												</td>
												<td align="right" style="padding-right: 10px">
													<c:out value="${ taxon.value}" />
												</td>
												<td align="right" style="padding-right: 10px">
													<c:out value="${ updatedPerTaxonCount[taxon.key]}" />
													&nbsp;&nbsp;
												</td>
												<td align="right">
													<c:out value="${ newPerTaxonCount[taxon.key]}" />
													&nbsp;
												</td>
											</tr>
										</c:forEach>
										<tr>
											<td style="padding-right: 10px">
												<a href='<c:url value="/arrays/showAllArrayDesigns.html"/>'>
													Array Designs: </a>
											</td>
											<td align="right" style="padding-right: 10px">
												<b><c:out value="${ stats.arrayDesignCount }" /> </b>
											</td>
											<td align="right" style="padding-right: 10px">
												<b><c:out value="${ stats.updatedArrayDesignCount}" />
												</b>&nbsp;&nbsp;
											</td>
											<td align="right">
												<b><c:out value="${ stats.newArrayDesignCount}" /> </b>&nbsp;
											</td>
										</tr>
										<tr>
											<td style="padding-right: 10px">
												Assays:
											</td>
											<td align="right" style="padding-right: 10px">
												<b><c:out value="${ stats.bioAssayCount }" /> </b>
											</td>
											<td align="right" style="padding-right: 10px">
												&nbsp;&nbsp;
											</td>
											<td align="right">
												<b><c:out value="${ stats.newBioAssayCount}" /> </b>&nbsp;
											</td>
										</tr>
									</table>
								</div>
							</div>
							<div class="roundedcornr_bottom_777249">
								<div></div>
							</div>
						</div>




					</div>
					<!-- end of "rightcolumn" div  -->

				</td>
			</tr>
		</table>
	</div>
</div>