<%@ include file="/common/taglibs.jsp"%>
<jsp:useBean id="simpleExpressionExperimentLoadCommand" scope="request"
	class="ubic.gemma.web.controller.expression.experiment.SimpleExpressionExperimentLoadCommand" />


<script type='text/javascript'
	src='/Gemma/dwr/interface/ProgressStatusService.js'></script>
<script type='text/javascript' src='/Gemma/dwr/engine.js'></script>
<script type='text/javascript' src='/Gemma/dwr/util.js'></script>
<script type='text/javascript' src='/Gemma/scripts/progressbar.js'></script>
<link rel="stylesheet" type="text/css" media="all"
	href="<c:url value='/styles/progressbar.css'/>" />

<title>Load an expression data set from a flat file</title>
<h1>
	Load an expression data set from a flat file
</h1>
<p>
	Select an existing array design and taxon; if either one for your
	experiment aren't listed, provide a name and it will be created based
	on the data.
</p>

<form method="post" name="arrayDesign"
	action="<c:url value="/loadSimpleExpressionExperiment.html"/>"
	enctype="multipart/form-data" onsubmit="startProgress()">

	<table>
		<tr>
			<td>

				<spring:bind path="simpleExpressionExperimentLoadCommand">
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
			</td>
		</tr>

		<tr>
			<td>
				<Gemma:label styleClass="desc" key="name" />
				<spring:bind path="simpleExpressionExperimentLoadCommand.name">
					<input type="text" name="<c:out value="${status.expression}"/>"
						value="<c:out value="${status.value}"/>" />
					<span class="fieldError">${status.errorMessage}</span>
				</spring:bind>
			</td>
		</tr>

		<tr>
			<td>
				<Gemma:label styleClass="desc" key="description" />
				<spring:bind
					path="simpleExpressionExperimentLoadCommand.description">
					<input type="textarea" rows="20" cols="120"
						name="<c:out value="${status.expression}"/>"
						value="<c:out value="${status.value}"/>" />
					<span class="fieldError">${status.errorMessage}</span>
				</spring:bind>
			</td>
		</tr>

		<tr>
			<td>
				<Gemma:label styleClass="desc" key="arrayDesign" />
				<spring:bind
					path="simpleExpressionExperimentLoadCommand.arrayDesigns">
					<select name="${status.expression}" multiple>
						<c:forEach items="${arrayDesigns}" var="arrayDesign">
							<spring:transform value="${arrayDesign}" var="name" />
							<option value="${name}"
								<c:if test="${status.value == name}">selected</c:if>>
								${name}
							</option>
						</c:forEach>
					</select>
					<span class="fieldError">${status.errorMessage}</span>
				</spring:bind>

			</td>

			<td>
				<Gemma:label styleClass="desc" key="arrayDesign.name" />
				<spring:bind
					path="simpleExpressionExperimentLoadCommand.arrayDesignName">
					<input type="text" name="<c:out value="${status.expression}"/>"
						value="<c:out value="${status.value}"/>" />
					<span class="fieldError">${status.errorMessage}</span>
				</spring:bind>
			</td>

			<td>
				<Gemma:label styleClass="desc" key="arrayDesign.imageClones" />
				<spring:bind
					path="simpleExpressionExperimentLoadCommand.probeIdsAreImageClones">
					<input type="hidden" name="_<c:out value="${status.expression}"/>">
					<input align="left" type="checkbox"
						name="<c:out value="${status.expression}"/>" value="true"
						<c:if test="${status.value}">checked</c:if> />
					<span class="fieldError"> <c:out
							value="${status.errorMessage}" /> </span>
				</spring:bind>
			</td>


		</tr>
		<tr>
			<td>
				<Gemma:label styleClass="desc" key="taxon.title" />
				<spring:bind path="simpleExpressionExperimentLoadCommand.taxon">
					<select name="${status.expression}">
						<c:forEach items="${taxa}" var="taxon">
							<spring:transform value="${taxon}" var="scientificName" />
							<option value="${scientificName}"
								<c:if test="${status.value == scientificName}">selected</c:if>>
								${scientificName}
							</option>
						</c:forEach>
					</select>
					<span class="fieldError">${status.errorMessage}</span>
				</spring:bind>

			</td>

			<td>
				<Gemma:label styleClass="desc" key="taxon.scientificName" />
				<spring:bind path="simpleExpressionExperimentLoadCommand.taxonName">
					<input type="text" name="<c:out value="${status.expression}"/>"
						value="<c:out value="${status.value}"/>" />
					<span class="fieldError">${status.errorMessage}</span>
				</spring:bind>
			</td>

		</tr>
		<tr>
			<td colspan="2">
				<Gemma:label styleClass="desc" key="data.file" />
				<spring:bind
					path="simpleExpressionExperimentLoadCommand.dataFile.file">
					<input type="file" size=30
						name="<c:out value="${status.expression}" />"
						value="<c:out value="${status.value}" />" />
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
							<Gemma:label styleClass="desc" key="name" />
							<spring:bind
								path="simpleExpressionExperimentLoadCommand.quantitationTypeName">
								<input type="text" name="<c:out value="${status.expression}"/>"
									value="<c:out value="${status.value}"/>" />
								<span class="fieldError">${status.errorMessage}</span>
							</spring:bind>
						</td>
					</tr>

					<tr>
						<td>
							<Gemma:label styleClass="desc" key="description" />
							<spring:bind
								path="simpleExpressionExperimentLoadCommand.quantitationTypeDescription">
								<input type="textarea" rows="20" cols="120"
									name="<c:out value="${status.expression}"/>"
									value="<c:out value="${status.value}"/>" />
								<span class="fieldError">${status.errorMessage}</span>
							</spring:bind>
						</td>
					</tr>


					<tr>
						<td>
							<Gemma:label styleClass="desc" key="standardQuantitationType" />
							<spring:bind path="simpleExpressionExperimentLoadCommand.type">
								<select name="${status.expression}">
									<c:forEach items="${standardQuantitationTypes}" var="type">
										<option value="${type}"
											<c:if test="${status.value == type}">selected</c:if>>
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
							<Gemma:label styleClass="desc" key="scale" />
							<spring:bind path="simpleExpressionExperimentLoadCommand.scale">
								<select name="${status.expression}">
									<c:forEach items="${scaleTypes}" var="type">
										<option value="${type}"
											<c:if test="${status.value == type}">selected</c:if>>
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
							<Gemma:label styleClass="desc" key="ratios" />
							<spring:bind path="simpleExpressionExperimentLoadCommand.isRatio">
								<input type="hidden"
									name="_<c:out value="${status.expression}"/>">
								<input align="left" type="checkbox"
									name="<c:out value="${status.expression}"/>" value="true"
									<c:if test="${status.value}">checked</c:if> />
								<span class="fieldError"> <c:out
										value="${status.errorMessage}" /> </span>
							</spring:bind>
						</td>

					</tr>
				</table>
			</td>
		</tr>
		<%--
				<tr>
					<td colspan="2">
						<table>
							<tr>
								<td>
									<h2>
										Describe the experimental design
									</h2>
								</td>
							</tr>
							<tr>
								<td>
									<Gemma:label styleClass="desc" key="name" />
									<spring:bind
										path="simpleExpressionExperimentLoadCommand.experimentalDesignName">
										<input type="text"
											name="<c:out value="${status.expression}"/>"
											value="<c:out value="${status.value}"/>" />
										<span class="fieldError">${status.errorMessage}</span>
									</spring:bind>
								</td>
							</tr>
							<tr>
								<td>
									<Gemma:label styleClass="desc" key="description" />
									<spring:bind
										path="simpleExpressionExperimentLoadCommand.experimentalDesignDescription">
										<input type="textarea" rows="20" cols="120"
											name="<c:out value="${status.expression}"/>"
											value="<c:out value="${status.value}"/>" />
										<span class="fieldError">${status.errorMessage}</span>
									</spring:bind>
								</td>
							</tr>
						</table>
					</td>
				</tr>
				<tr>
					<td>

						<input type="submit" class="button" name="submit"
							value="<fmt:message key="button.submit"/>" />
						<input type="submit" class="button" name="cancel"
							value="<fmt:message key="button.cancel"/>" />

					</td>
				</tr>--%>
	</table>


</form>

<script type="text/javascript">
			createDeterminateProgressBar();
		</script>


