<%@ include file="/common/taglibs.jsp"%>
<title><fmt:message key="mainMenu.title" /></title>
<table class="datasummary">
	<tr>
		<td colspan=2>
			<b>Data Summary</b>
		</td>
	</tr>
	<tr>
		<td>
			<a
				href="<c:url value="/expressionExperiment/showAllExpressionExperiments.html"/>">
				Expression Experiments: </a>
		</td>
		<td align="right">
			<b><c:out value="${ expressionExperimentCount}" /> </b>
		</td>
	</tr>
	<c:forEach var="taxon" items="${ taxonCount }">
		<tr>
			<td>
				&emsp;
				<c:out value="${ taxon.key}" />
			</td>
			<td align="right">
				<c:out value="${ taxon.value}" />
			</td>
		</tr>
	</c:forEach>
	<tr>
		<td>
			<a href="<c:url value="/arrays/showAllArrayDesigns.html"/>">
				Array Designs: </a>
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


<p>
	Query form coming soon!
</p>


<authz:authorize ifAnyGranted="admin">
	<hr />
	<h2>
		Administrative functions
	</h2>
	<ul class="glassList">
		<li>
			<a href="<c:url value="/geneFinder.html"/>"> <fmt:message
					key="menu.GeneFinder" /> </a>
		</li>
		<li>
			<a href="<c:url value="/searchCoexpression.html"/>"> <fmt:message
					key="menu.Coexpression" /> </a>
		</li>

		<li>
			<a href="<c:url value="/indexer.html"/>"> <fmt:message
					key="menu.compassIndexer" /> </a>
		</li>
		<li>
			<a href="<c:url value="/loadExpressionExperiment.html"/>"> <fmt:message
					key="menu.loadExpressionExperiment" /> </a>
		</li>
		<li>
			<a href="<c:url value="loadSimpleExpressionExperiment.html"/>">
				Load expression data from a tabbed file</a>
		</li>
		<li>
			<a href="<c:url value="/addTestData.html"/>">Add test data </a>
		</li>
		<li>
			<a href="<c:url value="/uploadFile.html"/>"> <fmt:message
					key="menu.selectFile" /> </a>
		</li>
		<li>
			<a href="<c:url value="/arrayDesign/associateSequences.html"/>">
				<fmt:message key="menu.arrayDesignSequenceAdd" /> </a>
		</li>
		<li>
			<a href="<c:url value="/genome/goldenPathSequenceLoad.html"/>"> <fmt:message
					key="menu.goldenPathSequenceLoad" /> </a>
		</li>
	</ul>

	<h2>
		Inactive, deprecated, or not ready for prime time
	</h2>
	<ul class="glassList">
		<li>
			<a href="<c:url value="/candidateGeneList.html"/>"> <fmt:message
					key="menu.CandidateGeneList" /> </a>
		</li>
		<li>
			<a href="<c:url value="/bibRefSearch.html"/>"> <fmt:message
					key="menu.flow.PubMedSearch" /> </a>
		</li>
	</ul>

</authz:authorize>
