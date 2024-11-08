<%@ include file="/common/taglibs.jsp" %>

<head>
<title>${fn:escapeXml(gene.name)} - ${fn:escapeXml(gene.officialName)}</title>
<meta name="description" content="${fn:escapeXml(gene.officialName)}" />
</head>

<input type="hidden" name="geneId" id="geneId" value="${gene.id}" />
<input type="hidden" name="geneNcbiId" id="geneNcbiId" value="${gene.ncbiId}" />
<input type="hidden" name="geneSymbol" id="geneSymbol" value="${fn:escapeXml(gene.officialSymbol)}" />
<input type="hidden" name="geneName" id="geneName" value="${fn:escapeXml(gene.officialName)}" />
<input type="hidden" name="geneTaxonName" id="geneTaxonName" value="${fn:escapeXml(gene.taxonCommonName)}" />
<input type="hidden" name="geneTaxonId" id="geneTaxonId" value="${gene.taxonId}" />

<div id="newGenePageWidget"></div>

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