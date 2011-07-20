<%@ include file="/common/taglibs.jsp"%>

<jsp:useBean id="gene" scope="request"
	class="ubic.gemma.model.genome.GeneImpl" />
<jsp:useBean id="representativeImages" scope="request"
	class="java.util.HashSet" />
<jsp:useBean id="homologues" scope="request" class="java.util.HashSet" />
<head>
	<jwr:script src='/scripts/ajax/ext/data/DwrProxy.js' />
	<jwr:script src='/scripts/app/gene.detail.js' />

	<security:authorize access="hasRole('GROUP_ADMIN')">
		<!-- script type="text/javascript">
Ext.namespace('Gemma');
Ext.onReady(function() {
	Ext.QuickTips.init();
	var id = dwr.util.getValue("auditableId");
	if (!id) {
		return;
	}
	var clazz = dwr.util.getValue("auditableClass");
	var auditable = {
		id : id,
		classDelegatingFor : clazz
	};
	var grid = new Gemma.AuditTrailGrid( {
		renderTo : 'auditTrail',
		auditable : auditable
	});
});
</script-->
	</security:authorize>
			<script type="text/javascript">
Ext.namespace('Gemma');
Ext.onReady(function() {
	Ext.QuickTips.init();
	
	var k = new Gemma.GenePage( {
			renderTo : 'newGenePageWidget',
			geneId: Ext.get("gene").getValue()
		});
});
</script>

	<title><c:if test="${not empty gene.officialSymbol}">
			<jsp:getProperty name="gene" property="officialSymbol" />
		</c:if> <fmt:message key="gene.details" />
	</title>
</head>
<body>

	<input type="hidden" name="gene" id="gene" value="${gene.id}" />
	<input type="hidden" name="geneName" id="geneName" value="${gene.name}" />
	<input type="hidden" name="taxon" id="taxon" value="${gene.taxon.id}" />
	
	<div id="newGenePageWidget"></div>
	
	<h3>
		<c:if test="${not empty gene.officialSymbol}">
		${gene.officialSymbol }
		<c:if test="${not empty gene.officialName}">
		    ${gene.officialName}
		</c:if>
			<c:if test="${not empty gene.taxon}">
			[${gene.taxon.scientificName}]
			 <input type="hidden" id="taxonScientificName"
					value="${gene.taxon.scientificName}" />
			</c:if>

		</c:if>
	</h3>


	<table cellspacing="6">
		<tr>
			<td align="right" valign="top">
				<strong> <fmt:message key="gene.aliases" /> </strong>
			</td>
			<td valign="top">
				<%
				    if ( gene.getAliases().size() > 0 ) {
				        java.util.Collection<ubic.gemma.model.genome.gene.GeneAlias> aliasObjects = gene.getAliases();
				        String[] aliases = new String[aliasObjects.size()];
				        int i = 0;
				        for ( ubic.gemma.model.genome.gene.GeneAlias a : aliasObjects ) {
				            aliases[i] = a.getAlias();
				            i++;
				        }
				        out.print( org.apache.commons.lang.StringUtils.join( aliases, ", " ) );
				    } else {
				        out.print( "No aliases defined" );
				    }
				%>

				<c:if test="${not empty gene.ncbiId}">
				&nbsp;&nbsp;
				<a title="NCBI Gene link"
						href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=gene&cmd=Retrieve&dopt=full_report&list_uids=${gene.ncbiId}">
						<img alt="NCBI Gene Link"
							src="<c:url value='/images/logo/ncbi.gif'/>" /> </a>

				</c:if>


			</td>
			<c:if test="${not empty homologues}">
				<tr>
					<td align="right" valign="top">
						<strong>Homologues</strong>
					</td>
					<td>
						<%
						    if ( homologues.size() > 0 ) {
						            for ( Object o : homologues ) {
						                ubic.gemma.model.genome.gene.GeneValueObject go = ( ubic.gemma.model.genome.gene.GeneValueObject ) o;
						                out
						                        .print( "<a title=\"View this homologous gene in Gemma\" href=\"/Gemma/gene/showGene.html?id="
						                                + go.getId()
						                                + "\">"
						                                + go.getOfficialSymbol()
						                                + "&nbsp;["
						                                + go.getTaxonCommonName() + "]</a>&nbsp;&nbsp;&nbsp;" );
						            }
						        }
						%>
					</td>
				</tr>
			</c:if>
		</tr>
		<tr>
			<td>
				<div class="clearfix">
					<strong> <fmt:message key="gene.group" /> </strong>
				</div>
			</td>
			<td>
				<div id="gene-group-grid"></div>
			</td>
		</tr>



		<tr>
			<td align="right" valign="top">
				<b>Probes</b> &nbsp;
				<a class="helpLink" href="?"
					onclick="showHelpTip(event, 'Number of probes for this gene on expression platforms in Gemma'); return false"><img
						src="/Gemma/images/help.png" /> </a>
			</td>
			<td valign="top">
				${compositeSequenceCount} &nbsp;
				<a href="/Gemma/gene/showCompositeSequences.html?id=${gene.id}">
					<img src="<c:url value='/images/magnifier.png'/>" /> </a>
			</td>
		</tr>

	</table>



	<h3>
		Differential expression

		<a class="helpLink" href="?"
			onclick="showHelpTip(event, 'Top data sets in which the gene is Differentially expressed in; results are listed per probe so a data set may be listed more than once. Use the link at right for more detailed differential analysis'); return false"><img
				src="/Gemma/images/help.png" /> </a> &nbsp;

		<a id="diff-link"
			title="Go To Advanced Differential Expression Search with <%out.print( gene.getOfficialSymbol() );%>"
			href="/Gemma/diff/diffExpressionSearch.html?g=<%out.print( gene.getId() );%>&thres=0.01&t=<%out.print( gene.getTaxon().getId() );%>&setName=All <%out.print( gene.getTaxon().getCommonName() );%>">
			<img src="<c:url value='/images/icons/diff-ex.png'/>" /> </a>


	</h3>
	<div id="diffExpression-msg">
	</div>
	<div id="diff-grid">
	</div>
	<h3>
		Coexpression

		<a class="helpLink" href="?"
			onclick="showHelpTip(event, 'Top genes with which this gene is coexpressed. Use the link at right to go to a more detailed coexpression results'); return false"><img
				src="/Gemma/images/help.png" /> </a> &nbsp;

		<a id="coexpression-link"
			title="Do Advanced Coexpression Search with <%out.print( gene.getOfficialSymbol() );%>"
			href="/Gemma/searchCoexpression.html?g=<%out.print( gene.getId() );%>&s=3&t=<%out.print( gene.getTaxon().getId() );%>&an=All <%out.print( gene.getTaxon().getCommonName() );%>">
			<img src="<c:url value='/images/icons/co-ex.png'/>" /> </a>


	</h3>
	<div id="coexpression-msg">
	</div>
	<div id="coexpression-grid">
	</div>

	<c:if test="${not empty abaImages}">
		<h3>
			Allen Brain Atlas expression pattern

			<a class="helpLink" href="?"
				onclick="showHelpTip(event, 'Below is a sampling of expression profile pictures from the allen brain atlas. Beside is a link to the allen brain atlas'); return false"><img
					src="/Gemma/images/help.png" /> </a>

			<a
				title='Go to Allen Brain Atlas details for <c:out value="${gene.officialSymbol}" />'
				href="${abaGeneUrl}" target="_blank"> <img
					src="/Gemma/images/logo/aba-icon.png" height="20" width="20" /> </a>
		</h3>


		<c:if test="${not empty gene.taxon && gene.taxon.id != 2}">
			<p>
				Images are for mouse gene ${gene.officialSymbol}.
			</p>
		</c:if>
		<div style="valign: top" class="clearfix">
			<c:forEach var="img" items="${abaImages}">
				<div style="cursor: pointer; float: left; padding: 8px">
					<a
						title='Allen Brain Atlas Image for <c:out value="${gene.officialSymbol}"/>, click to enlarge '
						onClick="Gemma.geneLinkOutPopUp( &#34; ${img.downloadExpressionPath} &#34; )">
						<img src="${img.expressionThumbnailUrl}" /> </a>
				</div>
			</c:forEach>
		</div>
		<br />
	</c:if>
	<div class="clearfix">
		<h3>
			<fmt:message key="gene.ontology" />
			terms
			<a class="helpLink" href="?"
				onclick="showHelpTip(event, 'Only Gene Ontology terms that are directly attached to this gene are shown. Implicit associations (i.e., with parent terms in the GO hierarchy) are not listed.'); return false"><img
					src="/Gemma/images/help.png" /> </a>

		</h3>

		<div id="go-grid"></div>
	</div>
	<div class="clearfix">
		<h3>
			<fmt:message key="gene.products" />
		</h3>

		<div id="geneproduct-grid"></div>
	</div>

	<c:if test="${not empty gene.description}">
		<div
			style="width: 400px; font-size: smaller; margin: 3px; padding: 5px; background-color: #DDDDDD">
			Notes: ${gene.description}
		</div>
	</c:if>



	<security:authorize access="hasRole('GROUP_ADMIN')">
		<div id="auditTrail"></div>
		<input type="hidden" name="auditableId" id="auditableId"
			value="${gene.id}" />
		<input type="hidden" name="auditableClass" id="auditableClass"
			value="${gene.class.name}" />
	</security:authorize>
</body>
