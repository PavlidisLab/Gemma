<%@ include file="/common/taglibs.jsp" %>

<title><fmt:message key="403.title"/></title>

<content tag="heading">
    <input type="hidden" id="reloadOnLogin" value="true"/>
</content>

<div class="padded">

    <h2><fmt:message key="403.title"/></h2>

    <p>
        <fmt:message key="403.message">
            <fmt:param>
                <c:url value="/home.html"/>
            </fmt:param>
        </fmt:message>
    </p>

    <hr class="normal">

    <security:authorize access="!hasAnyAuthority('GROUP_USER','GROUP_ADMIN')">
        <script type="text/javascript">
           Gemma.AjaxLogin.showLoginWindowFn( true );
        </script>
        <p>You do not have permission to access this page.</p>
    </security:authorize>

    <security:authorize access="hasAuthority('GROUP_ADMIN')">
        <p>${exception.message}</p>
        <Gemma:exception exception="${exception}"/>
    </security:authorize>

</div>
