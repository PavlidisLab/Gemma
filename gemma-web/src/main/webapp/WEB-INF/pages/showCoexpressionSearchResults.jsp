<%@ include file="/common/taglibs.jsp"%>

<jsp:useBean id="coexpressionSearchCommand" scope="request"
	class="ubic.gemma.web.controller.coexpressionSearch.CoexpressionSearchCommand" />


<h2>Results for
<% out.print(coexpressionSearchCommand.getSearchString()); %>
</h2> 

<display:table name="coexpressedGenes"
	class="list" requestURI="" id="foundGenes" 
	pagesize="200">
	<display:column property="name" sortable="true" titleKey="gene.name" />
	<display:setProperty name="basic.empty.showtable" value="false" />
</display:table>