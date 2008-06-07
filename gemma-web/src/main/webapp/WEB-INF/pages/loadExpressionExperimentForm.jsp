<%@ include file="/common/taglibs.jsp"%>
<jsp:useBean id="command" scope="request"
	class="ubic.gemma.web.controller.expression.experiment.ExpressionExperimentLoadCommand" />
<head>
	<script src="<c:url value='/scripts/ext/adapter/prototype/ext-prototype-adapter.js'/>" type="text/javascript"></script>
	<script src="<c:url value='/scripts/ext/ext-all.js'/>" type="text/javascript"></script>

	<script type="text/javascript" src="<c:url value='/scripts/progressbar.js'/>"></script>
	<script type="text/javascript" src="<c:url value='/scripts/ext/data/DwrProxy.js'/>"></script>
	<script type='text/javascript' src='/Gemma/dwr/interface/ExpressionExperimentLoadController.js'></script>
	<script type='text/javascript' src='/Gemma/dwr/interface/TaskCompletionController.js'></script>
	<script type='text/javascript' src='/Gemma/dwr/interface/ProgressStatusService.js'></script>
	<script type='text/javascript' src='/Gemma/dwr/engine.js'></script>
	<script type='text/javascript' src='/Gemma/dwr/util.js'></script>

	<%--
Note: to get the command object updated with values from the form one has to update the loadExpressionExperiment.js file as it builds the command obj on the fly
by getting the info it needs from the dom.  The initbinder method in the controller never gets called. 
 --%>
	<script type="text/javascript" src="<c:url value='/scripts/ajax/loadExpressionExperiment.js'/>" type="text/javascript"></script>

	<link rel="stylesheet" type="text/css" media="all" href="<c:url value='/styles/progressbar.css'/>" />
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
