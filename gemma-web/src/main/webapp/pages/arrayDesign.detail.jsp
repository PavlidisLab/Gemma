<%@ include file="/common/taglibs.jsp"%>
<!--  FIXME use a value object. -->

<head>
<Gemma:script src='/scripts/api/ext/data/DwrProxy.js' />

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

<title>${arrayDesignShortName} - ${arrayDesignName }</title>
</head>

<body>
	<input type="hidden" name="platformId" id="platformId" value="${arrayDesignId}" />
	<input type="hidden" name="shortName" id="shortName" value="${arrayDesignShortName}" />
	<input type="hidden" name="ame" id="name" value="${arrayDesignName}" />

</body>
