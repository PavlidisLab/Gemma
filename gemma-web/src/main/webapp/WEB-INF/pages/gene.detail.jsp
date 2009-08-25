<%@ include file="/common/taglibs.jsp"%>
<jsp:directive.page import="org.apache.commons.lang.StringUtils" />
<jsp:directive.page import="java.util.Collection" />
<jsp:directive.page import="java.util.HashSet" />
<jsp:directive.page import="ubic.gemma.model.genome.gene.*" />
<jsp:directive.page import="ubic.gemma.image.aba.Image" />

<jsp:useBean id="gene" scope="request" class="ubic.gemma.model.genome.GeneImpl" />
<jsp:useBean id="representativeImages" scope="request" class="java.util.HashSet" />

<jwr:script src='/scripts/ajax/ext/data/DwrProxy.js' />
<jwr:script src='/scripts/app/gene.detail.js' />

<security:authorize ifAnyGranted="admin">
	<script type="text/javascript">
	Ext.namespace('Gemma');
	Ext.onReady(function() {
	Ext.QuickTips.init();
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
</security:authorize>

<title> <c:if test="${gene.officialSymbol != null}">
	 <jsp:getProperty name="gene" property="officialSymbol" /> </c:if> <fmt:message key="gene.details" />
</title>

<h3>
	<c:if test="${gene.officialSymbol != null}">
	 <jsp:getProperty name="gene" property="officialSymbol" />
		<%
			if ( gene.getOfficialName() != null ) {
		%>
			<i> <jsp:getProperty name="gene" property="officialName" /></i>			
		<%} 

			   if ( gene.getTaxon() != null ) {
			                out.print( "[" + gene.getTaxon().getScientificName() + "]");
			   }
		%>
	</h3>
	</c:if>


<table cellspacing="6">
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
			
		<%
			if ( gene.getNcbiId() != null ) {
		%>
			&nbsp;&nbsp;
			<a title="NCBI Gene link"
				href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=gene&cmd=Retrieve&dopt=full_report&list_uids=<%out.print(gene.getNcbiId()); %>">
				<img alt="NCBI Gene Link" src="<c:url value='/images/logo/ncbi.gif'/>" /> </a>
			<%
			}
			%>	
			
			
			
		</td>
	</tr>
	<tr>
		<td align="right" valign="top">
			<b>Probes</b> &nbsp; <a class="helpLink" href="?"
				onclick="showHelpTip(event, 'Number of probes for this gene on expression platforms in Gemma'); return false"><img
					src="/Gemma/images/help.png" /> </a>
		</td>
		<td valign="top">			
			<a href="/Gemma/gene/showCompositeSequences.html?id=<%out.print(gene.getId());%>">
			<c:out value="${compositeSequenceCount}" />
			<img src="<c:url value='/images/magnifier.png'/>" />   
			</a>
		</td>
	</tr>	
			
</table>



<h3>
 Datasets differentially expressed in 
 	
				   <a class="helpLink" href="?"
				onclick="showHelpTip(event, 'Below are the data sets the gene is Differentially expressed in. Beside is a quick link for going to a more detailed differential analysis'); return false"><img
					src="/Gemma/images/help.png" /> </a>
					
									   &nbsp;
					
						<a title="Go To Advanced Differential Expression Search with <%out.print(gene.getOfficialSymbol()); %>"
				   href="/Gemma/diff/diffExpressionSearch.html?g=<%out.print(gene.getId());%>&thres=0.01&t=<%out.print(gene.getTaxon().getId());%>&setName=All <%out.print(gene.getTaxon().getCommonName());%>">
				   <img	src="<c:url value='/images/icons/diff-ex.png'/>" /> </a>
				   
 
 </h3>
	<div id="diffExpression-msg"> </div> <div id="diff-grid"> </div>
<h3>	
	 Top Coexpressed Genes 
	 
		   <a class="helpLink" href="?"	onclick="showHelpTip(event, 'Below is a summary coexpression analysis, beside is a link to go to the detailed coexpression results'); return false"><img
					src="/Gemma/images/help.png" /> </a>

	 		   &nbsp;
					
	 	<a title="Do Advanced Coexpression Search with <%out.print(gene.getOfficialSymbol()); %>"
				   href="/Gemma/searchCoexpression.html?g=<%out.print(gene.getId());%>&s=3&t=<%out.print(gene.getTaxon().getId());%>&an=All <%out.print(gene.getTaxon().getCommonName());%>">
				   <img	src="<c:url value='/images/icons/co-ex.png'/>" /> </a>
				   
		
</h3>
	<div id="coexpression-msg"> </div> <div id="coexpression-grid"> </div>		


		<h3> Allen Brain Atlas Expression Images 
		<a class="helpLink" href="?" onclick="showHelpTip(event, 'Below is a sampling of expression profile pictures from the allen brain atlas. Beside is a link to the allen brain atlas'); return false"><img
					src="/Gemma/images/help.png" /> </a>

		 <a title=" Allen Brain Atas details for <%out.print(gene.getOfficialSymbol());%>" href= <c:out value="${abaGeneUrl}" /> target="_blank" > <img
					src="/Gemma/images/logo/abaLogo.jpg" height=20 width=20/> </a> </h3> 
		<%		   		
		 for ( Object obj : representativeImages ) {		
		 	 ubic.gemma.image.aba.Image img = (ubic.gemma.image.aba.Image) obj;	               
		%>
		
				 	&nbsp;&nbsp;
			<a title="Allen Brian Atlas Image for <%out.print(gene.getOfficialSymbol());%> "
				  onclick="Gemma.geneLinkOutPopUp( &#34; <%out.print(img.getDownloadExpressionPath());%> &#34; )"> 
				   <img	src="<%out.print(img.getExpressionThumbnailUrl());%>"/> 
			</a>
		
		<%			                  
		 }//end of for loop
		%>

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

<tr>
	
<h3> <fmt:message key="gene.description" /> </h3>
	
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


<security:authorize ifAnyGranted="admin">
	<div id="auditTrail"></div>
	<input type="hidden" name="auditableId" id="auditableId" value="${gene.id}" />
	<input type="hidden" name="auditableClass" id="auditableClass" value="${gene.class.name}" />
</security:authorize>
