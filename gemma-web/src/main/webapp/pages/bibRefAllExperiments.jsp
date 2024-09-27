<%@ include file="/common/taglibs.jsp"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<head>
<title>All Bibliographic References for Experiments</title>
</head>

<div style="padding-left: 15px">
<h3>List of all ${fn:length(citationToEEs)} published papers with data in Gemma:</h3>
To search for a paper or experiment and see more details, visit the
<a href="${pageContext.request.contextPath}/bibRef/searchBibRefs.html">annotated paper search page</a>.
<br><br>
<table>
<c:forEach items="${citationToEEs}" var="citationToEE">
	<tr>
	<td>
		<c:out value="${citationToEE.key.citation}"></c:out>
	</td>
		<td style="padding-right: 10px">
		<a target="_blank" href="${citationToEE.key.pubmedURL}" rel="noopener noreferrer">
			<img src="${pageContext.request.contextPath}/images/pubmed.gif" alt="PubMed link"/>
		</a>
		</td>
		<td>
		<c:forEach items="${citationToEE.value}" var="ee">
			<a href="${pageContext.request.contextPath}/expressionExperiment/showExpressionExperiment.html?id=${ee.id}">
				<c:out value="${ee.shortName}"></c:out>
			</a>&nbsp;&nbsp;
		</c:forEach>
	</td>
	</tr>
</c:forEach>
</table>

</div>
