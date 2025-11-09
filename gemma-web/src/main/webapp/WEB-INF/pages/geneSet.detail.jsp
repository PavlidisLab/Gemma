<%@ include file="/WEB-INF/common/taglibs.jsp" %>
<jsp:useBean id="geneSet" scope="request" type="ubic.gemma.model.genome.gene.GeneSetValueObject" />
<head>
<title>${fn:escapeXml(geneSet.name)} Details</title>
<meta name="description" content="${fn:escapeXml(geneSet.description)}" />
</head>

<input id="geneSetId" type="hidden" value="${geneSet.id}" />
<input id="geneSetName" type="hidden" value="${fn:escapeXml(geneSet.name)}" />

<input type="hidden" id="reloadOnLogout" value="true">
<input type="hidden" id="reloadOnLogin" value="true" />

<div spellcheck="false">
    <div id="messages"></div>
</div>

<script>
Ext.namespace( 'Gemma' );
Ext.BLANK_IMAGE_URL = '${pageContext.request.contextPath}/images/default/s.gif';
Ext.state.Manager.setProvider( new Ext.state.CookieProvider() );

Ext.onReady( function() {
   Ext.QuickTips.init();

   // need wrapper panel because tabPanels can't have title headers (it's used for tabs)
   new Gemma.GemmaViewPort( {
      centerPanelConfig : new Ext.Panel( {
         items : [
            new Gemma.GeneSetPage( {
               geneSetId : Ext.get( "geneSetId" ).getValue()
            } ) ],
         layout : 'fit',
         title : 'Gene Group: \"' + Ext.get( "geneSetName" ).getValue() + '\"'
      } )
   } );
} );

</script>

