<%@ include file="/common/taglibs.jsp"%>

<spring:bind path="bibliographicReference.*">
    <c:if test="${not empty status.errorMessages}">
        <div class="error"><c:forEach var="error"
            items="${status.errorMessages}">
            <img src="<c:url value="/images/iconWarning.gif"/>"
                alt="<fmt:message key="icon.warning"/>" class="icon" />
            <c:out value="${error}" escapeXml="false" />
            <br />
        </c:forEach></div>
    </c:if>
</spring:bind>

<form method="post" action="<c:url value="/search.htm"/>"
    id="bibliographicReferenceForm" onsubmit="return onFormSubmit(this)">
<input type="hidden" name="_flowExecutionId"
    value="<c:out value="${flowExecutionId}"/>"> <input type="hidden"
    name="_eventId" value="submit"> <%--	
<input type="hidden" name="from" value="<c:out value="${param.from}"/>" />

<c:if test="${cookieLogin == 'true'}">
    <spring:bind path="user.password">
    <input type="hidden" name="password" value="<c:out value="${status.value}"/>"/>
    </spring:bind>
    <spring:bind path="user.confirmPassword">
    <input type="hidden" name="confirmPassword" value="<c:out value="${status.value}"/>"/>
    </spring:bind>
</c:if>
--%> <c:if test="${empty pubMed.title}">
    <%--    <input type="hidden" name="encryptPass" value="true" />  --%>
</c:if>

<table class="detail" >
    <c:set var="pageButtons">
        <tr>
            <td></td>
            <td class="buttonBar"><%--
            <input type="submit" class="button" name="save" 
                onclick="bCancel=false" value="<fmt:message key="button.save"/>" />
        --%> <input type="submit" class="button" name="save"
                onclick="bCancel=false;this.form._eventId.value='submit'"
                value="<fmt:message key="button.save"/>" /> <c:if
                test="${param.from == 'list'}">
                <input type="submit" class="button" name="delete"
                    onclick="bCancel=true;return confirmDelete('arrayDesign')"
                    value="<fmt:message key="button.delete"/>" />
            </c:if> <input type="submit" class="button" name="cancel"
                onclick="bCancel=true;this.form._eventId.value='cancel'"
                value="<fmt:message key="button.cancel"/>" /></td>
        </tr>
    </c:set>

    <tr>
        <th><Gemma:label key="pubMed.authors" /></th>
        <td><c:out value="${bibliographicReference.authorList}" /> <%--<c:out value="${arrayDesign.designProvider.name}"/>--%>
        </td>
    </tr>

    <tr>
        <th><Gemma:label key="pubMed.year" /></th>
        <td><c:out value="${bibliographicReference.publicationDate}" />
        </td>
    </tr>

    <tr>
        <th><Gemma:label key="pubMed.volume" /></th>
        <td><%--<c:out value="${bibliographicReference.volume}"/>--%> <input
            type="text" name="volume"
            value="<c:out value="${bibliographicReference.volume}"/>"
            id="volume" /></td>
    </tr>

    <tr>
        <th><Gemma:label key="pubMed.pages" /></th>
        <td><c:out value="${bibliographicReference.pages}" /></td>
    </tr>

    <tr>
        <th><Gemma:label key="pubMed.title" /></th>
        <td><spring:bind path="bibliographicReference.title">
            <c:choose>
                <c:when test="${empty pubMed.title}">
                    <textarea name="title" id="title" rows=8 cols=30><c:out
                        value="${status.value}" /></textarea>
                    <span class="fieldError"><c:out
                        value="${status.errorMessage}" /></span>
                </c:when>
                <c:otherwise>
                    <c:out value="${pubMed.title}" />
                    <input type="hidden" name="title"
                        value="<c:out value="${status.value}"/>"
                        id="title" />
                </c:otherwise>
            </c:choose>
        </spring:bind></td>
    </tr>

    <tr>
        <th><Gemma:label key="pubMed.abstract" /></th>
        <td><spring:bind path="bibliographicReference.abstractText">
            <textarea name="abstractText" id="abstractText" rows=8
                cols=30><c:out value="${status.value}" /></textarea>
            <span class="fieldError"><c:out
                value="${status.errorMessage}" /></span>
        </spring:bind></td>
    </tr>

    <%--    
<c:choose>
    <c:when test="${param.from == 'list' or param.method == 'Add'}">
    <tr>
        <td></td>
        <td>
            <fieldset class="pickList">
                <legend>
                    <fmt:message key="userProfile.assignRoles"/>
                </legend>
	            <table class="pickList">
	                <tr>
	                    <th class="pickLabel">
	                        <Gemma:label key="user.availableRoles" 
	                            colon="false" styleClass="required"/>
	                    </th>
	                    <td>
	                    </td>
	                    <th class="pickLabel">
	                        <Gemma:label key="user.roles"
	                            colon="false" styleClass="required"/>
	                    </th>
	                </tr>
	                <c:set var="leftList" value="${availableRoles}" scope="request"/>
	                <c:set var="rightList" value="${user.roleList}" scope="request"/>
	                <c:import url="/WEB-INF/pages/pickList.jsp">
	                    <c:param name="listCount" value="1"/>
	                    <c:param name="leftId" value="availableRoles"/>
	                    <c:param name="rightId" value="userRoles"/>
	                </c:import>
	            </table>
            </fieldset>
        </td>
    </tr>
    </c:when>
    <c:when test="${not empty user.userName}">
    <tr>
        <th>
            <Gemma:label key="user.roles"/>
        </th>
        <td>
        <c:forEach var="role" items="${user.userRoles}" varStatus="status">
            
            <c:out value="${role.userName}"/><c:if test="${!status.last}">,</c:if>
            <input type="hidden" name="userRoles"     
                value="<c:out value="${role.userName}"/>" />
        </c:forEach>
        </td>
    </tr>
    </c:when>
</c:choose>
--%>
    <%-- Print out buttons - defined at top of form --%>
    <%-- This is so you can put them at the top and the bottom if you like --%>
    <c:out value="${pageButtons}" escapeXml="false" />

</table>
</form>

<script type="text/javascript">
<!--
highlightFormElements();
<%-- if we're doing an add, change the focus --%>
<%--
<c:choose><c:when test="${user.userName == null}"><c:set var="focus" value="userName"/></c:when>
<c:when test="${cookieLogin == 'true'}"><c:set var="focus" value="firstName"/></c:when>
<c:otherwise><c:set var="focus" value="password"/></c:otherwise></c:choose>
--%>
var focusControl = document.forms["bibliographicReferenceForm"].elements["<c:out value="${focus}"/>"];
<%--
if (focusControl.type != "hidden" && !focusControl.disabled) {
    focusControl.focus();
}
--%>

function onFormSubmit(theForm) {
<%--
<c:if test="${param.from == 'list'}">
    selectAll('userRoles');
</c:if>
    return validateUser(theForm);
--%>    
}
// -->
</script>

<html:javascript formName="bibliographicReferenceForm"
    staticJavascript="false" />
<%--
<script type="text/javascript"
      src="<c:url value="/scripts/validator.jsp"/>"></script>
--%>

