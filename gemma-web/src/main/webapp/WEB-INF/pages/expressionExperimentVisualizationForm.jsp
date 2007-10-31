<%@ include file="/common/taglibs.jsp"%>

<jsp:useBean id="expressionExperimentVisualizationCommand"
	scope="request"
	class="ubic.gemma.web.controller.visualization.ExpressionExperimentVisualizationCommand" />

<title><fmt:message
		key="expressionExperimentVisualization.title" /></title>

<spring:bind path="expressionExperimentVisualizationCommand.*">
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
	action="<c:url value="/expressionExperiment/expressionExperimentVisualization.html"/>">

	<h2>
		<fmt:message key="expressionExperimentVisualization.title" />
	</h2>

	<table cellspacing="10">

		<tr>
			<td valign="top">
				<b> <fmt:message key="expressionExperiment.name" /> </b>
			</td>
			<td>
				<%
				if ( expressionExperimentVisualizationCommand.getName() != null ) {
				%>
				<jsp:getProperty name="expressionExperimentVisualizationCommand"
					property="name" />
				<%
				                } else {
				                out.print( "Name unavailable" );
				            }
				%>
			</td>
		</tr>
	</table>
	<hr />

	<table>
		<tr>
			<td valign="top">
				<b> <fmt:message key="label.search" /> </b>
			</td>

			<td>

				<spring:bind
					path="expressionExperimentVisualizationCommand.searchCriteria">
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
				<b> <fmt:message key="quantitationType.name" /> </b>
			</td>
			<td>
				<spring:bind
					path="expressionExperimentVisualizationCommand.quantitationType">
					<select name="${status.expression}" multiple>
						<c:forEach items="${quantitationTypes}" var="quantitationType">
							<spring:transform value="${quantitationType}" var="name" />
							<option value="${name}"
								<c:if test="${status.value == name}">selected</c:if>>
								${name}
							</option>
						</c:forEach>
					</select>
					<span class="fieldError">${status.errorMessage}</span>
				</spring:bind>

			</td>


		</tr>

		<tr>
			<td valign="top">
				<b> <fmt:message key="label.searchString" /> <br /> (comma
					sep.) </b>
			</td>
			<td>
				<spring:bind
					path="expressionExperimentVisualizationCommand.searchString">
					<input type="text" size=10
						name="<c:out value="${status.expression}"/>"
						value="<c:out value="${status.value}"/>" />
				</spring:bind>
			</td>
		</tr>

		<tr>
			<td valign="top">
				<b> <fmt:message key="label.viewSampling" /> <br />(Get a
					glimpse of the data) </b>
			</td>
			<td>
				<spring:bind
					path="expressionExperimentVisualizationCommand.viewSampling">
					<input type="hidden" name="_${status.expression}" />
					<input type="checkbox" name="${status.expression}" value="true"
						<c:if test="${status.value}">checked="checked"</c:if> />
					<span class="fieldError">${status.errorMessage}</span>
				</spring:bind>
			</td>
		</tr>

		<tr>
			<td valign="top">
				<b> <fmt:message key="label.maskMissing" /> </b>
			</td>
			<td>
				<spring:bind
					path="expressionExperimentVisualizationCommand.maskMissing">
					<input type="hidden" name="_${status.expression}" />
					<input type="checkbox" name="${status.expression}" value="true"
						<c:if test="${status.value}">checked="checked"</c:if> />
					<span class="fieldError">${status.errorMessage}</span>
				</spring:bind>
			</td>
		</tr>
	</table>
	<br />

	<spring:bind
		path="expressionExperimentVisualizationCommand.expressionExperimentId">
		<input type="hidden" name='id'
			value="<c:out value="${status.value}"/>" />
	</spring:bind>

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
