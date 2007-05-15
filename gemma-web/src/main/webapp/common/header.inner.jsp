<%@ include file="/common/taglibs.jsp"%>
<c:if test="${pageContext.request.locale.language != 'en'}">
	<div id="switchLocale">
		<a href="<c:url value='/mainMenu.html?locale=en'/>"><fmt:message
				key="webapp.name" /> in English</a>
	</div>
</c:if>

<c:if test="${appConfig['maintenanceMode']}">
	<div style="font-weight:bold; color:#AA4444; font-size:1.3em">
		Gemma is undergoing maintenance! Some functions may not be available.
	</div>
</c:if>

<div id="branding">
	<div id="headerLeft">
		<a href="/Gemma" ><img
				src="<c:url value='/images/logo/gemmalogo50.gif'/>" alt="gemma"
				width="177" height="79" /> </a>
	</div>

</div>

<%-- Put constants into request scope --%>
<Gemma:constants scope="request" />

 
<div id="search">
	<%@ include file="/common/search.jsp"%>
</div>

