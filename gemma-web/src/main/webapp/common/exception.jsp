<%@ include file="/common/taglibs.jsp" %>

<c:choose>
    <c:when test="${not empty requestScope['exception']}">
        <hr class="normal">
        <Gemma:exception exception="${requestScope['exception']}" />
    </c:when>
    <c:when test="${not empty requestScope['javax.servlet.error.exception']}">
        <hr class="normal">
        <%-- this is causing stackoverflow errors ... no idea why, since upgrading to spring 3.2 from 3.0.7 --%>
        <Gemma:exception exception="${requestScope['javax.servlet.error.exception']}" />
    </c:when>
    <c:when test="${not empty requestScope['javax.servlet.error.message']}">
        <hr class="normal">
        <div class="exception">
            <p class="message">${fn:escapeXml(requestScope['javax.servlet.error.message'])}" /></p>
        </div>
    </c:when>
</c:choose>