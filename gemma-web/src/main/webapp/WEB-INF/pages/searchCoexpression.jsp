<%@ include file="/common/taglibs.jsp"%>

<jsp:useBean id="coexpressionSearchCommand" scope="request"
	class="ubic.gemma.web.controller.coexpressionSearch.CoexpressionSearchCommand" />

<spring:bind path="coexpressionSearchCommand.*">
	<c:if test="${not empty status.errorMessages}">
		<div class="error">
			<c:forEach var="error" items="${status.errorMessages}">
				<img src="<c:url value="/images/iconWarning.gif"/>" alt="<fmt:message key="icon.warning"/>" class="icon" />
				<c:out value="${error}" escapeXml="false" />
				<br />
			</c:forEach>
		</div>
	</c:if>
</spring:bind>

<c:if test="${coexpressedGenes == null}">
	<title>Coexpression Search</title>
	<h2>
		Search for coexpressed genes:
	</h2>
</c:if>

<c:if test="${coexpressedGenes != null}">
	<title>Coexpression search for <c:out value="${sourceGene.officialSymbol}" /> (<c:out
			value="${sourceGene.officialName}" />)</title>
	<h2>
		Results for
		<a href="/Gemma/gene/showGene.html?id=<c:out value="${sourceGene.id}" />"> <c:out
				value="${sourceGene.officialSymbol}" /> </a> (
		<c:out value="${sourceGene.officialName}" />
		)
		<br />
		with
		<c:out value="${numSourceGeneGoTerms}" />
		GO Terms
	</h2>
	<hr>
	<h4>

		<a
			href="/Gemma/searchCoexpression.html?
			searchString=${sourceGene.officialSymbol}&
			stringency=${ coexpressionSearchCommand.stringency}&
			taxon=${coexpressionSearchCommand.taxon.id}&
			eeSearchString=${coexpressionSearchCommand.eeSearchString}&
			exactSearch=on">
			(Bookmarkable link) </a>

	</h4>

	<table class="datasummary" style="float: right">
		<tr>
			<td colspan=2 align=center>
				<b> Search Summary </b>
			</td>
		</tr>
		<tr>
			<td>
				Datasets searched
			</td>
			<td>
				<c:out value="${numSearchedExpressionExperiments}" />
			</td>
		</tr>
		<tr>
			<td colspan=2>
				Links
			</td>
		</tr>
		<tr>
			<td>
				&nbsp;&nbsp;Found
			</td>
			<td>
				<c:out value="${numGenes}" />
			</td>
		</tr>
		<tr>
			<td>
				&nbsp;&nbsp;Met stringency (+)
			</td>
			<td>
				<c:out value="${numPositiveCoexpressedGenes}" />
			</td>
		</tr>
		<tr>
			<td>
				&nbsp;&nbsp;Met stringency (-)
			</td>
			<td>
				<c:out value="${numNegativeCoexpressedGenes}" />
			</td>
		</tr>
		<authz:authorize ifAnyGranted="admin">
			<tr>
				<td colspan=2>
					Predicted Genes
				</td>
			</tr>
			<tr>
				<td>
					&nbsp;&nbsp;Found
				</td>
				<td>
					<c:out value="${numPredictedGenes}" />
				</td>
			</tr>
			<tr>
				<td>
					&nbsp;&nbsp;Met stringency
				</td>
				<td>
					<c:out value="${numStringencyPredictedGenes}" />
				</td>
			</tr>
			<tr>
				<td colspan=2>
					Probe Aligned Regions
				</td>
			</tr>
			<tr>
				<td>
					&nbsp;&nbsp;Found
				</td>
				<td>
					<c:out value="${numProbeAlignedRegions}" />
				</td>
			</tr>
			<tr>
				<td>
					&nbsp;&nbsp;Met stringency
				</td>
				<td>
					<c:out value="${numStringencyProbeAlignedRegions}" />
				</td>
			</tr>
		</authz:authorize>
	</table>
</c:if>

<form method="post" name="coexpressionSearch" action="<c:url value="/searchCoexpression.html"/>">

	<table class='searchTable'>
		<tr>
			<td class='label' valign="top">
				<b> Gene Name </b>
			</td>
			<td>
				<!-- If there are genes defined, just show those genes in a pulldown menu. 
				Otherwise, show a text field
			 -->
				<c:if test="${genes != null}">
					<spring:bind path="coexpressionSearchCommand.searchString">
						<select id="searchStringPullDown" name="${status.expression}"
							onchange="var popup = document.getElementById('searchStringPullDown');
						if (popup.value == 'Refine Search') {
							popup.style.display='none';
							popup.disabled='true';
							var text = document.getElementById('searchStringTextInput');
							text.style.display='inline';
							text.disabled='';
							var checkbox = document.getElementById('exactSearchCheckbox');
							checkbox.checked='';
							checkbox.style.display='inline';
							document.getElementById('exactSearchLabel').style.display='inline';						
							document.getElementById('geneIdSearch').value = 'false';
						}
						">
							<option value="">
								[Select a gene or refine the search]
							</option>
							<option value="Refine Search">
								[Refine Search]
							</option>
							<c:forEach items="${genes}" var="gene">
								<option value="${gene.id}">
									${gene.officialSymbol} : ${gene.officialName }
								</option>
							</c:forEach>
						</select>

						<input id="searchStringTextInput" disabled style="display: none" type="text" size=15
							name="<c:out value="${status.expression}"/>" value="${status.value}" />
						<input id="geneIdSearch" style="display: none" type="text" size=15 name="geneIdSearch" value="true" />
					</spring:bind>
					<spring:bind path="coexpressionSearchCommand.exactSearch">
						<input style="display: none" id="exactSearchCheckbox" type="checkbox" name="${status.expression}"
							<c:if test="${status.value == 'on'}">checked="checked"</c:if> />
						<input type="hidden" name="_<c:out value="${status.expression}"/>">
					</spring:bind>
					<span id="exactSearchLabel" style="display: none"> Exact search </span>
				</c:if>



				<c:if test="${genes == null}">
					<spring:bind path="coexpressionSearchCommand.searchString">
						<input type="text" size=15 name="<c:out value="${status.expression}"/>"
							value="<c:out value="${sourceGene.officialSymbol}"/>" />
					</spring:bind>
					<spring:bind path="coexpressionSearchCommand.exactSearch">
						<input id="exactSearchCheckbox" type="checkbox" name="${status.expression}"
							<c:if test="${status.value == 'on'}">checked="checked"</c:if> />
						<input type="hidden" name="_<c:out value="${status.expression}"/>">
					</spring:bind>
					Exact search
				</c:if>
			</td>
			<td>
				<a class="helpLink" href="?"
					onclick="showHelpTip(event, 
				'Term(s) to identify a gene; check \'exact search\' if you know the official symbol for the gene.'); return false">
					<img src="/Gemma/images/help.png" /> </a>
			</td>
		</tr>

		<tr>
			<td class='label' valign="top">
				<b> Experiment keywords </b>
			</td>
			<td>
				<spring:bind path="coexpressionSearchCommand.eeSearchString">
					<input type="text" size=30 name="<c:out value="${status.expression}"/>" value="<c:out value="${status.value}"/>" />
				</spring:bind>
			</td>
			<td>
				<a class="helpLink" href="?"
					onclick="showHelpTip(event, 
				'keywords of experiments to use in the coexpression analysis'); return false">
					<img src="/Gemma/images/help.png" /> </a>
			</td>
		</tr>

		<tr>
			<td class='label' valign="top">
				<b> <fmt:message key="label.species" /> </b>
			</td>


			<td>
				<spring:bind path="coexpressionSearchCommand.taxon">
					<select name="${status.expression}">
						<c:forEach items="${taxa}" var="taxon">
							<spring:transform value="${taxon}" var="scientificName" />
							<option value="${scientificName}" <c:if test="${status.value == scientificName}">selected </c:if>>
								${scientificName}
							</option>
						</c:forEach>
					</select>

				</spring:bind>
			</td>
			<td>
				<a class="helpLink" href="?"
					onclick="showHelpTip(event, 
				'Species to use in the coexpression search'); return false"> <img
						src="/Gemma/images/help.png" /> </a>
			</td>
		</tr>

		<tr>
			<td class='label' valign="top">
				<b> <fmt:message key="label.stringency" /> </b>
			</td>
			<td>
				<spring:bind path="coexpressionSearchCommand.stringency">
					<input type="text" size=1 name="<c:out value="${status.expression}"/>" value="<c:out value="${status.value}"/>" />
				</spring:bind>
			</td>
			<td>
				<a class="helpLink" href="?"
					onclick="showHelpTip(event, 
				'The number of datasets (experiments) that coexpress the gene before it is considered a positive result'); return false">
					<img src="/Gemma/images/help.png" /> </a>
			</td>
		</tr>
		<tr>
			<td>
				&nbsp;
			</td>
			<td>
				<input type="submit" class="button" name="submit" value="<fmt:message key="button.search"/>" />
			</td>
			<td>
				&nbsp;
			</td>
		</tr>

	</table>
</form>

<c:if test="${coexpressedGenes != null}">
	<c:if test="${numLinkedExpressionExperiments != 0}">
		<div style="float: left; padding: 2px;" onclick="Effect.toggle('datasetList', 'blind', {duration:0.1})">
			<img src="/Gemma/images/plus.gif" />
		</div>
	</c:if>
	<div style="margin-left: 16px;">
		<b> <c:out value="${numUsedExpressionExperiments}" /> datasets have relevent coexpression data <br /> <c:out
				value="${numQuerySpecificEEs}" /> datasets have <c:out value="${sourceGene.officialSymbol}" /> probes with no
			detected cross-hybridization potential</b>
		<br />
	</div>
	<div id="datasetList" style="display: none">
		<%-- inner div needed by Effect.toggle  --%>
		<div>

			<display:table pagesize="200" name="expressionExperiments" sort="list" defaultsort="2" class="list" requestURI=""
				id="expressionExperimentList"
				decorator="ubic.gemma.web.taglib.displaytag.expression.experiment.ExpressionExperimentWrapper">

				<display:column property="rawCoexpressionLinkCount" sortable="true" defaultorder="descending"
					titleKey="expressionExperiment.rawLinkcount" />

				<display:column property="coexpressionLinkCount" sortable="true" defaultorder="ascending"
					titleKey="expressionExperiment.linkcount" />
				<display:column property="nameLink" sortable="true" sortProperty="name" titleKey="expressionExperiment.name" />
				<display:column property="shortName" sortable="true" titleKey="expressionExperiment.shortName" />
				<display:column property="specific" sortable="true" titleKey="expressionExperiment.specific" />
				<authz:authorize ifAnyGranted="admin">
					<display:column property="arrayDesignLink" sortable="true" title="Arrays" />
				</authz:authorize>
				<display:column property="assaysLink" sortable="true" titleKey="bioAssays.title" />
				<display:setProperty name="basic.empty.showtable" value="false" />
			</display:table>
		</div>
	</div>
</c:if>


<br />


<script>
			var text = '<Gemma:help helpFile="CoexpressionSearchSupportHelp.html"/>';
			function getCoexpressionHelp(event) {showWideHelpTip(event,text); }
	
			var text2 = '<Gemma:help helpFile="CoexpressionSearchOverlapHelp.html"/>';
			function getOverlapHelp(event) {showWideHelpTip(event,text2); }
			
			var text3 = '<Gemma:help helpFile="ExpressionExperimentBits.html"/>';
			function getExpBitsHelp(event) {showWideHelpTip(event,text3); }
			
			var text4 = 'Make a gene in the list the query';
			function getQueryHelp(event) {showWideHelpTip(event,text4); }
</script>

<div id="coexpressed-genes" style="width: 600px;">
	<!--  -->
	<display:table name="coexpressedGenes" export="true" class="list" sort="list" requestURI="" id="foundGenes"
		decorator="ubic.gemma.web.taglib.displaytag.coexpressionSearch.CoexpressionWrapper" pagesize="200">
		<display:column media="html" property="gemmaLink" sortable="false"
			title="<a class='helpLink' name='?' href='' onclick='getQueryHelp(event);return false;'><img src='/Gemma/images/help.png' /></a>" />
		<display:column media="html" property="nameLink" sortable="true" sortProperty="geneName" title="Gene" />
		<display:column media="excel" property="geneName" title="Gene" />
		<display:column property="geneOfficialName" maxLength="40" sortable="true" titleKey="gene.officialName" />
		<display:column media="html" property="linkCount" sortable="true" sortProperty="maxLinkCount" style="width:50px;"
			title="Support&nbsp;<a class='helpLink' href='' onclick='getCoexpressionHelp(event);return false;'><img src='/Gemma/images/help.png' /></a>" />
		<display:column media="excel" property="linkCountExport" title="Support" />
		<display:column media="html" property="goOverlap" sortable="true" style="width:50px;"
			title="GO&nbsp;<a class='helpLink' href='' onclick='getOverlapHelp(event);return false;'><img src='/Gemma/images/help.png' /></a>" />
		<display:column media="excel" property="goOverlap" title="GO Overlap" />
		<display:column media="html" property="experimentBitImage" sortable="false"
			title="Exprs&nbsp;<a class='helpLink' href='' onclick='getExpBitsHelp(event);return false;'><img src='/Gemma/images/help.png' /></a>" />
		<display:setProperty name="basic.empty.showtable" value="false" />
	</display:table>
</div>

<authz:authorize ifAnyGranted="admin">

	<!--  ================ Predicted Gene Results ================== -->

	<c:if test="${coexpressedPredictedGenes != null}">

		<c:if test="${numLinkedPredictedExpressionExperiments != 0}">

			<!-- Toggles for the expand/hide datasetList table -->
			<span class="predictedDatasetList" onclick="return toggleVisibility('predictedDatasetList')"> <img
					src="/Gemma/images/chart_organisation_add.png" /> </span>
			<span class="predictedDatasetList" style="display: none" onclick="return toggleVisibility('predictedDatasetList')">
				<img src="/Gemma/images/chart_organisation_delete.png" /> </span>
		</c:if>
		<b> <c:out value="${numUsedPredictedExpressionExperiments}" /> datasets that had relevent Predicted Gene
			Coexpression data <c:if test="${numLinkedPredictedExpressionExperiments != 0}">
				<a href="#" onclick="return toggleVisibility('predictedDatasetList')">(details)</a>
			</c:if> </b>

		<br />
		<div class="predictedDatasetList" style="display: none">

			<display:table pagesize="100" name="predictedExpressionExperiments" sort="list" defaultsort="2" class="list"
				requestURI="" id="predictedExpressionExperimentList"
				decorator="ubic.gemma.web.taglib.displaytag.expression.experiment.ExpressionExperimentWrapper">

				<display:column property="rawCoexpressionLinkCount" sortable="true" defaultorder="descending"
					titleKey="expressionExperiment.rawLinkcount" />

				<display:column property="coexpressionLinkCount" sortable="true" defaultorder="descending"
					titleKey="expressionExperiment.linkcount" />

				<display:column property="nameLink" sortable="true" sortProperty="name" titleKey="expressionExperiment.name" />

				<display:column property="shortName" sortable="true" titleKey="expressionExperiment.shortName" />

				<authz:authorize ifAnyGranted="admin">
					<display:column property="arrayDesignLink" sortable="true" title="Arrays" />
				</authz:authorize>


				<display:column property="assaysLink" sortable="true" titleKey="bioAssays.title" />

				<display:setProperty name="basic.empty.showtable" value="false" />
			</display:table>
		</div>
	</c:if>

	<br />


	<display:table name="coexpressedPredictedGenes" export="true" class="list" sort="list" requestURI="" id="predictedFoundGenes"
		decorator="ubic.gemma.web.taglib.displaytag.coexpressionSearch.CoexpressionWrapper" pagesize="200">
		<display:column media="html" property="gemmaLink" sortable="false"
			title="<a class='helpLink' name='?' href='' onclick='getQueryHelp(event);return false;'><img src='/Gemma/images/help.png' /></a>" />
		<display:column media="html" property="nameLink" sortable="true" sortProperty="geneName" title="Gene" />
		<display:column media="excel" property="geneName" title="Gene" />
		<display:column property="geneOfficialName" maxLength="40" sortable="true" titleKey="gene.officialName" />
		<display:column media="html" property="simpleLinkCount" sortable="true" sortProperty="maxLinkCount" style="width:50px;"
			title="Support&nbsp;<a class='helpLink' href='' onclick='getCoexpressionHelp(event);return false;'><img src='/Gemma/images/help.png' /></a>" />
		<display:column media="excel" property="simpleLinkCountExport" title="Support" />
		<display:column media="html" property="goOverlap" sortable="true" style="width:50px;"
			title="GO&nbsp;<a class='helpLink' href='' onclick='getOverlapHelp(event);return false;'><img src='/Gemma/images/help.png' /></a>" />
		<display:column media="excel" property="goOverlap" title="GO Overlap" />
		<display:column media="html" property="experimentBitImage" sortable="false"
			title="Exprs&nbsp;<a class='helpLink' href='' onclick='getExpBitsHelp(event);return false;'><img src='/Gemma/images/help.png' /></a>" />
		<display:setProperty name="basic.empty.showtable" value="false" />
	</display:table>


	<!--  ================ Probe Aligned Region Results ================== -->

	<c:if test="${coexpressedAlignedRegions != null}">

		<c:if test="${numLinkedAlignedExpressionExperiments != 0}">

			<!-- Toggles for the expand/hide datasetList table -->
			<span class="alignedDatasetList" onclick="return toggleVisibility('alignedDatasetList')"> <img
					src="/Gemma/images/chart_organisation_add.png" /> </span>
			<span class="alignedDatasetList" style="display: none" onclick="return toggleVisibility('alignedDatasetList')">
				<img src="/Gemma/images/chart_organisation_delete.png" /> </span>
		</c:if>
		<b> <c:out value="${numUsedAlignedExpressionExperiments}" /> datasets had relevent Probe Aligned Region
			Coexpression data <c:if test="${numLinkedAlignedExpressionExperiments != 0}">
				<a href="#" onclick="return toggleVisibility('alignedDatasetList')">(details)</a>
			</c:if> </b>

		<br />
		<div class="alignedDatasetList" style="display: none">

			<display:table pagesize="100" name="alignedExpressionExperiments" sort="list" defaultsort="2" class="list"
				requestURI="" id="alignedExpressionExperimentList"
				decorator="ubic.gemma.web.taglib.displaytag.expression.experiment.ExpressionExperimentWrapper">

				<display:column property="rawCoexpressionLinkCount" sortable="true" defaultorder="descending"
					titleKey="expressionExperiment.rawLinkcount" />

				<display:column property="coexpressionLinkCount" sortable="true" defaultorder="descending"
					titleKey="expressionExperiment.linkcount" />

				<display:column property="nameLink" sortable="true" sortProperty="name" titleKey="expressionExperiment.name" />

				<display:column property="shortName" sortable="true" titleKey="expressionExperiment.shortName" />

				<authz:authorize ifAnyGranted="admin">
					<display:column property="arrayDesignLink" sortable="true" title="Arrays" />
				</authz:authorize>


				<display:column property="assaysLink" sortable="true" titleKey="bioAssays.title" />

				<display:setProperty name="basic.empty.showtable" value="false" />
			</display:table>
		</div>
	</c:if>

	<br />


	<display:table name="coexpressedAlignedRegions" export="true" class="list" sort="list" requestURI="" id="alignedFoundGenes"
		decorator="ubic.gemma.web.taglib.displaytag.coexpressionSearch.CoexpressionWrapper" pagesize="200">
		<display:column media="html" property="gemmaLink" sortable="false"
			title="<a class='helpLink' name='?' href='' onclick='getQueryHelp(event);return false;'><img src='/Gemma/images/help.png' /></a>" />
		<display:column media="html" property="nameLink" sortable="true" sortProperty="geneName" title="Gene" />
		<display:column media="excel" property="geneName" title="Gene" />
		<display:column property="geneOfficialName" maxLength="40" sortable="true" titleKey="gene.officialName" />
		<display:column media="html" property="simpleLinkCount" sortable="true" sortProperty="maxLinkCount" style="width:50px;"
			title="Support&nbsp;<a class='helpLink' href='' onclick='getCoexpressionHelp(event);return false;'><img src='/Gemma/images/help.png' /></a>" />
		<display:column media="excel" property="simpleLinkCountExport" title="Support" />
		<display:column media="html" property="goOverlap" sortable="true" style="width:50px;"
			title="GO&nbsp;<a class='helpLink' href='' onclick='getOverlapHelp(event);return false;'><img src='/Gemma/images/help.png' /></a>" />
		<display:column media="excel" property="goOverlap" title="GO Overlap" />
		<display:column media="html" property="experimentBitImage" sortable="false"
			title="Exprs&nbsp;<a class='helpLink' href='' onclick='getExpBitsHelp(event);return false;'><img src='/Gemma/images/help.png' /></a>" />
		<display:setProperty name="basic.empty.showtable" value="false" />
	</display:table>
</authz:authorize>
