<%@ include file="/WEB-INF/common/taglibs.jsp" %>

<title><fmt:message key="404.title" /></title>

<input type="hidden" id="reloadOnLogin" value="true" />

<div class="padded">
    <h2><fmt:message key="404.title" /></h2>
    <p>
        <fmt:message key="404.message">
            <fmt:param>
                <c:url value="/home.html"/>
            </fmt:param>
        </fmt:message>
    </p>
    <%@ include file="/WEB-INF/common/exception.jsp" %>
</div>