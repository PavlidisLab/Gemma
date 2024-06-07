<%@ include file="/common/taglibs.jsp" %>

<!--
This page handles servlet errors sent via HttpServletResponse.sendError().

If you can, avoid producing error this way and prefer raising an exception that will be caught by Spring MVC ExceptionResolver.
-->

<%@ page isErrorPage="true" %>

<page:applyDecorator name="default">
    <c:choose>
        <c:when test="${requestScope['javax.servlet.error.status_code'] == 400}">
            <%@ include file="/pages/error/400.jsp" %>
        </c:when>
        <c:when test="${requestScope['javax.servlet.error.status_code'] == 403}">
            <%@ include file="/pages/error/403.jsp" %>
        </c:when>
        <c:when test="${requestScope['javax.servlet.error.status_code'] == 404}">
            <%@ include file="/pages/error/404.jsp" %>
        </c:when>
        <c:when test="${requestScope['javax.servlet.error.status_code'] == 405}">
            <%@ include file="/pages/error/405.jsp" %>
        </c:when>
        <c:when test="${requestScope['javax.servlet.error.status_code'] == 406}">
            <%@ include file="/pages/error/406.jsp" %>
        </c:when>
        <c:when test="${requestScope['javax.servlet.error.status_code'] == 415}">
            <%@ include file="/pages/error/415.jsp" %>
        </c:when>
        <c:when test="${requestScope['javax.servlet.error.status_code'] == 500}">
            <%@ include file="/pages/error/500.jsp" %>
        </c:when>
        <c:when test="${requestScope['javax.servlet.error.status_code'] == 501}">
            <%@ include file="/pages/error/501.jsp" %>
        </c:when>
        <c:otherwise>
            <div class="padded">
                <h2>Unsupported Status Code</h2>
                <p>
                    There is no error page configured for the status code <c:out value="${requestScope['javax.servlet.error.status_code']}"/>.
                </p>
                <hr class="normal">
                <%@ include file="/common/exception.jsp" %>
            </div>
        </c:otherwise>
    </c:choose>
</page:applyDecorator>