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
<authz:authorize ifAnyGranted="admin">
	<a href="<c:url value="/arrays/generateArrayDesignSummary.html"/>"
		onclick="return confirm('Regenerate report for all platforms?');">
		Regenerate this report</a>
</authz:authorize>

<script type='text/javascript' src='/Gemma/scripts/prototype.js'></script>
<script type='text/javascript' src='/Gemma/scripts/expandableObjects.js'></script>

<display:table name="arrayDesigns" sort="list" class="list"
	requestURI="" id="arrayDesignList" pagesize="30"
	decorator="ubic.gemma.web.taglib.displaytag.expression.arrayDesign.ArrayDesignWrapper">
	<display:column property="name" sortable="true"
		href="showArrayDesign.html" paramId="id" paramProperty="id"
		titleKey="arrayDesign.name" />
	<display:column property="shortName" sortable="true"
		titleKey="arrayDesign.shortName" />
	<display:column property="taxon" sortable="true"
		titleKey="arrayDesign.taxon" />
	<display:column property="expressionExperimentCountLink"
		sortable="true" title="Expts" />
	<display:column property="summaryTable" title="Probe Summary" />
	<authz:authorize ifAnyGranted="admin">
		<display:column property="lastSequenceUpdate" sortable="true"
			title="Seq. Update" defaultorder="descending"
			comparator="ubic.gemma.web.taglib.displaytag.DateStringComparator" />
		<display:column property="lastSequenceAnalysis" sortable="true"
			title="Seq. Analysis" defaultorder="descending"
			comparator="ubic.gemma.web.taglib.displaytag.DateStringComparator" />
		<display:column property="lastGeneMapping" sortable="true"
			title="Gene mapping" defaultorder="descending"
			comparator="ubic.gemma.web.taglib.displaytag.DateStringComparator" />
		<display:column property="color" sortable="true"
			titleKey="arrayDesign.technologyType" />
		<display:column property="refreshReport" title="Refresh" />
	</authz:authorize>

	<display:setProperty name="basic.empty.showtable" value="true" />

</display:table>






