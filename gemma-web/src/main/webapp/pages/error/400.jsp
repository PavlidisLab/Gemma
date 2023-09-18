<%@ include file="/common/taglibs.jsp" %>

<title><fmt:message key="400.title"/></title>

<content tag="heading">
    <input type="hidden" id="reloadOnLogin" value="true"/>
</content>

<div class="padded">
    <h2><fmt:message key="400.title"/></h2>

    <p>
        <fmt:message key="400.message">
            <fmt:param>
                <c:url value="/home.html"/>
            </fmt:param>
        </fmt:message>
    </p>

    <hr class="normal">

    <p><c:out value="${exception.message}"/></p>

    <security:authorize access="hasAuthority('GROUP_ADMIN')">
        <Gemma:exception exception="${exception}"/>
    </security:authorize>

</div>