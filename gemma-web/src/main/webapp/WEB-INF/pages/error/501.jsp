<%@ include file="/WEB-INF/common/taglibs.jsp" %>

<title><fmt:message key="501.title"/></title>

<div class="padded">
    <h2><fmt:message key="501.title"/></h2>
    <p>
        <fmt:message key="501.message">
            <fmt:param>
                <c:url value="/home.html"/>
            </fmt:param>
        </fmt:message>
    </p>
    <%@ include file="/WEB-INF/common/exception.jsp" %>
</div>
