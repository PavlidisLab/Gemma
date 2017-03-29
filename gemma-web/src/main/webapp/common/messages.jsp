<%-- Error Messages --%>
<%@ include file="/common/taglibs.jsp"%>
<c:if test="${not empty errors}">
    <div class="error">	
        <c:forEach var="error" items="${errors}">
            <i class="red fa fa-warning fa-lg fa-fw"></i>
            <c:out value="${error}" escapeXml="false"/><br />
        </c:forEach>
    </div>
    <c:remove var="errors" scope="session"/>
</c:if>

<%-- Success Messages --%>
<c:if test="${not empty messages}">
    <div class="message">	
        <c:forEach var="msg" items="${messages}">
            <i class="qtp fa fa-info-circle fa-lg fa-fw"></i>
            <c:out value="${msg}" escapeXml="false"/><br />
        </c:forEach>
    </div>
    <c:remove var="messages" scope="session" />
</c:if>