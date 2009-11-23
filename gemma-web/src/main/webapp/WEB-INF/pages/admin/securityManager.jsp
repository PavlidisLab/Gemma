<%@ include file="/common/taglibs.jsp"%>

<jsp:useBean id="securityCommand" scope="request"
	class="ubic.gemma.web.controller.security.SecurityCommand" />

<title><fmt:message key="security.title" /></title>

<spring:bind path="securityCommand.*">
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

<form method="post" action="<c:url value="/securityManager.html"/>">

	<table>

		<tr>
			<b> Make Data Public/Private <br />
				<br /> </b>
		</tr>
		<tr>
			<td valign="top">
				<b> <fmt:message key="security.securableType" /> </b>
			</td>
			<td>

				<spring:bind path="securityCommand.securableType">
					<select name="${status.expression}">
						<c:forEach items="${securableTypes}" var="securableType">
							<option value="${securableType}"
								<c:if test="${status.value == securableType}">selected="selected" </c:if>>
								${securableType}
							</option>
						</c:forEach>
					</select>
					<span class="fieldError">${status.errorMessage}</span>
				</spring:bind>
			</td>
		</tr>

		<tr>
			<td valign="top">
				<b> <fmt:message key="security.shortName" /> <br /> </b>
			</td>
			<td>
				<spring:bind path="securityCommand.shortName">
					<input type="text" size=10
						name="<c:out value="${status.expression}"/>"
						value="<c:out value="${status.value}"/>" />
				</spring:bind>
			</td>
		</tr>

		<tr>
			<td valign="top">
				<b> <fmt:message key="security.securableMask" /> <br /> </b>
			</td>
			<td>
				<spring:bind path="securityCommand.mask">
					<input type="radio" name="mask" value="public" checked
						<c:if test='${status.value == "public"}'>checked</c:if>> Public
              		<span class="error"><c:out
							value="${status.errorMessage}" />
					</span>
					<br />
					<input type="radio" name="mask" value="private"
						<c:if test='${status.value == "private"}'>checked</c:if>> Private
              		<br />
				</spring:bind>
			</td>
		</tr>
	</table>

	<table>
		<tr>
			<td>
				<input type="submit" class="button" name="submit"
					value="<fmt:message key="button.submit"/>" />
				<input type="submit" class="button" name="cancel"
					value="<fmt:message key="button.cancel"/>" />
			</td>
		</tr>
	</table>

	<table>
		<tr>
			<br />
			<br />
			<b> Other Security Related Operations <br />
				<br /> </b>
		</tr>

		<tr>
			<ul class="glassList">
				<li>
					<a href="<c:url value="/register.html"/>"> Add Another User </a>
				</li>
			</ul>
		</tr>
	</table>

</form>
