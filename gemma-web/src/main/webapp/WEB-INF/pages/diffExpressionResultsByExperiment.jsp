<%@ include file="/common/taglibs.jsp"%>


<title><fmt:message key="expressionExperiments.title" /></title>

<h3>
	Displaying
	<b> <c:out value="${numDiffResults}" /> </b> dataset(s) where
	differential expression results exist for gene
	<b><c:out value="${geneOfficialSymbol}" /> </b>
	<br />
	<br />
	Threshold:
	<b><c:out value="${threshold}" /> </b>
	<br />
</h3>

<display:table name="differentialExpressionValueObjects">
	<display:column property="expressionExperiment.shortName" />
	<display:column property="expressionExperiment.name" />
	<display:column property="p" />
</display:table>

<br />
<a href="/Gemma/diff/diffExpressionSearch.html"> Differential
	Expression Search </a>
