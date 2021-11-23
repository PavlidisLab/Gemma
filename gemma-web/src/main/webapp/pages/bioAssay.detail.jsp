<%@ include file="/common/taglibs.jsp"%>
<jsp:useBean id="bioAssay" scope="request" class="ubic.gemma.model.expression.bioAssay.BioAssayValueObject" />

<title><fmt:message key="bioAssay.details" /></title>

<h2>
	<fmt:message key="bioAssay.details" />
</h2>
<table width="30%" cellspacing="10">
	<tr>
		<td class="label"><strong> <fmt:message key="bioAssay.name" />
		</strong></td>
		<td>${bioAssay.name}</td>
	</tr>
	<tr>
		<td class="label"><fmt:message key="databaseEntry.title" /></td>
		<td><Gemma:databaseEntry databaseEntryValueObject="${bioAssay.accession}" /></td>
	</tr>

	<tr>
		<td class="label"><strong> <fmt:message key="bioAssay.description" />
		</strong></td>
		<td>${bioAssay.description}</td>
	</tr>

	<tr>
		<td class="label"><strong>Sample</strong></td>
		<td><a href="/Gemma/bioMaterial/showBioMaterial.html?id=${bioAssay.sample.id}">${bioAssay.sample.name}</a></td>
	</tr>
	<tr>
		<td class="label"><strong>Platform</strong></td>
		<td><a href="/Gemma/arrays/showArrayDesign.html?id=${bioAssay.arrayDesign.id}">${bioAssay.arrayDesign.shortName}</a>&nbsp;
			${bioAssay.arrayDesign.name}</td>
	</tr>

	<c:if test="${not empty bioAssay.originalPlatform}">
		<tr>
			<td class="label"><strong>Original platform</strong></td>
			<td><a href="/Gemma/arrays/showArrayDesign.html?id=${bioAssay.originalPlatform.id}">${bioAssay.originalPlatform.shortName}</a>&nbsp;
				${bioAssay.originalPlatform.name}</td>
		</tr>
	</c:if>

</table>

