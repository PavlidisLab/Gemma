<%@ include file="/common/taglibs.jsp"%>
<c:if test="${pageContext.request.locale.language != 'en'}">
	<div id="switchLocale">
		<a href="<c:url value='/mainMenu.html?locale=en'/>"><fmt:message key="webapp.name" /> in English</a>
	</div>
</c:if>


<div id="branding">
	<h1>
		<a href="/Gemma">Gemma</a>
	</h1>
	<p>
		Microarray experiments on steroids.
	</p>
</div>
<hr />


<%-- Put constants into request scope --%>
<Gemma:constants scope="request" />

