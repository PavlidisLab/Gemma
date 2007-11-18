<%@ include file="/common/taglibs.jsp"%>

<title><fmt:message key="signup.title" /></title>
<content tag="heading">
<fmt:message key="signup.heading" />
</content>

	<spring:bind path="user.*">
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

	<p>
		<fmt:message key="signup.message" />
	</p>

	<div class="separator"></div>

	<form method="post" action="<c:url value="/signup.html"/>" id="signupForm" onsubmit="return validateUser(this)">
		<table>
			<tr>
				<th>
					<Gemma:label styleClass="desc" key="user.userName" />
				</th>
				<td>
					<spring:bind path="user.userName">
						<span class="fieldError"><c:out value="${status.errorMessage}" /> </span>
						<input type="text" name="userName" value="<c:out value="${status.value}"/>" id="userName" class="text large" />
					</spring:bind>
				</td>
			</tr>
			<tr>
				<th>
					<Gemma:label styleClass="desc" key="user.password" />
				</th>
				<td>
					<spring:bind path="user.newPassword">
						<span class="fieldError"> <c:out value="${status.errorMessage}" /> </span>
						<input type="password" id="newPassword" name="newPassword" class="text medium"
							value="<c:out value="${status.value}"/>" />
					</spring:bind>
				</td>
			</tr>
			<tr>
				<th>
					<Gemma:label styleClass="desc" key="user.confirmNewPassword" />
				</th>
				<td>
					<spring:bind path="user.confirmNewPassword">
						<span class="fieldError"> <c:out value="${status.errorMessage}" /> </span>
						<input type="password" name="confirmNewPassword" id="confirmNewPassword" value="<c:out value="${status.value}"/>"
							class="text medium" />

					</spring:bind>
				</td>
			</tr>
			<tr>
				<th>
					<Gemma:label styleClass="desc" key="user.name" />
				</th>
				<td>
					<spring:bind path="user.name">
						<span class="fieldError"> <c:out value="${status.errorMessage}" /> </span>
						<input type="text" name="name" value="<c:out value="${status.value}"/>" id="name" maxlength="50"
							class="text medium" />
					</spring:bind>
				</td>
			</tr>
			<tr>
				<th>
					<Gemma:label styleClass="desc" key="user.lastName" />
				</th>
				<td>
					<spring:bind path="user.lastName">
						<span class="fieldError"> <c:out value="${status.errorMessage}" /> </span>
						<input type="text" name="lastName" value="<c:out value="${status.value}"/>" id="lastName" />
					</spring:bind>
				</td>
			</tr>
			<tr>
				<th>
					<Gemma:label styleClass="desc" key="user.email" />
				</th>
				<td>
					<spring:bind path="user.email">
						<span class="fieldError"> <c:out value="${status.errorMessage}" /> </span>
						<input type="text" name="email" value="<c:out value="${status.value}"/>" id="email" class="text medium" />
					</spring:bind>
				</td>
			</tr>
			<tr>
				<th>
					<Gemma:label styleClass="desc" key="user.passwordHint" />
				</th>
				<td>
					<spring:bind path="user.passwordHint">
						<span class="fieldError"> <c:out value="${status.errorMessage}" /> </span>
						<input type="text" name="passwordHint" value="<c:out value="${status.value}"/>" id="passwordHint"
							class="text medium" />
					</spring:bind>
				</td>
			</tr>
			<tr>
				<th>
					<fmt:message key="user.isAdmin" />
				</th>
				<td>
					<spring:bind path="user.adminUser">
					<input type="hidden" name="_${status.expression}" />
					<input type="checkbox" name="${status.expression}" value="true"
						<c:if test="${status.value}">checked="checked"</c:if>/>
					<span class="fieldError">${status.errorMessage}</span>		
					</spring:bind>
				</td>
			</tr>
			<tr>
				<td></td>
				<td class="buttonBar bottom">
					<input type="submit" class="button" name="save" onclick="bCancel=false"
						value="<fmt:message key="button.register"/>" />

					<input type="submit" class="button" name="cancel" onclick="bCancel=true" value="<fmt:message key="button.cancel"/>" />
				</td>
			</tr>
		</table>
	</form>


	<script type="text/javascript">
    Form.focusFirstElement(document.forms["signupForm"]);
    highlightFormElements();
</script>

	<validate:javascript formName="user" staticJavascript="false" />
	<script type="text/javascript" src="<c:url value="/scripts/validator.jsp"/>"></script>