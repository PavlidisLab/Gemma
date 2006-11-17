<%@ include file="/common/taglibs.jsp"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<jsp:useBean id="expressionDataMatrix" scope="request"
    class="ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix" />
<html>
    <body>
        <h2>
            <fmt:message key="expression.visualization.results" />
        </h2>
		<div id="logo">
            <a> 
                <Gemma:expressionDataMatrix expressionDataMatrix="<%=expressionDataMatrix%>"/>
            </a>
        </div>
    </body>
</html>
