<%@ include file="/WEB-INF/common/taglibs.jsp" %>
<head>
<title>${fn:escapeXml(eeSet.name)}</title>
<meta name="description" content="${fn:escapeXml(eeSet.description)}" />
</head>

<input id="eeSetId" type="hidden" value="${eeSet.id}" />
<input id="eeSetName" type="hidden" value="${eeSet.name}" />

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
            new Gemma.ExpressionExperimentSetPage( {
               eeSetId : Ext.get( "eeSetId" ).getValue()
            } ) ],
         layout : 'fit',
         title : Ext.get( "eeSetName" ).getValue()
      } )
   } );
} );
</script>
