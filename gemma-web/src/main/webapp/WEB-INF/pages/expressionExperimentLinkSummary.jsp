<%@ include file="/common/taglibs.jsp"%>
<head>

	<title>Expression Experiment Link Summary</title>

	<jwr:script src='/scripts/ajax/ext/data/DwrProxy.js' />
	<jwr:script src='/scripts/app/expressionExperiment.js' />
	<jwr:script src='/scripts/app/ExpressionExperimentManage.js' />

</head>

<body>
	<div id="eemanage"></div>

	<h3>
		Expression Experiment Summaries
		<br>
		Displaying
		<b> <c:out value="${numExpressionExperiments}" /> </b> Datasets
	</h3>
	<a class="helpLink" href="?" onclick="showHelpTip(event, 'Summarizes multiple expression experiments.'); return false">Help</a>
	<div id="messages" style="margin: 10px; width: 400px"></div>
	<div id="taskId" style="display: none;"></div>
	<div id="progress-area" style="padding: 15px;"></div>
	<authz:authorize ifAnyGranted="admin">
		<p>
			<a href="<c:url value="/expressionExperiment/generateExpressionExperimentLinkSummary.html"/>"
				onclick="return confirm('Regenerate reports for all experiments?');"> Regenerate Expression Experiment Link Summaries
			</a>

		</p>
		<display:table pagesize="200" name="expressionExperiments" sort="list" defaultsort="10" defaultorder="descending"
			class="list" requestURI="" id="expressionExperimentList"
			decorator="ubic.gemma.web.taglib.displaytag.expression.experiment.ExpressionExperimentWrapper">

			<display:column property="nameLink" sortable="true" sortProperty="name" titleKey="expressionExperiment.name"
				comparator="ubic.gemma.web.taglib.displaytag.StringComparator" />
			<display:column property="status" sortable="true" titleKey="expressionExperiment.status"
				style="text-align:center; vertical-align:middle;" comparator="ubic.gemma.web.taglib.displaytag.StringComparator"
				defaultorder="descending" />
			<display:column property="shortName" sortable="true" titleKey="expressionExperiment.shortName"
				comparator="ubic.gemma.web.taglib.displaytag.StringComparator" />
			<display:column property="coexpressionLinkCount" sortable="true" titleKey="expressionExperiment.coexpressionLinkCount"
				comparator="ubic.gemma.web.taglib.displaytag.NumberComparator" defaultorder="descending" />
			<display:column property="processedExpressionVectorCount" sortable="true" defaultorder="descending"
				comparator="ubic.gemma.web.taglib.displaytag.NumberComparator"
				titleKey="expressionExperiment.preferredDesignElementDataVectorCount" />
			<display:column property="technologyType" sortable="true" defaultorder="descending" title="Tech" />
			<display:column property="bioMaterialCount" sortable="true" defaultorder="descending"
				titleKey="expressionExperiment.bioMaterialCount" comparator="ubic.gemma.web.taglib.displaytag.NumberComparator" />

			<display:column property="numAnnotations" sortable="true" defaultorder="descending" title="# Annots"
				comparator="ubic.gemma.web.taglib.displaytag.NumberComparator" />

			<display:column property="numFactors" sortable="true" defaultorder="descending" title="# Factors"
				comparator="ubic.gemma.web.taglib.displaytag.NumberComparator" />

			<display:column property="dateCreatedNoTime" sortable="true" defaultorder="descending" title="Created"
				comparator="ubic.gemma.web.taglib.displaytag.DateStringComparator" />

			<display:column property="dateDifferentialAnalysisNoTime" sortable="true" defaultorder="descending"
				titleKey="menu.differential" comparator="ubic.gemma.web.taglib.displaytag.DateStringComparator" />
			<display:column property="dateProcessedDataVectorUpdateNoTime" sortable="true" defaultorder="descending"
				title="Processed vectors" comparator="ubic.gemma.web.taglib.displaytag.DateStringComparator" />
			<display:column property="dateLastArrayDesignUpdatedNoTime" sortable="true" defaultorder="descending" title="Probe Map"
				comparator="ubic.gemma.web.taglib.displaytag.DateStringComparator" />
			<display:column property="dateLinkAnalysisNoTime" sortable="true" defaultorder="descending" title="Link Analyzed"
				comparator="ubic.gemma.web.taglib.displaytag.DateStringComparator" />
			<display:column property="refreshReport" title="Refresh" />
			<display:column property="delete" title="Delete" />
			<display:setProperty name="basic.empty.showtable" value="true" />

		</display:table>
	</authz:authorize>
	<authz:authorize ifNotGranted="admin">
		<p>
			Permission denied.
		</p>
	</authz:authorize>
</body>