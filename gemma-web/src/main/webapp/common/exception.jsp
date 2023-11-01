<%@ include file="/common/taglibs.jsp"%>

<c:choose>
    <c:when test="${not empty requestScope['exception']}">
        <hr class="normal">
        <p><c:out value="${fn:escapeXml(requestScope['exception'].message)}"/></p>
        <security:authorize access="hasAuthority('GROUP_ADMIN')">
            <Gemma:exception exception="${requestScope['exception']}"/>
        </security:authorize>
    </c:when>

    <c:when test="${not empty requestScope['javax.servlet.error.exception']}">
        <hr class="normal">
        <p><c:out value="${fn:escapeXml(requestScope['javax.servlet.error.exception'].message)}"/></p>
        <security:authorize access="hasAuthority('GROUP_ADMIN')">
            <%-- this is causing stackoverflow errors ... no idea why, since upgrading to spring 3.2 from 3.0.7 --%>
            <Gemma:exception exception="${requestScope['javax.servlet.error.exception']}"/>
        </security:authorize>
    </c:when>

    <c:when test="${not empty requestScope['javax.servlet.error.message']}">
        <hr class="normal">
        <p><c:out value="${fn:escapeXml(requestScope['javax.servlet.error.message'])}"/></p>
    </c:when>

</c:choose>