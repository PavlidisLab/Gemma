<%@ include file="/common/taglibs.jsp"%>
<%@ page import="java.util.*"%>
<%@ page import="ubic.gemma.model.expression.arrayDesign.ArrayDesignImpl"%>
<jsp:useBean id="arrayDesign" scope="request" class="ubic.gemma.model.expression.arrayDesign.ArrayDesignImpl" />

<!DOCTYPE html PUBLIC "-//W3C//Dtd html 4.01 transitional//EN">
<head>
	<title><jsp:getProperty name="arrayDesign" property="shortName" /> - <jsp:getProperty name="arrayDesign" property="name" /></title>

	<script src="<c:url value='/scripts/ext/adapter/prototype/ext-prototype-adapter.js'/>" type="text/javascript"></script>
	<script src="<c:url value='/scripts/ext/ext-all.js'/>" type="text/javascript"></script>
	<script type="text/javascript" src="<c:url value='/scripts/ext/data/ListRangeReader.js'/>"></script>
	<script type='text/javascript' src='/Gemma/dwr/interface/AuditController.js'></script>
	<script type='text/javascript' src='/Gemma/dwr/engine.js'></script>
	<script type='text/javascript' src='/Gemma/dwr/util.js'></script>
	<script type="text/javascript" src="<c:url value='/scripts/ajax/auditTrail.js'/>" type="text/javascript"></script>
	<script type="text/javascript" src="<c:url value='/scripts/progressbar.js'/>"></script>
	<script type="text/javascript" src="<c:url value='/scripts/ext/data/DwrProxy.js'/>"></script>
	<script type='text/javascript' src='/Gemma/dwr/interface/ArrayDesignController.js'></script>
	<script type='text/javascript' src='/Gemma/dwr/interface/ProgressStatusService.js'></script>
	<script type="text/javascript" src="<c:url value='/scripts/ajax/arrayDesign.js'/>" type="text/javascript"></script>
	<link rel="stylesheet" type="text/css" media="all" href="<c:url value='/styles/progressbar.css'/>" />
</head>

<h2>
	Details for:
	"<jsp:getProperty name="arrayDesign" property="name" />"
	<c:if test="${ troubleEvent != null}">
	&nbsp;
	<img src='<c:url value="/images/icons/warning.png"/>' height='16' width='16' alt='trouble'
			title='${ troubleEventDescription }' />
	</c:if>
	<c:if test="${ validatedEvent != null}">
	&nbsp;
	<img src='<c:url value="/images/icons/ok.png"/>' height='16' width='16' alt='validated'
			title='${ validatedEventDescription }' />
	</c:if>
</h2>

<!--
<div style='float:right; width:25em; margin:1em; clear:right;'>
<c:if test="${ troubleEvent != null}">
<form name='troubleEvent'>
<input type='hidden' name='detail' value='<c:out value="${ troubleEvent.detail }" />' />
</form>
<p class='trouble'>This dataset was flagged as <a href="?"
	onclick="showHelpTip(event, document.troubleEvent.detail.value); return false;">problematic</a>
	on <c:out value="${ troubleEvent.date }" /> by <c:out value="${ troubleEvent.performer.name }" />.</p>
</c:if>

<c:if test="${ validatedEvent != null}">
<p class='validated'>This dataset was validated
	on <c:out value="${ validatedEvent.date }" /> by <c:out value="${ validatedEvent.performer.name }" />.</p>
</c:if>
</div>
-->

<div id="messages" style="margin: 10px; width: 400px"></div>
<div id="taskId" style="display: none;"></div>
<div id="progress-area" ></div>

<!--  Summary of array design associations -->
<c:if test="${ summary != ''}">
	<table class='datasummaryarea'>
		<caption>
			Sequence analysis details
		</caption>

		<tr>
			<td>

				<div id="arraySummary_${arrayDesign.id}" name="arraySummary_${arrayDesign.id}">
					<table class='datasummary'>
						<tr>
							<td colspan=2 align=center>
							</td>
						</tr>
						<tr>
							<td>
								Probes
							</td>
							<td align="right">
								${numCompositeSequences}
							</td>
						</tr>
						<tr>
							<td>
								With seq.
							</td>
							<td align="right">
								${summary.numProbeSequences}
							</td>
						</tr>
						<tr>
							<td>
								With align
							</td>
							<td align="right">
								${summary.numProbeAlignments}
							</td>
						</tr>
						<tr>
							<td>
								Mapped
							</td>
							<td align="right">
								${summary.numProbesToGenes}
							</td>
						</tr>
						<tr>
							<td>
								&nbsp;&nbsp;Known
							</td>
							<td align="right">
								${summary.numProbesToKnownGenes }
							</td>
						</tr>
						<tr>
							<td>
								&nbsp;&nbsp;Predicted
							</td>
							<td align="right">
								${summary.numProbesToPredictedGenes}
							</td>
						</tr>
						<tr>
							<td>
								&nbsp;&nbsp;Unknown
							</td>
							<td align="right">
								${summary.numProbesToProbeAlignedRegions}
							</td>
						</tr>
						<tr>
							<td>
								Unique genes
							</td>
							<td align="right">
								${summary.numGenes}
							</td>
						</tr>
						<tr>
							<td colspan=2 align='center' class='small'>
								(as of ${summary.dateCached})
							</td>
						</tr>
					</table>
				</div>

			</td>
		</tr>
		<tr>
			<td colspan="2">
				<authz:authorize ifAnyGranted="admin">
					<input type="button" value="Refresh report" onClick="updateReport(${arrayDesign.id })" />
				</authz:authorize>
			</td>
		</tr>
		<tr>
			<script>
		var text = '<Gemma:help helpFile="sequenceAnalysisHelp.html"/>';
		function doit(event) {showWideHelpTip(event,text); }
		</script>
			<td colspan="2">
				<a class="helpLink" name="?" href="" onclick="doit(event);return false;"> <img src="/Gemma/images/help.png" />
				</a>
				<%--"<Gemma:help helpFile='sequenceAnalysisHelp.html'/>" --%>
			</td>
		</tr>
	</table>
</c:if>

<table style="width: 70%">
<%-- 	<tr>
		<td  style="width: 25%"  class="label">
			Name
		</td>
		<td>
			<jsp:getProperty name="arrayDesign" property="name" />
		</td>
	</tr> --%>
	<tr>
		<td  style="width: 25%"  class="label">
			Short name
		</td>
		<td>
			<jsp:getProperty name="arrayDesign" property="shortName" />
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
			&nbsp;
			<a title="Show details of probes"
				href="/Gemma/arrays/showCompositeSequenceSummary.html?id=<jsp:getProperty name="arrayDesign" property="id" />"><img
					src="/Gemma/images/magnifier.png" /> </a>

		</td>
	</tr>
	<tr>
		<td class="label">
			External accessions&nbsp;<a class="helpLink" href="?"
				onclick="showHelpTip(event, 'References to this design in other databases'); return false"><img
					src="/Gemma/images/help.png" /> </a>
		</td>
		<td>
			<%
			if ( ( arrayDesign.getExternalReferences() ) != null && ( arrayDesign.getExternalReferences().size() > 0 ) ) {
			%>
			<c:forEach var="accession" items="${ arrayDesign.externalReferences }">
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
			Experiments using this array
		</td>
		<td>
			<c:out value="${numExpressionExperiments}" />
			<a title="Show details of datasets"
				href="/Gemma/expressionExperiment/showAllExpressionExperiments.html?id=<c:out value="${expressionExperimentIds}" />">
				<img src="/Gemma/images/magnifier.png" /> </a>
		</td>
	</tr>
	<tr>
		<td class="label">
			Type&nbsp;<a class="helpLink" href="?"
				onclick="showHelpTip(event, 'Our best guess about what type of data is produced using this array'); return false"><img
					src="/Gemma/images/help.png" /> </a>
		</td>
		<td>
			<c:out value="${technologyType}" />
		</td>
	</tr>

	<tr>
		<td style="width: 25%" class="label">
			Description
			<a class="helpLink" href="?"
				onclick="showHelpTip(event, 'The description is usually provided by the original data source e.g. GEO.'); return false"><img
					src="/Gemma/images/help.png" /> </a>
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
	<c:if test="$subsumees != null}">
		<tr>
			<td class="label">
				Subsumes
				<a class="helpLink" href="?"
					onclick="showHelpTip(event, 'Array designs that this one \'covers\' -- it contains all the same sequences.'); return false"><img
						src="/Gemma/images/help.png" /> </a>
			</td>
			<td>
				<Gemma:arrayDesignGrouping subsumees="${subsumees }" />
			</td>
		</tr>
	</c:if>
	<c:if test="$subsumer != null}">
		<tr>
			<td class="label">
				Subsumed by
				<a class="helpLink" href="?" onclick="showHelpTip(event, 'Array design that \'covers\' this one. '); return false"><img
						src="/Gemma/images/help.png" /> </a>
			</td>
			<td>
				<Gemma:arrayDesignGrouping subsumer="${subsumer }" />
			</td>
		</tr>
	</c:if>
	<c:if test="$mergees != null}">
		<tr>
			<td class="label">
				Merger of
				<a class="helpLink" href="?"
					onclick="showHelpTip(event, 'Array designs that were merged to create this one.'); return false"><img
						src="/Gemma/images/help.png" /> </a>
			</td>
			<td>
				<Gemma:arrayDesignGrouping subsumees="${mergees }" />
			</td>
		</tr>
	</c:if>
	<c:if test="$merger != null}">
		<tr>
			<td class="label">
				Merged into
				<a class="helpLink" href="?" onclick="showHelpTip(event, 'Array design this one is merged into.'); return false"><img
						src="/Gemma/images/help.png" /> </a>
			</td>
			<td>
				<Gemma:arrayDesignGrouping subsumer="${merger }" />
			</td>
		</tr>
	</c:if>

	<tr>
		<td class="label">
			Annotation file(s)
			<a class="helpLink" href="?"
				onclick="showHelpTip(event, 'Text-based (tab-delimited) annotation files for this array, if available. The files include GO terms as directly annotated (brief), including all parent terms (All parents) or biological process terms only.'); return false"><img
					src="/Gemma/images/help.png" /> </a>
		</td>
		<td>
			<c:if test="${ noParentsAnnotationLink != null}">
				<a class="annotationLink" href=${noParentsAnnotationLink } />Brief</a>&nbsp;&nbsp;
			</c:if>
			<c:if test="${ allParentsAnnotationLink != null}">
				<a class="annotationLink" href=${allParentsAnnotationLink } />All parents</a>&nbsp;&nbsp;
			</c:if>
			<c:if test="${bioProcessAnnotationLink != null}">
				<a class="annotationLink" href=${bioProcessAnnotationLink } />Biological Process only</a>&nbsp;&nbsp;
			</c:if>
		</td>
	</tr>


	<authz:authorize ifAnyGranted="admin">
		<tr>
			<td colspan="2">


				<h3>
					History
				</h3>
				<div id="auditTrail" class="x-grid-mso"
					style="border: 1px solid #c3daf9; overflow: hidden; width: 630px; height: 250px;"></div>
				<input type="hidden" name="auditableId" id="auditableId" value="${arrayDesign.id}" />
				<input type="hidden" name="auditableClass" id="auditableClass" value="${arrayDesign.class.name}" />

			</td>
		</tr>
	</authz:authorize>
</table>

<div style="padding-top: 20px;">
	<table>
		<tr>
			<td colspan="2">
				<hr />
			</td>
		</tr>
		<tr>
			<td colspan="2">
				<div align="left">
					<input type="button" onclick="location.href='showAllArrayDesigns.html'" value="Show all array designs">
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
</div>

<div style="padding-top: 20px;">
	<form name="ArrayDesignFilter" action="filterArrayDesigns.html" method="POST">
		<h4>
			Enter search criteria for finding another array design here
		</h4>
		<input type="text" name="filter" size="66" />
		<input type="submit" value="Find" />
	</form>
</div>
