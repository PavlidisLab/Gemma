<%@ include file="/common/taglibs.jsp"%>

<html>
	<head>
		<title><fmt:message key="expressionExperiments.title" /></title>
	</head>
	<body>
		<h2>
			<fmt:message key="search.results" />
		</h2>
		<h3>
			<c:out value="${numExpressionExperiments }" />
			Expression Experiments found.
		</h3>
		<a class="helpLink" href="?" onclick="showHelpTip(event, 'Summarizes multiple expression experiments.'); return false">Help</a>

		<display:table name="expressionExperiments" class="list" requestURI="" id="expressionExperimentList"
			decorator="ubic.gemma.web.taglib.displaytag.expression.experiment.ExpressionExperimentWrapper">

			<display:column property="nameLink" sortable="true" sortProperty="name" titleKey="expressionExperiment.name" />

			<display:column property="dataSource" sortable="true" titleKey="externalDatabase.title" maxWords="20" />

 			<display:column property="assaysLink" sortable="true" titleKey="bioAssays.title" />

			<display:column property="taxon" sortable="true" titleKey="taxon.title" />

			<authz:authorize ifAnyGranted="admin">
				<display:column property="delete" sortable="false" titleKey="expressionExperiment.delete" />
			</authz:authorize>

			<display:setProperty name="basic.empty.showtable" value="true" />
		</display:table>

	</body>
</html>
