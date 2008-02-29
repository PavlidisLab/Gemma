<%@ include file="/common/taglibs.jsp"%>


<title><fmt:message key="expressionExperiments.title" />
</title>

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
	<tr>
		<td valign="middle" align="center">
			<b>Experiment</b>
		</td>
		<td valign="middle" align="center">
			<b>Description</b>
		</td>
		<td valign="middle" align="center">
			<b>q-value</b>
		</td>
	</tr>
	<c:forEach items="${diffResults}" var="diffResultsMap">
		<c:set var="e" value="${diffResultsMap.key}" />
		<c:set var="results" value="${diffResultsMap.value}" />

		<tr>
			<td>
				${e.shortName}
			</td>
			<td>
				${e.name}
			</td>
			<td>
				<c:forEach items="${results}" var="r">
					<c:set var="qvalues" value="${r.correctedPvalue}" />
					<pre>${qvalues}</pre>
				</c:forEach>
			</td>
		</tr>
	</c:forEach>
</table>

<br />
<a href="/Gemma/diff/diffExpressionSearch.html"> Differential
	Expression Search </a>
