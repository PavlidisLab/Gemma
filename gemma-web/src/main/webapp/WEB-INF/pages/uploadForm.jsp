<%@ include file="/common/taglibs.jsp"%>
<jsp:useBean id="fileUpload" scope="request" class="ubic.gemma.web.controller.common.auditAndSecurity.FileUpload" />
<%--  $Id$ --%>
<title><fmt:message key="upload.title" />
</title>
<content tag="heading">
<fmt:message key="upload.heading" />
</content>

<script type="text/javascript" src="<c:url value='/scripts/progressbar.js'/>"></script>
<script type='text/javascript' src='/Gemma/dwr/interface/ProgressStatusService.js'></script>
<script type='text/javascript' src='/Gemma/dwr/engine.js'></script>
<script type='text/javascript' src='/Gemma/dwr/util.js'></script>
<script type='text/javascript' src="<c:url value="scripts/progressbar.js"/>"></script>
<
<link rel="stylesheet" type="text/css" media="all" href="<c:url value='/styles/progressbar.css'/>" />

</style>
<spring:bind path="fileUpload.*">
	<c:if test="${not empty status.errorMessages}">
		<div class="error">
			<c:forEach var="error" items="${status.errorMessages}">
				<img src="<c:url value="/images/iconWarning.gif"/>" alt="<fmt:message key="icon.warning"/>" class="icon" />
				<c:out value="${error}" escapeXml="false" />
				<br />
			</c:forEach>
		</div>
	</c:if>
</spring:bind>


<fmt:message key="upload.message" />
<div class="separator"></div>

<%--
    The most important part is to declare your form's enctype to be "multipart/form-data"
--%>

<form method="post" id="uploadForm" action="<c:url value="/uploadFile.html"/>" enctype="multipart/form-data"
	onsubmit="startProgress()">
	<table class="detail">
		<tr>
			<th>
				<Gemma:label key="uploadForm.file" />
			</th>
			<td>
				<spring:bind path="fileUpload.file">
					<input type="file" name="file" id="file" size="50" value="<c:out value="${status.value}"/>" />
					<span class="fieldError"> <c:out value="${status.errorMessage}" /> </span>
				</spring:bind>
			</td>
		</tr>
		<tr>
			<td></td>
			<td class="buttonBar">
				<input type="submit" id="uploadbutton" name="upload" class="button" value="<fmt:message key="button.upload"/>" />
				<input type="submit" name="cancel" class="button" value="<fmt:message key="button.cancel"/>" />
			</td>
		</tr>
	</table>
</form>
<br />

<validate:javascript formName="fileUpload" staticJavascript="false" />
