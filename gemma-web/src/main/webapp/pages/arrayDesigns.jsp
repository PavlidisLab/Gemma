<head>
<title>Platforms</title>
</head>

<input type="hidden" id="reloadOnLogout" value="false">
<input type="hidden" id="reloadOnLogin" value="true" />

<script type="text/javascript">
Ext.state.Manager.setProvider( new Ext.state.CookieProvider() );
Ext.onReady( function() {
   Ext.QuickTips.init();
   new Gemma.GemmaViewPort( {
      centerPanelConfig : new Gemma.ArrayDesignsNonPagingGrid()
   } );
} );
</script>
