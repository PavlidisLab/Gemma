<%@ include file="/common/taglibs.jsp"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<jsp:useBean id="expressionDataMatrix" scope="request"
    class="ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix" />
<html>
    <body>
        <h2>
            <fmt:message key="expression.visualization.results" />
        </h2>
        <li>
        Expression Experiment:  <i><c:out value="${expressionExperiment.name}" /></i>
        </li>
        <br/>
        <li>
        Quantitation Type:  <i><c:out value="${quantitationType}" /></i> 
        </li>
        <br/>
        <li>
        <c:if test="${!viewSampling}">
        	Search Criteria :  <i><c:out value="${searchCriteria}" /> (<c:out value="${searchCriteriaValue}" />)</i>
       	</c:if>
       	<c:if test="${viewSampling}">
       	       <i>(Viewing a sampling of the data)</i>
       	</c:if>
       	</li>
       	<br/>
       	
		<div id="logo">
            <a> 
                <Gemma:expressionDataMatrix expressionDataMatrix="<%=expressionDataMatrix%>"/>
            </a>
        </div>
    </body>
</html>
