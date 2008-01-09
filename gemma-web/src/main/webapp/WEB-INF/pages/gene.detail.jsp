<%@ include file="/common/taglibs.jsp"%>
<jsp:directive.page import="org.apache.commons.lang.StringUtils" />
<jsp:directive.page import="java.util.Collection" />
<jsp:directive.page import="ubic.gemma.model.genome.gene.*" />
<jsp:useBean id="gene" scope="request" class="ubic.gemma.model.genome.GeneImpl" />

<script src="<c:url value='/scripts/ext/adapter/prototype/ext-prototype-adapter.js'/>" type="text/javascript"></script>
<script src="<c:url value='/scripts/ext/ext-all.js'/>" type="text/javascript"></script>

<script type="text/javascript" src="<c:url value='/scripts/ext/data/DwrProxy.js'/>"></script>

<script type="text/javascript" src="<c:url value='/scripts/ext/data/ListRangeReader.js'/>"></script>

<script type='text/javascript' src='/Gemma/dwr/interface/GeneController.js'></script>

<script type='text/javascript' src='/Gemma/dwr/engine.js'></script>
<script type='text/javascript' src='/Gemma/dwr/util.js'></script>


<script type="text/javascript" src="<c:url value='/scripts/ajax/gene.detail.js'/>" type="text/javascript"></script>

<script type="text/javascript" src="<c:url value='/scripts/scriptaculous/effects.js'/>"></script>
<authz:authorize ifAnyGranted="admin">
	<script type='text/javascript' src='/Gemma/dwr/interface/AuditController.js'></script>
	<script type="text/javascript" src="<c:url value='/scripts/ajax/auditTrail.js'/>" type="text/javascript"></script>
</authz:authorize>

<title><fmt:message key="gene.details" /></title>

<h1>
	<fmt:message key="gene.details" />
	<c:if test="${gene.officialSymbol != null}">
	for <jsp:getProperty name="gene" property="officialSymbol" />
	</c:if>
</h1>
<table cellspacing="6">
	<tr>
		<td align="right" valign="top">
			<b> <fmt:message key="gene.officialSymbol" /> </b>
		</td>
		<td valign="top">
			<%
			if ( gene.getOfficialSymbol() != null ) {
			%>
			<jsp:getProperty name="gene" property="officialSymbol" />
			<%
			                } else {
			                out.print( "No official symbol available" );
			            }
			%>
			<%
			if ( gene.getNcbiId() != null ) {
			%>
			&nbsp;&nbsp;
			<a title="NCBI"
				href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=gene&cmd=Retrieve&dopt=full_report&list_uids=<%out.print(gene.getNcbiId()); %>">
				<img alt="NCBI" src="<c:url value='/images/logo/ncbi.gif'/>" /> </a>
			<%
			}
			%>

		</td>
	</tr>

	<tr>
		<td align="right" valign="top">
			<b> <fmt:message key="gene.officialName" /> </b>
		</td>
		<td valign="top">
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
		<td align="right" valign="top">
			<b> <fmt:message key="gene.description" /> </b>
		</td>
		<td valign="top">

			<%
			if ( gene.getDescription() != null ) {
			%>
			<div class="clob" style="height: 20px;">
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
		<td align="right" valign="top">
			<b> <fmt:message key="gene.taxon" /> </b>
		</td>
		<td valign="top">
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
		<td align="right" valign="top">
			<b> <fmt:message key="gene.aliases" /> </b>
		</td>
		<td valign="top">
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
	<tr>
		<td align="right" valign="top">
			<b>Probes</b><a class="helpLink" href="?"
				onclick="showHelpTip(event, 'Number of probes for this gene on expression platforms in Gemma'); return false"><img
					src="/Gemma/images/help.png" /> </a>
		</td>
		<td valign="top">
			<c:out value="${compositeSequenceCount}" />
			<a href="/Gemma/gene/showCompositeSequences.html?id=<%out.print(gene.getId());%>"><img
					src="<c:url value='/images/magnifier.png'/>" /> </a>
		</td>
	</tr>
</table>




<h3>
	<fmt:message key="gene.ontology" />
	terms
	<a class="helpLink" href="?"
		onclick="showHelpTip(event, 'Only Gene Ontology terms that are directly attached to this gene are shown. Implicit associations (i.e., with parent terms in the GO hierarchy) are not listed.'); return false"><img
			src="/Gemma/images/help.png" /> </a>

</h3>

<div id="go-grid" class="x-grid-mso" style="border: 1px solid #c3daf9; overflow: hidden; width: 550px; height: 250px;"></div>
<input type="hidden" name="gene" id="gene" value="${gene.id}" />



<h3>
	<fmt:message key="gene.products" />
</h3>

<div id="geneproduct-grid" class="x-grid-mso"
	style="border: 1px solid #c3daf9; overflow: hidden; width: 430px; height: 250px;"></div>


<authz:authorize ifAnyGranted="admin">
	<h3>
		History
	</h3>
	<div id="auditTrail" class="x-grid-mso" style="border: 1px solid #c3daf9; overflow: hidden; width: 650px;"></div>
	<input type="hidden" name="auditableId" id="auditableId" value="${gene.id}" />
	<input type="hidden" name="auditableClass" id="auditableClass" value="${gene.class.name}" />
</authz:authorize>
