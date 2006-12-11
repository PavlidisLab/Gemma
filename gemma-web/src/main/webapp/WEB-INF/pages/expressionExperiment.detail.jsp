<jsp:directive.page import="org.apache.commons.lang.StringUtils" />
<%@ include file="/common/taglibs.jsp"%>
<jsp:useBean id="expressionExperiment" scope="request"
	class="ubic.gemma.model.expression.experiment.ExpressionExperimentImpl" />
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">

<title>Dataset details <%
					if ( expressionExperiment.getName() != null ) {
					%>: <jsp:getProperty name="expressionExperiment" property="name" />
					<%
					                } 
					%></title>
		<h2>
			<fmt:message key="expressionExperiment.details" />
		</h2>
		<a class="helpLink" href="?"
			onclick="showHelpTip(event, 'This page shows the details for a specific expression experiment; further details can be obtained by following the links on this page.'); return false">Help</a>


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
                    <div class="clob"><jsp:getProperty name="expressionExperiment"
							property="description" /></div>
				 
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
						<br />
					</c:forEach>
					<%
					                    if ( expressionExperiment.getInvestigators().size() > 1 ) {
					                    out.print( " et al. " );
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
					<%
					                if ( expressionExperiment.getAccession() != null ) {
					                if ( expressionExperiment.getAccession().getExternalDatabase().getName().equalsIgnoreCase( "GEO" ) ) {
					                    out.print( expressionExperiment.getAccession().getAccession()
					                            + "<a target='_blank' href='http://www.ncbi.nlm.nih.gov/geo/query/acc.cgi?acc="
					                            + expressionExperiment.getAccession().getAccession() + "'>(GEO)</a>" );
					                } else {
					                    out.print( expressionExperiment.getAccession().getAccession() );
					                }
					            } else {
					                out.print( "Accession unavailable" );
					            }
					%>
				</td>
			</tr>
			<tr>
				<td class="label">
					<fmt:message key="pubMed.publication" />
				</td>
				<td>
					<%
					                if ( expressionExperiment.getPrimaryPublication() != null ) {
					                out.print( expressionExperiment.getPrimaryPublication().getCitation() );
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
					<fmt:message key="bioAssays.title" />
				<td>
					<%
					out.print( expressionExperiment.getBioAssays().size() );
					%>
					(
					<a
						href="/Gemma/expressionExperiment/showBioAssaysFromExpressionExperiment.html?id=<%out.print(expressionExperiment.getId());%>">
						Click for details</a> )
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
							(link) </a>
						<br />
					</c:forEach>

				</td>
			</tr>
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
					%> </a>
			</h3>
			<p>
				<b>Description:</b>
				<%
				                        out
				                        .print( StringUtils.abbreviate( expressionExperiment.getExperimentalDesign().getDescription(),
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
			}
			else {
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
				<display:column property="description" sortable="true"
					maxWords="100" />
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
					<display:column property="description" sortable="true"
						maxWords="100" />
					<display:setProperty name="basic.empty.showtable" value="false" />
				</display:table>
			</aa:zone>
			<%
			}
			%>
		</authz:authorize>

		<h3>
			<fmt:message key="designElementDataVectors.title" />
		</h3>
		<p>
			There are
			<b> <c:out value="${designElementDataVectorCount}" /> </b>
			expression profiles for this experiment.
			<BR />
			<BR />
			<b>Details by quantitation type:</b>
		</p>
		<aazone tableId="dataVectorList" zone="dataVectorTable" />
		<aa:zone name="dataVectorTable">
			<display:table name="qtCountSet" class="list"
				requestURI="/Gemma/expressionExperiment/showExpressionExperiment.html"
				id="dataVectorList" pagesize="10"
				decorator="ubic.gemma.web.taglib.displaytag.expression.experiment.ExpressionExperimentWrapper">
				<display:column property="qtName" sortable="true" maxWords="20"
					titleKey="quantitationType.name" />
				<display:column property="qtValue" sortable="true" maxWords="20"
					titleKey="quantitationType.countVectors" />
				<display:setProperty name="basic.empty.showtable" value="false" />
			</display:table>
		</aa:zone>

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

				<authz:acl domainObject="${expressionExperiment}"
					hasPermission="1,6">
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
				</authz:authorize>
			</tr>
		</table>
		<script type="text/javascript"
			src="<c:url value="/scripts/aa-init.js"/>"></script>
