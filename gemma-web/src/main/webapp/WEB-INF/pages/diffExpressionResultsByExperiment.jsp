<%@ include file="/common/taglibs.jsp"%>


<title>Differential expression results for ${geneOfficialSymbol}</title>

<h3>
	Displaying
	<b> ${numDiffResults}</b> dataset(s) where differential expression of
	<b> ${geneOfficialSymbol} </b> meet your threhsold
	<br />
	<br />
	Threshold Q-value:
	<b><c:out value="${threshold}" /> </b>
	<br />
</h3>

<display:table name="differentialExpressionValueObjects" pagesize="200" sort="list" defaultsort="3" class="list" >
	<display:column property="expressionExperiment.shortName" title="Short name" sortable="true" />
	<display:column property="expressionExperiment.name" title ="Experiment name" sortable="true" />
	<display:column property="p" title="FDR (q)" sortable="true" />
</display:table>

<br />
<a href="/Gemma/diff/diffExpressionSearch.html"> Differential Expression Search </a>
