<%@ include file="/common/taglibs.jsp"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
    <body>
        <h2>
            <fmt:message key="expression.visualization.results" />
        </h2>
        
		<div id="logo">
            <a href="<%=request.getContextPath()%>">
                <%-- <img src="${visualization}"/> --%>
                <%--<applet code="/applets/HtmlMatrixVisualizerApplet.class" width=550 height=250></applet>--%>
                <jsp:plugin type="applet" code="applet.HtmlMatrixVisualizerApplet.class" codebase="/Gemma"
                jreversion="1.5" width="160" height="150">
                <jsp:params>
                	<jsp:param name="bgcolor" value="ccddff" />
                </jsp:params>
                	<jsp:fallback>Plugin tag OBJECT or EMBED bot supported by browser</jsp:fallback>
                </jsp:plugin>
                
            </a>
        </div>
    </body>
</html>
