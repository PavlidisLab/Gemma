<%@ include file="/common/taglibs.jsp"%>

<jsp:useBean id="coexpressionSearchCommand" scope="request"
	class="ubic.gemma.web.controller.coexpressionSearch.CoexpressionSearchCommand" />


<display:table name="coexpressedGenes"
	class="list" requestURI="" id="foundGenes" 
	decorator="ubic.gemma.web.taglib.displaytag.coexpressionSearch.CoexpressionWrapper" 
	pagesize="200">
	<display:column property="nameLink" sortable="true" titleKey="gene.name" href="gene/showGene.html" paramId="id" paramProperty="id"/>
	<display:column property="officialName" sortable="true" titleKey="gene.officialName" />
	<display:setProperty name="basic.empty.showtable" value="false" />
</display:table>