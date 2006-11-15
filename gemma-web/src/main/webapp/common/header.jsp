<%@ include file="/common/taglibs.jsp"%>
<c:if test="${pageContext.request.locale.language != 'en'}">
	<div id="switchLocale">
		<a href="<c:url value='/mainMenu.html?locale=en'/>"><fmt:message key="webapp.name" /> in English</a>
	</div>
</c:if>


<div id="branding">
	<div id="headerLeft">
		<a href="/Gemma" target="_blank"><img
			src="<c:url value='/images/logo/gemmalogo.gif'/>" alt="gemma" width="299" height="175"  /> </a>
	</div>
	<div id="headerRight">	
		<a href="http://bioinformatics.ubc.ca/" target="_blank"><img src="<c:url value='/images/logo/ubiclogo.gif'/>"
				alt="UBiC" width="159" height="175" /> </a>
	</div>
</div>

<div id="search">
	<%@ include file="/common/search.jsp"%>
</div>
<hr />


<%-- Put constants into request scope --%>
<Gemma:constants scope="request" />

