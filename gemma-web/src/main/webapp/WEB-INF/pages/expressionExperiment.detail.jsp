<%@ include file="/common/taglibs.jsp"%>
<jsp:useBean id="expressionExperiment" scope="request"
	class="ubic.gemma.model.expression.experiment.ExpressionExperimentImpl" />
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">

<html>
	<body>
		<h2>
			<fmt:message key="expressionExperiment.details" />
		</h2>
		<table width="100%" cellspacing="10">
			<tr>
				<td valign="top">
					<b> <fmt:message key="expressionExperiment.name" /> </b>
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
				<td valign="top">
					<b> <fmt:message key="expressionExperiment.description" /> </b>
				</td>
				<td>
					<%
					if ( expressionExperiment.getDescription() != null ) {
					%>
					<textarea name="" rows=5 cols=60 readonly=true><jsp:getProperty name="expressionExperiment" property="description" /></textarea>
					<%
					                } else {
					                out.print( "Description unavailable" );
					            }
					%>
				</td>
			</tr>
			<tr>
				<td valign="top">
					<b> <fmt:message key="expressionExperiment.source" /> </b>
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
				<td valign="top">
					<b> <fmt:message key="expressionExperiment.owner" /> </b>
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
				<td valign="top">
					<b> <fmt:message key="databaseEntry.title" /> </b>
				</td>
				<td>
					<%
					            if ( expressionExperiment.getAccession() != null ) {
					                out.print(expressionExperiment.getAccession().getAccession());
					            } else {
					                out.print( "Accession unavailable" );
					            }
					%>
				</td>
			</tr>
			<tr>
				<td valign="top">
					<b> <fmt:message key="pubMed.publication" /> </b>
				</td>
				<td>
					<%
					if ( expressionExperiment.getPrimaryPublication() != null ) {
					%>
					<jsp:getProperty name="expressionExperiment" property="primaryPublication" />
					<%
					                } else {
					                out.print( "Primary publication unavailable" );
					            }
					%>
				</td>
			</tr>
			<authz:authorize ifAllGranted="admin">
				<tr>
					<td valign="top">
						<b> <fmt:message key="auditTrail.date" /> </b>
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
		</table>

		<br />

		<h3>
			<fmt:message key="bioAssays.title" />
		</h3>
		There are
		<a
			href="/Gemma/expressionExperiment/showBioAssaysFromExpressionExperiment.html?id=<%out.print(expressionExperiment.getId());%> ">
			<%
			out.print( expressionExperiment.getBioAssays().size() );
			%> </a> bioAssays for this expression experiment, with the following designs.
		<br />
		<display:table name="arrayDesigns" class="list" requestURI="" id="arrayList" export="true"
			pagesize="10">
			<display:column property="name" sortable="true" maxWords="20"
				href="/Gemma/arrays/showArrayDesign.html" paramId="id" paramProperty="id" />
			<display:setProperty name="basic.empty.showtable" value="false" />
		</display:table>			
		<br />
		
        <h3>
            <fmt:message key="experimentalDesign.title" />
        </h3>
        <a 
        	href="/Gemma/experimentalDesign/showExperimentalDesign.html?id=<%out.print(expressionExperiment.getExperimentalDesign().getId());%> ">
        <%out.print(expressionExperiment.getExperimentalDesign().getName());%>
        </a>
        <br />
        <br />
        <b>Description:</b>
        <br />
        <textarea name="" rows=5 cols=60 readonly=true><%out.print(expressionExperiment.getExperimentalDesign().getDescription());%></textarea>
        <br />
        <br />
        This experimental design has 
        <%out.print(expressionExperiment.getExperimentalDesign().getExperimentalFactors().size()); %>
        experimental factors.
        <br />

		<h3>
			<fmt:message key="investigators.title" />
		</h3>
		<display:table name="expressionExperiment.investigators" class="list" requestURI="" id="contactList" export="true"
			pagesize="10" decorator="ubic.gemma.web.taglib.displaytag.expression.experiment.ExpressionExperimentWrapper">
			<display:column property="name" sortable="true" maxWords="20"
				href="/Gemma/experimentalDesign/showExperimentalDesign.html" paramId="name" paramProperty="name" />
			<display:column property="phone" sortable="true" maxWords="100" />
			<display:column property="fax" sortable="true" maxWords="100" />
			<display:column property="email" sortable="true" maxWords="100" />
			<display:setProperty name="basic.empty.showtable" value="false" />
			<display:setProperty name="basic.msg.empty_list" value="No investigators are associated with this experiment." /> 
		</display:table>

		<%
		if ( expressionExperiment.getAnalyses().size() > 0 ) {
		%>
		<h3>
			<fmt:message key="analyses.title" />
		</h3>
		<% 
		} 
		%>
		
		<display:table name="expressionExperiment.analyses" class="list" requestURI="" id="analysisList" export="true"
			pagesize="10" decorator="ubic.gemma.web.taglib.displaytag.expression.experiment.ExpressionExperimentWrapper">
			<display:column property="name" sortable="true" maxWords="20"
				href="/Gemma/experimentalDesign/showExperimentalDesign.html" paramId="name" paramProperty="name" />
			<display:column property="description" sortable="true" maxWords="100" />
			<display:setProperty name="basic.empty.showtable" value="false" />
		</display:table>

		<%
		if ( expressionExperiment.getSubsets().size() > 0 ) {
		%>
		<h3>
			<fmt:message key="expressionExperimentSubsets.title" />
		</h3>
		<% 
		} 
		%>
				
		<display:table name="expressionExperiment.subsets" class="list" requestURI="" id="subsetList" export="true"
			pagesize="10" decorator="ubic.gemma.web.taglib.displaytag.expression.experiment.ExpressionExperimentSubSetWrapper">
			<display:column property="nameLink" sortable="true" maxWords="20" titleKey="expressionExperimentSubsets.name" />
			<display:column property="description" sortable="true" maxWords="100" />
			<display:setProperty name="basic.empty.showtable" value="false" />
		</display:table>

		<h3>
			<fmt:message key="designElementDataVectors.title" />
		</h3>
		There are
		<b>
		<c:out value="${designElementDataVectorCount}"/> 
 		</b> design elements for this expression
		experiment.
		<br />
		<display:table name="qtCountSet" class="list" requestURI="" id="dataVectorList" export="true" pagesize="10"
			decorator="ubic.gemma.web.taglib.displaytag.expression.experiment.ExpressionExperimentWrapper">
			<display:column property="qtName" sortable="true" maxWords="20" titleKey="quantitationType.name" />
			<display:column property="qtValue" sortable="true" maxWords="20" titleKey="quantitationType.countVectors" />
			<display:setProperty name="basic.empty.showtable" value="false" />
		</display:table>



		<br />

		<hr />
		<hr />

		<table>
			<tr>
				<td COLSPAN="2">
					<div align="left">
						<input type="button" onclick="location.href='showAllExpressionExperiments.html'" value="Back">
					</div>
				</td>

				<td COLSPAN="2">
					<div align="left">
						<input type="button" onclick="location.href='searchExpressionExperiment.html?id=<%=request.getAttribute( "id" )%>'"
							value="Visual">
					</div>
				</td>

				<authz:acl domainObject="${expressionExperiment}" hasPermission="1,6">
					<td COLSPAN="2">
						<div align="left">
							<input type="button" onclick="location.href='editExpressionExperiment.html?id=<%=request.getAttribute( "id" )%>'"
								value="Edit">
						</div>
					</td>
				</authz:acl>

				<td COLSPAN="2">
					<div align="left">
						<input type="button" onclick="location.href='deleteExpressionExperiment.html?id=<%=request.getAttribute( "id" )%>'"
							value="Delete">
					</div>
				</td>
			</tr>
		</table>
	</body>
</html>
