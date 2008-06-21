<%@ include file="/common/taglibs.jsp"%>
<head>
	<title><fmt:message key="login.title" /></title>
	<content tag="heading">
	<fmt:message key="login.heading" />
	</content>
	<meta name="menu" content="Login" />

	<jwr:script src='/scripts/ajax/ext/data/DwrProxy.js' />
	<jwr:script src='/scripts/app/login.js' />
</head>
<body id="login" />

	<p>
		Note that logging in is not necessary for regular users. Return to the
		<a href="<c:url value="/mainMenu.html"/>">Main menu</a> to start using Gemma.
	</p>

	<form method="post" id="loginForm" action="<c:url value="/j_security_check"/>"
		onsubmit="saveUsername(this);return validateForm(this);">
		<table>
			<tr>
				<td colspan="2">
					<c:if test="${param.error != null}">
						<div class="error fade-ffff00" id="loginError" style="margin-right: 0; margin-bottom: 3px; margin-top: 3px">
							<img src="<c:url value="/images/iconWarning.gif"/>" alt="<fmt:message key="icon.warning"/>" class="icon" />
							<fmt:message key="errors.password.mismatch" />
							(
							<c:out value="${sessionScope.ACEGI_SECURITY_LAST_EXCEPTION.message}" />
							)
						</div>
					</c:if>
				</td>
			</tr>
			<tr>
				<th>
					<label for="j_username" class="required" id="j_username.label">
						<fmt:message key="label.username" />
						:
					</label>
				</th>
				<td>
					<div id="nick"></div>
					<input type="text" name="j_username" id="j_username" size="25" tabindex="1" />
				</td>
			</tr>
			<tr>
				<th>
					<label for="j_password" class="required" id="j_password.label">
						<fmt:message key="label.password" />
						:
					</label>
				</th>
				<td>
					<input type="password" name="j_password" id="j_password" size="20" tabindex="2" />
				</td>
			</tr>
			<c:if test="${appConfig.rememberMeEnabled}">
				<tr>
					<td></td>
					<td>
						<input type="checkbox" name="rememberMe" id="rememberMe" tabindex="3" />
						<label for="rememberMe">
							<fmt:message key="login.rememberMe" />
						</label>
					</td>
				</tr>
			</c:if>
			<c:if test="${not appConfig.rememberMeEnabled}">(remember me not enabled)</c:if>
			<tr>
				<td></td>
				<td>
					<input type="submit" class="button" name="login" value="<fmt:message key="button.login"/>" tabindex="4" />
					<input type="reset" class="button" name="reset" value="<fmt:message key="button.reset"/>" tabindex="5"
						onclick="$('j_username').focus();" />
				</td>

			</tr>
			<%--  Signup disabled
		<tr>
			<td></td>
			<td>
			
				<br />
				<fmt:message key="login.signup">
					<fmt:param>
						<c:url value="/signup.html" />
					</fmt:param>
				</fmt:message>
			</td>
			
		</tr> --%>
		</table>
	</form>




	<%-- Password hint disabled
	<p>
		<fmt:message key="login.passwordHint" />
	</p>
	--%>
</body>


