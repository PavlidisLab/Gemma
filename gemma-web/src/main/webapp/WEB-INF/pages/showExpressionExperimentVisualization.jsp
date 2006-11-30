<%@ include file="/common/taglibs.jsp"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<jsp:useBean id="expressionDataMatrix" scope="request"
    class="ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix" />
<html>
    <body>
        <h2>
            <fmt:message key="expression.visualization.results" />
        </h2>
        Expression Experiment : <br />
        <ul>
        <li><c:out value="${expressionExperiment.name}" /> </li>
        </ul>
        Quantitation Type : <br />
        <ul>
        <li><c:out value="${quantitationType}" /> </li>
        </ul>
        
        <c:if test="${!viewSampling}">
        	Search Criteria : <br />
       		<ul>
       		<li><c:out value="${searchCriteria}" /> (<c:out value="${searchCriteriaValue}" />)</li>
       		</ul>
       	</c:if>
       	
       	<c:if test="${viewSampling}">
       	       (Viewing a sampling of the data) <br />
       	</c:if>
       	
		<div id="logo">
            <a> 
                <Gemma:expressionDataMatrix expressionDataMatrix="<%=expressionDataMatrix%>"/>
            </a>
        </div>
    </body>
</html>
