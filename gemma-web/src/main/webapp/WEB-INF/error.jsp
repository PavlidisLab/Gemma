<%@ page isErrorPage="true" %>

<%@ include file="/WEB-INF/common/taglibs.jsp" %>

<%--
This page handles servlet errors sent via HttpServletResponse.sendError(). See the <error-page/> declaration in web.xml.

If you can, avoid producing error this way and prefer raising an exception that will be caught by Spring MVC ExceptionResolver.
--%>

<page:applyDecorator name="default">
    <c:choose>
        <c:when test="${requestScope['javax.servlet.error.status_code'] == 400}">
            <%@ include file="/WEB-INF/pages/error/400.jsp" %>
        </c:when>
        <c:when test="${requestScope['javax.servlet.error.status_code'] == 403}">
            <%@ include file="/WEB-INF/pages/error/403.jsp" %>
        </c:when>
        <c:when test="${requestScope['javax.servlet.error.status_code'] == 404}">
            <%@ include file="/WEB-INF/pages/error/404.jsp" %>
        </c:when>
        <c:when test="${requestScope['javax.servlet.error.status_code'] == 405}">
            <%@ include file="/WEB-INF/pages/error/405.jsp" %>
        </c:when>
        <c:when test="${requestScope['javax.servlet.error.status_code'] == 406}">
            <%@ include file="/WEB-INF/pages/error/406.jsp" %>
        </c:when>
        <c:when test="${requestScope['javax.servlet.error.status_code'] == 415}">
            <%@ include file="/WEB-INF/pages/error/415.jsp" %>
        </c:when>
        <c:when test="${requestScope['javax.servlet.error.status_code'] == 500}">
            <%@ include file="/WEB-INF/pages/error/500.jsp" %>
        </c:when>
        <c:when test="${requestScope['javax.servlet.error.status_code'] == 501}">
            <%@ include file="/WEB-INF/pages/error/501.jsp" %>
        </c:when>
        <c:otherwise>
            <div class="padded">
                <h2>Unsupported Status Code</h2>
                <p>
                    There is no error page configured for the status code <c:out
                        value="${requestScope['javax.servlet.error.status_code']}" />.
                </p>
                <hr class="normal">
                <%@ include file="/WEB-INF/common/exception.jsp" %>
            </div>
        </c:otherwise>
    </c:choose>
</page:applyDecorator>