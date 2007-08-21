<%@ include file="/common/taglibs.jsp"%>

<title><fmt:message key="userProfile.title" />
</title>
<content tag="heading">
<fmt:message key="userProfile.heading" />
</content>
<meta name="menu" content="UserMenu" />
<script type="text/javascript"
	src="<c:url value='/scripts/selectbox.js'/>"></script>

<spring:bind path="user.*">
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

<authz:authorize ifAnyGranted="admin">
	<p class="left">
		Go to the
		<a href="<c:url value="/activeUsers.html" />">User list</a>
	</p>
</authz:authorize>

<form method="post" action="<c:url value="/editUser.html"/>"
	id="userForm" onsubmit="return onFormSubmit(this)">
	<spring:bind path="user.id">
		<input type="hidden" name="id"
			value="<c:out value="${status.value}"/>" />
	</spring:bind>
	<input type="hidden" name="from" value="<c:out value="${param.from}"/>" />

	<spring:bind path="user.password">
		<input type="hidden" name="password"
			value="<c:out value="${status.value}"/>" />
	</spring:bind>

	<c:if test="${empty user.userName}">
		<input type="hidden" name="encryptPass" value="true" />
	</c:if>

	<ul>
		<li class="buttonBar center">
			<c:set var="buttons">
				<input type="submit" class="button" name="save"
					onclick="bCancel=false" value="<fmt:message key="button.save"/>" />

				<c:if test="${param.from == 'list' and param.method != 'Add'}">
					<input type="submit" class="button" name="delete"
						onclick="bCancel=false;return confirmDelete('user')"
						value="<fmt:message key="button.delete"/>" />
				</c:if>

				<input type="submit" class="button" name="cancel"
					onclick="bCancel=true" value="<fmt:message key="button.cancel"/>" />
			</c:set>
			<c:out value="${buttons}" escapeXml="false" />
		</li>
		<li class="info">
			<c:choose>
				<c:when test="${param.from == 'list'}">
					<p>
						<fmt:message key="userProfile.admin.message" />
					</p>
				</c:when>
				<c:otherwise>
					<p>
						<fmt:message key="userProfile.message" />
					</p>
				</c:otherwise>
			</c:choose>
		</li>
		<li>
			<Gemma:label styleClass="desc" key="user.userName" />
			<spring:bind path="user.userName">
				<input type="text" name="userName"
					value="<c:out value="${status.value}"/>" id="userName"
					class="text large" />
				<span class="fieldError"> <c:out
						value="${status.errorMessage}" /> </span>
			</spring:bind>
		</li>


		<li>
			<Gemma:label styleClass="desc" key="user.name" />

			<spring:bind path="user.name">
				<input type="text" name="name"
					value="<c:out value="${status.value}"/>" id="name" maxlength="50"
					class="text large" />
				<span class="fieldError"> <c:out
						value="${status.errorMessage}" /> </span>
			</spring:bind>
		</li>
		<li>
			<Gemma:label styleClass="desc" key="user.lastName" />

			<spring:bind path="user.lastName">
				<input type="text" name="lastName"
					value="<c:out value="${status.value}"/>" id="lastName"
					maxlength="50" class="text large" />
				<span class="fieldError"> <c:out
						value="${status.errorMessage}" /> </span>
			</spring:bind>
		</li>
		<li>

			<Gemma:label styleClass="desc" key="user.email" />

			<spring:bind path="user.email">
				<input type="text" name="email"
					value="<c:out value="${status.value}"/>" id="email"
					class="text large" />
				<span class="fieldError"> <c:out
						value="${status.errorMessage}" /> </span>
			</spring:bind>

		</li>
		<li>
			<Gemma:label styleClass="desc" key="user.passwordHint" />

			<spring:bind path="user.passwordHint">
				<input type="text" name="passwordHint"
					value="<c:out value="${status.value}"/>" id="passwordHint"
					class="text large" />
				<span class="fieldError"> <c:out
						value="${status.errorMessage}" /> </span>
			</spring:bind>
		</li>

		<c:if test="${cookieLogin != 'true'}">
			<li>
				<div>
					<p>
						To change password, fill in the next two fields
					</p>
					<div class="left">
						<Gemma:label styleClass="desc" key="user.newpassword" />

						<spring:bind path="user.newPassword">
							<input type="password" id="newPassword" name="newPassword"
								class="text medium" value="<c:out value="${status.value}"/>"
								onchange="passwordChanged(this)" />
							<span class="fieldError"> <c:out
									value="${status.errorMessage}" /> </span>
						</spring:bind>
					</div>

					<div>
						<Gemma:label key="user.confirmPassword" />
						<spring:bind path="user.confirmNewPassword">
							<span class="fieldError"><c:out
									value="${status.errorMessage}" /> </span>
							<input type="password" name="confirmNewPassword"
								id="confirmNewPassword" value="<c:out value="${status.value}"/>"
								class="text medium" />
						</spring:bind>
					</div>
				</div>
			</li>
		</c:if>


		<c:choose>
			<c:when test="${param.from == 'list' or param.method == 'Add'}">
				<%-- Administrative tool only --%>
				<li>
					<fieldset>
						<legend>
							<fmt:message key="userProfile.accountSettings" />
						</legend>

						<spring:bind path="user.enabled">
							<input type="hidden"
								name="_<c:out value="${status.expression}"/>" value="visible" />
							<input type="checkbox"
								name="<c:out value="${status.expression}"/>"
								<c:if test="${status.value}">checked="checked"</c:if> />
						</spring:bind>
						<label for="enabled" class="choice">
							<fmt:message key="user.enabled" />
						</label>
					</fieldset>
				</li>
				<li>
					<fieldset class="pickList">
						<legend>
							<fmt:message key="userProfile.assignRoles" />
						</legend>
						<table class="pickList">
							<tr>
								<th class="pickLabel">
									<Gemma:label key="user.availableRoles" colon="false"
										styleClass="required" />
								</th>
								<td>
								</td>
								<th class="pickLabel">
									<Gemma:label key="user.roles" colon="false"
										styleClass="required" />
								</th>
							</tr>
							<c:set var="leftList" value="${availableRoles}" scope="request" />
							<c:set var="rightList" value="${user.roles}" scope="request" />
							<c:import url="/WEB-INF/pages/pickList.jsp">
								<c:param name="listCount" value="1" />
								<c:param name="leftId" value="availableRoles" />
								<c:param name="rightId" value="roles" />
							</c:import>
						</table>
					</fieldset>
				</li>
			</c:when>
			<c:when test="${not empty user.userName}">
				<%-- Show the roles and status for this user --%>

				<li>
					<strong><Gemma:label key="user.roles" /> </strong>

					<spring:bind path="user.roles">
						<input type="text" name="roles"
							value="<c:out value="${status.value}"/>" id="roles"
							class="text large" />
						<span class="fieldError"> <c:out
								value="${status.errorMessage}" /> </span>
					</spring:bind>

					<spring:bind path="user.enabled">
						<input type="hidden" name="<c:out value="${status.expression}"/>"
							value="<c:out value="${status.value}"/>" />
					</spring:bind>
				</li>

			</c:when>
		</c:choose>


		<li class="buttonBar bottom">
			<c:out value="${buttons}" escapeXml="false" />
		</li>
	</ul>

</form>

<script type="text/javascript">
      Form.focusFirstElement($('userForm'));
    highlightFormElements();
 
 /* If the user has javascript turned off, there is a pretty big problem here */
    function passwordChanged(passwordField) {
            createFormElement("input", "hidden",
                              "passwordChange", "passwordChange",
                              "true", passwordField.form);
    }

<!-- This is here so we can exclude the selectAll call when roles is hidden -->
function onFormSubmit(theForm) {
<c:if test="${param.from == 'list'}">
    selectAll('roles');
</c:if>
    return validateUser(theForm);
}
</script>

<validate:javascript formName="user" staticJavascript="false" />
<script type="text/javascript"
	src="<c:url value="/scripts/validator.jsp"/>"></script>
