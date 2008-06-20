<%-- 

Display table of expression experiments.
$Id$ 
--%>
<head>
	<%@ include file="/common/taglibs.jsp"%>
	<title><fmt:message key="expressionExperiments.title" /></title>


	<script type="text/javascript" src="<c:url value='/scripts/progressbar.js'/>"></script>
	<script type="text/javascript" src="<c:url value='/scripts/ext/data/DwrProxy.js'/>"></script>
	<script type='text/javascript' src='/Gemma/dwr/interface/ExpressionExperimentController.js'></script>
	<script type='text/javascript' src='/Gemma/dwr/interface/ProgressStatusService.js'></script>
	<script type='text/javascript' src='/Gemma/dwr/interface/TaskCompletionController.js'></script>
	<script type='text/javascript' src='/Gemma/dwr/engine.js'></script>
	<script type='text/javascript' src='/Gemma/dwr/util.js'></script>
	<script type="text/javascript" src="<c:url value='/scripts/ajax/expressionExperiment.js'/>" type="text/javascript"></script>
	<link rel="stylesheet" type="text/css" media="all" href="<c:url value='/styles/progressbar.css'/>" />

</head>
<h3>
	Displaying
	<b> <c:out value="${numExpressionExperiments}" /> </b> datasets
	<c:choose>
		<c:when test="${taxon != null}">
	for <c:out value="${taxon.commonName}" />
		</c:when>
	</c:choose>
	<c:choose>
		<c:when test="${showAll == false}">
			<span style="font-size: smaller">&nbsp;&nbsp;(<a
				href="<c:url value="/expressionExperiment/showAllExpressionExperiments.html" />">Show all</a>)</span>
		</c:when>
	</c:choose>
</h3>
<div id="messages" style="margin: 10px; width: 400px"></div>
<div id="taskId" style="display: none;"></div>
<div id="progress-area" style="padding: 15px;"></div>

<form
	style="border-color: #444; border-style: solid; border-width: 1px; width: 450px; padding: 10px; background-color: #DDD"
	name="ExpresssionExperimentFilter" action="filterExpressionExperiments.html" method="POST">
	<span class="list">Enter search criteria for finding datasets</span>
	<br>
	<input type="text" name="filter" size="48" />
	<input type="submit" value="Search" />
</form>

<display:table pagesize="100" name="expressionExperiments" sort="list" requestURI="" id="expressionExperimentList"
	decorator="ubic.gemma.web.taglib.displaytag.expression.experiment.ExpressionExperimentWrapper">

	<display:column property="nameLink" sortable="true" sortProperty="name" titleKey="expressionExperiment.name"
		comparator="ubic.gemma.web.taglib.displaytag.StringComparator" />

	<authz:authorize ifAnyGranted="admin">
		<display:column property="status" sortable="true" titleKey="expressionExperiment.status"
			style="text-align:center; vertical-align:middle;" comparator="ubic.gemma.web.taglib.displaytag.StringComparator"
			defaultorder="descending" />
	</authz:authorize>

	<display:column property="shortName" sortable="true" titleKey="expressionExperiment.shortName" />

	<authz:authorize ifAnyGranted="admin">
		<display:column property="arrayDesignLink" sortable="true" defaultorder="descending" title="Arrays"
			comparator="ubic.gemma.web.taglib.displaytag.NumberComparator" />
	</authz:authorize>


	<display:column property="assaysLink" sortable="true" sortProperty="bioAssayCount" titleKey="bioAssays.title"
		defaultorder="descending" comparator="ubic.gemma.web.taglib.displaytag.NumberComparator" />

	<display:column property="taxon" sortable="true" titleKey="taxon.title" />

	<authz:authorize ifAnyGranted="admin">
		<display:column property="dateCreatedNoTime" sortable="true" defaultorder="descending" title="Created" />
		<display:column property="edit" sortable="false" title="Edit" />
		<display:column property="delete" sortable="false" titleKey="expressionExperiment.delete" />
	</authz:authorize>

	<display:setProperty name="basic.empty.showtable" value="true" />
</display:table>
