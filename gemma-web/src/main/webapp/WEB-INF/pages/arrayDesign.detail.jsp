<%@ include file="/common/taglibs.jsp"%>
<%@ page import="ubic.gemma.model.expression.arrayDesign.ArrayDesignImpl;"%>
<jsp:useBean id="arrayDesign" scope="request" class="ubic.gemma.model.expression.arrayDesign.ArrayDesignImpl" />

<head>

<title><jsp:getProperty name="arrayDesign" property="shortName" /> - <jsp:getProperty name="arrayDesign"
		property="name" /></title>


<jwr:script src='/scripts/ajax/ext/data/DwrProxy.js' />
<jwr:script src='/scripts/app/arrayDesign.js' />


<security:authorize access="hasRole('GROUP_ADMIN')">
	<script type="text/javascript">
	Ext.namespace('Gemma');
	Ext.onReady( function() {
		var id = dwr.util.getValue("auditableId");
		if (!id) {
			return;
		}
		var clazz = dwr.util.getValue("auditableClass");
		var auditable = {
			id :id,
			classDelegatingFor :clazz
		};
		var grid = new Gemma.AuditTrailGrid( {
			renderTo :'auditTrail',
			auditable :auditable
		});
	});
</script>
</security:authorize>

</head>


<h2>
	Details for: "
	<jsp:getProperty name="arrayDesign" property="name" />
	"
	<c:if test="${(not empty troubleEvent)}">
	&nbsp;
	<img src='<c:url value="/images/icons/stop.png"/>' height='16' width='16' alt='trouble'
			title='${ troubleEventDescription }' />
	</c:if>
	<c:if test="${ validatedEvent != null}">
	&nbsp;
	<img src='<c:url value="/images/icons/ok.png"/>' height='16' width='16' alt='validated'
			title='${ validatedEventDescription }' />
	</c:if>
</h2>

<div id="messages" style="margin: 10px; width: 400px"></div>

<!--  Summary of array design associations -->


<table class='datasummaryarea'>

	<c:if test="${(not empty summary)}">
		<caption>Sequence analysis details</caption>
		<tr>
			<td>

				<div id="arraySummary_${arrayDesign.id}">
					<table class='datasummary'>
						<tr>
							<td colspan="2" align="center"></td>
						</tr>
						<tr>
							<td>Probes</td>
							<td align="right">${numCompositeSequences}</td>
						</tr>
						<tr>
							<td>With seq.</td>
							<td align="right">${summary.numProbeSequences}</td>
						</tr>
						<tr>
							<td>With align</td>
							<td align="right">${summary.numProbeAlignments}</td>
						</tr>
						<tr>
							<td>Mapped to known genes</td>
							<td align="right">${summary.numProbesToKnownGenes}</td>
						</tr>
						<tr>
							<td>Unique genes</td>
							<td align="right">${summary.numGenes}</td>
						</tr>
						<tr>
							<td colspan="2" align='center' class='small'>(as of ${summary.dateCached})</td>
						</tr>
					</table>
				</div>

			</td>
		</tr>
		<tr>
			<script>
		var text = '<Gemma:help helpFile="sequenceAnalysisHelp.html"/>';
		function doit(event) {showWideHelpTip(event,text); }
		</script>
			<td colspan="2"><a class="helpLink" name="?" href="" onclick="doit(event);return false;"> <img
					src="/Gemma/images/help.png" />
			</a></td>
		</tr>
	</c:if>
	<tr>
		<td colspan="2"><security:authorize access="hasRole('GROUP_ADMIN')">
				<input type="button" value="Refresh report" onClick="updateArrayDesignReport(${arrayDesign.id })" />
			</security:authorize></td>
	</tr>

</table>



<table style="width: 70%">

	<tr>
		<td style="width: 25%" class="label">Short name</td>
		<td><jsp:getProperty name="arrayDesign" property="shortName" /></td>
	</tr>

	<tr>
		<td style="width: 25%" class="label">Alternate names</td>
		<td><span id="alternate-names">${alternateNames}</span> <security:authorize access="hasRole('GROUP_ADMIN')">&nbsp;
			<a href="#" title="Add a new alternate name for this design" onClick="getAlternateName(${arrayDesign.id })"><img
					src="/Gemma/images/icons/add.png" /> </a>
			</security:authorize></td>
	</tr>
	<tr>
		<td class="label">Species</td>
		<td><c:out value="${taxon}" /></td>
	</tr>
	<tr>
		<td class="label">Number of probes</td>
		<td><c:out value="${numCompositeSequences}" /> &nbsp; <a title="Show details of probes"
			href="/Gemma/arrays/showCompositeSequenceSummary.html?id=<jsp:getProperty name="arrayDesign" property="id" />"><img
				src="/Gemma/images/magnifier.png" /> </a></td>
	</tr>
	<tr>
		<td class="label">External accessions&nbsp; <a class="helpLink" href="?"
			onclick="showHelpTip(event, 'References to this design in other databases'); return false"><img
				src="/Gemma/images/help.png" /> </a>
		</td>
		<td><c:if test="${(not empty arrayDesign.externalReferences)}">
				<c:forEach var="accession" items="${ arrayDesign.externalReferences }">
					<Gemma:databaseEntry databaseEntry="${accession}" />
					<br />
				</c:forEach>
			</c:if></td>
	</tr>
	<tr>
		<td class="label">Experiments using this array</td>
		<td>${numExpressionExperiments} <a title="Show details of datasets"
			href="/Gemma/expressionExperiment/showAllExpressionExperiments.html?id=${expressionExperimentIds}"> <img
				src="/Gemma/images/magnifier.png" />
		</a>
		</td>
	</tr>
	<tr>
		<td class="label">Type&nbsp; <a class="helpLink" href="?"
			onclick="showHelpTip(event, 'Our best guess about what type of data is produced using this array'); return false"><img
				src="/Gemma/images/help.png" /> </a>
		</td>
		<td>${technologyType}</td>
	</tr>

	<tr>
		<td style="width: 25%" class="label">Description <a class="helpLink" href="?"
			onclick="showHelpTip(event, 'The description is usually provided by the original data source e.g. GEO.'); return false"><img
				src="/Gemma/images/help.png" /> </a>
		</td>
		<td><c:choose>

				<c:when test="${(not empty arrayDesign.description)}">
					<div class="clob">${arrayDesign.description}</div>
				</c:when>
				<c:otherwise>(none provided)</c:otherwise>
			</c:choose></td>
	</tr>
	<c:if test="${(not empty subsumees)}">
		<tr>
			<td class="label">Subsumes <a class="helpLink" href="?"
				onclick="showHelpTip(event, 'Array designs that this one \'covers\' -- it contains all the same sequences.'); return false"><img
					src="/Gemma/images/help.png" /> </a>
			</td>
			<td><Gemma:arrayDesignGrouping subsumees="${subsumees }" /></td>
		</tr>
	</c:if>
	<c:if test="${(not empty subsumer)}">
		<tr>
			<td class="label">Subsumed by <a class="helpLink" href="?"
				onclick="showHelpTip(event, 'Array design that \'covers\' this one. '); return false"><img
					src="/Gemma/images/help.png" /> </a>
			</td>
			<td><Gemma:arrayDesignGrouping subsumer="${subsumer }" /></td>
		</tr>
	</c:if>
	<c:if test="${(not empty mergees)}">
		<tr>
			<td class="label">Merger of <a class="helpLink" href="?"
				onclick="showHelpTip(event, 'Array designs that were merged to create this one.'); return false"><img
					src="/Gemma/images/help.png" /> </a>
			</td>
			<td><Gemma:arrayDesignGrouping subsumees="${mergees }" /></td>
		</tr>
	</c:if>
	<c:if test="${(not empty merger)}">
		<tr>
			<td class="label">Merged into <a class="helpLink" href="?"
				onclick="showHelpTip(event, 'Array design this one is merged into.'); return false"><img
					src="/Gemma/images/help.png" /> </a>
			</td>
			<td><Gemma:arrayDesignGrouping subsumer="${merger }" /></td>
		</tr>
	</c:if>

	<tr>
		<td class="label">Annotation file(s) <a class="helpLink" href="?"
			onclick="showHelpTip(event, 'Text-based (tab-delimited) annotation files for this array, if available. The files include GO terms as directly annotated (brief), including all parent terms (All parents) or biological process terms only.'); return false"><img
				src="/Gemma/images/help.png" /> </a>
		</td>
		<td><c:choose>
				<c:when
					test="${(empty noParentsAnnotationLink) && (empty allParentsAnnotationLink) && (empty bioProcessAnnotationLink)}">
					Unavailable&nbsp;&nbsp;
				</c:when>
				<c:otherwise>
					<c:choose>
						<c:when test="${empty noParentsAnnotationLink}">
							Unavailable
						</c:when>
						<c:otherwise>
							<a class="annotationLink" href="<c:url value="${noParentsAnnotationLink}" />">Brief</a>
						</c:otherwise>
					</c:choose>
					&nbsp;&nbsp;
					<c:choose>
						<c:when test="${empty allParentsAnnotationLink}">
							Unavailable
						</c:when>
						<c:otherwise>
							<a class="annotationLink" href="<c:url value="${allParentsAnnotationLink }" />">All parents</a>
						</c:otherwise>
					</c:choose>
					&nbsp;&nbsp;
					<c:choose>
						<c:when test="${empty bioProcessAnnotationLink}">
							Unavailable
						</c:when>
						<c:otherwise>
							<a class="annotationLink" href="<c:url value="${bioProcessAnnotationLink }" />">Biological Process only</a>
						</c:otherwise>
					</c:choose>
				</c:otherwise>

			</c:choose></td>
	</tr>
</table>


<security:authorize access="hasRole('GROUP_ADMIN')">
	<div id="auditTrail"></div>
	<input type="hidden" name="auditableId" id="auditableId" value="${arrayDesign.id}" />
	<input type="hidden" name="auditableClass" id="auditableClass" value="${arrayDesign.class.name}" />
</security:authorize>

