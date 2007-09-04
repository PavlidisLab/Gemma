<%@ include file="/common/taglibs.jsp"%>
<%@ page import="java.util.*"%>
<%@ page
	import="ubic.gemma.model.expression.arrayDesign.ArrayDesignImpl"%>
<jsp:useBean id="arrayDesign" scope="request"
	class="ubic.gemma.model.expression.arrayDesign.ArrayDesignImpl" />

<!DOCTYPE html PUBLIC "-//W3C//Dtd html 4.01 transitional//EN">
<head>
	<title><jsp:getProperty name="arrayDesign" property="name" />
	</title>

	<script
		src="<c:url value='/scripts/ext/adapter/prototype/ext-prototype-adapter.js'/>"
		type="text/javascript"></script>
	<script src="<c:url value='/scripts/ext/ext-all-debug.js'/>"
		type="text/javascript"></script>
	<script type="text/javascript"
		src="<c:url value='/scripts/ext/data/ListRangeReader.js'/>"></script>
	<script type="text/javascript"
		src="<c:url value='/scripts/ext/data/DwrProxy.js'/>"></script>
	<script type='text/javascript'
		src='/Gemma/dwr/interface/AuditController.js'></script>
	<script type='text/javascript' src='/Gemma/dwr/engine.js'></script>
	<script type='text/javascript' src='/Gemma/dwr/util.js'></script>
	<script type="text/javascript"
		src="<c:url value='/scripts/ajax/auditTrail.js'/>"
		type="text/javascript"></script>
</head>

<h2>
	Details for
	<jsp:getProperty name="arrayDesign" property="name" /> (<jsp:getProperty name="arrayDesign" property="shortName" />)
</h2>


<!--  Summary of array design associations -->
<c:if test="${ summary != ''}">
	<table class='datasummaryarea'>
		<caption>
			Sequence analysis details


		</caption>
		<tr>
			<td>

				<table class='datasummary'>
					<tr>
						<td colspan=2 align=center>
						</td>
					</tr>
					<tr>
						<td>
							Probes
						</td>
						<td>
							${numCompositeSequences}
						</td>
					</tr>
					<authz:authorize ifAnyGranted="admin">

						<tr>
							<td>
								With sequences
							</td>
							<td>
								${summary.numProbeSequences}
							</td>
						</tr>
						<tr>
							<td>
								With genome alignments
							</td>
							<td>
								${summary.numProbeAlignments}
							</td>
						</tr>
					</authz:authorize>
					<tr>
						<td>
							Mapping to genes
						</td>
						<td>
							${summary.numProbesToGenes}
						</td>
					</tr>
					<tr>
						<td align="right">
							Known genes
						</td>
						<td>
							${summary.numProbesToKnownGenes }
						</td>
					</tr>
					<tr>
						<td align="right">
							Predicted genes
						</td>
						<td>
							${summary.numProbesToPredictedGenes}
						</td>
					</tr>
					<tr>
						<td align="right">
							Probe-aligned regions
						</td>
						<td>
							${summary.numProbesToProbeAlignedRegions}
						</td>
					</tr>
					<tr>
						<td>
							Unique genes represented
						</td>
						<td>
							${summary.numGenes}
						</td>
					</tr>
					<tr>
						<td colspan=2 align='center' class='small'>
							(as of ${summary.dateCached})
						</td>
					</tr>
				</table>

			</td>
		</tr>
		<tr>
			<script>
		var text = '<Gemma:help helpFile="sequenceAnalysisHelp.html"/>';
		function doit(event) {showWideHelpTip(event,text); }
		</script>
			<td colspan="2">
				<a class="helpLink" name="?" href=""
					onclick="doit(event);return false;"> <img
						src="/Gemma/images/help.png" /> </a>
				<%--"<Gemma:help helpFile='sequenceAnalysisHelp.html'/>" --%>
			</td>
		</tr>
	</table>
</c:if>

<table>
	<tr>
		<td class="label">
			Name
		</td>
		<td>
			<jsp:getProperty name="arrayDesign" property="name" />
		</td>
	</tr>
	<tr>
		<td class="label">
			Provider
		</td>
		<td>
			<%
			if ( arrayDesign.getDesignProvider() != null ) {
			%>
			<%=arrayDesign.getDesignProvider().getName()%>
			<%
			} else {
			%>
			(Not listed)
			<%
			}
			%>
		</td>
	</tr>
	<tr>
		<td class="label">
			Species
		</td>
		<td>
			<c:out value="${taxon}" />
		</td>
	</tr>
	<tr>
		<td class="label">
			Number of probes
		</td>
		<td>
			<c:out value="${numCompositeSequences}" />
			<a
				href="/Gemma/arrays/showCompositeSequenceSummary.html?id=<jsp:getProperty name="arrayDesign" property="id" />"><img
					src="/Gemma/images/magnifier.png" /> </a>

		</td>
	</tr>
	<tr>
		<td class="label">
			Accessions
		</td>
		<td>
			<%
			if ( ( arrayDesign.getExternalReferences() ) != null && ( arrayDesign.getExternalReferences().size() > 0 ) ) {
			%>
			<c:forEach var="accession"
				items="${ arrayDesign.externalReferences }">
				<Gemma:databaseEntry databaseEntry="${accession}" />
				<br />
			</c:forEach>
			<%
			}
			%>
		</td>
	</tr>
	<tr>
		<td class="label">
			Expression experiments using this array
		</td>
		<td>
			<c:out value="${numExpressionExperiments}" />
			<a
				href="/Gemma/expressionExperiment/showAllExpressionExperiments.html?id=<c:out value="${expressionExperimentIds}" />">
				<img src="/Gemma/images/magnifier.png" /> </a>
		</td>
	</tr>
	<tr>
		<td class="label">
			Type
		</td>
		<td>
			<c:out value="${technologyType}" />
		</td>
	</tr>

	<tr>
		<td class="label">
			Description
		</td>
		<td>
			<%
			if ( arrayDesign.getDescription() != null && arrayDesign.getDescription().length() > 0 ) {
			%>
			<div class="clob">
				<jsp:getProperty name="arrayDesign" property="description" />
			</div>
			<%
			} else {
			%>
			(None provided)
			<%
			}
			%>
		</td>
	</tr>
	<tr>
		<td class="label">
			Subsumes
			<a class="helpLink" href="?"
				onclick="showHelpTip(event, 'Array designs that this one \'covers\' -- it contains all the same sequences.'); return false"><img
					src="/Gemma/images/help.png" /> </a>
		</td>
		<td>
			<Gemma:arrayDesignGrouping subsumees="${subsumees }" />
	</tr>
	<tr>
		<td class="label">
			Subsumed by
			<a class="helpLink" href="?"
				onclick="showHelpTip(event, 'Array design that \'covers\' this one. '); return false"><img
					src="/Gemma/images/help.png" /> </a>
		</td>
		<td>
			<Gemma:arrayDesignGrouping subsumer="${subsumer }" />
	</tr>

	<tr>
		<td class="label">
			Merger of
			<a class="helpLink" href="?"
				onclick="showHelpTip(event, 'Array designs that were merged to create this one.'); return false"><img
					src="/Gemma/images/help.png" /> </a>
		</td>
		<td>
			<Gemma:arrayDesignGrouping subsumees="${mergees }" />
	</tr>
	<tr>
		<td class="label">
			Merged into
			<a class="helpLink" href="?"
				onclick="showHelpTip(event, 'Array design this one is merged into.'); return false"><img
					src="/Gemma/images/help.png" /> </a>
		</td>
		<td>
			<Gemma:arrayDesignGrouping subsumer="${merger }" />
	</tr>
	<authz:authorize ifAnyGranted="admin">
		<tr>
			<td colspan="2">


				<h3>
					History
				</h3>
				<div id="auditTrail" class="x-grid-mso"
					style="border: 1px solid #c3daf9; overflow: hidden; width:630px; height:250px;"></div>
				<input type="hidden" name="auditableId" id="auditableId"
					value="${arrayDesign.id}" />
				<input type="hidden" name="auditableClass" id="auditableClass"
					value="${arrayDesign.class.name}" />

			</td>
		</tr>
	</authz:authorize>
</table>

<table>
	<tr>
		<td colspan="2">
			<hr />
		</td>
	</tr>
	<tr>
		<td colspan="2">
			<div align="left">
				<input type="button"
					onclick="location.href='showAllArrayDesigns.html'"
					value="Show all array designs">
			</div>
		</td>
		<authz:authorize ifAnyGranted="admin">
			<td COLSPAN="2">
				<div align="left">
					<input type="button"
						onclick="location.href='/Gemma/arrayDesign/editArrayDesign.html?id=<%=request.getAttribute( "id" )%>'"
						value="Edit">
				</div>
			</td>
		</authz:authorize>
	</tr>
</table>



<hr />

<form name="ArrayDesignFilter" action="filterArrayDesigns.html"
	method="POST">
	<h4>
		Enter search criteria for finding another array design here
	</h4>
	<input type="text" name="filter" size="66" />
	<input type="submit" value="Find" />
</form>
