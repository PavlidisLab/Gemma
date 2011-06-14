
<!--  Default header for only the main gemma home page -->
<%@ include file="/common/taglibs.jsp"%>
<jwr:script src='/scripts/ajax/ext/data/DwrProxy.js' />
<c:if test="${pageContext.request.locale.language != 'en'}">
	<div id="switchLocale">
		<a href="<c:url value='/gemmaClassic.html?locale=en'/>"><fmt:message key="webapp.name" /> in English</a>
	</div>
</c:if>

<c:if test="${appConfig['maintenanceMode']}">
	<div style="font-weight: bold; color: #AA4444; font-size: 1.3em">
		Gemma is undergoing maintenance! Some functions may not be available.
	</div>
</c:if>

<div id="search">
	<%@ include file="/common/search.jsp"%>
</div>
<div id="branding">

	<div id="headerLeft">
		<a href="<c:url value='/gemmaClassic.html'/>"><img src="<c:url value='/images/logo/gemma-lg153x350.gif'/>" alt="gemma" />
		</a>
	</div>


</div>

<%-- Put constants into request scope --%>
<Gemma:constants scope="request" />
