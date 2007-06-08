<%-- $Id$ --%>
<%@ include file="/common/taglibs.jsp"%>

<!--  Summary of array design associations -->
<%-- Admin only --%>
<authz:authorize ifAnyGranted="admin">
	<c:if test="${summary != null }">
		<table class='datasummary'>
			<tr>
				<td colspan=2 align=center>
				</td>
			</tr>
			<authz:authorize ifAnyGranted="admin">
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
			</authz:authorize>
			<tr>
				<td>
					Probes mapping to gene(s)
				</td>
				<td>
					${summary.numProbesToGenes}
				</td>
			</tr>
			<tr>
				<td>
					&nbsp;&nbsp;Probes mapping to probe-aligned region(s)
				</td>
				<td>
					${summary.numProbesToProbeAlignedRegions}
				</td>
			</tr>
			<tr>
				<td>
					&nbsp;&nbsp;Probes mapping to predicted genes
				</td>
				<td>
					${summary.numProbesToPredictedGenes}
				</td>
			</tr>
			<tr>
				<td>
					&nbsp;&nbsp;Probes mapping to known genes
				</td>
				<td>
					${summary.numProbesToKnownGenes }
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
				<td colspan=2 align='center' class='small'>
					(as of ${summary.dateCached})
				</td>
			</tr>
		</table>
	</c:if>
</authz:authorize>

<h1>
	Platforms
</h1>
<title>Platforms</title>


<form name="ArrayDesignFilter" action="filterArrayDesigns.html"
	method="POST">
	<h4>
		Enter search criteria for finding a specific array design here
	</h4>
	<input type="text" name="filter" size="66" />
	<input type="submit" value="Find" />
</form>



<h3>
	Displaying
	<c:out value="${numArrayDesigns}" />
	Platforms
</h3>
<authz:authorize ifAnyGranted="admin">
	<a href="<c:url value="/arrays/generateArrayDesignSummary.html"/>"
		onclick="return confirm('Regenerate report for all platforms?');">
		Regenerate this report</a>
</authz:authorize>

<script type='text/javascript' src='/Gemma/scripts/prototype.js'></script>
<script type='text/javascript' src='/Gemma/scripts/expandableObjects.js'></script>

<display:table name="arrayDesigns" sort="list" requestURI="" id="arrayDesignList" pagesize="50"
	decorator="ubic.gemma.web.taglib.displaytag.expression.arrayDesign.ArrayDesignWrapper">
	<display:column property="name" sortable="true"
		comparator="ubic.gemma.web.taglib.displaytag.StringComparator"
		href="showArrayDesign.html" paramId="id" paramProperty="id"
		titleKey="arrayDesign.name" />
	<display:column property="shortName" sortable="true"
		titleKey="arrayDesign.shortName"
		comparator="ubic.gemma.web.taglib.displaytag.StringComparator" />
	<display:column property="taxon" sortable="true"
		titleKey="arrayDesign.taxon" />
	<display:column property="expressionExperimentCountLink"
		sortable="true" title="Expts"
		comparator="ubic.gemma.web.taglib.displaytag.NumberComparator" />
	<display:column property="summaryTable" title="Probe Summary" />
	<authz:authorize ifAnyGranted="admin">
		<display:column property="lastSequenceUpdateDate" sortable="true"
			comparator="ubic.gemma.web.taglib.displaytag.DateStringComparator"
			title="Seq. Update" defaultorder="descending" />
		<display:column property="lastRepeatMaskDate" sortable="true"
			comparator="ubic.gemma.web.taglib.displaytag.DateStringComparator"
			title="Rep. mask" defaultorder="descending" />
		<display:column property="lastSequenceAnalysisDate" sortable="true"
			comparator="ubic.gemma.web.taglib.displaytag.DateStringComparator"
			title="Seq. Analysis" defaultorder="descending" />
		<display:column property="lastGeneMappingDate" sortable="true"
			comparator="ubic.gemma.web.taglib.displaytag.DateStringComparator"
			title="Gene mapping" defaultorder="descending" />
		<display:column property="color" sortable="true"
			titleKey="arrayDesign.technologyType" />
		<display:column property="refreshReport" title="Refresh" />
		<display:column property="delete" sortable="false"
			titleKey="arrayDesign.delete" />
	</authz:authorize>

	<display:setProperty name="basic.empty.showtable" value="true" />

</display:table>






