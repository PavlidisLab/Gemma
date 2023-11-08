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

    <security:authorize access="!isAuthenticated()">
        <script type="text/javascript">
           Gemma.AjaxLogin.showLoginWindowFn( true );
        </script>
    </security:authorize>

    <%@ include file="/common/exception.jsp" %>

</div>
