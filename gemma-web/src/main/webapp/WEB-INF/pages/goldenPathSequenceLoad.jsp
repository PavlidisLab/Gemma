<%@ include file="/common/taglibs.jsp"%>
<jsp:useBean id="goldenPathSequenceLoadCommand" scope="request"
	class="ubic.gemma.web.controller.genome.GoldenPathSequenceLoadCommand" />

<head>
	<title><fmt:message key="Load Golden Path sequence data into Gemma" /></title>
	<content tag="heading">
	<fmt:message key="goldenpathload.heading" />
	</content>


	<jwr:script src='/scripts/ajax/ext/data/DwrProxy.js' />
</head>
<body>
	<h1>
		Load Golden Path sequence data into Gemma
	</h1>

	<p>
		<strong>This is normally a one-time process for each taxon. Don't load more than once unless you know what you are
			doing!</strong> Also, the list of taxa doens't imply the Golden Path database for each taxon is installed. You have to make sure
		it's available.
	</p>
	<p>
		The limit parameter is provided for testing purposes. Set it to -1 to load all sequences.
	</p>

	<form method="post" action="<c:url value="/genome/goldenPathSequenceLoad.html"/>" onsubmit="startProgress()">

		<table>
			<tr>
				<td>

					<spring:bind path="goldenPathSequenceLoadCommand">
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

				</td>
			</tr>

			<tr>
				<td>
					<Gemma:label styleClass="desc" key="taxon.title" />
					<spring:bind path="goldenPathSequenceLoadCommand.taxon">
						<select name="${status.expression}">
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
			</tr>

			<tr>
				<td>
					<Gemma:label styleClass="desc" key="limit" />
					<spring:bind path="goldenPathSequenceLoadCommand.limit">
						<input type="text" name="<c:out value="${status.expression}"/>" value="<c:out value="${status.value}"/>" />
						<span class="fieldError">${status.errorMessage}</span>
					</spring:bind>

				</td>
			</tr>
			<tr>
				<td>

					<input type="submit" class="button" name="submit" value="<fmt:message key="button.submit"/>" />
					<input type="submit" class="button" name="cancel" value="<fmt:message key="button.cancel"/>" />

				</td>
			</tr>
		</table>

	</form>

</body>
