<%-- $Id$ --%>
<%@ include file="/common/taglibs.jsp"%>
<head>
	<jwr:script src='/scripts/ajax/ext/data/DwrProxy.js' />
	<jwr:script src='/scripts/app/arrayDesign.js' />
</head>

<!--  Summary of array design associations -->
<%-- Admin only --%>
<security:authorize access="hasRole('GROUP_ADMIN')">
	<c:if test="${summary != null }">
		<table class='datasummary'>
			<tr>
				<td colspan="2" align="center">
				</td>
			</tr>
			<tr>
					<td>
						Probes with sequences
					</td>
					<td>
						${summary.numProbeSequences}
					</td>
				</tr>
				<tr>
					<td>
						Probes with genome alignments
					</td>
					<td>
						${summary.numProbeAlignments}
					</td>
				</tr>
			<tr>
				<td>
					Probes with mapping
				</td>
				<td>
					${summary.numProbesToGenes}
				</td>
			</tr>
			<tr>
				<td>
					Unique genes represented
				</td>
				<td>
					${summary.numGenes}
				</td>
			</tr>
			<tr>
				<td colspan='2' align='center' class='small'>
					(as of ${summary.dateCached})
				</td>
			</tr>
		</table>
	</c:if>
</security:authorize>

<h1>
	Platforms
</h1>
<title>Platforms</title>

<input type="hidden" id="reloadOnLogin" value="true"/>

<form name="ArrayDesignFilter" action="filterArrayDesigns.html" method="POST">
	<h4>
		Enter search criteria for finding a specific array design here
	</h4>
	<input type="text" name="filter" size="66" />
	<input type="submit" value="Find" />
</form>

<div id="messages" style="margin: 10px; width: 400px"></div>
<div id="progress-area" style="padding: 15px;"></div>

<h3>
	Displaying
	<c:out value="${numArrayDesigns}" />
	Platforms.
</h3>
<p>
	<c:choose>
		<c:when test="${!showMergees || !showOrphans}">
			<c:if test="${!showMergees}">Platforms that have been merged are hidden. </c:if>
			<c:if test="${!showOrphans}">Platforms with no datasets are hidden. </c:if>
			<a href="<c:url value='/arrays/showAllArrayDesigns.html?showOrph=true&showMerg=true'/>">Show all</a>
		</c:when>
		<c:otherwise>
			<a href="<c:url value='/arrays/showAllArrayDesigns.html?showOrph=false&showMerg=false'/>">Hide</a> orphans and mergees.
	</c:otherwise>
	</c:choose>
</p>

<security:authorize access="hasRole('GROUP_ADMIN')">
	<a href="<c:url value="/arrays/generateArrayDesignSummary.html"/>"
		onclick="return confirm('Regenerate report for all platforms?');"> Regenerate this report</a>
</security:authorize>


<display:table name="arrayDesigns" sort="list" requestURI="" id="arrayDesignList" pagesize="50"
	decorator="ubic.gemma.web.taglib.displaytag.expression.arrayDesign.ArrayDesignWrapper">
	<display:column property="name" sortable="true" comparator="ubic.gemma.web.taglib.displaytag.StringComparator"
		href="showArrayDesign.html" paramId="id" paramProperty="id" titleKey="arrayDesign.name" />
	<security:authorize access="hasRole('GROUP_ADMIN')">
		<display:column property="status" sortable="true" titleKey="arrayDesign.status"
			style="text-align:center; vertical-align:middle;" comparator="ubic.gemma.web.taglib.displaytag.StringComparator"
			defaultorder="descending" />
	</security:authorize>
	<display:column property="shortName" sortable="true" titleKey="arrayDesign.shortName"
		comparator="ubic.gemma.web.taglib.displaytag.StringComparator" />
	<display:column property="taxon" sortable="true" titleKey="arrayDesign.taxon" />
	<display:column property="expressionExperimentCountLink" sortable="true" title="Expts"
		comparator="ubic.gemma.web.taglib.displaytag.NumberComparator" />
	<display:column property="summaryTable" title="Probe Summary" />
	<security:authorize access="hasRole('GROUP_ADMIN')">
		<display:column property="lastSequenceUpdateDate" sortable="true"
			comparator="ubic.gemma.web.taglib.displaytag.DateStringComparator" title="Seq. Update" defaultorder="descending" />
		<display:column property="lastRepeatMaskDate" sortable="true"
			comparator="ubic.gemma.web.taglib.displaytag.DateStringComparator" title="Rep. mask" defaultorder="descending" />
		<display:column property="lastSequenceAnalysisDate" sortable="true"
			comparator="ubic.gemma.web.taglib.displaytag.DateStringComparator" title="Seq. Analysis" defaultorder="descending" />
		<display:column property="lastGeneMappingDate" sortable="true"
			comparator="ubic.gemma.web.taglib.displaytag.DateStringComparator" title="Gene mapping" defaultorder="descending" />
		<display:column property="color" sortable="true" titleKey="arrayDesign.technologyType" />
		<display:column property="refreshReport" title="Refresh" />
		<display:column property="delete" sortable="false" titleKey="arrayDesign.delete" />
	</security:authorize>

	<display:setProperty name="basic.empty.showtable" value="true" />

</display:table>






