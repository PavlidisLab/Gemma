<%@ include file="/common/taglibs.jsp"%>
<%@ page import="java.util.*"%>
<%@ page import="ubic.gemma.model.expression.arrayDesign.ArrayDesignImpl"%>
<jsp:useBean id="arrayDesign" scope="request" class="ubic.gemma.model.expression.arrayDesign.ArrayDesignImpl" />

<!DOCTYPE html PUBLIC "-//W3C//Dtd html 4.01 transitional//EN">
<title>Array Design Details</title><content tag="heading">Array Design Details</content>

<!--  Summary of array design associations -->
<c:if test="${ summary != ''}" >
<table class='datasummaryarea'>
<caption>
	<jsp:getProperty name="arrayDesign" property="name" />
</caption>
<tr>
<td>
${ summary } <br>
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
				</td>
			</tr>
			<tr>
				<td class="label">
					Accessions
				</td>
				<td>
					<%
					  if ( ( arrayDesign.getExternalReferences() ) != null
					                    && ( arrayDesign.getExternalReferences().size() > 0 ) ) {
					%>
					<c:forEach var="accession"
						items="${ arrayDesign.externalReferences }">
        				<Gemma:databaseEntry
            				databaseEntry="${accession}" />
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
					<a href="/Gemma/expressionExperiment/showAllExpressionExperiments.html?id=<c:out value="${expressionExperimentIds}" />">
						<img src="/Gemma/images/magnifier.png" />
					</a>
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
						<div class="clob"> <jsp:getProperty name="arrayDesign" property="description" /> </div>
					<%
					} else {
					%>
					(None provided)
					<%
					}
					%>
				</td>
			</tr>
			

			<%-- FIXME - show some of the design elements --%>	
			<table>
			<tr>
				<td colspan="2">
					<hr />
				</td>
			</tr>
			<tr>
				<td colspan="2">
					<div align="left">
						<input type="button" onclick="location.href='showAllArrayDesigns.html'" value="Back">
					</div>
				</td>
				<authz:acl domainObject="${arrayDesign}" hasPermission="1,6">
					<td COLSPAN="2">
						<div align="left">
							<input type="button"
								onclick="location.href='/Gemma/arrayDesign/editArrayDesign.html?id=<%=request.getAttribute( "id" )%>'"
								value="Edit">
						</div>
					</td>
					<td COLSPAN="2">
						<div align="left">
							<input type="button"
								onclick="location.href='/Gemma/arrays/generateArrayDesignSummary.html?id=<%=request.getAttribute( "id" )%>'"
								value="Refresh Summary">
						</div>
					</td>
				</authz:acl>
			</tr>
		</table>
			
			<display:table name="test" class="list"
				requestURI="" id="trow" pagesize="100">
				<display:column title="cs"><c:out value="${trow[0]}" /></display:column>
				<display:setProperty name="basic.empty.showtable" value="false" />
			</display:table>
		
		<a class="helpLink" href="/Gemma/static/arrayDesignReportHelp.html">Report Help</a>
