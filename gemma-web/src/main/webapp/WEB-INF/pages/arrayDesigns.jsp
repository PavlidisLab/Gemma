<%-- $Id$ --%>
<%@ include file="/common/taglibs.jsp"%>

<!--  Summary of array design associations -->
<%-- Admin only --%>
<authz:authorize ifAnyGranted="admin">
<c:if test="${summaryString != null }" >
${summaryString}
</c:if>
</authz:authorize>

		<h1>
		Platforms
		</h1>
		<title>Platforms</title>


	<form name="ArrayDesignFilter" action="filterArrayDesigns.html" method="POST">
			<h4> Enter search criteria for finding a specific array design here </h4>
			<input type="text" name="filter" size="66" />
			<input type="submit" value="Find"/>			
	</form>



		<h3>
			Displaying <c:out value="${numArrayDesigns}" /> Platforms
		</h3>

				<display:table name="arrayDesigns" sort="list" class="list" requestURI="" id="arrayDesignList"
				pagesize="30" decorator="ubic.gemma.web.taglib.displaytag.expression.arrayDesign.ArrayDesignWrapper">
					<display:column property="name" sortable="true" href="showArrayDesign.html" paramId="id" paramProperty="id"
						titleKey="arrayDesign.name" sortProperty="name" comparator="ubic.gemma.web.taglib.displaytag.StringComparator"/>
					<display:column property="shortName" sortable="true" titleKey="arrayDesign.shortName" />
					<display:column property="expressionExperimentCountLink" sortable="true" title="Expts"
					sortProperty="expressionExperimentCount"  comparator="ubic.gemma.web.taglib.displaytag.NumberComparator" />
					<authz:authorize ifAnyGranted="admin">
						<display:column property="color" sortable="true" titleKey="arrayDesign.technologyType" />
					</authz:authorize>
					<authz:authorize ifAnyGranted="admin">
						<display:column property="delete" sortable="false" titleKey="arrayDesign.delete" />
					</authz:authorize>
					<display:setProperty name="basic.empty.showtable" value="true" />
				</display:table>




