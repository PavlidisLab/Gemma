<%-- $Id$ --%>
<%@ include file="/common/taglibs.jsp"%>
<head>
	<script src="<c:url value='/scripts/ext/adapter/prototype/ext-prototype-adapter.js'/>" type="text/javascript"></script>
	<script src="<c:url value='/scripts/ext/ext-all.js'/>" type="text/javascript"></script>
	<script type="text/javascript" src="<c:url value='/scripts/progressbar.js'/>"></script>
	<script type="text/javascript" src="<c:url value='/scripts/ext/data/DwrProxy.js'/>"></script>
	<script type='text/javascript' src='/Gemma/dwr/interface/ArrayDesignController.js'></script>
	<script type='text/javascript' src='/Gemma/dwr/interface/ProgressStatusService.js'></script>
	<script type='text/javascript' src='/Gemma/dwr/interface/TaskCompletionController.js'></script>
	<script type='text/javascript' src='/Gemma/dwr/engine.js'></script>
	<script type='text/javascript' src='/Gemma/dwr/util.js'></script>
	<script type="text/javascript" src="<c:url value='/scripts/ajax/arrayDesign.js'/>" type="text/javascript"></script>
	<link rel="stylesheet" type="text/css" media="all" href="<c:url value='/styles/progressbar.css'/>" />
</head>

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

<authz:authorize ifAnyGranted="admin">
	<a href="<c:url value="/arrays/generateArrayDesignSummary.html"/>"
		onclick="return confirm('Regenerate report for all platforms?');"> Regenerate this report</a>
</authz:authorize>

<script type='text/javascript' src='/Gemma/scripts/prototype.js'></script>
<script type='text/javascript' src='/Gemma/scripts/expandableObjects.js'></script>

<display:table name="arrayDesigns" sort="list" requestURI="" id="arrayDesignList" pagesize="50"
	decorator="ubic.gemma.web.taglib.displaytag.expression.arrayDesign.ArrayDesignWrapper">
	<display:column property="name" sortable="true" comparator="ubic.gemma.web.taglib.displaytag.StringComparator"
		href="showArrayDesign.html" paramId="id" paramProperty="id" titleKey="arrayDesign.name" />
	<authz:authorize ifAnyGranted="admin">
		<display:column property="status" sortable="true" titleKey="arrayDesign.status"
			style="text-align:center; vertical-align:middle;" comparator="ubic.gemma.web.taglib.displaytag.StringComparator"
			defaultorder="descending" />
	</authz:authorize>
	<display:column property="shortName" sortable="true" titleKey="arrayDesign.shortName"
		comparator="ubic.gemma.web.taglib.displaytag.StringComparator" />
	<display:column property="taxon" sortable="true" titleKey="arrayDesign.taxon" />
	<display:column property="expressionExperimentCountLink" sortable="true" title="Expts"
		comparator="ubic.gemma.web.taglib.displaytag.NumberComparator" />
	<display:column property="summaryTable" title="Probe Summary" />
	<authz:authorize ifAnyGranted="admin">
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
	</authz:authorize>

	<display:setProperty name="basic.empty.showtable" value="true" />

</display:table>






