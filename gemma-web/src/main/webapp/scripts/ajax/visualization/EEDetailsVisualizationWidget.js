Ext.namespace('Gemma');

var m_profiles =[];	//Data returned by server for visulization (alread processed by client)
var m_eevo;
var m_geneIds;
var m_geneSymbols;

HEATMAP_CONFIG = {
		xaxis : {
			noTicks : 0
		},
		yaxis : {
			noTicks : 0
		},
		grid : {
			labelMargin : 0,
			marginColor : "white"
			// => margin in pixels
			// color : "white" //this turns the letters in the legend to white
		},
		shadowSize : 0,
	
		label : true	
};

Gemma.EEDetailsVisualizationWindow = Ext.extend(Ext.Window, {

	height :100,
	width : 220,
	listeners : {
				resize : {
							fn : function(component, adjWidth, adjHeight, rawWidth, rawHeight) {

								// Change the div so that it is the size of the panel surrounding it.
								vizDiv = Ext.get('vizDiv');
								if (!vizDiv)
									return;
								vizDiv.setHeight(rawHeight - 27); // magic 27
								vizDiv.setWidth(rawWidth - 1);
								vizDiv.repaint();

								component.vizPanel.refreshWindow();

							}.createDelegate(this)
						}
					},
					
	dedvCallback : function(data){
	
			
				// Need to transform the coordinate data from an object to an
				// array for flotr/HeatMap display
		
				var flotrData = [];
				var coordinateProfile = data[0].data.profiles;

				//this.setTitle("Visualization of gene: <a   target='_blank' ext:qtip=' "+ gene.officialSymbol+ " ' href='/Gemma/gene/showGene.html?id=" + gene.id + "'> " + gene.officialName + "</a>");
	

				//Set the height of the window based on data
//				var window = this.findParentByType(Gemma.EEDetailsVisualizationWindow);
//				window.setHeight(coordinateProfile.size()*20);

				for (var i = 0; i < coordinateProfile.size(); i++) { 
					var coordinateObject = coordinateProfile[i].points;

					var probeId = coordinateProfile[i].probe.id;
					var probe = coordinateProfile[i].probe.name;
					var genes = coordinateProfile[i].genes;

					var geneNames = genes[0].name;
					for (var k = 1; k < genes.size(); k++) {
						geneNames = geneNames + "," + genes[k].name;
					}

					// turn data points into a structure usuable by heatmap
					var oneProfile = [];
					for (var j = 0; j < coordinateObject.size(); j++) {
						var point = [coordinateObject[j].x, coordinateObject[j].y];
						oneProfile.push(point);
					}

					var plotConfig = {
						data : oneProfile,
						genes : genes,
						label : probe + " (" + geneNames + ")",
						labelID : probeId,
						lines : {
							lineWidth : Gemma.LINE_THICKNESS
						},
						//Needs to be added so switching views work 
						probe : { id : probeId, name : probe},
						points : coordinateObject


					};

					flotrData.push(plotConfig);
				}

				flotrData.sort(Gemma.graphSort);
				m_profiles = flotrData;
				m_eevo = data[0].data.eevo;

				var downloadDedvLink =  String.format("<a ext:qtip='Download raw data in a tab delimted format'  target='_blank'  href='/Gemma/dedv/downloadDEDV.html?ee={0} &g={1}' > [download raw data]</a>",
				m_eevo.id,m_geneIds);
				this.setTitle( "Visualization of " +m_geneSymbols + " in "+ m_eevo.shortName +"<br>" + downloadDedvLink);
				
				
				Heatmap.draw(Ext.get('vizDiv'), m_profiles, HEATMAP_CONFIG);
				
				//$("heatmapLoadMask").loadMask.hide();
				this.loadMask.hide();

			
	},
	
	displayWindow : function(eeId, genes) {

		var geneIds =[];
		var geneSymbols = [];
		
		for(var i= 0; i<genes.size(); i++){
			geneIds.push(genes[i].id);
			geneSymbols.push(genes[i].officialSymbol);
		}
		
		var params = [];
		params.push([eeId]);
		params.push(geneIds);
		m_geneIds =  geneIds;
		m_geneSymbols = geneSymbols;

		this.dv.store.load({
					params : params,
					callback : this.dedvCallback.createDelegate(this)
				});
				
		this.show();
		
		Ext.apply(this, {
			loadMask : new Ext.LoadMask(this.getEl(), {
				id : "heatmapLoadMask",
				msg : "Loading probe level data ..."
			})
		});

		this.loadMask.show();


	},
	
	initComponent : function() {

		
		this.dv = new Ext.DataView({
			autoHeight : true,
			emptyText : 'Unable to visualize missing data',
			loadingText : 'Loading data ...',
			store : new Gemma.VisualizationStore({
						readMethod : DEDVController.getDEDVForVisualization
					})
		});


		this.vizPanel = new Ext.Panel({

					id : 'visualizationWindow',
					closeAction : 'destroy',
					bodyStyle : "background:white",
					constrainHeader : true,
					layout : 'fit',
					// hidden : true,
					stateful : false,
					autoScroll : true,

					html : {
						id : 'vizDiv',
						tag : 'div',
						style : 'width:' + 220 + 'px;height:' + 100 + 'px; margin:5px 2px 2px 5px;'
					},

					refreshWindow : function(data) {
						// Should redraw to fit current window width and hight.

						if (data == null) {
							data = m_profiles;
						}

						$('vizDiv').innerHTML = '';
				
						Heatmap.draw($('vizDiv'), data, HEATMAP_CONFIG);

						},

					displayWindow : function(eevo, data) {

						if (data == null)
							data = m_profiles;
							
						if (eevo == null)
							eevo = m_eevo;
							
						this.setTitle("<a   target='_blank' href='/Gemma/expressionExperiment/showExpressionExperiment.html?id=" +eevo.id+ " '> " + eevo.shortName + "</a>: "  +eevo.name);

						if (!this.isVisible()) {
							this.setVisible(true);
							this.show();
						}

							$('vizDiv').innerHTML = '';
							Heatmap.draw($('vizDiv'), data, HEATMAP_CONFIG);
						
	
					}

				});

		Ext.apply(this, {
					items : [ this.vizPanel]
				});

		Gemma.EEDetailsVisualizationWindow.superclass.initComponent.call(this);

	}

	
});

Gemma.EEDetailsVisualizationWidget = Ext.extend(Ext.Panel, {

	layout : 'border',
	width : 390,
	height : 360,
	frame : true,

	
	visualizeHandler : function(){
		//Get expressionExperiment ID
		//Get Gene IDs
		//DedvController.
			// destroy if already open
				if (this.visWindow) {
					this.visWindow.close();
				}

				this.visWindow = new Gemma.EEDetailsVisualizationWindow({
				});
//				this.visWindow.setHeight(100);
//				this.visWindow.setWidth(220);
				
				
				var geneList = this.geneChooserPanel.getGenes();
				
				if (geneList.size() < 1){
					Ext.Msg.alert('Status', 'Please select a gene first');
					return;

				}
				var eeId = Ext.get("eeId").getValue();
				
				this.visWindow.displayWindow(eeId, geneList);
	},
	
	
	
	onRender : function() {
		Gemma.EEDetailsVisualizationWidget.superclass.onRender.apply(this, arguments);

		Ext.apply(this, {
			loadMask : new Ext.LoadMask(this.getEl(), {
				id : "geneLoadMask",
				msg : "Preparing Coexpression Interface  ..."
			})
		});

		this.loadMask.show();

	},
	
	initComponent : function() {

		this.geneChooserPanel = new Gemma.GeneChooserPanel({
			height : 100,
			width : 400,
			region : 'center',
			id : 'gene-chooser-panel'
		});

		var allPanel = new Ext.Panel({
			renderTo : 'visualization',
			title : "Visualization",
			layout : 'table',
			baseCls : 'x-plain-panel',
			autoHeight : true,
			width : 400,
			layoutConfig : {
				columns : 2
			},
			items : [this.geneChooserPanel],
			buttons : [{
					id : "visualizeButton",
					text : "Visualize",
					handler : this.visualizeHandler.createDelegate(this, [], false)
				}],
			enabled : false
		});

	Gemma.EEDetailsVisualizationWidget.superclass.initComponent.call(this);
		

		this.geneChooserPanel.on("addgenes", function(geneids) {
			if (this.geneChooserPanel.getGeneIds().length > 1) {
				var cmp = Ext.getCmp("visualizeButton");
				cmp.enable();
			}

		}, this);

		this.geneChooserPanel.on("removegenes", function() {
			if (this.geneChooserPanel.getGeneIds().length < 1) {
				var cmp = Ext.getCmp("visualizeButton");
				//cmp.setValue(false);
				cmp.disable();
			} 
		}, this);

		/*
		 * This horrible mess. We listen to taxon ready event and filter the presets on the taxon.
		 */
		this.geneChooserPanel.toolbar.taxonCombo.on("ready", function(taxon) {

			$("geneLoadMask").loadMask.hide();

			}.createDelegate(this), this);
	}

}//initComponent



);