<%@ include file="/common/taglibs.jsp"%>
<jsp:useBean id="command" scope="request"
	class="ubic.gemma.web.controller.expression.experiment.ExpressionExperimentLoadCommand" />


<html>
	<head>
		<title><fmt:message key="expressionExperiment.load.title" />
		</title>
		<content tag="heading">
		<fmt:message key="expressionExperiment.load.title" />
		</content>



		<script type='text/javascript' src='dwr/engine.js'></script>
		<script type='text/javascript' src='dwr/util.js'></script>
		<script type='text/javascript' src='dwr/interface/ProgressMonitor'></script>
		<script type='text/javascript' src="<c:url value="scripts/progress.js"/>"></script>
		<style type="text/css">
#progressBar { padding-top: 5px; }
#progressBarBox { width: 350px; height: 20px; border: 1px inset; background: #EEEEEE;}
#progressBarBoxContent { width: 0; height: 20px; border-right: 1px solid #444444; background: #9ACB34; }
</style>
	</head>
	<body>
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


		<fmt:message key="expressionExperiment.load.message" />

		<form method="post" id="loadExpressionExperimentForm" action="<c:url value="/loadExpressionExperiment.html"/>"
			onsubmit="startProgress()">


			<table class="detail">
				<tr>
					<th>
						<Gemma:label key="databaseEntry.accession.title" />
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
						<Gemma:label key="expressionExperiment.load.platformOnly" />
					</th>
					<td align="left">
						<spring:bind path="command.loadPlatformOnly">
							<input type="hidden" name="_<c:out value="${status.expression}"/>">
							<input align="left" type="checkbox" name="<c:out value="${status.expression}"/>" value="true"
								<c:if test="${status.value}">checked</c:if> />
							<span class="fieldError"> <c:out value="${status.errorMessage}" /> </span>
						</spring:bind>
					</td>
				</tr>
				<tr>
					<td />
					<td align="center" class="buttonBar">
						<input type="submit" name="upload" class="button" onclick="bCancel=false;this.form._eventId.value='submit'"
							value="<fmt:message key="button.upload"/>" />
						<input type="submit" name="cancel" class="button" onclick="bCancel=true;this.form._eventId.value='cancel'"
							value="<fmt:message key="button.cancel"/>" />
					</td>

				</tr>
			</table>
		</form>

		<script type="text/javascript">
	createProgressBar();
</script>

		<script type="text/javascript">
		<!--
highlightFormElements();
// -->
		</script>
	</body>
</html>


<validate:javascript formName="expressionExperimentLoadCommand" staticJavascript="false" />
<script type="text/javascript" src="<c:url value="/scripts/validator.jsp"/>"></script>

