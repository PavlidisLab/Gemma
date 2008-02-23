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


<title><fmt:message key="mainMenu.title" /></title>



<table class="datasummary">
	<tr>
		<td colspan="2">
			<b>Data Summary</b>
		</td>
	</tr>
	<tr>
		<td>
			<a href="<c:url value="/expressionExperiment/showAllExpressionExperiments.html"/>"> Expression Experiments: </a>
		</td>
		<td align="right">
			<b><c:out value="${ expressionExperimentCount}" /> </b>
		</td>
	</tr>
	<c:forEach var="taxon" items="${ taxonCount }">
		<tr>
			<td>
				&emsp;
				<a href="/Gemma/expressionExperiment/showAllExpressionExperiments.html?taxonId=<c:out value="${ taxon.key.id}" />">
					<c:out value="${ taxon.key.scientificName}" /> </a>

			</td>
			<td align="right">
				<c:out value="${ taxon.value}" />
			</td>
		</tr>
	</c:forEach>
	<tr>
		<td>
			<a href="<c:url value="/arrays/showAllArrayDesigns.html"/>"> Array Designs: </a>
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

<div class="separator"></div>

<div id="coexpression-form" style="width: 200px">
	<script src="<c:url value='/scripts/ext/adapter/prototype/ext-prototype-adapter.js'/>" type="text/javascript"></script>
	<script src="<c:url value='/scripts/ext/ext-all-debug.js'/>" type="text/javascript"></script>
	<script src="<c:url value='/scripts/ext/RowExpander.js'/>" type="text/javascript"></script>
	<script type="text/javascript" src="<c:url value='/scripts/ext/data/ListRangeReader.js'/>"></script>
	<script type="text/javascript" src="<c:url value='/scripts/ext/data/DwrProxy.js'/>"></script>
	<script type='text/javascript' src='/Gemma/dwr/engine.js'></script>
	<script type='text/javascript' src='/Gemma/dwr/util.js'></script>
	<script type='text/javascript' src='/Gemma/dwr/interface/GenePickerController.js'></script>
	<script type='text/javascript' src='/Gemma/dwr/interface/ExtCoexpressionSearchController.js'></script>
	<script type='text/javascript' src='/Gemma/scripts/ajax/coexpression/GeneCombo.js'></script>
	<script type='text/javascript' src='/Gemma/scripts/ajax/coexpression/TaxonCombo.js'></script>
	<script type='text/javascript' src='/Gemma/scripts/ajax/coexpression/GeneChooserPanel.js'></script>
	<script type='text/javascript' src='/Gemma/scripts/ajax/util/GemmaGridPanel.js'></script>
	<script type='text/javascript' src='/Gemma/scripts/ajax/util/PagingDataStore.js'></script>
	<script type='text/javascript' src='/Gemma/scripts/ajax/util/PagingToolbar.js'></script>
	<script type='text/javascript' src='/Gemma/scripts/ajax/coexpression/CoexpressionGrid.js'></script>
	<script type='text/javascript' src='/Gemma/scripts/ajax/coexpression/CoexpressionDatasetGrid.js'></script>
	<script type='text/javascript' src='/Gemma/scripts/ajax/coexpression/CoexpressionSearchForm.js'></script>
	<script type='text/javascript' src='/Gemma/scripts/ajax/coexpression/CoexpressionSearchLite.js'></script>
</div>

<!-- 
<form method="post" name="coexpressionSearch" action="<c:url value="/searchCoexpression.html"/>">

	<table>
		<caption>
			Coexpression Analysis
		</caption>
		<tr>
			<td valign="top">
				<b> Gene Name </b>
			</td>
			<td>

				<spring:bind path="coexpressionSearchCommand.searchString">
					<input type="text" size=19 name="<c:out value="${status.expression}"/>" value="<c:out value="${status.value}"/>" />
				</spring:bind>
				<spring:bind path="coexpressionSearchCommand.exactSearch">
					<input type="hidden" name="${status.expression}" value="auto" />
				</spring:bind>
			</td>
			<td>
				<a class="helpLink" href="?" onclick="showHelpTip(event, 
				'Official symbol of a gene'); return false"> <img
						src="/Gemma/images/help.png" /> </a>
			</td>
		</tr>


		<tr>
			<td valign="top">
				<b> <fmt:message key="label.species" /> </b>
			</td>

			<td>
				<spring:bind path="coexpressionSearchCommand.taxon">
					<select name="${status.expression}">
						<c:forEach items="${taxa}" var="taxon">
							<spring:transform value="${taxon}" var="scientificName" />
							<option value="${taxon.scientificName}" <c:if test="${status.value == taxon.scientificName}">selected </c:if>>
								${taxon.scientificName}
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
			<td>
				&nbsp;
			</td>
			<td>
				<input type="submit" class="button" name="submit" value="<fmt:message key="button.search"/>" />
			</td>
		</tr>
	</table>

	<spring:bind path="coexpressionSearchCommand.stringency">
		<input type="hidden" name="<c:out value="${status.expression}"/>" value="<c:out value="${previousStringency}"/>" />
	</spring:bind>

</form>
-->

<authz:authorize ifAnyGranted="admin">
	<hr />
	<h2>
		Administrative functions
	</h2>
	<ul class="glassList">

		<li>
			<a href="<c:url value="/indexer.html"/>"> <fmt:message key="menu.compassIndexer" /> </a>
		</li>
		<li>
			<a href="<c:url value="/securityManager.html"/>"> <fmt:message key="menu.securityManager" /> </a>
		</li>
		<li>
			<a href="<c:url value="/maintenanceMode.html" />">Manage maintenance mode</a>
		</li>
		<li>
			<a href="<c:url value="/whatsnew/generateCache.html" />">Regenerate What's New Cache</a>
		</li>
		<li>
			<a href="<c:url value="loadSimpleExpressionExperiment.html"/>"> Load expression data from a tabbed file</a>
		</li>
		<li>
			<a href="<c:url value="/arrayDesign/associateSequences.html"/>"> <fmt:message key="menu.arrayDesignSequenceAdd" />
			</a>
		</li>
	</ul>
	<h2>
		Inactive, deprecated, or not ready for prime time
	</h2>
	<ul class="glassList">
		<li>
			<a href="<c:url value="/uploadFile.html"/>"> <fmt:message key="menu.selectFile" /> </a>
		</li>
		<li>
			<a href="<c:url value="/geneFinder.html"/>"> <fmt:message key="menu.GeneFinder" /> </a>
		</li>
		<li>
			<a href="<c:url value="/candidateGeneList.html"/>"> <fmt:message key="menu.CandidateGeneList" /> </a>
		</li>
		<li>
			<a href="<c:url value="/bibRefSearch.html"/>"> <fmt:message key="menu.flow.PubMedSearch" /> </a>
		</li>
		<li>
			<a href="<c:url value="/searchCoexpressionMultiple.html"/>"> <fmt:message key="menu.searchCoexpressionMultiple" />
			</a>
		</li>
	</ul>

</authz:authorize>
