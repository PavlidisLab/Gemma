<%@ include file="/common/taglibs.jsp"%>
<title><fmt:message key="mainMenu.title" /></title>

		<jwr:script src='/scripts/ajax/ext/data/DwrProxy.js' />
		<jwr:script src='/scripts/app/AnalysisResultsSearch.js' />

<script type="text/javascript">
	Ext.BLANK_IMAGE_URL = '/Gemma/images/default/s.gif';
	Ext.onReady( function() {
		Ext.QuickTips.init();
		Ext.state.Manager.setProvider(new Ext.state.CookieProvider());
	});
</script>

	<div id="analysis-results-search-form" align="center" ></div>
	<br>
	<div id="analysis-results-search-form-messages" align="left" ></div>
	<div id="analysis-results-search-form-results" align="left" ></div>
	
<br>

<table id="frontPageContent" align="left" style="text-align: left">
<tr><td style="font-size:0.9em;vertical-align:top;padding-right:10px">


<div id="left-bar-messages">
	<h2>
		Welcome!
	</h2>
	<p style="font-size: 0.90em">
		Gemma is a database and software system for the
		<strong>meta-analysis of gene expression data</strong>. Gemma contains data from hundreds of public
		<a href="<c:url value="/expressionExperiment/showAllExpressionExperiments.html"/>">microarray data sets</a>,
		referencing hundreds of
		<a href="<c:url value="/bibRef/showAllEeBibRefs.html"/>">published papers</a>. Users can search, access and visualize
		<a href="<c:url value="/searchCoexpression.html"/>">coexpression</a> and
		<a href="<c:url value="/diff/diffExpressionSearch.html"/>">differential expression</a> results.
	</p>
	<p style="font-size: 0.9em">
		More information about the project is
		<a href="<c:url value="/static/about.html"/>">here</a>. Gemma also has a
		<a href="http://chibi.ubc.ca/faculty/pavlidis/wiki/display/gemma">Wiki</a> where you can read additional
		documentation, in addition to the in-line help.
	</p>
</div>


<%--Don't show this area if the user is logged in. --%>
<security:authorize access="isAnonymous()">
	<div style="margin-bottom: 10px;">
		<div id="contact">
			<div>
				<strong>Get an account</strong>
				<p class="emphasized" style="font-size: 0.90em">
					Most features of Gemma are open to guests. However, to access some functionality, such as data upload, you'll need
					an account.
					<strong><a href="<c:url value="/register.html"/>">Sign up</a> </strong>, or
					<strong><a href="<c:url value="/login.jsp" />">log in</a> </strong> if you already have an account.
				</p>
			</div>
		</div>
	</div>
</security:authorize>

<%--Don't show this area if the user is admin. --%>
<security:authorize access="!hasRole('GROUP_ADMIN')">

<!-- div style="margin-bottom: 10px;">
	<div id="contact">
			<strong>Contacting us</strong>
			<p class="emphasized" style="font-size: 0.90em">
				To get emails about updates to the Gemma software, subscribe to the
				<a href="http://lists.chibi.ubc.ca/mailman/listinfo/gemma-announce">Gemma-announce mailing list</a>. Please send bug
				reports or feature requests
				<a href="mailto:gemma@chibi.ubc.ca">here</a>.
			</p>
	</div>
</div-->
</security:authorize>

	<security:authorize access="hasRole('GROUP_ADMIN')">
		<div id="adminFunctions" style="margin-bottom: 10px;font-size: 0.90em;">
				<strong> More administrative functions </strong>
				<ul class="compactList" style="padding-left: 15px;">
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
	</security:authorize>


</td><td>
				

				
<!-- Box for summary of data in Gemma and summery of updates and new additions -->
<div class="rightcolumn" style="font-size: smaller">

	<div class="roundedcornr_box_777249" style="margin-bottom: 15px;">
		<div class="roundedcornr_top_777249">
			<div></div>
		</div>
		<div class="roundedcornr_content_777249">
		<div style="font-size: small; padding-bottom:5px;"><b>
<a target="_blank" href="http://www.chibi.ubc.ca/faculty/pavlidis/wiki/display/gemma/All+news">Updated: <c:out value="${ updateDate}" /></a></b></div>
			<div id="dataSummary" style="margin-left: 15px;margin-right: 15px">
				<table  style="white-space:nowrap">
					<tr>
						<td width="350px"><strong>Data Summary</strong></td>
						<td align="right" width="60px">Total</td>
						<c:if test="${ drawUpdatedColumn}">
							<td align="right" width="60px">Updated</td>
						</c:if>
						<c:if test="${ drawNewColumn}">
							<td align="right" width="40px">New</td>
						</c:if>
					</tr>
					<tr>
						<td width="350px">
							<a href='<c:url value="/expressionExperiment/showAllExpressionExperiments.html"/>'> Expression Experiments: </a>
						</td>
						<td align="right">
							<b><c:out value="${ expressionExperimentCount}" /> </b>
						</td>
						<td align="right">
							<b><c:out value="${ updatedExpressionExperimentCount}" /></b>&nbsp;&nbsp;
						</td>
						<td align="right">
							<b><c:out value="${ newExpressionExperimentCount}" /></b>&nbsp;&nbsp;
						</td>
					</tr>
					<c:forEach var="taxon" items="${ taxonCount }">
						<tr>
							<td width="350px">
								&emsp;
								<a
									href='/Gemma/expressionExperiment/showAllExpressionExperiments.html?taxonId=<c:out value="${ taxon.key.id}" />'>
									<c:out value="${ taxon.key.scientificName}" /> </a>

							</td>
							<td align="right">
								<c:out value="${ taxon.value}" />
							</td>
							<td align="right">
								<c:out value="${ updatedPerTaxonCount[taxon.key]}" />&nbsp;&nbsp;
							</td>
							<td align="right">
								<c:out value="${ newPerTaxonCount[taxon.key]}" />&nbsp;&nbsp;
							</td>
						</tr>
					</c:forEach>
					<tr>
						<td width="350px">
							<a href='<c:url value="/arrays/showAllArrayDesigns.html"/>'> Array Designs: </a>
						</td>
						<td align="right">
							<b><c:out value="${ stats.arrayDesignCount }" /></b>
						</td>
						<td align="right">
							<b><c:out value="${ stats.updatedArrayDesignCount}" /></b>&nbsp;&nbsp;
						</td>
						<td align="right">
							<b><c:out value="${ stats.newArrayDesignCount}" /></b>&nbsp;&nbsp;
						</td>
					</tr>
					<tr>
						<td width="350px">
							Assays:
						</td>
						<td align="right">
							<b><c:out value="${ stats.bioAssayCount }" /> </b>
						</td>
							<td align="right">
								&nbsp;&nbsp;
							</td>
							<td align="right">
								<b><c:out value="${ stats.newBioAssayCount}" /></b>&nbsp;&nbsp;
							</td>
					</tr>
				</table>
			</div>
		</div>
		<div class="roundedcornr_bottom_777249">
			<div></div>
		</div>
	</div>




</div> <!-- end of "rightcolumn" div  -->

</td></tr>
</table>

