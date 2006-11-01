<%@ include file="/common/taglibs.jsp"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<jsp:useBean id="expressionDataMatrixVisualizer" scope="request"
    class="ubic.gemma.visualization.DefaultExpressionDataMatrixVisualizer" />
<html>
    <body>
        <h2>
            <fmt:message key="expression.visualization.results" />
        </h2>
		<div id="logo">
            <a> 
                <Gemma:expressionDataMatrixVisualizer expressionDataMatrixVisualizer="<%=expressionDataMatrixVisualizer%>"/>
            </a>
        </div>
    </body>
</html>
