<%@ include file="/common/taglibs.jsp"%>

<title><fmt:message key="upload.title" /></title>
<content tag="heading">
<fmt:message key="upload.heading" />
</content>

<jsp:useBean id="fileUpload" scope="request"
    class="edu.columbia.gemma.web.controller.common.auditAndSecurity.FileUpload" />
 
<spring:bind path="fileUpload.*">
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
 

<fmt:message key="upload.message" />
<div class="separator"></div>

<%--
    The most important part is to declare your form's enctype to be "multipart/form-data"
--%>
<%-- uncomment for spring mvc version
<form method="post" id="uploadForm"
    action="<c:url value="/uploadFile.html"/>"
    enctype="multipart/form-data"
    onsubmit="return validateFileUpload(this)">
--%>

<%-- use this for flow version --%>

<form method="post" action="<c:url value="/flowController.htm"/>"  enctype="multipart/form-data"
    id="uploadFile" onsubmit="return onFormSubmit(this)">
<%-- The following fields are to support use of this form in flows --%>
	<input type="hidden" name="_flowExecutionId"  value="<%=request.getAttribute("flowExecutionId") %>"> 
    <input  type="hidden" name="_eventId" value="">
	<input type="hidden"  name="_flowId" value="fileUploader"> 
   
<table class="detail">
    <tr>
        <th><Gemma:label key="uploadForm.name" /></th>
        <td><spring:bind path="fileUpload.name">
            <input type="text" name="name" id="name" size="40"
                value="<c:out value="${status.value}"/>" />
            <span class="fieldError"><c:out
                value="${status.errorMessage}" /></span>
        </spring:bind></td>
    </tr>
    <tr>
        <th><Gemma:label key="uploadForm.file" /></th>
        <td><spring:bind path="fileUpload.file">
            <input type="file" name="file" id="file" size="50"
                value="<c:out value="${status.value}"/>" />
            <span class="fieldError"><c:out
                value="${status.errorMessage}" /></span>
        </spring:bind></td>
    </tr>
    <tr>
        <td></td>
        <td class="buttonBar"><input type="submit" name="upload"
            class="button" onclick="bCancel=false;this.form._eventId.value='submit'"
            value="<fmt:message key="button.upload"/>" /> <input
            type="submit" name="cancel" class="button"
            onclick="bCancel=true;this.form._eventId.value='cancel'"
            value="<fmt:message key="button.cancel"/>" /></td>
    </tr>
</table>
</form>

<script type="text/javascript">
<!--
highlightFormElements();
// -->
</script>
<%--
<html:javascript formName="fileUpload" staticJavascript="false" />
<script type="text/javascript"
    src="<c:url value="/scripts/validator.jsp"/>"></script>
--%>