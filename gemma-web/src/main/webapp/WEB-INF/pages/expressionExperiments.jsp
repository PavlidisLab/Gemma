<%@ include file="/common/taglibs.jsp"%>


		<title><fmt:message key="expressionExperiments.title" /></title>

	<form name="ExpresssionExperimentFilter" action="filterExpressionExperiments.html" method="POST">
			<h4> Enter search criteria for finding a specific dataset here </h4>
			<input type="text" name="filter" size="78" />
			<input type="submit" value="Find"/>			
		</form>
	
		<h3>
			Displaying  <b> <c:out value="${numExpressionExperiments}" /> </b> Datasets		
		</h3>		
		<a class="helpLink" href="?" onclick="showHelpTip(event, 'Summarizes multiple expression experiments.'); return false">Help</a>

		<display:table pagesize="50" name="expressionExperiments" sort="list" class="list" requestURI="" id="expressionExperimentList"
			decorator="ubic.gemma.web.taglib.displaytag.expression.experiment.ExpressionExperimentWrapper">

			<display:column property="nameLink" sortable="true" sortProperty="name" titleKey="expressionExperiment.name" />

			<display:column property="shortName" sortable="true" titleKey="expressionExperiment.shortName" />

			<authz:authorize ifAnyGranted="admin">
	 			<display:column property="arrayDesignLink" sortable="true" title="Arrays" />
			</authz:authorize>
		

 			<display:column property="assaysLink" sortable="true" titleKey="bioAssays.title" />

			<display:column property="taxon" sortable="true" titleKey="taxon.title" />

			<authz:authorize ifAnyGranted="admin">
				<display:column property="delete" sortable="false" titleKey="expressionExperiment.delete" />
			</authz:authorize>

			<display:setProperty name="basic.empty.showtable" value="true" />
		</display:table>
