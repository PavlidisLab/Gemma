<%@ include file="/common/taglibs.jsp"%>


<%-- Security fields used in Java script calls to hide or display information on pages, used to be in footer --%>
<security:authorize access="hasRole('GROUP_ADMIN')">
	<input type="hidden" name="hasAdmin" id="hasAdmin" value="true" />
</security:authorize>
<security:authorize access="!hasRole('GROUP_ADMIN')">
	<input type="hidden" name="hasAdmin" id="hasAdmin" value="" />
</security:authorize>
<security:authorize access="hasRole('GROUP_USER')">
	<input type="hidden" name="hasUser" id="hasUser" value="true" />
</security:authorize>
<security:authorize access="!hasRole('GROUP_USER')">
	<input type="hidden" name="hasUser" id="hasUser" value="" />
</security:authorize>
<security:authorize ifAnyGranted="GROUP_USER,GROUP_ADMIN">
	<input type="hidden" name="loggedIn" id="loggedIn" value="true" />
</security:authorize>
<security:authorize ifNotGranted="GROUP_USER,GROUP_ADMIN">
	<input type="hidden" name="loggedIn" id="loggedIn" value="" />
</security:authorize>


<c:if test="${not empty pageContext.request.remoteUser}">
	<input type="hidden" id="username-logged-in" value="<security:authentication property="principal.username" />" />
</c:if>
<c:if test="${empty pageContext.request.remoteUser}">
	<input type="hidden" id="username-logged-in" value="" />
</c:if>

<%-- dump some other useful settings --%>
<input type="hidden" id="coexpressionSearch.maxGenesPerQuery" value='${appConfig["gemma.coexpressionSearch.maxGenesPerQuery"]}' />
<input type="hidden" id="coexpressionSearch.maxGenesPerCoexVisQuery" value='${appConfig["gemma.coexpressionSearch.maxGenesPerCoexVisQuery"]}' />
<input type="hidden" id="gemma.coexpressionSearch.maxResultsPerQueryGene" value='${appConfig["gemma.coexpressionSearch.maxResultsPerQueryGene"]}' />
