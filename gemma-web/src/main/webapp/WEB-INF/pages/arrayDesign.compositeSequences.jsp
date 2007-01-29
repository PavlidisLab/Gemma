<%@ include file="/common/taglibs.jsp"%>
<jsp:useBean id="arrayDesign" scope="request"
	class="ubic.gemma.model.expression.arrayDesign.ArrayDesignImpl" />

<script type="text/javascript"
	src="<c:url value="/scripts/scrolltable.js"/>"></script>
<link rel="stylesheet" type="text/css"
	href="<c:url value='/styles/scrolltable.css'/>" />

<title>Composite Sequences : <jsp:getProperty name="arrayDesign"
		property="name" /></title>

<content tag="heading">
Composite Sequence Viewer : Platform
<jsp:getProperty name="arrayDesign" property="name" />
(
<jsp:getProperty name="arrayDesign" property="shortName" />
)

</content>


<p>
	Displaying ${numCompositeSequences} probes.
</p>

<form name="CompositeSequenceFilter"
	action="<c:url value="/designElement/filterCompositeSequences.html" />"
	method="POST">
	<h4>
		Enter search criteria for finding specific probes here
	</h4>
	<input type="text" name="filter" size="78" />
	<input type="hidden" name="arid"  value="<jsp:getProperty name="arrayDesign" property="id" />" />
	<input type="submit" value="Find" />
</form>

<c:if test="${sequenceData != null}">
	<script type="text/javascript" src="<c:url value="/scripts/aa.js"/>"></script>

	<aa:zone name="csTable">
		<table width="100%" align="center" bgcolor="#CCFFFF" border="1">
			<tr>
				<td>
					Composite Sequence information will be displayed here when a
					composite sequence is clicked.
				</td>
			</tr>
		</table>
	</aa:zone>
	<script type='text/javascript'
		src='/Gemma/scripts/expandableObjects.js'></script>
	<div id="tableContainer" class="tableContainer">
		<display:table name="sequenceData" sort="list" class="scrollTable"
			requestURI="" id="arrayDesignSequenceList" pagesize="500"
			decorator="ubic.gemma.web.taglib.displaytag.expression.arrayDesign.ArrayDesignMapResultWrapper">
			<display:column property="compositeSequenceNameLink" sortable="true"
				title="Composite Sequence" sortProperty="compositeSequenceName"
				headerClass="fixedHeader" />
			<display:column property="bioSequenceName" sortable="true"
				title="BioSequence" headerClass="fixedHeader" />
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
<a class="helpLink" href="?"
	onclick="showHelpTip(event, 'Summarizes multiple probes (array design elements).'); return false"><img
		src="/Gemma/images/help.png" />
</a>
