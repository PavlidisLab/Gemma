<%@ include file="/common/taglibs.jsp"%>

<jsp:useBean id="coexpressionSearchCommand" scope="request"
	class="ubic.gemma.web.controller.coexpressionSearch.CoexpressionSearchCommand" />

<spring:bind path="coexpressionSearchCommand.*">
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

<form method="post"
	action="<c:url value="/searchCoexpression.html"/>">

	<table>
		<tr>
			<td valign="top">
				<b> <fmt:message key="label.search" /> </b>
			</td>

			<td>

				<spring:bind path="coexpressionSearchCommand.searchCriteria">
					<select name="${status.expression}">
						<c:forEach items="${searchCategories}" var="searchCategory">
							<option value="${searchCategory}"
								<c:if test="${status.value == searchCategory}">selected="selected" </c:if>>
								${searchCategory}
							</option>
						</c:forEach>
					</select>
					<span class="fieldError">${status.errorMessage}</span>
				</spring:bind>
			</td>
		</tr>

		<tr>
			<td valign="top">
				<b> <fmt:message key="label.searchString" /> <br /> </b>
			</td>
			<td>
				<spring:bind path="coexpressionSearchCommand.searchString">
					<input type="text" size=10
						name="<c:out value="${status.expression}"/>"
						value="<c:out value="${status.value}"/>" />
				</spring:bind>
			</td>
		</tr>
		
		<tr>
			<td valign="top">
				<b> <fmt:message key="label.species" /> </b>
			</td>
			<td>
				<spring:bind path="coexpressionSearchCommand.species">
					<select name="${status.expression}">
						<c:forEach items="${speciesCategories}" var="speciesCategory">
							<option value="${speciesCategory}"
								<c:if test="${status.value == speciesCategory}">selected="selected" </c:if>>
								${speciesCategory}
							</option>
						</c:forEach>
					</select>
				</spring:bind>
			</td>
		</tr>

		<tr>
			<td valign="top">
				<b> <fmt:message key="label.stringency" /> </b>
			</td>
			<td>
				<spring:bind path="coexpressionSearchCommand.stringency">
					<input "type="text" size=1
						name="<c:out value="${status.expression}"/>"
						value="<c:out value="${status.value}"/>" />
				</spring:bind>
			</td>
		</tr>
		<tr>
			<td valign="top">
				<b> <fmt:message key="label.suppressVisualizations" /> </b>
			</td>
			<td>
				<spring:bind
					path="coexpressionSearchCommand.suppressVisualizations">
					<input type="hidden" name="_${status.expression}" />
					<input type="checkbox" name="${status.expression}" value="true" />
					<c:if test="${status.value}">checked="checked"</c:if>
					<span class="fieldError">${status.errorMessage}</span>
				</spring:bind>
			</td>
		</tr>
	</table>
	<br />
	
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


</form>
