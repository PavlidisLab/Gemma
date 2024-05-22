<%@ include file="/common/taglibs.jsp"%>
<head>
<jwr:script src='/scripts/api/ext/data/DwrProxy.js' />

<script type="text/javascript">
   Ext.namespace( 'Gemma' );
   Ext.onReady( function() {
      Ext.QuickTips.init();
      // need wrapper panel because tabPanels can't have title headers (it's used for tabs)
      new Gemma.GemmaViewPort( {
         centerPanelConfig : new Ext.Panel( {
            items : [ new Gemma.GenePage( {
               geneId : Ext.get( "geneId" ).getValue(),
               geneNcbiId : Ext.get( "geneNcbiId" ).getValue(),
               geneSymbol : Ext.get( "geneSymbol" ).getValue(),
               geneName : Ext.get( "geneName" ).getValue(),
               geneTaxonName : Ext.get( "geneTaxonName" ).getValue(),
               geneTaxonId : Ext.get( "geneTaxonId" ).getValue()
            } ) ],
            layout : 'fit',
            title : 'Gene details: ' + Ext.get( "geneSymbol" ).getValue() + ' ('
               + Ext.get( "geneTaxonName" ).getValue() + ')'
         //title:Ext.get("geneSymbol").getValue() + ' - ' + Ext.get("geneName").getValue() + ' (' + Ext.get("geneTaxonName").getValue() + ')'
         } )
      } );
   } );
</script>

<title><c:if test="${not empty geneOfficialSymbol}">
			${geneOfficialSymbol}
		</c:if> <fmt:message key="gene.details" /></title>
</head>

<body>

	<input type="hidden" name="geneId" id="geneId" value="${geneId}" />
	<input type="hidden" name="geneNcbiId" id="geneNcbiId" value="${geneNcbiId}" />
	<input type="hidden" name="geneSymbol" id="geneSymbol" value="${geneOfficialSymbol}" />
	<input type="hidden" name="geneName" id="geneName" value="${geneOfficialName}" />
	<input type="hidden" name="geneTaxonName" id="geneTaxonName" value="${geneTaxonCommonName}" />
	<input type="hidden" name="geneTaxonId" id="geneTaxonId" value="${geneTaxonId}" />

	<div id="newGenePageWidget"></div>

</body>