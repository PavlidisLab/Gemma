<%@ include file="/common/taglibs.jsp"%>
<%@ page
	import="edu.columbia.gemma.web.controller.common.auditAndSecurity.FileUpload"%>
<%@ page session="false"%>

<title><fmt:message key="display.title" /></title>
<content tag="heading">
<fmt:message key="display.heading" />
</content>

Below is a list of attributes that were gathered in
FileUploadController.java.

<div class="separator"></div>

<%-- The following form is to support use of this form in flows --%>
<form name="upLoadDisplay" action="flowController.htm"><input
	type="hidden" name="_flowExecutionId"
	value="<%=request.getAttribute("flowExecutionId") %>"></form>

<table class="detail" cellpadding="5">

	<tr>
		<th>Friendly Name:</th>
		<td><c:out value="${friendlyName}" /></td>
	</tr>
	<tr>
		<th>Filename:</th>
		<td><c:out value="${fileName}" /></td>
	</tr>
	<tr>
		<th>File content type:</th>
		<td><c:out value="${contentType}" /></td>
	</tr>
	<tr>
		<th>File size:</th>
		<td><c:out value="${size}" /></td>
	</tr>
	<tr>
		<th class="tallCell">File Location:</th>
		<td>The file has been written to: <br />
		<a href="<c:out value="${link}"/>"> <c:out value="${location}"
			escapeXml="false" /></a></td>
	</tr>

	<tr>
		<td></td>
		<td class="buttonBar"><input type="button" name="done" id="done"
			value="Done" onclick="location.href='mainMenu.html'" /> <input
			type="button" name="upload" id="Upload" value="Upload Another"
			onclick="location.href='flowController.htm?_flowId=fileUploader&_eventId=finish'" />
		</td>
	</tr>
</table>

<%

		FileUpload file = ( FileUpload ) request.getAttribute( "readFile" );
		if ( file != null && file.getFile() != null && file.getFile().length > 0 ) {
			%>
<TABLE border="1">
	<TR>
		<TD><PRE><%=new String( file.getFile() ) %></PRE></TD>
	</TR>
</TABLE>
<%

		}
	%>


