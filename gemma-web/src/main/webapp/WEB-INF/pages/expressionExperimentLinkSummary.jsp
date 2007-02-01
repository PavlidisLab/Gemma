<%@ include file="/common/taglibs.jsp"%>


<title>Expression Experiment Link Summary</title>


<h3>
	Expression Experiment Summaries
	<br>
	Displaying
	<b> <c:out value="${numExpressionExperiments}" /> </b> Datasets
</h3>
<a class="helpLink" href="?"
	onclick="showHelpTip(event, 'Summarizes multiple expression experiments.'); return false">Help</a>

<authz:authorize ifAnyGranted="admin">
	<p>
		<a
			href="<c:url value="/expressionExperiment/generateExpressionExperimentLinkSummary.html"/>" onclick="return confirm('Regenerate reports for all experiments?');">
			Regenerate Expression Experiment Link Summaries </a>

	</p>
</authz:authorize>

<display:table pagesize="50" name="expressionExperiments" sort="list"
	class="list" requestURI="" id="expressionExperimentList"
	decorator="ubic.gemma.web.taglib.displaytag.expression.experiment.ExpressionExperimentWrapper">

	<display:column property="nameLink" sortable="true" sortProperty="name"
		titleKey="expressionExperiment.name"
		comparator="ubic.gemma.web.taglib.displaytag.StringComparator" />

	<display:column property="shortName" sortable="true"
		titleKey="expressionExperiment.shortName" />
	<display:column property="coexpressionLinkCount" sortable="true"
		titleKey="expressionExperiment.coexpressionLinkCount" />
	<display:column property="preferredDesignElementDataVectorCount"
		sortable="true"
		titleKey="expressionExperiment.preferredDesignElementDataVectorCount" />
	<display:column property="bioMaterialCount" sortable="true"
		titleKey="expressionExperiment.bioMaterialCount" />
	<display:column property="dateCachedNoTime" sortable="true"
		title="Cached" />
	<display:column property="dateCreatedNoTime" sortable="true"
		title="Created" />
	<display:column property="dateLastUpdatedNoTime" sortable="true"
		title="Updated" />
	<authz:authorize ifAnyGranted="admin">
		<display:column property="refreshReport" title="Refresh" />
	</authz:authorize>

	<display:setProperty name="basic.empty.showtable" value="true" />
</display:table>
