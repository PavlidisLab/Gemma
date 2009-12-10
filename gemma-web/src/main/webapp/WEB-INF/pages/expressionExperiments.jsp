
<%@ include file="/common/taglibs.jsp"%>
<%-- 

Display table of expression experiments.
$Id$ 
--%>
<head>
	<title><fmt:message key="expressionExperiments.title" />
	</title>
</head>

<div id="messages" style="margin: 10px; width: 400px"></div>
<div id="taskId" style="display: none;"></div>
<div id="progress-area" style="padding: 15px;"></div>



<c:choose>
	<c:when test="${numExpressionExperiments > 0}">

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

		<%-- Note: the d=1 in the urls here is just a hack to keep '#' from messing up id parsing. It's not a real parameter --%>
		<security:authorize ifAnyGranted="GROUP_ADMIN,GROUP_USER">
			<c:choose>
				<c:when test="${taxon != null && not empty eeids}">
					<p>
						For a view that allows editing of your experiments that are on this list, go to the
						<a
							href="<c:url value="/expressionExperiment/showAllExpressionExperimentLinkSummaries.html?taxon=${taxon.id}&d=1
				" />">Data
							Manager</a> for this taxon.
					</p>
				</c:when>
				<c:otherwise>
					<c:if test="${not empty eeids && fn:length(eeids) < 200}">
						<p>
							For a view that allows editing of your experiments that are on this list, go to the
							<a
								href="<c:url value="/expressionExperiment/showAllExpressionExperimentLinkSummaries.html?ids=${fn:join(eeids, ',')}&d=1
				" />">Data
								Manager</a>
						</p>
					</c:if>
				</c:otherwise>
			</c:choose>
		</security:authorize>


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

			<security:authorize ifAnyGranted="GROUP_ADMIN">
				<display:column property="status" sortable="true" titleKey="expressionExperiment.status"
					style="text-align:center; vertical-align:middle;" comparator="ubic.gemma.web.taglib.displaytag.StringComparator"
					defaultorder="descending" />
			</security:authorize>

			<display:column property="shortName" sortable="true" titleKey="expressionExperiment.shortName" />

			<security:authorize ifAnyGranted="GROUP_ADMIN">
				<display:column property="arrayDesignLink" sortable="true" defaultorder="descending" title="Arrays"
					comparator="ubic.gemma.web.taglib.displaytag.NumberComparator" />
			</security:authorize>


			<display:column property="assaysLink" sortable="true" sortProperty="bioAssayCount" titleKey="bioAssays.title"
				defaultorder="descending" comparator="ubic.gemma.web.taglib.displaytag.NumberComparator" />

			<display:column property="taxon" sortable="true" titleKey="taxon.title" />

			<security:authorize ifAnyGranted="GROUP_ADMIN">
				<display:column property="dateCreatedNoTime" sortable="true" defaultorder="descending" title="Created" />
			</security:authorize>

			<display:setProperty name="basic.empty.showtable" value="true" />
		</display:table>

	</c:when>
	<c:otherwise>
		<div style="margin: 4px">
			<img src="<c:url value='/images/icons/exclamation.png' />" />
			&nbsp; Either you didn't select any experiments, or you don't have permissions to view the ones you chose.
		</div>
		<br />
		<form
			style="border-color: #444; border-style: solid; border-width: 1px; width: 450px; padding: 10px; background-color: #DDD"
			name="ExpresssionExperimentFilter" action="filterExpressionExperiments.html" method="POST">
			<span class="list">Enter search criteria for finding datasets</span>
			<br>
			<input type="text" name="filter" size="48" />
			<input type="submit" value="Search" />
		</form>

	</c:otherwise>
</c:choose>

