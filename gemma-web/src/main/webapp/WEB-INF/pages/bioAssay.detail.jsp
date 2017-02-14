<%@ include file="/common/taglibs.jsp"%>
<jsp:useBean id="bioAssay" scope="request"
	class="ubic.gemma.model.expression.bioAssay.BioAssayValueObject" />

<title><fmt:message key="bioAssay.details" /></title>

<div class="padded">
	<h2>
		<fmt:message key="bioAssay.details" />
	</h2>
</div>

<div class="padded v-padded">

	<table class="detail row-separated pad-cols">
		<tr>
			<td class="label">
				<strong> <fmt:message key="bioAssay.name" />
				</strong>
			</td>
			<td>${bioAssay.name}</td>
		</tr>
		<tr>
			<td class="label">
				<fmt:message key="databaseEntry.title" />
			</td>
			<td>
				<Gemma:databaseEntry
					databaseEntryValueObject="${bioAssay.accession}" />
			</td>
		</tr>

		<tr>
			<td class="label">
				<strong> <fmt:message key="bioAssay.description" />
				</strong>
			</td>
			<td>${bioAssay.description}</td>
		</tr>


		<tr>
			<td class="label">
				<strong>Sample</strong>
			</td>
			<td>
				<a
					href="/Gemma/bioMaterial/showBioMaterial.html?id=${bioAssay.sample.id}">${bioAssay.sample.name}</a>
			</td>
		</tr>
		<tr>
			<td class="label">
				<strong>Array design</strong>
			</td>
			<td>
				<a
					href="/Gemma/arrays/showArrayDesign.html?id=${bioAssay.arrayDesign.id}">${bioAssay.arrayDesign.shortName}</a>
				${bioAssay.arrayDesign.name}
			</td>
		</tr>


	</table>

</div>