<%@ include file="/common/taglibs.jsp" %>

<head>
<title>${fn:escapeXml(expressionExperiment.shortName)} - ${fn:escapeXml(expressionExperiment.name)}</title>
<meta name="description" content="${fn:escapeXml(expressionExperiment.description)}" />
<meta name="keywords" content="${fn:escapeXml(keywords)}" />
<c:if test="${not empty expressionExperiment.primaryPublication}">
<Gemma:citationMeta citation="${expressionExperiment.primaryPublication}" />
</c:if>
<Gemma:script src='/scripts/app/eeDataFetch.js' />
</head>

<input id="eeId" type="hidden" value="${expressionExperiment.id}" />
<input id="taxonName" type="hidden" value="${fn:escapeXml(expressionExperiment.taxon.commonName)}" />
<input id="eeShortName" type="hidden" value="${fn:escapeXml(expressionExperiment.shortName)}" />
<input type="hidden" id="reloadOnLogout" value="true" />
<input type="hidden" id="reloadOnLogin" value="true" />

<div spellcheck="false" id="eedetails">
    <div id="messages"></div>
</div>

<div id="eepage"></div>

<script>
Ext.namespace( 'Gemma' );
Ext.BLANK_IMAGE_URL = '${pageContext.request.contextPath}/images/default/s.gif';
Ext.state.Manager.setProvider( new Ext.state.CookieProvider() );

Ext.onReady( function() {
   Ext.QuickTips.init();

   // need wrapper panel because tabPanels can't have title headers (it's used for tabs)
   new Gemma.GemmaViewPort( {
      centerPanelConfig : new Ext.Panel( {
         items : [ new Gemma.ExpressionExperimentPage( {eeId : Ext.get( "eeId" ).getValue()} ) ], layout : 'fit',
         title : Ext.get( "eeShortName" ).getValue()
      } )
   } );
} );
</script>
