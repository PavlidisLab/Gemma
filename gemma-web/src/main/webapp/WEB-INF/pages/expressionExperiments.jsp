
<%@ include file="/common/taglibs.jsp"%>
<%-- 

Display table of expression experiments.
$Id$ 
--%>
<head>
	<title><fmt:message key="expressionExperiments.title" />
	</title>
</head>

<div id="messages" style="margin: 10px; width: 400px">
	${message}
</div>
<div id="taskId" style="display: none;"></div>
<div id="progress-area" style="padding: 15px;"></div>

<script type="text/javascript">
	Ext.state.Manager.setProvider(new Ext.state.CookieProvider( ));
	Ext.onReady(function(){
		Ext.QuickTips.init();
		// this is to overcome a vbox-collapse bug
		// see overrides.js for more
		function onExpandCollapse(c) {
				//Horrible Ext 2.* collapse handling has to be defeated...
		        if (c.queuedBodySize) {
		            delete c.queuedBodySize.width;
		            delete c.queuedBodySize.height;
		        }
		        var parent =c.findParentByType('panel');
		        if(parent) {parent.doLayout();}
		}
		
		var summaryPanel = new Gemma.ExpressionExperimentsSummaryPanel({
			height:260,
			flex:'0',
			listeners: {
				expand: onExpandCollapse,
				collapse: onExpandCollapse
			}
		});

		var eeGrid = new Gemma.ExperimentPagingGrid({
		 	flex:1,
			listeners: {
				expand: onExpandCollapse,
				collapse: onExpandCollapse
			}
		});
	
		summaryPanel.on('showExperimentsByIds', function(ids){
			eeGrid.loadExperimentsById(ids);
		});
		summaryPanel.on('showExperimentsByTaxon', function(ids){
			eeGrid.loadExperimentsByTaxon(ids);
		});
	
		var mainPanel = new Ext.Panel({
				layout:'vbox',
				layoutConfig:{
					align: 'stretch'
				},
		 		align: 'stretch',
		 			items:[ summaryPanel, eeGrid]
			 	});
		
		var viewPort = new Gemma.GemmaViewPort({
		 	centerPanelConfig: mainPanel
		});
		
	});


</script>

<input type="hidden" id="reloadOnLogout" value="false">