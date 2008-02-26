<%-- 
author: keshav
version: $Id$
--%>


<%@ include file="/common/taglibs.jsp"%>

<jsp:useBean id="diffExpressionSearchCommand" scope="request"
	class="ubic.gemma.web.controller.diff.DiffExpressionSearchCommand" />

<title><fmt:message key="diffExpressionSearch.title" />
</title>

<spring:bind path="diffExpressionSearchCommand.*">
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
	action="<c:url value="/diff/diffExpressionSearch.html"/>">

	<h2>
		<fmt:message key="diffExpressionSearch.title" />
	</h2>

	<hr />

	<table>

		<tr>
			<td valign="top">
				<b> <fmt:message key="gene.officialSymbol" /> <br /> </b>
			</td>
			<td>
				<spring:bind path="diffExpressionSearchCommand.geneOfficialSymbol">
					<input type="text" size=10
						name="<c:out value="${status.expression}"/>"
						value="<c:out value="${status.value}"/>" />
				</spring:bind>
			</td>
		</tr>
		<tr>
			<td valign="top">
				<b> <fmt:message key="gene.taxon" /> </b>
			</td>
			<td>

				<spring:bind path="diffExpressionSearchCommand.taxonName">
					<select name="${status.expression}">
						<c:forEach items="${taxa}" var="taxon">
							<option value="${taxon}"
								<c:if test="${status.value == securableType}">selected="selected" </c:if>>
								${taxon}
							</option>
						</c:forEach>
					</select>
					<span class="fieldError">${status.errorMessage}</span>
				</spring:bind>
			</td>
		</tr>
		<tr>
			<td valign="top">
				<b> <fmt:message key="diff.threshold" /> <br /> </b>
			</td>
			<td>
				<spring:bind path="diffExpressionSearchCommand.threshold">
					<input type="text" size=5
						name="<c:out value="${status.expression}"/>"
						value="<c:out value="${status.value}"/>" />
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
<%-- TODO
<validate:javascript formName="expressionExperimentForm" staticJavascript="false"/>
<script type="text/javascript"
      src="<c:url value="/scripts/validator.jsp"/>"></script>
--%>
