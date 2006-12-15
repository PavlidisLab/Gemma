<%@ include file="/common/taglibs.jsp"%>
<%@ page import="java.util.*"%>
<%@ page import="ubic.gemma.model.expression.arrayDesign.ArrayDesignImpl"%>
<jsp:useBean id="arrayDesign" scope="request" class="ubic.gemma.model.expression.arrayDesign.ArrayDesignImpl" />

<!DOCTYPE html PUBLIC "-//W3C//Dtd html 4.01 transitional//EN">
<title>Array Design Details</title><content tag="heading">Array Design Details</content>

<!--  Summary of array design associations -->
<table class='datasummary'>
<caption>
	<jsp:getProperty name="arrayDesign" property="name" />
</caption>
<tr>
<td colspan=2 align=center>
<b>Summary</b>
</td>
</tr>
<tr>
<%-- Admin only for sequence and blat associations --%>
<authz:authorize ifAnyGranted="admin">
<td>
	Probes with sequences
</td>
<td>
	<c:out value="${numCsBioSequences}" />
</td> 
</tr>
<tr>
<td>
	Probes with genome alignments
</td>
<td>
	<c:out value="${numCsBlatResults}" />
</td>
</tr>
</authz:authorize>
<!--  End admin only -->
<tr>
<td>
	Probes mapping to gene(s)
</td>
<td>
	<c:out value="${numCsGenes}" />
</td>
</tr>
<tr>
<td>
	Unique genes represented
</td>
<td>
	<c:out value="${numGenes}" />
</td>
</tr>
</table>

		 
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
						<c:if test='${accession != null && accession.accession != ""}'>

							<c:if test='${fn:startsWith(accession.accession, "GPL")}'>
								${accession.accession }
								<a target='_blank' href='http://www.ncbi.nlm.nih.gov/geo/query/acc.cgi?acc=${accession.accession }'>
									<img src='/Gemma/images/logo/ncbi.gif' />
								</a>
							</c:if>
							<c:if test='${!fn:startsWith(accession.accession, "GPL")}'>
								${accession.accession }
							</c:if>
							<br />
						</c:if>
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
				</authz:acl>
			</tr>
		</table>
