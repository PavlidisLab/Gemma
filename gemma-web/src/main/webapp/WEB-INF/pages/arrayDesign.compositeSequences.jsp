<%@ include file="/common/taglibs.jsp"%>
<jsp:useBean id="arrayDesign" scope="request" class="ubic.gemma.model.expression.arrayDesign.ArrayDesignImpl" />

<title>Composite Sequences : <jsp:getProperty name="arrayDesign" property="name" /> </title>

<content tag="heading">Composite Sequences : <jsp:getProperty name="arrayDesign" property="name" /></content>

<c:if test="${sequenceData != null}">
	<script type='text/javascript' src='/Gemma/scripts/expandableObjects.js'></script>
	<display:table name="sequenceData" sort="list" class="list" requestURI="" id="arrayDesignSequenceList"
		pagesize="200" decorator="ubic.gemma.web.taglib.displaytag.expression.arrayDesign.ArrayDesignMapResultWrapper">
		<display:column property="compositeSequenceName" sortable="true" title="Composite Sequence"/>
		<display:column property="bioSequenceName" sortable="true" title="BioSequence" />
		<display:column property="numBlatHits" sortable="true"  title="# Hits"/>
		<display:column property="geneList" title="Genes" />
		<display:setProperty name="basic.empty.showtable" value="true" />
	</display:table>
</c:if>