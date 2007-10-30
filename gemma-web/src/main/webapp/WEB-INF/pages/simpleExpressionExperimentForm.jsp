<%@ include file="/common/taglibs.jsp"%>
<jsp:useBean id="simpleExpressionExperimentLoadCommand" scope="request"
	class="ubic.gemma.web.controller.expression.experiment.SimpleExpressionExperimentLoadCommand" />
<head>
	<script src="<c:url value='/scripts/ext/adapter/prototype/ext-prototype-adapter.js'/>" type="text/javascript"></script>
	<script src="<c:url value='/scripts/ext/ext-all.js'/>" type="text/javascript"></script>
	<script type="text/javascript" src="<c:url value='/scripts/progressbar.js'/>"></script>
	<script type="text/javascript" src="<c:url value='/scripts/ext/data/DwrProxy.js'/>"></script>
	<script type="text/javascript" src="<c:url value='/scripts/ajax/fileUpload.js'/>"></script>
	<script type='text/javascript' src='/Gemma/dwr/interface/ProgressStatusService.js'></script>
	<script type='text/javascript' src='/Gemma/dwr/engine.js'></script>
	<script type='text/javascript' src='/Gemma/dwr/util.js'></script>
	<script type="text/javascript" src="<c:url value='/scripts/ajax/simpleEELoad.js'/>"></script>
	<script type='text/javascript' src='/Gemma/dwr/interface/SimpleExpressionExperimentLoadController.js'></script>
	<script type='text/javascript' src='/Gemma/dwr/interface/TaskCompletionController.js'></script>
	<script type='text/javascript' src='/Gemma/dwr/interface/FileUploadController.js'></script>
	<link rel="stylesheet" type="text/css" media="all" href="<c:url value='/styles/progressbar.css'/>" />
	<title>Load an expression data set from a flat file</title>
</head>
<body>
	<h1>
		Load an expression data set from a flat file
	</h1>
	<p>
		Select an existing array design and taxon; if either one for your experiment aren't listed, provide a name and it will
		be created based on the data.
	</p>

	<div id="messages" style="margin: 10px; width: 400px"></div>

	<div id="progress-area" style="margin: 20px; padding: 5px;"></div>
	<div id="taskId" style="display: none;"><%=request.getSession().getAttribute( "tmpTaskId" )%></div>
	<spring:bind path="simpleExpressionExperimentLoadCommand">
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


	<div style="margin: 10px;" id="file-upload"></div>


	<form id="simpleEELoad" method="post" name="simpleEELoad"  >

		<table>

			<tr>
				<td valign='top'>
					<Gemma:label helpTip="true" key="simpleEEForm.file" />
					<input type="file" name="file" id="file" />
				</td>
			</tr>

			<tr>
				<td valign='top'>
					<Gemma:label key="simpleEEForm.name" />
					<spring:bind path="simpleExpressionExperimentLoadCommand.name">
						<input type="text" name="<c:out value="${status.expression}"/>" id="<c:out value="${status.expression}"/>"
							value="<c:out value="${status.value}"/>" />
						<span class="fieldError">${status.errorMessage}</span>
					</spring:bind>
				</td>
			</tr>
			<tr>
				<td valign='top'>
					<Gemma:label key="simpleEEForm.shortName" />
					<spring:bind path="simpleExpressionExperimentLoadCommand.shortName">
						<input type="text" name="<c:out value="${status.expression}"/>" id="<c:out value="${status.expression}"/>"
							value="<c:out value="${status.value}"/>" />
						<span class="fieldError">${status.errorMessage}</span>
					</spring:bind>
				</td>
			</tr>

			<tr>
				<td>
					<Gemma:label key="simpleEEForm.description" />
					<spring:bind path="simpleExpressionExperimentLoadCommand.description">
						<textarea rows="5" cols="60" id="<c:out value="${status.expression}"/>"
							name="<c:out value="${status.expression}"/>" value="<c:out value="${status.value}"/>" /></textarea>
						<span class="fieldError">${status.errorMessage}</span>
					</spring:bind>
				</td>
			</tr>

			<tr>
				<td valign='top'>
					<Gemma:label key="simpleEEForm.arrayDesigns" />
					<spring:bind path="simpleExpressionExperimentLoadCommand.arrayDesigns">
						<select name="${status.expression}" multiple size='5' id="<c:out value="${status.expression}"/>">
							<c:forEach items="${arrayDesigns}" var="arrayDesign">
								<spring:transform value="${arrayDesign.id }" var="id" />
								<option value="${id}" <c:if test="${status.value == id}">selected</c:if>>
									${arrayDesign.name} (${arrayDesign.shortName })
								</option>
							</c:forEach>
						</select>
						<span class="fieldError">${status.errorMessage}</span>
					</spring:bind>

				</td>

				<td valign='top'>
					<Gemma:label key="simpleEEForm.arrayDesign.name" />
					<spring:bind path="simpleExpressionExperimentLoadCommand.arrayDesignName">
						<input type="text" name="<c:out value="${status.expression}"/>" id="<c:out value="${status.expression}"/>"
							value="<c:out value="${status.value}"/>" />
						<span class="fieldError">${status.errorMessage}</span>
					</spring:bind>
				</td>

				<td valign='top'>
					<Gemma:label key="simpleEEForm.probeIdsAreImageClones" />
					<spring:bind path="simpleExpressionExperimentLoadCommand.probeIdsAreImageClones">
						<input type="hidden" name="_<c:out value="${status.expression}"/>">
						<input align="left" type="checkbox" id="<c:out value="${status.expression}"/>"
							name="<c:out value="${status.expression}"/>" value="true" <c:if test="${status.value}">checked</c:if> />
						<span class="fieldError"> <c:out value="${status.errorMessage}" /> </span>
					</spring:bind>
				</td>


			</tr>
			<tr>
				<td valign='top'>
					<Gemma:label key="simpleEEForm.taxon.title" />
					<spring:bind path="simpleExpressionExperimentLoadCommand.taxon">
						<select name="${status.expression}" id="<c:out value="${status.expression}"/>">
							<c:forEach items="${taxa}" var="taxon">
								<spring:transform value="${taxon}" var="scientificName" />
								<option value="${scientificName}" <c:if test="${status.value == scientificName}">selected</c:if>>
									${scientificName}
								</option>
							</c:forEach>
						</select>
						<span class="fieldError">${status.errorMessage}</span>
					</spring:bind>

				</td>

				<td valign='top'>
					<Gemma:label key="simpleEEForm.taxon.scientificName" />
					<spring:bind path="simpleExpressionExperimentLoadCommand.taxonName">
						<input type="text" name="<c:out value="${status.expression}"/>" value="<c:out value="${status.value}"/>"
							id="<c:out value="${status.expression}"/>" />
						<span class="fieldError">${status.errorMessage}</span>
					</spring:bind>
				</td>

			</tr>

			<tr>
				<td colspan="2">
					<table>
						<tr>
							<td>
								<h2>
									Describe the quantitation type
								</h2>
							</td>
						</tr>


						<tr>
							<td>
								<Gemma:label key="simpleEEForm.quantitationTypeName" />
								<spring:bind path="simpleExpressionExperimentLoadCommand.quantitationTypeName">
									<input type="text" name="<c:out value="${status.expression}"/>" id="<c:out value="${status.expression}"/>"
										value="<c:out value="${status.value}"/>" />
									<span class="fieldError">${status.errorMessage}</span>
								</spring:bind>
							</td>
						</tr>

						<tr>
							<td>
								<Gemma:label key="simpleEEForm.quantitationTypeDescription" />
								<spring:bind path="simpleExpressionExperimentLoadCommand.quantitationTypeDescription">
									<input type="textarea" rows="20" cols="120" name="<c:out value="${status.expression}"/>"
										id="<c:out value="${status.expression}"/>" value="<c:out value="${status.value}"/>" />
									<span class="fieldError">${status.errorMessage}</span>
								</spring:bind>
							</td>
						</tr>


						<tr>
							<td>
								<Gemma:label key="simpleEEForm.type" />
								<spring:bind path="simpleExpressionExperimentLoadCommand.type">
									<select name="${status.expression}" id="<c:out value="${status.expression}"/>">
										<c:forEach items="${standardQuantitationTypes}" var="type">
											<option value="${type}" <c:if test="${status.value == type}">selected</c:if>>
												${type}
											</option>
										</c:forEach>
									</select>
									<span class="fieldError">${status.errorMessage}</span>
								</spring:bind>

							</td>
						</tr>

						<tr>
							<td>
								<Gemma:label key="simpleEEForm.scale" />
								<spring:bind path="simpleExpressionExperimentLoadCommand.scale">
									<select name="${status.expression}" id="<c:out value="${status.expression}"/>">
										<c:forEach items="${scaleTypes}" var="type">
											<option value="${type}" <c:if test="${status.value == type}">selected</c:if>>
												${type}
											</option>
										</c:forEach>
									</select>
									<span class="fieldError">${status.errorMessage}</span>
								</spring:bind>

							</td>
						</tr>
						<tr>
							<td>
								<Gemma:label key="simpleEEForm.isRatio" />
								<spring:bind path="simpleExpressionExperimentLoadCommand.isRatio">
									<input type="hidden" name="_<c:out value="${status.expression}"/>">
									<input align="left" type="checkbox" id="<c:out value="${status.expression}"/>"
										name="<c:out value="${status.expression}"/>" value="true" <c:if test="${status.value}">checked</c:if> />
									<span class="fieldError"> <c:out value="${status.errorMessage}" /> </span>
								</spring:bind>
							</td>

						</tr>
					</table>
				</td>
			</tr>


		</table>
		<input id="upload-button" type="submit" onClick="return false;" />
	</form>

	<validate:javascript formName="simpleEEForm" staticJavascript="false" cdata="false" xhtml="true" />
</body>
