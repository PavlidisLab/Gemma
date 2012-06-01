<%@ include file="/common/taglibs.jsp"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<head>
<title>All Bibliographic References for Experiments</title>
</head>

<div style="padding-left: 15px">
<h3>List of all ${fn:length(citationToEEs)} published papers with data in Gemma:</h3>

<table>
<c:forEach items="${citationToEEs}" var="citationToEE">
	<tr>
	<td>
		<a href="/Gemma/expressionExperiment/showExpressionExperiment.html?id=${citationToEE.value.id}">
		
		<c:out value="${citationToEE.value.shortName}"></c:out>
		</a>
	</td>
	<td style="padding-right: 10px">
		<a href="/Gemma/expressionExperiment/showExpressionExperiment.html?id=${citationToEE.key.pubmedURL}">
			<img src="/Gemma/images/pubmed.gif" alt="PubMed link"/>
		</a>
	<td>
		<c:out value="${citationToEE.key.citation}"></c:out>
	</td></tr>
</c:forEach>
</table>

</div>
