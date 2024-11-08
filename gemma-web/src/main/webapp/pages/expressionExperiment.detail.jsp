<%@ include file="/common/taglibs.jsp"%>

<head>
<title>${expressionExperiment.shortName} Details</title>
<Gemma:script src='/scripts/api/ext/data/DwrProxy.js' />
<Gemma:script src='/scripts/app/eeDataFetch.js' />

</head>

<script>
   Ext.namespace('Gemma');
   Ext.BLANK_IMAGE_URL = '${pageContext.request.contextPath}/images/default/s.gif';
   Ext.state.Manager.setProvider(new Ext.state.CookieProvider());

   Ext.onReady(function() {
      Ext.QuickTips.init();

      // need wrapper panel because tabPanels can't have title headers (it's used for tabs)
      new Gemma.GemmaViewPort({ centerPanelConfig : new Ext.Panel({
         items : [ new Gemma.ExpressionExperimentPage({ eeId : Ext.get("eeId").getValue() }) ], layout : 'fit',
         title : Ext.get("eeShortName").getValue() }) });
   });
</script>

<input id="eeId" type="hidden" value="${eeId}" />
<input id="taxonName" type="hidden" value="${taxonName}  " />
<input id="eeShortName" type="hidden" value="${expressionExperiment.shortName}  " />

<input type="hidden" id="reloadOnLogout" value="true">
<input type="hidden" id="reloadOnLogin" value="true" />

<div spellcheck="false" id="eedetails">
	<div id="messages"></div>
</div>

<div id="eepage"></div>
