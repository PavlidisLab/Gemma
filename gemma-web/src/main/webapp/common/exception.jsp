<%@ include file="/common/taglibs.jsp" %>

<c:choose>
    <c:when test="${not empty requestScope['exception']}">
        <hr class="normal">
        <Gemma:exception exception="${requestScope['exception']}" />
    </c:when>
    <c:when test="${not empty requestScope['jakarta.servlet.error.exception']}">
        <hr class="normal">
        <%-- this is causing stackoverflow errors ... no idea why, since upgrading to spring 4.2 from 3.0.7 --%>
        <Gemma:exception exception="${requestScope['jakarta.servlet.error.exception']}" />
    </c:when>
    <c:when test="${not empty requestScope['jakarta.servlet.error.message']}">
        <hr class="normal">
        <div class="exception">
            <p class="message">${fn:escapeXml(requestScope['jakarta.servlet.error.message'])}</p>
        </div>
    </c:when>
</c:choose>