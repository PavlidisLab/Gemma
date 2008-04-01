<%@ include file="/common/taglibs.jsp"%>
<jsp:useBean id="expressionExperiment" scope="request"
	class="ubic.gemma.model.expression.experiment.ExpressionExperimentImpl" />
<head>
	<title>Expression experiment <%
	    if ( expressionExperiment.getName() != null ) {
	%>: <jsp:getProperty name="expressionExperiment" property="name" /> <%
     }
 %>
	</title>

	<script src="<c:url value='/scripts/ext/adapter/prototype/ext-prototype-adapter.js'/>" type="text/javascript"></script>
	<script src="<c:url value='/scripts/ext/ext-all.js'/>" type="text/javascript"></script>
	<script type="text/javascript" src="<c:url value='/scripts/ext/data/ListRangeReader.js'/>"></script>
	<script type="text/javascript" src="<c:url value='/scripts/ext/data/DwrProxy.js'/>"></script>
	<script type='text/javascript' src='/Gemma/dwr/engine.js'></script>
	<script type='text/javascript' src='/Gemma/dwr/util.js'></script>
	<script type='text/javascript' src='/Gemma/dwr/interface/ExpressionExperimentController.js'></script>
	<script type='text/javascript' src='/Gemma/dwr/interface/TaskCompletionController.js'></script>
	<script type='text/javascript' src='/Gemma/dwr/interface/ExpressionExperimentDataFetchController.js'></script>
	<script type='text/javascript' src='/Gemma/dwr/interface/ProgressStatusService.js'></script>
	<script type='text/javascript' src="<c:url value='/scripts/ajax/util/GemmaGridPanel.js'/>"></script>
	<script type='text/javascript' src="<c:url value='/scripts/ajax/annotation/AnnotationGrid.js'/>"></script>
	<script type="text/javascript" src="<c:url value='/scripts/progressbar.js'/>"></script>
	<script type='text/javascript' src="<c:url value='/scripts/ajax/expressionExperimentAnnots.js'/>"></script>
	<script type='text/javascript' src="<c:url value='/scripts/ajax/eeDesignMatrix.js'/>"></script>
	<script type="text/javascript" src="<c:url value='/scripts/ajax/eeDataFetch.js'/>"></script>
	<script type="text/javascript" src="<c:url value='/scripts/scriptaculous/effects.js'/>"></script>

	<authz:authorize ifAnyGranted="admin">
		<script type="text/javascript" src='/Gemma/dwr/interface/OntologyService.js'></script>
		<script type='text/javascript' src='/Gemma/dwr/interface/MgedOntologyService.js'></script>
		<script type='text/javascript' src='/Gemma/dwr/interface/CharacteristicBrowserController.js'></script>
		<script type='text/javascript' src="<c:url value='/scripts/ajax/annotation/CharacteristicCombo.js'/>"></script>
		<script type='text/javascript' src="<c:url value='/scripts/ajax/annotation/MGEDCombo.js'/>"></script>
		<script type='text/javascript' src="<c:url value='/scripts/ajax/annotation/AnnotationToolBar.js'/>"></script>
		<script type='text/javascript' src="<c:url value='/scripts/ajax/annotation/ExpressionExperimentToolBar.js'/>"></script>

		<script type='text/javascript' src='/Gemma/dwr/interface/AuditController.js'></script>
		<script type="text/javascript" src="<c:url value='/scripts/ajax/auditTrail.js'/>" type="text/javascript"></script>
	</authz:authorize>

	<link rel="stylesheet" type="text/css" media="all" href="<c:url value='/styles/progressbar.css'/>" />
</head>

<authz:authorize ifAnyGranted="admin">
	<input type="hidden" name="hasAdmin" id="hasAdmin" value="true" />
</authz:authorize>
<authz:authorize ifNotGranted="admin">
	<input type="hidden" name="hasAdmin" id="hasAdmin" value="" />
</authz:authorize>
<div id="messages" style="margin: 10px; width: 400px"></div>
<div id="taskId" style="display: none;"></div>
<div id="progress-area"></div>

<form style="float: right;" name="ExpresssionExperimentFilter" action="filterExpressionExperiments.html" method="POST">
	<a class="helpLink" href="?" onclick="showHelpTip(event, 'Search for another experiment'); return false"><img
			src="<c:url value="/images/help.png"/>" alt="help"> </a>
	<input type="text" name="filter" size="38" />
	<input type="submit" value="Find a dataset" />
</form>

<content tag="heading">
Experiment detail view for
<%
    if ( expressionExperiment.getName() != null ) {
%>
:
<jsp:getProperty name="expressionExperiment" property="name" />
-
<%
    }
%>
<jsp:getProperty name="expressionExperiment" property="shortName" />
&nbsp;
<a class="helpLink" href="?"
	onclick="showHelpTip(event, 'This page shows the details for a specific expression experiment; further details can be obtained by following the links on this page.'); return false"><img
		src="<c:url value="/images/help.png"/>" alt="help"> </a>
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
<c:if test="${ samplesRemoved != null}">
&nbsp;
<img src='<c:url value="/images/icons/exclamation.png"/>' height='16' width='16' alt='samplesRemoved'
		title='${ samplesRemovedDescription }' />
</c:if>
</content>


<table width="100%" cellspacing="3">
	<tr>
		<td class="label">
			<fmt:message key="expressionExperiment.name" />
		</td>
		<td>
			<%
			    if ( expressionExperiment.getName() != null ) {
			%>
			<jsp:getProperty name="expressionExperiment" property="name" />
			<%
			    } else {
			        out.print( "No name available" );
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
	<tr>
		<td class="label">
			<fmt:message key="expressionExperiment.owner" />
		</td>
		<td>
			<%
			    if ( expressionExperiment.getOwner() != null ) {
			%>
			<jsp:getProperty name="expressionExperiment" property="owner" />
			<%
			    } else {
			        out.print( "Public" );
			    }
			%>
		</td>
	</tr>
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
	<div id="eeAnnotations" class="x-grid-mso" style="border: 1px solid #c3daf9; overflow: hidden;"></div>
	<input type="hidden" name="eeId" id="eeId" value="${expressionExperiment.id}" />
	<input type="hidden" name="eeClass" id="eeClass" value="${expressionExperiment.class.name}" />
</div>

<%
    if ( expressionExperiment.getExperimentalDesign() != null ) {
%>
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

	<div id="eeDesignMatrix" class="x-grid-mso" style="border: 1px solid #c3daf9; overflow: hidden;"></div>
</div>

<%
    }
%>

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
			<Gemma:expressionQC ee="${expressionExperiment.id}" />
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

	<h3>
		History
	</h3>
	<div id="auditTrail" class="x-grid-mso" style="border: 1px solid #c3daf9; overflow: hidden; width: 650px;"></div>
	<input type="hidden" name="auditableId" id="auditableId" value="${expressionExperiment.id}" />
	<input type="hidden" name="auditableClass" id="auditableClass" value="${expressionExperiment.class.name}" />
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
				<input type="button"
					onclick=" if (confirmDelete('Expression experiment'))  {location.href='deleteExpressionExperiment.html?id=<%=request.getAttribute( "id" )%>';} else{ return false;}"
					value="Delete">
			</td>
			<td>

				<input type="button"
					onclick="location.href='/Gemma/expressionExperiment/generateExpressionExperimentLinkSummary.html?id=<%=request.getAttribute( "id" )%>'"
					value="Refresh Link Summary">
			</td>
		</authz:authorize>
	</tr>
</table>
