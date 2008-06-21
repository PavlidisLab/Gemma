<%@ include file="/common/taglibs.jsp"%>
<jsp:useBean id="experimentalDesign" scope="request" class="ubic.gemma.model.expression.experiment.ExperimentalDesignImpl" />
<jsp:useBean id="expressionExperiment" scope="request"
	class="ubic.gemma.model.expression.experiment.ExpressionExperimentImpl" />
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<head>
	<title><fmt:message key="experimentalDesign.details" /></title>

	<jwr:script src='/scripts/ajax/ext/data/DwrProxy.js' />
	<jwr:script src='/scripts/app/ExperimentalDesign.js' />

</head>

<authz:authorize ifAnyGranted="admin">
	<input type="hidden" name="hasAdmin" id="hasAdmin" value="true" />
</authz:authorize>
<authz:authorize ifNotGranted="admin">
	<input type="hidden" name="hasAdmin" id="hasAdmin" value="" />
</authz:authorize>

<input type="hidden" name="expressionExperimentID" value="${expressionExperiment.id}" />
<input type="hidden" name="experimentalDesignID" value="${experimentalDesign.id}" />

<div id="messages" style="margin: 10px; width: 400px"></div>
<div id="taskId" style="display: none;"></div>
<div id="progress-area"></div>

<div style="padding: 2px;" onclick="Effect.toggle('edDetail', 'blind', {duration:0.1})">
	<h2>
		<img src="/Gemma/images/plus.gif" />
		<fmt:message key="experimentalDesign.details" />
		for
		<a href='<c:out value="${expressionExperimentUrl}" />'><jsp:getProperty name="expressionExperiment"
				property="shortName" /> </a>
	</h2>
</div>
<div id="edDetail" style="display: none">
	<div>
		<%-- inner div needed for effect  --%>
		<table cellspacing="10">
			<tr>
				<td class="label">
					<b><fmt:message key="experimentalDesign.name" /> </b>
				</td>
				<td>
					<%
					if ( experimentalDesign.getName() != null ) {
					%>
					<jsp:getProperty name="experimentalDesign" property="name" />
					<%
					                } else {
					                out.print( "Experimental Design Name unavailable" );
					            }
					%>
				</td>
			</tr>
			<tr>
				<td class="label">
					<b> <fmt:message key="experimentalDesign.description" /> </b>
				</td>
				<td>
					<%
					if ( experimentalDesign.getDescription() != null ) {
					%>
					<jsp:getProperty name="experimentalDesign" property="description" />
					<%
					                } else {
					                out.print( "Description unavailable" );
					            }
					%>
				</td>
			</tr>

			<tr>
				<td class="label">
					<b> <fmt:message key="experimentalDesign.replicateDescription" /> </b>
				</td>
				<td>
					<%
					if ( experimentalDesign.getReplicateDescription() != null ) {
					%>
					<jsp:getProperty name="experimentalDesign" property="replicateDescription" />
					<%
					                } else {
					                out.print( "Replicate description unavailable" );
					            }
					%>
				</td>
			</tr>

			<tr>
				<td class="label">
					<b> <fmt:message key="experimentalDesign.qualityControlDescription" /> </b>
				</td>
				<td>
					<%
					if ( experimentalDesign.getQualityControlDescription() != null ) {
					%>
					<jsp:getProperty name="experimentalDesign" property="qualityControlDescription" />
					<%
					                } else {
					                out.print( "Quality control description unavailable" );
					            }
					%>
				</td>
			</tr>

			<tr>
				<td class="label">
					<b> <fmt:message key="experimentalDesign.normalizationDescription" /> </b>
				</td>
				<td>
					<%
					if ( experimentalDesign.getNormalizationDescription() != null ) {
					%>
					<jsp:getProperty name="experimentalDesign" property="normalizationDescription" />
					<%
					                } else {
					                out.print( "Normalization description unavailable" );
					            }
					%>
				</td>
			</tr>

			<%
			                    if ( experimentalDesign.getExperimentalFactors() != null
			                    && experimentalDesign.getExperimentalFactors().size() > 0 ) {
			%>
			<tr>
				<td class="label">
					Design File
				</td>
				<td>
					<a href="#"
						onClick="fetchData(false, ${expressionExperiment.id }, 'text', null, ${expressionExperiment.experimentalDesign.id})">Click
						to start download</a>

					<a class="helpLink" href="?"
						onclick="showHelpTip(event, 'Tab-delimited design file for this experiment, if available.'); return false"><img
							src="/Gemma/images/help.png" /> </a>
				</td>
			</tr>
			<%
			}
			%>

			<authz:authorize ifAllGranted="admin">
				<tr>
					<td class="label">
						<fmt:message key="auditTrail.date" />
					</td>
					<td>
						<%
						                if ( experimentalDesign.getAuditTrail() != null ) {
						                out.print( experimentalDesign.getAuditTrail().getCreationEvent().getDate() );
						            } else {
						                out.print( "Create date unavailable" );
						            }
						%>
					</td>
				</tr>
			</authz:authorize>
		</table>
	</div>
</div>

<!-- Expression Experiment Details  -->

<div style="padding: 2px;" onclick="Effect.toggle('eeDetail', 'blind', {duration:0.1})">
	<h2>
		<img src="/Gemma/images/plus.gif" />
		<fmt:message key="expressionExperiment.details" />
	</h2>
</div>
<div id="eeDetail" style="display: none">
	<div>
		<%-- inner div needed for effect  --%>
		<table cellspacing="10">
			<tr>
				<td class="label">
					<b><fmt:message key="expressionExperiment.name" /> </b>
				</td>
				<td>
					<%
					if ( expressionExperiment.getName() != null ) {
					%>
					<jsp:getProperty name="expressionExperiment" property="name" />
					<%
					                } else {
					                out.print( "Expression Experiment Name unavailable" );
					            }
					%>
				</td>
			</tr>
			<tr>
				<td class="label">
					<fmt:message key="expressionExperiment.description" />
				</td>
				<td>
					<%
					if ( expressionExperiment.getDescription() != null ) {
					%>
					<div class="clob" style="width: 40%;">
						<jsp:getProperty name="expressionExperiment" property="description" />
					</div>

					<%
					                } else {
					                out.print( "Description unavailable" );
					            }
					%>
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
					<%
					if ( expressionExperiment.getPrimaryPublication() != null ) {
					%>
					<Gemma:citation citation="${expressionExperiment.primaryPublication }" />
					<%
					                } else {
					                out.print( "Primary publication unavailable" );
					            }
					%>
				</td>
			</tr>
		</table>
	</div>
</div>

<!-- Experimental Factors -->

<div id="experimentalFactorPanel" style="margin-bottom: 1em;"></div>
<form name="factorValueForm">
	<div id="factorValuePanel" style="margin-bottom: 1em;"></div>
</form>
<div id="bioMaterialsPanel"></div>

