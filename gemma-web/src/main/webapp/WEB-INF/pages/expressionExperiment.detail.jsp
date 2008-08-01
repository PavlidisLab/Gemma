<%@ include file="/common/taglibs.jsp"%>
<jsp:useBean id="expressionExperiment" scope="request"
	class="ubic.gemma.model.expression.experiment.ExpressionExperimentImpl" />
<head>
	<title>${expressionExperiment.name}</title>

	<jwr:script src='/scripts/ajax/ext/data/DwrProxy.js'  />
	<jwr:script src='/scripts/app/eeDataFetch.js' />

	<authz:authorize ifAnyGranted="admin">
		<script type="text/javascript">
	Ext.namespace('Gemma');
	Ext.onReady(function() {
	var id = dwr.util.getValue("auditableId");
 	if (!id) { return; }
	var clazz = dwr.util.getValue("auditableClass");
	var auditable = {
		id : id,
		classDelegatingFor : clazz
	};
	var grid = new Gemma.AuditTrailGrid({
		renderTo : 'auditTrail',
		auditable : auditable
	});
});
</script>
	</authz:authorize>
	<script type="text/javascript">
	Ext.onReady(DesignMatrix.init, DesignMatrix);
	Ext.onReady(function() {
	var eeId = dwr.util.getValue("eeId");
	var eeClass = dwr.util.getValue("eeClass");
	var admin = dwr.util.getValue("hasAdmin");

	var grid = new Gemma.AnnotationGrid( { renderTo : "eeAnnotations",
				readMethod : ExpressionExperimentController.getAnnotation,
				readParams : [{
					id : eeId,
					classDelegatingFor : eeClass
				}],
				
				writeMethod : OntologyService.saveExpressionExperimentStatement,				
				
				removeMethod : OntologyService.removeExpressionExperimentStatement,
				
				editable : admin,
				mgedTermKey : "experiment",
				entId : eeId
			});
	
});			
	 </script>

</head>

<authz:authorize ifAnyGranted="admin">
	<input type="hidden" name="hasAdmin" id="hasAdmin" value="true" />
</authz:authorize>
<authz:authorize ifNotGranted="admin">
	<input type="hidden" name="hasAdmin" id="hasAdmin" value="" />
</authz:authorize>
<div id="messages" style="margin: 10px; width: 400px"></div>
<div id="progress-area"></div>

<form style="float: right;" name="ExpresssionExperimentFilter" action="filterExpressionExperiments.html" method="POST">
	<a class="helpLink" href="?" onclick="showHelpTip(event, 'Search for another experiment'); return false"><img
			src="<c:url value="/images/help.png"/>" alt="help"> </a>
	<input type="text" name="filter" size="38" />
	<input type="submit" value="Find a dataset" />
</form>

<content tag="heading">
${expressionExperiment.name} - ${expressionExperiment.shortName} &nbsp;

<c:if test="${ troubleEvent != null}">
&nbsp;
<img src='<c:url value="/images/icons/warning.png"/>' height='16' width='16' alt='trouble'
		title='${ troubleEventDescription}' />
</c:if>
<c:if test="${ validatedEvent != null}">
&nbsp;
<img src='<c:url value="/images/icons/ok.png"/>' height='16' width='16' alt='validated'
		title='${ validatedEventDescription }' />
</c:if>
<c:if test="${ samplesRemoved != null}">
&nbsp;
<img src='<c:url value="/images/icons/exclamation.png"/>' height='16' width='16' alt='samplesRemoved'
		title='${ samplesRemovedDescription }' />
</c:if>
</content>


<table width="100%" cellspacing="3">
	<tr>
		<td class="label">
			<fmt:message key="expressionExperiment.description" />
		</td>
		<td>
			<div class="clob" style="width: 40%;">
				${expressionExperiment.description }
			</div>
		</td>
	</tr>
	<%--<tr>
	
		<td class="label">
			<fmt:message key="expressionExperiment.source" />
		</td>
		<td>
			<%
			    if ( expressionExperiment.getSource() != null ) {
			%>
			<jsp:getProperty name="expressionExperiment" property="source" />
			<%
			    } else {
			        out.print( "Source unavailable" );
			    }
			%>
		</td>
	</tr>
	--%>
	<authz:authorize ifAnyGranted="admin">
		<tr>
			<td class="label">
				Security
			</td>
			<td>
				<c:choose>
					<c:when test='${isPrivate}'>
						<img src="/Gemma/images/icons/lock.png" />
					</c:when>
					<c:otherwise>
						<%
						    out.print( "Public" );
						%>
					</c:otherwise>
				</c:choose>
			</td>
		</tr>
	</authz:authorize>

	<tr>
		<td class="label">
			<fmt:message key="investigators.title" />
		</td>
		<td>
			<%
			    if ( ( expressionExperiment.getInvestigators() ) != null
			            && ( expressionExperiment.getInvestigators().size() > 0 ) ) {
			%>
			<c:forEach end="0" var="investigator" items="${ expressionExperiment.investigators }">
				<c:out value="${ investigator.name}" />
			</c:forEach>
			<%
			    if ( expressionExperiment.getInvestigators().size() > 1 ) {
			            out.print( ", et al. " );
			        }
			    } else {
			        out.print( "No investigators known" );
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
	<authz:authorize ifAllGranted="admin">
		<tr>
			<td class="label">
				<fmt:message key="auditTrail.date" />
			</td>
			<td>
				<%
				    if ( expressionExperiment.getAuditTrail() != null ) {
				        out.print( expressionExperiment.getAuditTrail().getCreationEvent().getDate() );
				    } else {
				        out.print( "Create date unavailable" );
				    }
				%>
			</td>
		</tr>
	</authz:authorize>
	<tr>
		<td class="label">
			Profiles
		</td>
		<td>
			${designElementDataVectorCount}
			<a class="helpLink" href="?"
				onclick="showHelpTip(event, 
				'The total number of expression profiles for this experiment, combined across all quantitation types.'); return false">
				<img src="/Gemma/images/help.png" /> </a>
		</td>
	</tr>
	<tr>
		<td class="label">
			<fmt:message key="bioAssays.title" />
		</td>
		<td>
			<%
			    out.print( expressionExperiment.getBioAssays().size() );
			%>
			<a
				href="/Gemma/expressionExperiment/showBioAssaysFromExpressionExperiment.html?id=<%out.print(expressionExperiment.getId());%>">
				<img src="/Gemma/images/magnifier.png" /> </a>
		</td>

	</tr>
	<tr>
		<td class="label">
			<fmt:message key="arrayDesigns.title" />
		</td>
		<td>
			<c:forEach var="arrayDesign" items="${ arrayDesigns }">
				<c:out value="${ arrayDesign.name}" />
				<a href="/Gemma/arrays/showArrayDesign.html?id=<c:out value="${ arrayDesign.id}" />"> <img
						src="/Gemma/images/magnifier.png" /> </a>
				<br />
			</c:forEach>

		</td>
	</tr>

	<tr>
		<td class="label">
			Data File
		</td>
		<td>
			<a href="#" onClick="fetchData(true, ${expressionExperiment.id }, 'text', null, null)">Click to start download</a>

			<a class="helpLink" href="?"
				onclick="showHelpTip(event, 'Tab-delimited data file for this experiment, if available.'); return false"><img
					src="/Gemma/images/help.png" /> </a>
		</td>
	</tr>

	<authz:authorize ifAllGranted="admin">
		<tr>
			<td class="label">
				Coexpression Links
			</td>
			<td>
				<c:if test="${ eeCoexpressionLinks != null}">
				${eeCoexpressionLinks}
				</c:if>
				<c:if test="${ eeCoexpressionLinks == null}">
					(count not available)
				</c:if>
			</td>
		</tr>
	</authz:authorize>

</table>

<h3>
	Annotations&nbsp;
	<a class="helpLink" href="?" onclick="showHelpTip(event, 'Terms describing the experiment, if any'); return false"><img
			src="/Gemma/images/help.png" /> </a>
</h3>
<div style="padding: 2px;" onclick="Effect.toggle('annots', 'blind', {duration:0.1})">
	<img src="/Gemma/images/plus.gif" />
	<br />
</div>
<div id="annots">
	<div id="eeAnnotations"></div>
	<input type="hidden" name="eeId" id="eeId" value="${expressionExperiment.id}" />
	<input type="hidden" name="eeClass" id="eeClass" value="${expressionExperiment.class.name}" />
</div>

<h3>
	<fmt:message key="experimentalDesign.title" />
	&nbsp;
	<a
		href="/Gemma/experimentalDesign/showExperimentalDesign.html?id=<%out.print(expressionExperiment.getExperimentalDesign().getId());%> ">
		<%
		    if ( expressionExperiment.getExperimentalDesign().getName() != null ) {
		        out.print( expressionExperiment.getExperimentalDesign().getName() );
		    }
		%> <img src="/Gemma/images/magnifier.png" /> </a>
</h3>
<div style="padding: 2px;" onclick="Effect.toggle('design', 'blind', {duration:0.1})">
	<img src="/Gemma/images/plus.gif" />
	<br />
</div>
<div id="design">
	<Gemma:eeDesign experimentalDesign="${expressionExperiment.experimentalDesign}"
		expressionExperiment="${expressionExperiment}"></Gemma:eeDesign>

	<div id="eeDesignMatrix"></div>
</div>

<h3>
	Quantitation Types
	<a class="helpLink" href="?"
		onclick="showHelpTip(event, 
				'Quantitation types are the different measurements available for this experiment.'); return false">
		<img src="/Gemma/images/help.png" /> </a> (${qtCount} items)
</h3>
<div style="padding: 2px;" onclick="Effect.toggle('qts', 'blind', {duration:0.1})">
	<img src="/Gemma/images/plus.gif" />
	<br />
</div>
<div id="qts" style="display: none">
	<div>
		<%-- inner div needed for effect  --%>
		<display:table name="quantitationTypes" class="scrollTable"
			requestURI="/Gemma/expressionExperiment/showExpressionExperiment.html" id="dataVectorList" pagesize="100"
			decorator="ubic.gemma.web.taglib.displaytag.quantitationType.QuantitationTypeWrapper">
			<display:column property="data" sortable="false" title="Get data" />
			<display:column property="qtName" sortable="true" maxWords="20" titleKey="name" />
			<display:column property="description" sortable="true" maxLength="20" titleKey="description" />
			<display:column property="qtPreferredStatus" sortable="true" maxWords="20" titleKey="quantitationType.preferred" />
			<display:column property="qtRatioStatus" sortable="true" maxWords="20" titleKey="quantitationType.ratio" />
			<display:column property="qtBackground" sortable="true" maxWords="20" titleKey="quantitationType.background" />
			<display:column property="qtBackgroundSubtracted" sortable="true" maxWords="20"
				titleKey="quantitationType.backgroundSubtracted" />
			<display:column property="qtNormalized" sortable="true" maxWords="20" titleKey="quantitationType.normalized" />
			<display:column property="generalType" sortable="true" />
			<display:column property="type" sortable="true" />
			<display:column property="representation" sortable="true" title="Repr." />
			<display:column property="scale" sortable="true" />
			<display:setProperty name="basic.empty.showtable" value="false" />
		</display:table>
	</div>
</div>

<authz:authorize ifAnyGranted="admin">
	<h3>
		Quality Control information
	</h3>
	<div style="padding: 2px;" onclick="Effect.toggle('qc', 'blind', {duration:0.1})">
		<img src="/Gemma/images/plus.gif" />
		<br />
	</div>
	<div id="qc" style="display: none">
		<div>
			<%-- inner div needed for effect  --%>
			<Gemma:expressionQC ee="${eeId}" />
		</div>
	</div>
	<h3>
		Biomaterials and Assays
		<a
			href="/Gemma/expressionExperiment/showBioMaterialsFromExpressionExperiment.html?id=<%=request.getAttribute( "id" )%>">(list
			samples)</a>
	</h3>
	<div style="padding: 2px;" onclick="Effect.toggle('bms', 'blind', {duration:0.1})">
		<img src="/Gemma/images/plus.gif" />
		<br />
	</div>
	<div id="bms" style="display: none">
		<div>
			<%-- inner div needed for effect  --%>
			<Gemma:assayView expressionExperiment="${expressionExperiment}" />
		</div>
	</div>


	<div id="auditTrail"></div>
	<input type="hidden" name="auditableId" id="auditableId" value="${eeId}" />
	<input type="hidden" name="auditableClass" id="auditableClass" value="${eeClass}" />
	<c:if test="${ lastArrayDesignUpdate != null}">
		<p>
			The last time an array design associated with this experiment was updated: ${lastArrayDesignUpdate.date}
		</p>
	</c:if>
</authz:authorize>

<table>
	<tr>
		<td>
			<input type="button"
				onclick="location.href='expressionExperimentVisualization.html?id=<%=request.getAttribute( "id" )%>'"
				value="Visualize">
		</td>
		<authz:authorize ifAnyGranted="admin">
			<td>
				<input type="button" onclick="location.href='editExpressionExperiment.html?id=<%=request.getAttribute( "id" )%>'"
					value="Edit">
			</td>
			<td>
				<input type="button" onclick="deleteExperiment(<%=request.getAttribute( "id" )%>);" value="Delete">
			</td>
			<td>

				<input type="button" onclick="updateEEReport(<%=request.getAttribute( "id" )%>);" value="Refresh Link Summary">
			</td>
		</authz:authorize>
	</tr>
</table>
