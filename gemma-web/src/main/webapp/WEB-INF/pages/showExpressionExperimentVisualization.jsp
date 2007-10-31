<%@ include file="/common/taglibs.jsp"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<jsp:useBean id="expressionDataMatrix" scope="request"
	class="ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix" />
<jsp:useBean id="command" scope="request"
	class="ubic.gemma.web.controller.visualization.ExpressionExperimentVisualizationCommand" />

<title><fmt:message key="expression.visualization.results" /></title>

<h2>
	<fmt:message key="expression.visualization.results" />
</h2>
<li>
	Expression Experiment:
	<i><c:out value="${expressionExperiment.name}" /> (<a
		href="<c:url value="/expressionExperiment/showExpressionExperiment.html?id=${expressionExperiment.id}"/>"><c:out
				value="${expressionExperiment.shortName }" />)</a> </i>
</li>
<br />
<li>
	Quantitation Type:
	<i><c:out value="${quantitationType.name}" /> </i>
</li>
<br />
<li>
	<c:if test="${!viewSampling}">
        	Search Criteria :  <i><c:out value="${searchCriteria}" />
			(<c:out value="${searchString}" />)</i>
	</c:if>
	<c:if test="${viewSampling}">
		<i>(Viewing a sampling of the data)</i>
	</c:if>
</li>
<br />
<c:if test="${maskMissing}">
	<li>
		<i>(Masking missing values)</i>
	</li>
</c:if>
<br />

<div id="logo">
	<a> <Gemma:expressionDataMatrix genes="${genes}"
			expressionDataMatrix="<%=expressionDataMatrix%>" /> </a>
</div>
