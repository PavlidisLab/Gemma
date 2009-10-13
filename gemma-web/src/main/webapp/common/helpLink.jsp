<%-- Adds the page help link if not a help page already --%>
<%@ include file="/common/taglibs.jsp"%>

<c:choose>
	<c:when
		test="${fn:endsWith(pageContext.request.requestURI, '.jsp') or fn:containsIgnoreCase(pageContext.request.requestURI, '_help') or fn:containsIgnoreCase(pageContext.request.requestURI, 'static')}">
	</c:when>
	<c:otherwise>
		<div id="help" style="font-size: smaller; float: right;">
			<a target="_blank"
				href="
	<%
	/*
	* Replace '.html' with _help.html'.
	*/

	String helpuri = request.getRequestURI().substring(0,
						request.getRequestURI().length() - 5)
						+ "_help.html";
				helpuri = helpuri.replace("Gemma/", "Gemma/static/");
				out.print(helpuri);
 
				%>
		">page
				help</a>
		</div>
	</c:otherwise>
</c:choose>
