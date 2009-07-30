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
			<a href="/Gemma/gene/showCompositeSequences.html?id=<%out.print(gene.getId());%>">
			<c:out value="${compositeSequenceCount}" />
			<img src="<c:url value='/images/magnifier.png'/>" />   
			</a>
		</td>
	</tr>
	
		<%
			if ( (gene.getId() != null) && (gene.getOfficialSymbol() != null) && (gene.getTaxon() != null)  && (gene.getTaxon().getCommonName() != null) ) {
		%>
		
	<tr>
		<td align="right" valign="top">
			<b>Run Analysis </b><a class="helpLink" href="?"
				onclick="showHelpTip(event, 'A quick link to using this gene in a coexpression or differential analysis'); return false"><img
					src="/Gemma/images/help.png" /> </a>
		</td>
		<td valign="top">
		 
		 	&nbsp;&nbsp;
				<a title="Do Coexpression Search with <%out.print(gene.getOfficialSymbol()); %>"
				   href="/Gemma/searchCoexpression.html?g=<%out.print(gene.getId());%>&s=3&t=<%out.print(gene.getTaxon().getId());%>&an=All <%out.print(gene.getTaxon().getCommonName());%>">
				   <img	src="<c:url value='/images/icons/co-ex.png'/>" /> </a>
				   
				   
			&nbsp;&nbsp;
				<a title="Do Differential Expression Search with <%out.print(gene.getOfficialSymbol()); %>"
				   href="/Gemma/diff/diffExpressionSearch.html?g=<%out.print(gene.getId());%>&thres=0.01&t=<%out.print(gene.getTaxon().getId());%>&setName=All <%out.print(gene.getTaxon().getCommonName());%>">
				   <img	src="<c:url value='/images/icons/diff-ex.png'/>" /> </a>

				   
		</td>
	</tr>
			<%
			}
			%>
		
	<tr>
		<td align="right" valign="top">
			<b>Allen Brain Atlas Expression Images </b><a class="helpLink" href="?"
				onclick="showHelpTip(event, 'A picture of the expression profile from the allen bran atlas'); return false"><img
					src="/Gemma/images/help.png" /> </a>
		</td>
		
		<td valign="top">
		 <a title=" Allen Brain Atas details for <%out.print(gene.getOfficialSymbol());%>" href= <c:out value='${abaGeneUrl}' />	><img
					src="/Gemma/images/logo/abaLogo.jpg" height=20 width=20/> </a>
		<%		   		
		 for ( Object obj : representativeImages ) {		
		 	 ubic.gemma.image.aba.Image img = (ubic.gemma.image.aba.Image) obj;	               
		%>
		
				 	&nbsp;&nbsp;
			<a title="Allen Brian Atlas Image for <%out.print(gene.getOfficialSymbol());%> "
				   onClick="
				   			        imgSrc =  '<a  title= \' Allen Brain Atas details for <%out.print(gene.getOfficialSymbol());%> \'  href= 	<c:out value='${abaGeneUrl}' />   target=\'_blank\'/> <img	src=\'<%out.print(img.getDownloadExpressionPath());%>\'> </a>';
	  								imgTitle = '<img height=15  src=/Gemma/images/abaExpressionLegend.gif> ';
				   					  win = new Ext.Window({							             							  							            						             				          
							                html: imgSrc,    
							                title : imgTitle,   
							                resizeable : false,    
							                autoScroll : true 
						        });
						        win.show(this);
       						 " />
				   <img	src="<%out.print(img.getExpressionThumbnailUrl());%>" /> </a>
		
		<%			                  
		 }//end of for loop
		%>
			   
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


<security:authorize ifAnyGranted="admin">
	<div id="auditTrail"></div>
	<input type="hidden" name="auditableId" id="auditableId" value="${gene.id}" />
	<input type="hidden" name="auditableClass" id="auditableClass" value="${gene.class.name}" />
</security:authorize>
