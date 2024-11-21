<%@ include file="/common/taglibs.jsp" %>
<c:if test="${not empty messages}">
    <div class="message">
        <c:forEach var="msg" items="${messages}">
            <i class="qtp fa fa-info-circle fa-lg fa-fw"></i>
            <c:out value="${msg}" escapeXml="false" /><br />
        </c:forEach>
    </div>
    <c:remove var="messages" scope="session" />
</c:if>