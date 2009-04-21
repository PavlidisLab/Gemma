Ext.namespace('Gemma');

Gemma.ZOOM_PLOT_SIZE = 400;	//This constant doesn't get picked up intime from VisualizationWidget.js

Gemma.CoexpressionVisualizationWindow = Ext.extend(Ext.Window, {
	id : 'CoexpressionVisualizationWindow',
	closeAction : 'destroy',
	bodyStyle : "background:white",
	layout : 'border',
	constrainHeader : true,
	title : "Visualization",
	height : Gemma.ZOOM_PLOT_SIZE,
	width : 600,
	stateful : false,

	listeners : {
		show : {
			fn : function(window) {
				this.onLegendClick = function(event, component) {
					var probeId = event.getTarget().id;
					var record = window.dv.getSelectedRecords()[0];
					var profiles = record.get("profiles");

					// FIXME make legend bold so there is a user clue as to the line being bolded.

					for (var i = 0; i < profiles.size(); i++) {

						if (profiles[i].labelID == probeId) {
							if (profiles[i].selected) {
								profiles[i].lines.lineWidth = profiles[i].lines.lineWidth / Gemma.SELECTED;
								profiles[i].selected = false;
							} else {
								profiles[i].selected = true;
								profiles[i].lines.lineWidth = profiles[i].lines.lineWidth * Gemma.SELECTED;
							}
							break;
						}
					}

					window.zoomPanel.refreshWindow(profiles);

				};

				var zoomLegendDiv = Ext.get("zoomLegend");
				zoomLegendDiv.on('click', this.onLegendClick.createDelegate(this));

			}.createDelegate(this)
		}
	},

	initComponent : function() {

		var template = Gemma.getTemplate();
		
		this.dv = new Ext.DataView({
			autoHeight : true,
			emptyText : 'Unable to visualize missing data',
			loadingText : 'Loading data ...',
			store : new Gemma.VisualizationStore(),	

			tpl : template,
			
			setTemplate : function(tpl, refresh){
				//TODO factor this out and create custom DataView (also in DiffExpressionVisualizationWidget)
				this.tpl = tpl;
					if(refresh){
						this.refresh();
					}else{
						var sel = this.getSelectedIndexes();
						this.tpl.overwrite(this.el, this.collectData(this.store.getRange(), 0));
						this.all.fill(Ext.query(this.itemSelector, this.el.dom));
						this.updateIndexes(0);
						this.select(sel);
					}
				},

			
			listeners : {
				selectionchange : {
					fn : function(dv, nodes) {

						var record = dv.getRecords(nodes)[0];
						var eevo = record.get("eevo");
						var profiles = record.get("profiles");

						// An attempt to hide the zoom panel and have it expand out nicely... no luck *sigh*
						if (!this.zoomPanel.isVisible()) {
							this.setWidth(Gemma.PLOT_SIZE + Gemma.ZOOM_PLOT_SIZE);
							this.zoomPanel.show();
						}
						this.zoomPanel.displayWindow(eevo, profiles);

					}.createDelegate(this)
				}
			},

			singleSelect : true,

			itemSelector : 'div.vizWrap',

			prepareData : function(data) {

				// Need to transform the coordinate data from an object to an
				// array for flotr
				// probe, genes
				var flotrData = [];
				var coordinateProfile = data.profiles;
				var coldGeneName = coordinateProfile[0].genes[0].name;
				var coldIndex = 0, hotIndex = 0;

				for (var i = 0; i < coordinateProfile.size(); i++) { 
					var coordinateObject = coordinateProfile[i].points;

					var probeId = coordinateProfile[i].probe.id;
					var probe = coordinateProfile[i].probe.name;
					var genes = coordinateProfile[i].genes;
					var color = coordinateProfile[i].color;
					var factor = coordinateProfile[i].factor;
					var fade = factor < 2;

					var geneNames = genes[0].name;
					for (var k = 1; k < genes.size(); k++) {
						geneNames = geneNames + "," + genes[k].name;
					}
					if (factor == 2) {
						geneNames = geneNames + "*";
					}

					// turn data points into a structure usuable by flotr
					var oneProfile = [];
					for (var j = 0; j < coordinateObject.size(); j++) {
						var point = [coordinateObject[j].x, coordinateObject[j].y];
						oneProfile.push(point);
					}

					if (fade) {
						color = color == 'red' ? Gemma.HOT_FADE_COLOR : Gemma.COLD_FADE_COLOR;
					}

					var plotConfig = {
						data : oneProfile,
						color : color,
						genes : genes,
						label : probe + " (" + geneNames + ")",
						labelID : probeId,
						factor : factor,
						lines : {
							lineWidth : Gemma.LINE_THICKNESS
						},
						//Needs to be added so switching views work 
						probe : { id : probeId, name : probe},
						points : coordinateObject


					};

					flotrData.push(plotConfig);
				}
				data.profiles = flotrData;

				// Sort data so that greyed out lines get drawn 1st (don't overlap signifigant probes)
				data.profiles.sort(Gemma.graphSort);

				return data;
			}
		});

		this.thumbnailPanel = new Ext.Panel({
					title : 'query gene (red) with coexpressed gene (black)',
					region : 'west',
					split : true,
					width : Gemma.PLOT_SIZE + 50,
					collapsible : true,
					margins : '3 0 3 3',
					cmargins : '3 3 3 3',
					items : this.dv,
					autoScroll : true,
					stateful : false,
					html : {
						id : 'zoomLegend',
						tag : 'div',
						style : 'width:' + Gemma.PLOT_SIZE + 'px;height:' + Gemma.PLOT_SIZE + 'px; float:left;',
						html : "legend"
					}

				});

		this.zoomPanel = new Ext.Panel({

					region : 'center',
					split : true,
					width : Gemma.ZOOM_PLOT_SIZE,
					height : Gemma.ZOOM_PLOT_SIZE,
					id : 'visualization-zoom-window',
					closeAction : 'destroy',
					bodyStyle : "background:white",
					constrainHeader : true,
					layout : 'fit',
					title : "Click thumbnail to zoom in",
					// hidden : true,
					stateful : false,
					autoScroll : Gemma.HEATMAP_VIEW ? true : false,
					listeners : {
						resize : {
							fn : function(component, adjWidth, adjHeight, rawWidth, rawHeight) {

								// Change the div so that it is the size of the panel surrounding it.
								zoomPanelDiv = Ext.get('graphzoompanel');
								zoomPanelDiv.setHeight(rawHeight - 27); // magic 27
								zoomPanelDiv.setWidth(rawWidth - 1);
								zoomPanelDiv.repaint();

								component.refreshWindow();

							}.createDelegate(this)
						}
					},

					html : {
						id : 'graphzoompanel',
						tag : 'div',
						style : 'width:' + Gemma.ZOOM_PLOT_SIZE + 'px;height:' + Gemma.ZOOM_PLOT_SIZE + 'px;'
					},

					refreshWindow : function(profiles) {
						// Should redraw to fit current window width and hight.

						if (profiles == null) {
							var window = this.findParentByType(Gemma.CoexpressionVisualizationWindow)
							var record = window.dv.getSelectedRecords()[0];
							// This gets called because window gets resized at startup.
							if (record == null)
								return;
							profiles = record.get("profiles");
						}

						if (Gemma.HEATMAP_VIEW){
							$('graphzoompanel').innerHTML = '';
							Heatmap.draw($('graphzoompanel'), profiles, Gemma.GRAPH_ZOOM_CONFIG);
						}
						else {
							Flotr.draw($('graphzoompanel'), profiles, Gemma.GRAPH_ZOOM_CONFIG);

						}
					},

					displayWindow : function(eevo, profiles) {

						this.setTitle("<a   target='_blank' href='/Gemma/expressionExperiment/showExpressionExperiment.html?id=" +eevo.id+ " '> " + eevo.shortName + "</a>: "  +eevo.name);

						if (!this.isVisible()) {
							this.setVisible(true);
							this.show();
						}

						if (Gemma.HEATMAP_VIEW){
							$('graphzoompanel').innerHTML = '';
							Heatmap.draw($('graphzoompanel'), profiles, Gemma.GRAPH_ZOOM_CONFIG);
						}
						else{
							Flotr.draw($('graphzoompanel'), profiles, Gemma.GRAPH_ZOOM_CONFIG);

						}
	
					}

				});

		Ext.apply(this, {
					items : [this.thumbnailPanel, this.zoomPanel],
					buttons : [{
						text : "Switch View",
						id : "toggleView",
						handler : this.switchView.createDelegate(this, [], false)
						}]
				});

		Gemma.CoexpressionVisualizationWindow.superclass.initComponent.call(this);

	},
	
	switchView : function(){

		//TODO: change title in zoompanel (remove red and black info about genes)
		//TODO: change info on button to relfect curent state of viewing
		toggleButton = Ext.get("toggleView");

		if (Gemma.HEATMAP_VIEW){
			//toggleButton.setText("HeatMap View");
//			this.thumbnailPanel.setTitle(queryGene.officialSymbol + " (red) with " + coexpressedGene.officialSymbol
//				+ " (black)  <br>" + downloadDedvLink);
			Gemma.HEATMAP_VIEW = false;
		}
		else{
			//toggleButton.setText("Graph View")
//			this.thumbnailPanel.setTitle(queryGene.officialSymbol + " with " + coexpressedGene.officialSymbol
//				+ " <br> " + downloadDedvLink);
				
			var zoomLegendDiv = $("zoomLegend");
			if (zoomLegendDiv){
				zoomLegendDiv.innerHTML = '';
			}
			
			Gemma.HEATMAP_VIEW = true;
		}
		
		var template = Gemma.getTemplate();
		
		this.dv.setTemplate ( template, false);
		 
		
	},

	displayWindow : function(eeIds, queryGene, coexpressedGene) {
					
		
		this.setTitle("Visualization of query gene: <a   target='_blank' ext:qtip=' "+ queryGene.officialName+ " ' href='/Gemma/gene/showGene.html?id=" + queryGene.id + "'> " + queryGene.officialSymbol
		+ "</a> with coexpressed gene <a  target='_blank' ext:qtip=' "+ coexpressedGene.officialName+ " ' href='/Gemma/gene/showGene.html?id=" + coexpressedGene.id + "'> " + coexpressedGene.officialSymbol + "</a>"	 );

		var downloadDedvLink =  String.format("<a ext:qtip='Download coexpression data in a tab delimted format'  target='_blank'  href='/Gemma/dedv/downloadDEDV.html?ee={0} &g={1},{2}' > [download raw data]</a>",
				eeIds, queryGene.id, coexpressedGene.id);

		this.thumbnailPanel.setTitle(queryGene.officialSymbol + " (red) with " + coexpressedGene.officialSymbol
				+ " (black)  <br>" + downloadDedvLink);

				
				
				
		var params = [];
		params.push(eeIds);
		params.push(queryGene.id);
		params.push(coexpressedGene.id);
		this.show();
		this.dv.store.load({
					params : params
				});

	}

});

