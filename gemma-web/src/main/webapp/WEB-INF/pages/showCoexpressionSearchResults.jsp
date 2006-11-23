<%@ include file="/common/taglibs.jsp"%>

<jsp:useBean id="coexpressionSearchCommand" scope="request"
	class="ubic.gemma.web.controller.coexpressionSearch.CoexpressionSearchCommand" />


<h2>Results for
<% out.print(coexpressionSearchCommand.getSearchString()); %>
</h2> 

<display:table name="foundGenes"
	class="list" requestURI="" id="foundGenes" 
	pagesize="10">
	<display:setProperty name="basic.empty.showtable" value="false" />
</display:table>