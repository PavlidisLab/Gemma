<%@ include file="/common/taglibs.jsp" %>

<%--@elvariable id="arrayDesign" type="ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject"--%>
<%--@elvariable id="technologyTypes" type="java.util.List"--%>

<head>
<title>Edit ${fn:escapeXml(arrayDesign.name)}</title>
</head>

<spring:bind path="arrayDesign.*">
	<c:if test="${not empty status.errorMessages}">
		<div class="error">
			<c:forEach var="error" items="${status.errorMessages}">
				<fmt:message key="icon.warning" var="warningIconAlt"/>
				<Gemma:img src="/images/iconWarning.gif" alt="${warningIconAlt}" cssClass="icon" />
				<c:out value="${error}" escapeXml="false" />
				<br />
			</c:forEach>
		</div>
	</c:if>
</spring:bind>

<form method="post" action="<c:url value="/arrayDesign/editArrayDesign.html"/>" id="arrayDesignForm"
        onsubmit="return validateArrayDesign(this)">

    <table>
        <tr>
            <td class="label">
                <label key="arrayDesign.name"></label>
            </td>
            <td>
                <spring:bind path="arrayDesign.name">
                    <input size="120" type="text" name="name" value="<c:out value="${status.value}"/>" id="name" />
                    <span class="fieldError"><c:out value="${status.errorMessage}" /> </span>
                </spring:bind>
            </td>
        </tr>
        <tr>
            <td class="label">
                <label key="arrayDesign.shortName"></label>
            </td>
            <td>
                <spring:bind path="arrayDesign.shortName">
                    <input type="text" name="shortName" value="<c:out value="${status.value}"/>" id="shortName" />
                    <span class="fieldError"><c:out value="${status.errorMessage}" /> </span>

                </spring:bind>
            </td>
        </tr>
        <tr>
            <td class="label">
                <label key="arrayDesign.technologyType"></label>
            </td>
            <td>
                <spring:bind path="arrayDesign.technologyType">
                    <select name="${status.expression}">
                        <c:forEach items="${technologyTypes}" var="type">
                            <option value="${type}" <c:if test="${status.value == type}">selected</c:if>>
                                    ${type}
                            </option>
                        </c:forEach>
                    </select>
                </spring:bind>
            </td>
        </tr>

        <tr>
            <td class="label">
                <label key="arrayDesign.description"></label>
            </td>
            <td>
                <spring:bind path="arrayDesign.description">
                    <textarea name="description" id="description" rows=8 cols=80><c:out
                            value="${status.value}" /></textarea>
                    <span class="fieldError"><c:out value="${status.errorMessage}" /> </span>
                </spring:bind>
            </td>
        </tr>
        <tr>
            <td></td>
            <td>
                <input type="submit" class="button" name="save" onclick="bCancel=false"
                        value="<fmt:message key="button.save"/>" />
                <input type="submit" class="button" name="cancel" onclick="bCancel=true"
                        value="<fmt:message key="button.cancel"/>" />
                <input type="hidden" name="id" value="${arrayDesign.id}" />
            </td>
        </tr>

    </table>
</form>

<validate:javascript formName="arrayDesign" staticJavascript="false" />
<script type="text/javascript" src="<c:url value="/scripts/validator.jsp"/>"></script>
