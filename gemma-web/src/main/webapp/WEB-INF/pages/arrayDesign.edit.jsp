<%@ include file="/common/taglibs.jsp"%>

<title> Edit Array Design </title>

<spring:bind path="arrayDesign.*">
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

<form method="post" action="<c:url value="/arrayDesign/editArrayDesign.html"/>" id="arrayDesignForm"
	onsubmit="return validateArrayDesign(this)">

	<table>

		<tr>
			<th>
				<Gemma:label key="arrayDesign.name" />
			</th>
			<td>
				<spring:bind path="arrayDesign.name">
					<c:choose>
						<c:when test="${empty arrayDesign.name}">
							<input type="text" name="name" value="<c:out value="${status.value}"/>" id="name" />
							<span class="fieldError"><c:out value="${status.errorMessage}" /> </span>
						</c:when>
						<c:otherwise>
							<c:out value="${arrayDesign.name}" />
							<input type="hidden" name="name" value="<c:out value="${status.value}"/>" id="name" />
						</c:otherwise>
					</c:choose>
				</spring:bind>
			</td>
		</tr>

		<tr>
			<th>
				<Gemma:label key="arrayDesign.manufacturer" />
			</th>
			<td>
				<c:choose>
					<c:when test="${empty arrayDesign.designProvider.name}">
						<input type="text" name="${status.expression}" value="<c:out value="${status.value}"/>" id="contact.name" />
						<span class="fieldError"><c:out value="${status.errorMessage}" /> </span>
					</c:when>
					<c:otherwise>
						<c:out value="${arrayDesign.designProvider.name}" />
						<input type="hidden" name="${status.expression}" value="<c:out value="${status.value}"/>" id="contact.name" />
					</c:otherwise>
				</c:choose>
			</td>
		</tr>


		<tr>
			<th>
				<Gemma:label key="arrayDesign.description" />
			</th>
			<td>
				<spring:bind path="arrayDesign.description">
					<textarea name="description" id="description" rows=8 cols=30><c:out value="${status.value}" /></textarea>
					<span class="fieldError"><c:out value="${status.errorMessage}" /> </span>
				</spring:bind>
			</td>
		</tr>

		<tr>
			<th>
				<Gemma:label key="arrayDesign.advertisedNumberOfDesignElements" />
			</th>
			<td>
				<spring:bind path="arrayDesign.advertisedNumberOfDesignElements">
					<input type="text" name="${status.expression}" value="${status.value}" />
					<span class="fieldError">${status.errorMessage}</span>
				</spring:bind>
			</td>
		</tr>

		<tr>
			<td></td>
			<td>
				<input type="submit" class="button" name="save" onclick="bCancel=false" value="<fmt:message key="button.save"/>" />
				<input type="submit" class="button" name="cancel" onclick="bCancel=true" value="<fmt:message key="button.cancel"/>" />
			</td>
		</tr>

	</table>
</form>

<validate:javascript formName="arrayDesign" staticJavascript="false" />
<script type="text/javascript" src="<c:url value="/scripts/validator.jsp"/>"></script>
