<%@ include file="/common/taglibs.jsp" %>

<head>
<title>${fn:escapeXml(arrayDesign.shortName)} - ${fn:escapeXml(arrayDesign.name)}</title>
<meta name="description" content="${fn:escapeXml(arrayDesign.description)}" />
<script type="text/javascript">
Ext.namespace( 'Gemma' );
Ext.onReady( function() {
   Ext.QuickTips.init();
   // need wrapper panel because tabPanels can't have title headers (it's used for tabs)
   new Gemma.GemmaViewPort( {
      centerPanelConfig : new Ext.Panel( {
         items : [ new Gemma.PlatformPage( {
            platformId : Ext.get( "platformId" ).getValue()
         } ) ],
         layout : 'fit',
         title : 'Platform details: ' + Ext.get( "shortName" ).getValue()
      } )
   } );
} );
</script>
</head>

<input type="hidden" name="platformId" id="platformId" value="${arrayDesign.id}" />
<input type="hidden" name="shortName" id="shortName" value="${fn:escapeXml(arrayDesign.shortName)}" />
<input type="hidden" name="ame" id="name" value="${fn:escapeXml(arrayDesign.name)}" />