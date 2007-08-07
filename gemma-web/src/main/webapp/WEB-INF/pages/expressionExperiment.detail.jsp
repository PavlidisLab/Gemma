<jsp:directive.page import="org.apache.commons.lang.StringUtils" />
<%@ include file="/common/taglibs.jsp"%>
<jsp:useBean id="expressionExperiment" scope="request"
	class="ubic.gemma.model.expression.experiment.ExpressionExperimentImpl" />
<head>
<title>
	Expression experiment <%
	if ( expressionExperiment.getName() != null ) {
	%>: <jsp:getProperty name="expressionExperiment" property="name" /> <%
 }
 %>
 </title>
 
	<script src="<c:url value='/scripts/ext/adapter/prototype/ext-prototype-adapter.js'/>" type="text/javascript"></script>
	<script src="<c:url value='/scripts/ext/ext-all.js'/>" type="text/javascript"></script>
	<script type="text/javascript" src="<c:url value='/scripts/ext/data/ListRangeReader.js'/>"></script>
	<script type="text/javascript" src="<c:url value='/scripts/ext/data/DwrProxy.js'/>"></script>
	<script type='text/javascript' src='/Gemma/dwr/interface/AuditController.js'></script>
	<script type='text/javascript' src='/Gemma/dwr/engine.js'></script>
	<script type='text/javascript' src='/Gemma/dwr/util.js'></script>

</head>

<form style="float: right;" name="ExpresssionExperimentFilter"
	action="filterExpressionExperiments.html" method="POST">
	<a class="helpLink" href="?"
		onclick="showHelpTip(event, 'Search for another experiment'); return false"><img
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
</content>



<table width="100%" cellspacing="10">
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
			<div class="clob" style="width:40%;">
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
			<c:forEach end="0" var="investigator"
				items="${ expressionExperiment.investigators }">
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
			<Gemma:databaseEntry
				databaseEntry="${expressionExperiment.accession}" />
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
			<Gemma:citation
				citation="${expressionExperiment.primaryPublication }" />
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
				<a
					href="/Gemma/arrays/showArrayDesign.html?id=<c:out value="${ arrayDesign.id}" />">
					<img src="/Gemma/images/magnifier.png" /> </a>
				<br />
			</c:forEach>

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

<authz:authorize ifAnyGranted="admin">

<!-- This is for the EE annotator  -->
		<div id="eeAnnotator" class="x-grid-mso" style="border: 1px solid #c3daf9; overflow: hidden; width:650px; height:30px;"></div>
		  <script type="text/javascript" src="<c:url value='/scripts/ajax/eeAnnotator.js'/>" type="text/javascript"></script>
		  <script type="text/javascript" src='/Gemma/dwr/interface/OntologyService.js'></script>
		  <script type='text/javascript' src='/Gemma/dwr/interface/MgedOntologyService.js'></script>	
	
</authz:authorize>

<div id="annotationTableContainer" class="tableContainer">
	<h3>
		Annotation
	</h3>
	<c:choose>
		<c:when test="${empty expressionExperiment.characteristics}">
			<p>
				No annotation found.
			</p>
		</c:when>
		<c:otherwise>
			<display:table name="characteristics" class="scrollTable"
			 requestURI="/Gemma/expressionExperiment/showExpressionExperiment.html"
			 id="annotationTable" pagesize="5"
			 decorator="ubic.gemma.web.taglib.displaytag.common.description.CharacteristicWrapper">
			<display:column property="descriptionString" sortable="false" title="" />
			</display:table>
		</c:otherwise>
	</c:choose>
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
			out.print( expressionExperiment.getExperimentalDesign().getName() );
			%>
			<img src="/Gemma/images/magnifier.png" />
		</a>
	</h3>

	<Gemma:eeDesign
		experimentalDesign="${expressionExperiment.experimentalDesign}"></Gemma:eeDesign>
	<Gemma:eeDesignMatrix
		expressionExperiment="${expressionExperiment}"></Gemma:eeDesignMatrix>
	<%
	}
	%>
	
	<%--
	<authz:authorize ifAnyGranted="admin">
	<%
	if ( expressionExperiment.getSubsets().size() > 0 ) {
	%>
	<div style="margin:0px 0px 0px 20px;><script type="text/javascript" src="<c:url value="/scripts/aa.js"/>"></script>
	<h3>
		<fmt:message key="expressionExperimentSubsets.title" />&nbsp;<a class="helpLink" href="?"
	onclick="showHelpTip(event, 'Subsets are much like experimental design factors...'); return false"><img
		src="<c:url value="/images/help.png"/>" alt="help"> </a>
	</h3>
	<aazone tableId="subsetList" zone="subsetTable" />
	<aa:zone name="subsetTable">
		<display:table name="expressionExperiment.subsets" class="list" defaultsort="1" 
			requestURI="/Gemma/expressionExperiment/showExpressionExperiment.html"
			id="subsetList" pagesize="10" 
			decorator="ubic.gemma.web.taglib.displaytag.expression.experiment.ExpressionExperimentSubSetWrapper">
			<display:column defaultorder="ascending"  title="Factor" property="description" sortable="true" maxWords="100" />
			<display:column title="Value" property="name" sortable="true" maxWords="20"
				titleKey="expressionExperimentSubsets.name" />
			<display:column sortable="true" title="Assays" property="bioAssaySize" />
			<display:setProperty name="basic.empty.showtable" value="false" />
		</display:table>
	</aa:zone>
	</div>
	<%
	}
	%>
	</authz:authorize>
	--%>

<h3>
	Quantitation Types
	<a class="helpLink" href="?"
		onclick="showHelpTip(event, 
				'Quantitation types are the different measurements available for this experiment.'); return false">
		<img src="/Gemma/images/help.png" /> </a>
</h3>
<div id="tableContainer" class="tableContainer">
	<display:table name="quantitationTypes" class="scrollTable"
		requestURI="/Gemma/expressionExperiment/showExpressionExperiment.html"
		id="dataVectorList" pagesize="30"
		decorator="ubic.gemma.web.taglib.displaytag.quantitationType.QuantitationTypeWrapper">
		<%-- <display:column property="data" sortable="false" title="Get data" /> --%>
		<display:column property="qtName" sortable="true" maxWords="20"
			titleKey="name" />
		<display:column property="description" sortable="true" maxLength="20"
			titleKey="description" />
		<display:column property="qtPreferredStatus" sortable="true"
			maxWords="20" titleKey="quantitationType.preferred" />
		<display:column property="qtRatioStatus" sortable="true" maxWords="20"
			titleKey="quantitationType.ratio" />
		<display:column property="qtBackground" sortable="true" maxWords="20"
			titleKey="quantitationType.background" />
		<display:column property="qtBackgroundSubtracted" sortable="true"
			maxWords="20" titleKey="quantitationType.backgroundSubtracted" />
		<display:column property="qtNormalized" sortable="true" maxWords="20"
			titleKey="quantitationType.normalized" />
		<display:column property="generalType" sortable="true" />
		<display:column property="type" sortable="true" />
		<display:column property="representation" sortable="true"
			title="Repr." />
		<display:column property="scale" sortable="true" />
		<display:setProperty name="basic.empty.showtable" value="false" />
	</display:table>
</div>
<authz:authorize ifAnyGranted="admin">
	<h3>
		Biomaterials and Assays <a href="/Gemma/expressionExperiment/showBioMaterialsFromExpressionExperiment.html?id=<%=request.getAttribute( "id" )%>">(list samples)</a>
	</h3>
	<Gemma:assayView expressionExperiment="${expressionExperiment}"></Gemma:assayView>
</authz:authorize>


<authz:authorize ifAnyGranted="admin">
	<!-- the import of auditTrail.js has to happen here or non-admin users
	     will see an error because auditableId isn't defined; -->
    <script type="text/javascript" src="<c:url value='/scripts/ajax/auditTrail.js'/>" type="text/javascript"></script>
	<h3>History</h3>
	<div id="auditTrail" class="x-grid-mso" style="border: 1px solid #c3daf9; overflow: hidden; width:630px; height:250px;"></div>
	<input type="hidden" name="auditableId" id="auditableId" value="${expressionExperiment.id}" />
	<input type="hidden" name="auditableClass" id="auditableClass" value="${expressionExperiment.class.name}" />
</authz:authorize>


<table>
	<tr>
		<td COLSPAN="2">
			<div align="left">
				<input type="button"
					onclick="location.href='showAllExpressionExperiments.html'"
					value="Back">
			</div>
		</td>

		<td COLSPAN="2">
			<div align="left">
				<input type="button"
					onclick="location.href='expressionExperimentVisualization.html?id=<%=request.getAttribute( "id" )%>'"
					value="Visual">
			</div>
		</td>

		<authz:authorize ifAnyGranted="admin">
			<td COLSPAN="2">
				<div align="left">
					<input type="button"
						onclick="location.href='editExpressionExperiment.html?id=<%=request.getAttribute( "id" )%>'"
						value="Edit">
				</div>
			</td>
		</authz:authorize>

		<authz:authorize ifAnyGranted="admin">
			<td COLSPAN="2">
				<div align="left">
					<input type="button"
						onclick=" if (confirmDelete('Expression experiment'))  {location.href='deleteExpressionExperiment.html?id=<%=request.getAttribute( "id" )%>';} else{ return false;}"
						value="Delete">
				</div>


			</td>
			<td COLSPAN="2">
				<div align="left">
					<input type="button"
						onclick="location.href='/Gemma/expressionExperiment/generateExpressionExperimentLinkSummary.html?id=<%=request.getAttribute( "id" )%>'"
						value="Refresh Link Summary">
				</div>
			</td>
		</authz:authorize>
	</tr>
</table>