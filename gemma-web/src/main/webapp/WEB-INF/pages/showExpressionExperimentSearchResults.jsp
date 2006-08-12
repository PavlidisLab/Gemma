<%@ include file="/common/taglibs.jsp"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<jsp:useBean id="matrixVisualizer" scope="request"
    class="ubic.gemma.visualization.ExpressionDataMatrixVisualizer" />
<html>
    <body>
        <h2>
            <fmt:message key="expression.visualization.results" />
        </h2>
		<div id="logo">
				<%--<img src="<%=expressionDataMatrixVisualization.getOutfile()%>" width="300" height="300"/>--%>

                <%--<applet code="/applets/HtmlMatrixVisualizerApplet.class" width=550 height=250></applet>--%>
       
                <%--<jsp:plugin type="applet" archive="Blur.jar" codebase="/Gemma/applet"--%>
                <%--
                <jsp:plugin type="applet" code="applet.HtmlMatrixVisualizerApplet.class" codebase="/Gemma"
                jreversion="1.5" width="160" height="150">
                <jsp:params>
                	<jsp:param name="bgcolor" value="ccddff" />	
                </jsp:params>
                	<jsp:fallback>Plugin tag OBJECT or EMBED bot supported by browser</jsp:fallback>
                </jsp:plugin>
                --%>
            <a> 
            	<%--
            	<%expressionDataMatrixVisualization.drawDynamicImage( stream);%>
            	<%response.setContentType( "image/jpg" );%>
  				--%>
                <Gemma:matrixVisualizer matrixVisualizer="<%=matrixVisualizer%>"/>
                
            </a>
        </div>
    </body>
</html>
