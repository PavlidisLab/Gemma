<%@ include file="/common/taglibs.jsp"%>

<spring:bind path="bioMaterial.*">
	<c:if test="${not empty status.errorMessages}">
		<div class="error">
			<c:forEach var="error" items="${status.errorMessages}">
				<img src="<c:url value="/images/iconWarning.gif"/>"
					alt="<fmt:message key="icon.warning"/>" class="icon" />
				<c:out value="${error}" escapeXml="false" />
				<br />
			</c:forEach>
		</div>
	</c:if>
</spring:bind>

<title><fmt:message key="bioMaterial.title" />
</title>

<h2>
	<fmt:message key="bioMaterial.title" />
</h2>
<form method="post"
	action="<c:url value="/bioMaterial/editBioMaterial.html"/>">
	<table width="100%" cellspacing="10">
		<tr>
		<%-- this is just a placeholder, it needs to be filled --%>
			<td valign="top">
				<b> <fmt:message key="bioMaterial.name" /> </b>
			</td>
			<td>
				<spring:bind path="bioMaterial.name">
					<input type="text" name="<c:out value="${status.expression}"/>"
						value="<c:out value="${status.value}"/>" />
				</spring:bind>
			</td>
		</tr>

	</table>
	
	<table>
		<tr>
			<td>
				<input type="submit" class="button" name="save"
					value="<fmt:message key="button.save"/>" />
				<input type="submit" class="button" name="cancel"
					value="<fmt:message key="button.cancel"/>" />
			</td>
		</tr>
	</table>
</form>
