<%@ include file="/common/taglibs.jsp"%>
<jsp:useBean id="command" scope="request"
	class="ubic.gemma.web.controller.expression.experiment.ExpressionExperimentLoadCommand" />
<head>

	<jwr:script src='/scripts/ajax/ext/data/DwrProxy.js' />
	<jwr:script src='/scripts/app/loadExpressionExperiment.js' />

</head>

<title><fmt:message key="expressionExperimentLoad.title" />
</title>
<content tag="heading">
<fmt:message key="expressionExperimentLoad.title" />
</content>

<spring:bind path="command.*">
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


<fmt:message key="expressionExperimentLoad.message" />

<div id="messages" style="margin: 10px; width: 400px"></div>

<table class="detail">
	<tr>
		<th>
			<Gemma:label key="expressionExperimentLoad.accession" />
		</th>
		<td>
			<spring:bind path="command.accession">
				<input type="text" name="accession" id="accession" size="40" value="<c:out value="${status.value}"/>" />
				<span class="fieldError"> <c:out value="${status.errorMessage}" /> </span>
			</spring:bind>
		</td>
	</tr>
	<tr>
		<th>
			<Gemma:label key="expressionExperimentLoad.arrayExpress" />
			<a class="helpLink" href="?"
				onclick="showHelpTip(event, 'Check if data is to come from ArrayExpress.'); return false"><img
					src="/Gemma/images/help.png" /> </a>
		</th>
		<td align="left">
			<spring:bind path="command.arrayExpress">
				<input type="hidden" name="_<c:out value="${status.expression}"/>">
				<input id="arrayExpress" align="left" type="checkbox" name="<c:out value="${status.expression}"/>" value="true"
					<c:if test="${status.value}">checked</c:if> />
				<span class="fieldError"> <c:out value="${status.errorMessage}" /> </span>
			</spring:bind>
		</td>
	</tr>
	<tr>
		<th>
			<Gemma:label key="expressionExperimentLoad.platformOnly" />
			<a class="helpLink" href="?"
				onclick="showHelpTip(event, 'Load an array design only, not  expression data.'); return false"><img
					src="/Gemma/images/help.png" /> </a>
		</th>
		<td align="left">
			<spring:bind path="command.loadPlatformOnly">
				<input type="hidden" name="_<c:out value="${status.expression}"/>">
				<input align="left" type="checkbox" name="<c:out value="${status.expression}"/>" value="true" id="loadPlatformOnly"
					<c:if test="${status.value}">checked</c:if> />
				<span class="fieldError"> <c:out value="${status.errorMessage}" /> </span>
			</spring:bind>
		</td>
	</tr>
	<tr>
		<th>
			<Gemma:label key="expressionExperimentLoad.suppressMatching" />
			<a class="helpLink" href="?"
				onclick="showHelpTip(event, 'Check this box if you know that samples were run on only one platform each. Otherwise an attempt will be made to identify biological replicates on different platforms.'); return false"><img
					src="/Gemma/images/help.png" /> </a>
		</th>
		<td align="left">
			<spring:bind path="command.suppressMatching">
				<input type="hidden" name="_<c:out value="${status.expression}"/>">
				<input id="suppressMatching" align="left" type="checkbox" name="<c:out value="${status.expression}"/>" value="true"
					<c:if test="${status.value}">checked</c:if> />
				<span class="fieldError"> <c:out value="${status.errorMessage}" /> </span>
			</spring:bind>
		</td>
	</tr>
	</tr>
	<th>
		<Gemma:label key="expressionExperimentLoad.splitByPlatform" />
		<a class="helpLink" href="?"
			onclick="showHelpTip(event, 'For multi-platform studies, check this box if you want the sample run on each platform to be considered separate experiments. If checked implies 'suppress matching''); return false"><img
				src="/Gemma/images/help.png" /> </a>
	</th>
	<td align="left">
		<spring:bind path="command.splitByPlatform">
			<input type="hidden" name="_<c:out value="${status.expression}"/>">
			<input id="splitByPlatform" align="left" type="checkbox" name="<c:out value="${status.expression}"/>" value="true"
				<c:if test="${status.value}">checked</c:if> />
			<span class="fieldError"> <c:out value="${status.errorMessage}" /> </span>
		</spring:bind>
	</td>
	</tr>


	<tr>
		<th>
			<Gemma:label key="expressionExperimentLoad.arrayDesign" />
		</th>
		<td>
			<spring:bind path="expressionExperimentLoadCommand.arrayDesigns">
				<select id="arrayDesign" name="${status.expression}" multiple size='5'>
					<c:forEach items="${arrayDesigns}" var="arrayDesign">
						<option value="${arrayDesign.name}">
							${arrayDesign.name}
						</option>
					</c:forEach>
				</select>
				<span class="fieldError">${status.errorMessage}</span>
			</spring:bind>

		</td>
	</tr>
	<tr>
		<td colspan="2" style="padding: 10px" align="center" class="buttonBar">
			<div id="upload-button"></div>
		</td>
	</tr>
</table>

<div id="progress-area" style="padding: 5px;"></div>

<validate:javascript formName="expressionExperimentLoad" staticJavascript="false" />
