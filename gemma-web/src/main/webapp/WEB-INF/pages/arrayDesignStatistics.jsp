<%-- $Id$ --%>
<%@ include file="/common/taglibs.jsp"%>
<script type='text/javascript' src='/Gemma/scripts/expandableObjects.js'></script>
<!--  Summary of array design associations -->
<%-- Admin only --%>
<security:authorize access="hasRole('GROUP_ADMIN')">
	<c:if test="${summaryString != null }">
${summaryString}
</c:if>
</security:authorize>

<h1>
	Platforms
</h1>
<title>Platforms</title>


<h3>
	Displaying
	<c:out value="${numArrayDesigns}" />
	Platforms
</h3>

<security:authorize access="hasRole('GROUP_ADMIN')">
	<a href="<c:url value="/arrays/generateArrayDesignSummary.html"/>"
		onclick="return confirm('Regenerate report for all platforms?');">
		Regenerate this report</a>
</security:authorize>


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
	<security:authorize access="hasRole('GROUP_ADMIN')">
		<display:column property="lastSequenceUpdate" sortable="true"
			title="Seq. Update" defaultorder="descending"/>
		<display:column property="lastSequenceAnalysis" sortable="true"
			title="Seq. Analysis" defaultorder="descending"/>
		<display:column property="lastGeneMapping" sortable="true"
			title="Gene mapping" defaultorder="descending"/>
		<display:column property="color" sortable="true"
			titleKey="arrayDesign.technologyType" />
		<display:column property="refreshReport" title="Refresh" />
	</security:authorize>

	<display:setProperty name="basic.empty.showtable" value="true" />

</display:table>




