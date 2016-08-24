<%@ include file="/common/taglibs.jsp"%>
<head>
<title><fmt:message key="bioAssays.title" /></title>

<jwr:script src='/scripts/api/ext/data/DwrProxy.js' />

<script type="text/javascript">
   Ext.BLANK_IMAGE_URL = '/Gemma/images/default/s.gif';

   Ext.onReady(function() {

      Ext.QuickTips.init();
      Ext.state.Manager.setProvider(new Ext.state.CookieProvider());

      var manager = new Gemma.EEManager({ editable : true, id : "eemanager" });

      manager.on('done', function() {
         window.location.reload(true);
      });

      new Gemma.BioAssayGrid({ editable : true, renderTo : 'bioAssayTable', eeId : Ext.get("eeId").getValue() });

   });
</script>

</head>
<h2>
	<fmt:message key="bioAssays.title" />
	for <a href='<c:url value="showExpressionExperiment.html" />?id=${expressionExperiment.id }'>${expressionExperiment.shortName}</a>
</h2>
<p>
	View the <a href='<c:url value="/experimentalDesign/showExperimentalDesign.html?eeid=${expressionExperiment.id }" />'>Experimental
		design</a>
</p>

<Gemma:expressionQC ee="${expressionExperiment.id}" numOutliersRemoved="${numOutliersRemoved}" numPossibleOutliers="${numPossibleOutliers}" hasCorrMat="${hasCorrMat}" hasNodeDegreeDist="${hasNodeDegreeDist }"
	hasPCA="${hasPCA}" numFactors="${numFactors}" hasMeanVariance="${hasMeanVariance }" hasCorrDist="${hasCorrDist}"
	eeManagerId="eemanager" />

<div style="padding: 20px; margin: 10px;" id="bioAssayTable" />

<input id="eeId" type="hidden" value="${expressionExperiment.id}" />

