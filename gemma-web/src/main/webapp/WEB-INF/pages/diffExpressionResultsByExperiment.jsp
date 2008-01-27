<%@ include file="/common/taglibs.jsp"%>


<title><fmt:message key="expressionExperiments.title" />
</title>

<h3>
	Displaying
	<b> <c:out value="${numDiffResults}" /> </b> dataset(s) where
	differential expression results exist for gene
	<b><c:out value="${gene}" /> </b> that meet the threshold of
	<b><c:out value="${threshold}" /> </b>
</h3>


<c:forEach items="${diffResults}" var="diffResultsMap">
    ${diffResultsMap.key} has the value ${diffResultsMap.value}
</c:forEach>
