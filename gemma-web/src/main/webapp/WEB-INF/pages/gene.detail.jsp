<%@ include file="/common/taglibs.jsp"%>
<jsp:directive.page import="org.apache.commons.lang.StringUtils" />
<jsp:directive.page import="java.util.Collection" />
<jsp:directive.page import="ubic.gemma.model.genome.gene.*" />
<jsp:useBean id="gene" scope="request"
	class="ubic.gemma.model.genome.GeneImpl" />

	

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<title><fmt:message key="gene.details" /></title>

<h2>
	<fmt:message key="gene.details" />
</h2>
<table width="100%" cellspacing="10">
	<tr>
		<td valign="top">
			<b> <fmt:message key="gene.officialSymbol" /> </b>
		</td>
		<td>
			<%
			if ( gene.getOfficialSymbol() != null ) {
			%>
			<jsp:getProperty name="gene" property="officialSymbol" />
			<%
			                } else {
			                out.print( "No official symbol available" );
			            }
			%>
		</td>
	</tr>

	<tr>
		<td valign="top">
			<b> <fmt:message key="gene.officialName" /> </b>
		</td>
		<td>
			<%
			if ( gene.getOfficialName() != null ) {
			%>
			<jsp:getProperty name="gene" property="officialName" />
			<%
			                } else {
			                out.print( "No official name available" );
			            }
			%>
		</td>
	</tr>

	<tr>
		<td valign="top">
			<b> <fmt:message key="gene.ncbi" /> </b>
		</td>
		<td>

			<%
			if ( gene.getNcbiId() != null ) {
			%>
			<jsp:getProperty name="gene" property="ncbiId" />
			<a
				href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=gene&cmd=Retrieve&dopt=full_report&list_uids=<%out.print(gene.getNcbiId()); %>">
				(NCBI link)</a>
			<%
			                } else {
			                out.print( "No NCBI ID available" );
			            }
			%>
		</td>
	</tr>

	<tr>
		<td valign="top">
			<b> <fmt:message key="gene.description" /> </b>
		</td>
		<td>

			<%
			if ( gene.getDescription() != null ) {
			%>
			<div class="clob">
				<jsp:getProperty name="gene" property="description" />
			</div>
			<%
			                } else {
			                out.print( "Description unavailable" );
			            }
			%>
		</td>
	</tr>

	<tr>
		<td valign="top">
			<b> <fmt:message key="gene.taxon" /> </b>
		</td>
		<td>
			<%
			                if ( gene.getTaxon() != null ) {
			                out.print( gene.getTaxon().getScientificName() );
			            } else {
			                out.print( "Taxon unavailable" );
			            }
			%>
		</td>
	</tr>

	<tr>
		<td valign="top">
			<b> <fmt:message key="gene.aliases" /> </b>
		</td>
		<td>
			<%
			                if ( gene.getAliases().size() > 0 ) {
			                Collection<GeneAlias> aliasObjects = gene.getAliases();
			                String[] aliases = new String[aliasObjects.size()];
			                int i = 0;
			                for ( GeneAlias a : aliasObjects ) {
			                    aliases[i] = a.getAlias();
			                    i++;
			                }
			                out.print( StringUtils.join( aliases, ", " ) );
			            } else {
			                out.print( "No aliases defined" );
			            }
			%>
		</td>
	</tr>
</table>

<c:if test="${numOntologyEntries > 0 }">
<h3>
	<fmt:message key="gene.ontology" />
	
</h3>
</c:if>

<display:table name="ontologyEntries" class="list" requestURI=""
	id="ontologyEntriesList" pagesize="10"
	decorator="ubic.gemma.web.taglib.displaytag.OntologyWrapper">
	<display:column property="accession" sortable="true" maxWords="20" />
	<display:column property="value" sortable="true" maxWords="20" />
	<display:column property="category" sortable="true" maxWords="20" />
	<display:column property="description" sortable="true" maxWords="20" />
	<display:setProperty name="basic.empty.showtable" value="false" />
</display:table>





<%
if ( gene.getAccessions().size() > 0 ) {
%>
<h3>
	<fmt:message key="gene.accessions" />
</h3>
<%
}
%>

<display:table name="gene.accessions" class="list" requestURI=""
	id="accessionsList" pagesize="10"
	decorator="ubic.gemma.web.taglib.displaytag.gene.GeneWrapper">
	<display:column property="accession" sortable="true" maxWords="20" />
	<display:setProperty name="basic.empty.showtable" value="false" />
</display:table>

<%
if ( gene.getProducts().size() > 0 ) {
%>
<h3>
	<fmt:message key="gene.products" />
</h3>
<%
}
%>

<table>
<tr>
<td>
<display:table name="gene.products" class="list" requestURI=""
	id="productsList" pagesize="10"
	decorator="ubic.gemma.web.taglib.displaytag.gene.GeneWrapper">
	<display:column property="name" sortable="true" maxWords="20" />
	<display:column property="description" sortable="true" maxWords="20" />
	<display:setProperty name="basic.empty.showtable" value="false" />
</display:table>
</td>
</tr>
</table>
<br />
There are
<b> <a
	href="/Gemma/gene/showCompositeSequences.html?id=<%out.print(gene.getId());%>"><c:out
			value="${compositeSequenceCount}" /> </a> </b>
composite sequences associated with this gene.
<br />
