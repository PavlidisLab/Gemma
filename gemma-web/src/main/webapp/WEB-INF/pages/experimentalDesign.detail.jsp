<%@ include file="/common/taglibs.jsp"%>
<jsp:useBean id="experimentalDesign" scope="request"
	class="ubic.gemma.model.expression.experiment.ExperimentalDesignImpl" />
<jsp:useBean id="expressionExperiment" scope="request"
	class="ubic.gemma.model.expression.experiment.ExpressionExperimentImpl" />
<head>
	<title><fmt:message key="experimentalDesign.details" />
	</title>
	<jwr:script src='/scripts/ajax/ext/data/DwrProxy.js' />
	<jwr:script src='/scripts/app/eeDataFetch.js' />
	<jwr:script src='/scripts/app/ExperimentalDesign.js' />
</head>

<input type="hidden" id="reloadOnLogout" value="true">
<input type="hidden" id="reloadOnLogin" value="true"/>

<input type="hidden" id="expressionExperimentID" value="${expressionExperiment.id}" />
<input type="hidden" id="experimentalDesignID" value="${experimentalDesign.id}" />
<input type="hidden" id="currentUserCanEdit" value="${currentUserCanEdit}" />

<div id="messages" style="margin: 10px; width: 400px"></div>

<div style="padding: 2px; margin-bottom: 10px;">
	<h2>
		<fmt:message key="experimentalDesign.details" />
		for
		<a href='<c:out value="${expressionExperimentUrl}" />'><jsp:getProperty name="expressionExperiment"
				property="shortName" /> </a>
	</h2>

	<c:choose>
		<c:when test="${!hasPopulatedDesign}">
			<strong>This experiment does not have any experimental design details filled in.</strong>
		</c:when>
		<c:otherwise>
			<p>
				Download design File:
				<a href="#"
					onClick="fetchData(false, ${expressionExperiment.id }, 'text', null, ${expressionExperiment.experimentalDesign.id})">Click
					to start download</a>

				<a class="helpLink" href="?"
					onclick="showHelpTip(event, 'Tab-delimited design file for this experiment, if available.'); return false"><img
						src="/Gemma/images/help.png" /> </a>
			</p>

		</c:otherwise>
	</c:choose>


	<table cellspacing="3">
		<tr>
			<td class="label">
				<b><fmt:message key="expressionExperiment.name" /> </b>
			</td>
			<td>
				<c:choose>
					<c:when test="${not empty expressionExperiment.name}">
				${expressionExperiment.name}
			</c:when>
					<c:otherwise>(Name not available)</c:otherwise>
				</c:choose>
			</td>
		</tr>
		<tr>
			<td class="label">
				<fmt:message key="expressionExperiment.description" />
			</td>
			<td>
				<c:choose>
					<c:when test="${not empty expressionExperiment.description}">
				${expressionExperiment.description}
			</c:when>
					<c:otherwise>(Description not available)</c:otherwise>
				</c:choose>
			</td>
		</tr>
		<tr>
			<td class="label">
				<fmt:message key="databaseEntry.title" />
			</td>
			<td>
				<Gemma:databaseEntry databaseEntry="${expressionExperiment.accession}" />
			</td>
		</tr>
		<tr>
			<td class="label">
				<fmt:message key="pubMed.publication" />
			</td>


			<td>
				<c:choose>
					<c:when test="${not empty expressionExperiment.primaryPublication}">
						<Gemma:citation citation="${expressionExperiment.primaryPublication }" />
					</c:when>
					<c:otherwise>(Primary publication not available)</c:otherwise>
				</c:choose>
			</td>


		</tr>
	</table>
</div>
<security:accesscontrollist domainObject="${expressionExperiment}" hasPermission="WRITE,ADMINISTRATION">
	<c:if test="${!hasPopulatedDesign}">
		<div style="width: 600px; background-color: #EEEEEE; margin: 7px; padding: 7px;">
			<p>
				Use the form below to populate the experimental design details. Alternatively you can
				<a href="#" onClick="showDesignUploadForm()">upload</a> a design description file. Instructions are
				<a target="_blank"
					href="<c:url value='http://www.chibi.ubc.ca/faculty/pavlidis/wiki/display/gemma/Experimental+Design+Upload' />">here</a>.
				If you want to use the upload method, you can get a blank
				<a href="#"
					onClick="fetchData(false, ${expressionExperiment.id }, 'text', null, ${expressionExperiment.experimentalDesign.id})">template
					file</a> to get started.
			</p>
		</div>
	</c:if>
</security:accesscontrollist>

<!-- Experimental Factors -->

<%-- This form element is needed for the checkboxes in the factor value panel --%>
<form name="factorValueForm">
	<div id="experimentalDesignPanel"></div>
</form>
<div id="experimentalFactorPanel" style="margin-bottom: 1em;"></div>

<div id="factorValuePanel" class="x-hide-display" style="margin-bottom: 1em;"></div>

<div id="bioMaterialsPanel" class="x-hide-display"></div>