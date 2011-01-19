<%-- Adds the page help link if not a help page already --%>
<%@ include file="/common/taglibs.jsp"%>

<c:choose>
	<c:when
		test="${fn:endsWith(pageContext.request.requestURI, '.jsp') 
		or fn:containsIgnoreCase(pageContext.request.requestURI, '_help') 
		or fn:containsIgnoreCase(pageContext.request.requestURI, 'static')}">
	</c:when>
	<c:otherwise>
		<%
		/*
		* Replace '.html' with _help.html'.
		*/
		String helpuri = request.getRequestURI().substring(0,
							request.getRequestURI().length() - 5)
							+ "_help.html";
		helpuri = helpuri.replace("Gemma/", "Gemma/static/");
		
		/*
		 * Check if help file exists, should not show link if it doesn't 
		 */
		String path = getServletContext().getRealPath(helpuri).replace("Gemma/Gemma/", "Gemma/");
		 if ((new java.io.File(path)).exists()){%>
		<div id="help" style="font-size: smaller; float: right;">
			<a target="_blank" href="<%=helpuri%>">page help</a>
		</div>		
		<% }%>
	</c:otherwise>
</c:choose>
