<%@ include file="/common/taglibs.jsp"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">

<head>
	<jwr:script src='/scripts/ajax/ext/data/DwrProxy.js' />    
     <script type='text/javascript' src='/Gemma/static/heatmaplib.js'></script>
	 
	 <script type='text/javascript' src='/Gemma/dwr/interface/DifferentialExpressionSearchController.js'></script>
	<!-- jwr:script src='/scripts/app/AnalysisResultsSearch.js' /-->

	<script type="text/javascript">
	
	//Ext.state.Manager.setProvider(new Ext.state.CookieProvider( ));
	//Ext.QuickTips.init();
	//var data = {};
	Ext.onReady( function() {
			var metaVizApp;		

			metaVizApp = new Gemma.MetaHeatmapDataSelection();
			metaVizApp.show();

//			DifferentialExpressionSearchController.getVisualizationTestData(function(data) {
//				metaVizApp = new Gemma.MetaHeatmapApp({visualizationData : data, renderTo:'meta-heatmap-div'});
//				metaVizApp.doLayout();
//       		});
		}
	);
	</script>
</head>
	<!-- div align="center">
		<div style="width: 900px">
			<div id="analysis-results-search-form-warnings" align="left"></div>
			<div id="analysis-results-search-form" align="center"></div>
			<br>
			<div id="analysis-results-search-form-messages" align="left"></div>
			<div id="analysis-results-search-form-results" align="left"></div>
		</div>
	</div-->
<a href='<c:url value="/home2.html"/>'>Start a new search</a><br/><br/>
<div id="meta-heatmap-div"></div>