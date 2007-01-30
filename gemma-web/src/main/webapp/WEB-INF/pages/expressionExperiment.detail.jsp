<jsp:directive.page import="org.apache.commons.lang.StringUtils" />
<%@ include file="/common/taglibs.jsp"%>
<jsp:useBean id="expressionExperiment" scope="request"
	class="ubic.gemma.model.expression.experiment.ExpressionExperimentImpl" />
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">

<title>Dataset details <%
if ( expressionExperiment.getName() != null ) {
%>: <jsp:getProperty name="expressionExperiment" property="name" /> <%
 }
 %>
</title>
<h2>
	<fmt:message key="expressionExperiment.details" />
</h2>
<a class="helpLink" href="?"
	onclick="showHelpTip(event, 'This page shows the details for a specific expression experiment; further details can be obtained by following the links on this page.'); return false">Help</a>


<table width="100%" cellspacing="10">
	<tr>
		<td class="label" >
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
			<div class="clob">
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
	<tr><td class="label">
			Profiles
		</td>
		<td>${designElementDataVectorCount} <a class="helpLink" href="?"
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
			%> <a
				href="/Gemma/expressionExperiment/showBioAssaysFromExpressionExperiment.html?id=<%out.print(expressionExperiment.getId());%>">
				<img src="/Gemma/images/magnifier.png" /></a> 
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
					<img src="/Gemma/images/magnifier.png" /></a>
				<br />
			</c:forEach>

		</td>
	</tr>
	<authz:authorize ifAllGranted="admin">
		<tr>
			<td class="label">
				Coexpression Links
			<td>
				${eeCoexpressionLinks}
			</td>
		</tr>
	</authz:authorize>
</table>

<authz:authorize ifAnyGranted="admin">
	<%
	if ( expressionExperiment.getExperimentalDesign() != null ) {
	%>
	<h3>
		<fmt:message key="experimentalDesign.title" />
		:
		<a
			href="/Gemma/experimentalDesign/showExperimentalDesign.html?id=<%out.print(expressionExperiment.getExperimentalDesign().getId());%> ">
			<%
			out.print( expressionExperiment.getExperimentalDesign().getName() );
			%> (view) </a>
	</h3>
	<p>
		<b>Description:</b>
		<%
		                            out.print( StringUtils.abbreviate( expressionExperiment.getExperimentalDesign().getDescription(),
		                            100 ) );
		%>
		<BR />
		<BR />
		This experimental design has
		<%
		out.print( expressionExperiment.getExperimentalDesign().getExperimentalFactors().size() );
		%>
		experimental factors.
	</p>
	<%
	} else {
	%>
			No experimental design information for this experiment.
			<%
	}
	%>

	<%
	if ( expressionExperiment.getAnalyses().size() > 0 ) {
	%>
	<h3>
		<fmt:message key="analyses.title" />
	</h3>
	<display:table name="expressionExperiment.analyses" class="list"
		requestURI="" id="analysisList" pagesize="10"
		decorator="ubic.gemma.web.taglib.displaytag.expression.experiment.ExpressionExperimentWrapper">
		<display:column property="name" sortable="true" maxWords="20"
			href="/Gemma/experimentalDesign/showExperimentalDesign.html"
			paramId="name" paramProperty="name" />
		<display:column property="description" sortable="true" maxWords="100" />
		<display:setProperty name="basic.empty.showtable" value="false" />
	</display:table>
	<%
	}
	%>
	<%
	if ( expressionExperiment.getSubsets().size() > 0 ) {
	%>
	<script type="text/javascript" src="<c:url value="/scripts/aa.js"/>"></script>
	<h3>
		<fmt:message key="expressionExperimentSubsets.title" />
	</h3>
	<aazone tableId="subsetList" zone="subsetTable" />
	<aa:zone name="subsetTable">
		<display:table name="expressionExperiment.subsets" class="list"
			requestURI="/Gemma/expressionExperiment/showExpressionExperiment.html"
			id="subsetList" pagesize="10"
			decorator="ubic.gemma.web.taglib.displaytag.expression.experiment.ExpressionExperimentSubSetWrapper">
			<display:column property="nameLink" sortable="true" maxWords="20"
				titleKey="expressionExperimentSubsets.name" />
			<display:column property="description" sortable="true" maxWords="100" />
			<display:setProperty name="basic.empty.showtable" value="false" />
		</display:table>
	</aa:zone>
	<%
	}
	%>
</authz:authorize>

<h3>Quantitation Types <a class="helpLink" href="?"
					onclick="showHelpTip(event, 
				'Quantitation types are the different measurements available for this experiment.'); return false">
					<img src="/Gemma/images/help.png" /> </a>
</h3>
<div id="tableContainer" class="tableContainer">
	<display:table name="quantitationTypes" class="scrollTable"
		requestURI="/Gemma/expressionExperiment/showExpressionExperiment.html"
		id="dataVectorList" pagesize="30"
		decorator="ubic.gemma.web.taglib.displaytag.quantitationType.QuantitationTypeWrapper">
		<display:column property="qtName" sortable="true" maxWords="20"
			titleKey="name" />
		<display:column property="description" sortable="true" maxLength="20"
			titleKey="description" />
		<display:column property="qtPreferredStatus" sortable="true"
			maxWords="20" titleKey="quantitationType.preferred" />
		<display:column property="qtBackground" sortable="true" maxWords="20"
			titleKey="quantitationType.background" />
		<display:column property="qtBackgroundSubtracted" sortable="true"
			maxWords="20" titleKey="quantitationType.backgroundSubtracted" />
		<display:column property="qtNormalized" sortable="true" maxWords="20"
			titleKey="quantitationType.normalized" />
		<display:column property="generalType" sortable="true" />
		<display:column property="type" sortable="true" />
		<display:column property="representation" sortable="true" title="Repr." />
		<display:column property="scale" sortable="true" />
		<display:setProperty name="basic.empty.showtable" value="false" />
	</display:table>
</div>
<authz:authorize ifAnyGranted="admin">
	<h3>
		Biomaterials and Assays
	</h3>
	<Gemma:assayView expressionExperiment="${expressionExperiment}"></Gemma:assayView>
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

		<authz:acl domainObject="${expressionExperiment}" hasPermission="1,6">
			<td COLSPAN="2">
				<div align="left">
					<input type="button"
						onclick="location.href='editExpressionExperiment.html?id=<%=request.getAttribute( "id" )%>'"
						value="Edit">
				</div>
			</td>
		</authz:acl>

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
<script type="text/javascript"
	src="<c:url value="/scripts/aa-init.js"/>"></script>
