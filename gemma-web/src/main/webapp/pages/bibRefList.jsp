<%@ include file="/common/taglibs.jsp" %>
<head>
<title>Bibliographic References</title>
</head>

<script type="text/javascript">
Ext.namespace( 'Gemma' );
Ext.onReady( function() {
   Ext.QuickTips.init();
   new Gemma.GemmaViewPort( {
      centerPanelConfig : new Gemma.BibliographicReference.Browser()
   } );
} );
</script>
