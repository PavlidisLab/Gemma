<%@ include file="/common/taglibs.jsp"%>
<head>
	<title>${expressionExperiment.shortName} Details</title>
	<jwr:script src='/scripts/ajax/ext/data/DwrProxy.js'
		useRandomParam="false" />
	<jwr:script src='/scripts/app/eeDataFetch.js' useRandomParam="false" />

</head>
		 
	<script>
	Ext.namespace('Gemma');
	Ext.BLANK_IMAGE_URL = '/Gemma/images/default/s.gif';
	Ext.state.Manager.setProvider(new Ext.state.CookieProvider());

	Ext.onReady( function() {
		Ext.QuickTips.init();
		 
		new Gemma.ExpressionExperimentPage( {
			renderTo : 'eepage',
			eeId: Ext.get("eeId").getValue()
		});
	});
	
</script>

<input id="eeId" type="hidden" value="${eeId}" />
<input id="taxonName" type="hidden" value="${taxonName}  " />

<div spellcheck="false" id="eedetails">
	<div id="messages"></div>
</div>

<div id="eepage"></div>
