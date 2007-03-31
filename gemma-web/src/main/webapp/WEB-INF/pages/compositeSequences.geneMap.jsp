<%@ include file="/common/taglibs.jsp"%>
<jsp:useBean id="arrayDesign" scope="request"
	class="ubic.gemma.model.expression.arrayDesign.ArrayDesignImpl" />

<script type="text/javascript"
	src="<c:url value="/scripts/scrolltable.js"/>"></script>
<link rel="stylesheet" type="text/css"
	href="<c:url value='/styles/scrolltable.css'/>" />


<title><c:if test="${arrayDesign.id != null}">
Probes for : <jsp:getProperty name="arrayDesign" property="name" />
	</c:if> <c:if test="${gene != null}">
Probes for : ${gene.officialSymbol}
</c:if></title>

<h2>
	<a class="helpLink" href="?"
		onclick="showHelpTip(event, 'This page displays information on multiple probes (array design elements). Enter a gene symbol or probe identifier into the form to find matches.'); return false"><img
			src="/Gemma/images/help.png" /> </a> Probe Viewer

	<c:if test="${arrayDesign.id != null}">		
for <br />
		<jsp:getProperty name="arrayDesign" property="name" />
(
<a
			href="<c:url value="/arrays/showArrayDesign.html?id=${arrayDesign.id }" />"
			<jsp:getProperty name="arrayDesign" property="shortName" /></a>
)
</c:if>
</h2>


<p>
	Displaying ${numCompositeSequences} probes.
</p>

<c:if test="${arrayDesign.id != null}">
	<form name="CompositeSequenceFilter"
		action="<c:url value="/designElement/filterCompositeSequences.html" />"
		method="POST">
		<h4>
			Enter search criteria for finding specific probes here
		</h4>
		<input type="text" name="filter" size="78" />
		<input type="hidden" name="arid"
			value="<jsp:getProperty name="arrayDesign" property="id" />" />
		<input type="submit" value="Find" />
	</form>
</c:if>

<br />

<c:if test="${sequenceData != null}">
	<script type="text/javascript" src="<c:url value="/scripts/aa.js"/>"></script>

	<aa:zone name="csTable">
		<table width="100%" align="center" bgcolor="#CCFFFF" border="1">
			<tr>
				<td>
					Detailed probe information will be displayed here when a links is
					clicked in the lower table.
				</td>
			</tr>
		</table>
	</aa:zone>
	<c:if test="${arrayDesign.id != null}">	
	(this page only displays a maximum of 100 probes at a time).
	</c:if>
	<br />
	<hr />
	<a class="helpLink" href="?"
		onclick="showWideHelpTip(event, 'Display of probe information for an entire microarray design or your search results. Columns are: <ul><li>Probe name: the manufacturer probe identifier. Click to view details.</li><li>Sequence name</li><li>#Hits: Number of distinct high-quality genome alignments for this sequence.</li><li>Genes that this probe is predicted to assay (count given by number in parentheses)</li></ul>'); return false"><img
			src="/Gemma/images/help.png" /> </a>

	<script type='text/javascript'
		src='/Gemma/scripts/expandableObjects.js'></script>
	<div id="tableContainer" class="tableContainer">
		<display:table name="sequenceData" sort="list" class="scrollTable"
			requestURI="" id="arrayDesignSequenceList" pagesize="500"
			decorator="ubic.gemma.web.taglib.displaytag.expression.arrayDesign.ArrayDesignMapResultWrapper">
			<display:column property="compositeSequenceNameLink" sortable="true"
				title="Probe name" sortProperty="compositeSequenceName"
				headerClass="fixedHeader" />
			<display:column property="bioSequenceName" sortable="true"
				title="Sequence name" headerClass="fixedHeader" />
			<display:column property="numBlatHits" sortable="true" title="# Hits"
				headerClass="fixedHeader" />
			<display:column property="geneList" title="Genes"
				headerClass="fixedHeader" />
			<display:setProperty name="basic.empty.showtable" value="true" />
		</display:table>
	</div>


	<script type="text/javascript"
		src="<c:url value="/scripts/aa-init.js"/>"></script>

</c:if>
