//Ext.namespace('Gemma');
///**
// * Panel for meta-heatmap visualization and manipulation. Includes main visualization pane and filter/sort/zoom side bar.
// * 
// * @version $Id$
// * @see 
// */
//
//
//Gemma.MetaHeatmapNavigationPanel = Ext.extend(Ext.Panel, {
//	initComponent: function() {
//		var config = {
//				height: 300,
//				layout:'hbox',
//				layoutConfig: {
//				    align : 'stretch',
//				   
//				}
//		};
//		
//		Ext.apply(this, Ext.apply(this.initialConfig, config));		 
//	    Gemma.MetaHeatmapNavigationPanel.superclass.initComponent.apply(this, arguments);
//			    
//	    var VisualizationArgs = {};
//	    VisualizationArgs.geneGroups = [];
//	    VisualizationArgs.datasetGroups = [];
//	    var taxonCombo = new Gemma.TaxonCombo();
//
//	    var genePickerPanel = new Ext.Panel({ width: 300, height: 299})
//		var datasetPickerPanel = new Ext.Panel({ width: 300, height: 299})
//
//		var addNewGeneGroupButton = new Ext.Button ({text:'Add gene group',width:298});
//		genePickerPanel.add(addNewGeneGroupButton);    
//		var addNewDatasetGroupButton = new Ext.Button ({text:'Add dataset group',width:298});
//		datasetPickerPanel.add(addNewDatasetGroupButton);    
//
//		addNewGeneGroupButton.on("click", function(event) {
//		  	var genePicker = new Gemma.GeneGroupCombo( {
//				width : 298
//			});
//			genePicker.setTaxon(taxonCombo.getTaxon());					
//			VisualizationArgs.geneGroups.push(genePicker);
//			genePickerPanel.add(genePicker);
//			genePickerPanel.doLayout();
//			}, this);
//
//		addNewDatasetGroupButton.on("click", function(event) {
//		  	var datasetPicker = new Gemma.DatasetGroupCombo( {
//				width : 298
//			});
//			VisualizationArgs.datasetGroups.push(datasetPicker);
//			datasetPickerPanel.add(datasetPicker);
//			datasetPickerPanel.doLayout();
//			}, this);
//
//		
//		var goButton = new Ext.Button ({text:'Visualize!'});
//	    
//		goButton.on("click", function(event) {
//				var geneGroups =[];
//				for (var i=0; i < VisualizationArgs.geneGroups.length; i++) {
//					geneGroups.push(VisualizationArgs.geneGroups[i].getGeneGroup().id);  
//			}
//			
//			var datasetGroups = [];
//				for (var i=0; i < VisualizationArgs.datasetGroups.length; i++) {
//					datasetGroups.push(VisualizationArgs.datasetGroups[i].getSelected().id);  
//			}
//
//			alert("Sending dataset "+datasetGroups.length + "num gene groups"+geneGroups.length);
//			
//		    DifferentialExpressionSearchController.getDataForNewVisualizationTest(taxonCombo.getSelected().id,datasetGroups, geneGroups, function(data) {
//		    	data.datasetGroupLabels = [];
//					for (var i=0; i < VisualizationArgs.datasetGroups.length; i++) {
//						data.datasetGroupLabels.push(VisualizationArgs.datasetGroups[i].getSelected().get("name"));  
//				}
//				alert("size:"+data.datasetGroupLabels.length+ " 0:" +data.datasetGroupLabels[0]);    		
//		    	heatmap = new org.systemsbiology.visualization.BioHeatMap(document.getElementById('bio-heat'));
//		    	heatmap.draw(data, {drawBorder:false });
//
//		   		});
//			}, this);
//
//		this.add(taxonCombo);
//		this.add(genePickerPanel);
//		this.add(datasetPickerPanel);
//		this.add(goButton);
//		this.doLayout();					
//	},
//	
//	onRender:function() {
//		Gemma.MetaHeatmapNavigationPanel.superclass.onRender.apply(this, arguments);
//	}		
//
//});
//    		
//Gemma.MetaHeatmapVisualizationPanel = Ext.extend(Ext.Panel, {
//	initComponent: function() {
//		var config = {
//				autoScroll: true,
//				items: [{html: "<div id=\"bio-heat\" />"}]
//		};
//		Ext.apply(this, Ext.apply(this.initialConfig, config));		 
//		Gemma.MetaHeatmapVisualizationPanel.superclass.initComponent.apply(this, arguments);
//	
//	},
//	onRender:function() {
//		Gemma.MetaHeatmapVisualizationPanel.superclass.onRender.apply(this, arguments);
//		
//	}		
//
//});
//
//Ext.reg('app_panel', Gemma.MetaHeatmapNavigationPanel);
//Ext.reg('viz_panel', Gemma.MetaHeatmapVisualizationPanel);
//
//Gemma.MetaHeatmapPanel = Ext.extend(Ext.Panel, {
//	initComponent: function(){
//		var config = {
//				title: 'asdasdasda',
//				id: 'sdsds',
//				height: 800,
//				layout:'border',
//				defaults: {
//					collapsible: true,
//					split: true,
//					bodyStyle: 'padding:15px'
//				},
//				items: [{
//					xtype: 'app_panel',
//					title: 'Navigation',
//					region:'north',
//					margins: '0 0 0 0',
//					cmargins: '0 0 0 0',
//					height: 200,
//					minSize: 100,
//					maxSize: 250
//				},{
//					xtype: 'viz_panel',
//					title: 'Main Content',					
//					collapsible: false,
//					region:'center',
//					margins: '0 0 0 0'
//				}],
//		}
//				
//		Ext.apply(this, Ext.apply(this.initialConfig, config));		
//		Gemma.MetaHeatmapPanel.superclass.initComponent.apply(this, arguments);
//		
//		},
//		
//		onRender:function() {
//		Gemma.MetaHeatmapPanel.superclass.onRender.apply(this, arguments);
//		}		
//
//});