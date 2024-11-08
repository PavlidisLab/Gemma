<%@ include file="/common/taglibs.jsp" %>
<head>
<title><fmt:message key="personDetail.title" /></title>
</head>

<h2><fmt:message key="personDetail.heading" /></h2>

<form method="post" action="">
    <table class="detail">
        <tr>
            <th>
                <label key="person.firstName"></label>
            </th>

            <td align="right" width="31%">
                <strong>First Name</strong>
            </td>
            <td>
                <spring:bind path="person.firstName">
                    <%--
                <input type="text" name="<c:out value="${status.expression}"/>" id="<c:out value="${status.expression}"/>" 
                    value="<c:out value="${status.value}"/>" />
                <span class="fieldError"><c:out value="${status.errorMessage}"/></span>
            --%>
                    <input type="text" name="firstName" id="<c:out value="${status.expression}"/>"
                            value="<c:out value="${status.value}"/>" />
                    <span class="fieldError"><c:out value="${status.errorMessage}" />
					</span>
                </spring:bind>
            </td>
        </tr>


        <tr>
            <th>
                <label key="person.middleName"></label>
            </th>

            <td align="right" width="31%">
                <strong>Middle Name:</strong>
            </td>
            <td>
                <spring:bind path="person.middleName">
                    <%--
                <input type="text" name="<c:out value="${status.expression}"/>" id="<c:out value="${status.expression}"/>" 
                    value="<c:out value="${status.value}"/>" />
                <span class="fieldError"><c:out value="${status.errorMessage}"/></span>
            --%>
                    <input type="text" name="middleName" id="<c:out value="${status.expression}"/>"
                            value="<c:out value="${status.value}"/>" />
                    <span class="fieldError"><c:out value="${status.errorMessage}" />
					</span>
                </spring:bind>
            </td>
        </tr>

        <tr>

            <th>
                <label key="person.lastName"></label>
            </th>

            <td align="right" width="31%">
                <strong>Last Name:</strong>
            </td>
            <td>
                <spring:bind path="person.lastName">
                    <%--
                <input type="text" name="<c:out value="${status.expression}"/>" id="<c:out value="${status.expression}"/>" 
                    value="<c:out value="${status.value}"/>" />
                <span class="fieldError"><c:out value="${status.errorMessage}"/></span>
            --%>
                    <input type="text" name="lastName" id="<c:out value="${status.expression}"/>"
                            value="<c:out value="${status.value}"/>" />
                    <span class="fieldError"><c:out value="${status.errorMessage}" />
					</span>
                </spring:bind>
            </td>
        </tr>


        <tr>
            <td></td>
            <td align="right"></td>
            <td class="buttonBar">
                <input type="submit" class="button" name="save" onclick="bCancel=false"
                        value="<fmt:message key="button.save"/>" />
                <input type="submit" class="button" name="delete" onclick="bCancel=true;return confirmDelete('Person')"
                        value="<fmt:message key="button.delete"/>" />
                <input type="submit" class="button" name="cancel" onclick="bCancel=true"
                        value="<fmt:message key="button.cancel"/>" />
            </td>
        </tr>
    </table>
</form>

<validate:javascript formName="person" cdata="false" dynamicJavascript="true" staticJavascript="false" />
<script type="text/javascript" src="<c:url value="/scripts/validator.jsp"/>"></script>
<script type="text/javascript">
document.forms["person"].elements["firstName"].focus();
</script>
