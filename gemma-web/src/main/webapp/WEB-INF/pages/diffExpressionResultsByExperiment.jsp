<%@ include file="/common/taglibs.jsp"%>


<title><fmt:message key="expressionExperiments.title" /></title>

<h3>
	Displaying
	<b> <c:out value="${numDiffResults}" /> </b> dataset(s) where
	differential expression results exist for gene
	<b><c:out value="${gene.name}" /> </b>
	<br />
	<br />
	Threshold:
	<b><c:out value="${threshold}" /> </b>
	<br />
</h3>

<table border="1">
	<c:forEach items="${diffResults}" var="diffResultsMap">
		<c:set var="e" value="${diffResultsMap.key}" />
		<c:set var="results" value="${diffResultsMap.value}" />

		<tr>
			<td valign="middle" align="center">
				<b>Experiment</b>
			</td>
			<td valign="middle" align="center">
				<b>Description</b>
			</td>
			<td valign="middle" align="center">
				<b>p-value</b>
			</td>
		</tr>
		<tr>
			<td>
				${e.shortName}
			</td>
			<td>
				${e.description}
			</td>
			<td>
				<c:forEach items="${results}" var="r">
					<c:set var="pvalues" value="${r.pvalue}" />
					<pre>${pvalues}</pre>
				</c:forEach>
			</td>
		</tr>
	</c:forEach>
</table>

<br />
<a href="/Gemma/diff/diffExpressionSearch.html"> Differential
	Expression Search </a>