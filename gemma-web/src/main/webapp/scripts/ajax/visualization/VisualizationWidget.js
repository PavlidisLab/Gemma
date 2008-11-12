Ext.namespace('Gemma');

/**
 * @extends Ext.data.Store
 */

Gemma.VisualizationStore = function(config) {

	this.record = Ext.data.Record.create([{
				name : "id",
				type : "int"
			}, {
				name : "eevo"
			}, {
				name : "profiles"
			}]);

	if (config && config.readMethod)
		this.readMethod = config.readMethod;
	else
		this.readMethod = DEDVController.getDEDVForCoexpressionVisualization;

	this.proxy = new Ext.data.DWRProxy(this.readMethod);

	this.reader = new Ext.data.ListRangeReader({
				id : "id"
			}, this.record);

	Gemma.VisualizationStore.superclass.constructor.call(this, config);

};

/**
 * 
 * @class Gemma.VisualizationStore
 * @extends Ext.data.Store
 */

Ext.extend(Gemma.VisualizationStore, Ext.data.Store, {});

Gemma.SELECTED = 2; // Multiply the line thickness by this factor when it is selected in the legend
Gemma.LINE_THICKNESS = 2; // Basic line thinkness (multipled by factor to get actual thickness)
Gemma.PLOT_SIZE = 100;
Gemma.ZOOM_PLOT_SIZE = 400;
Gemma.COLD_COLORS = ["#0033FF", "#6699FF", "#3399CC", "#336666", "#66CCCC", "#33FF99", "#339966", "#009900", "#66CC66",
		"#00CC00"];

Gemma.HOT_COLORS = ["#990000", "#CC0000", "#FF0000", "#FF3366", "#FF0033", "#660000", "#FF6600", "#FF3300", "#FF6633",
		"#990033"];

Gemma.GRAPH_ZOOM_CONFIG = {
	xaxis : {
		noTicks : 0
	},
	yaxis : {
		noTicks : 0
	},
	grid : {
		labelMargin : 0
		// => margin in pixels
		// color : "white" //this turns the letters in the legend to white
	},
	shadowSize : 0,

	legend : {
		show : true,
		container : 'zoomLegend'
		// labelFormatter : formatLabel

		// position : 'nw'
		// backgroundOpacity : 0.5
	}

};
// Tests if gene is in the array of genes. uses the name of the gene to resolove
// identity.
Gemma.geneContained = function(geneName, arrayOfGenes) {
	for (var i = 0; i < arrayOfGenes.size(); i++) {
		if (arrayOfGenes[i].name === geneName)
			return true;
	}
	return false;

};

Gemma.ProfileTemplate = Ext.extend(Ext.XTemplate, {

			graphConfig : {
				lines : {
					lineWidth : 1
				},
				xaxis : {
					noTicks : 0
				},
				yaxis : {
					noTicks : 0
				},
				grid : {
					color : "white"
				},
				shadowSize : 0,

				legend : {
					show : false
				}
			},

			overwrite : function(el, values, ret) {
				Gemma.ProfileTemplate.superclass.overwrite.call(this, el, values, ret);
				for (var i = 0; i < values.length; i++) {
					var record = values[i];
					var shortName = record.eevo.shortName;
					var newDiv = Ext.DomHelper.append(shortName + '_vizwrap', {
								tag : 'div',
								id : shortName + "_vis",
								style : 'width:' + Gemma.PLOT_SIZE + 'px;height:' + Gemma.PLOT_SIZE + 'px;'
							});

					// Must use prototype extraction here -- putting in newDiv fails.
					Flotr.draw($(shortName + "_vis"), record.profiles, this.graphConfig);
				}
			}
		});

Gemma.HeatmapTemplate = Ext.extend(Ext.XTemplate, {

			overwrite : function(el, values, ret) {
				Gemma.HeatmapTemplate.superclass.overwrite.call(this, el, values, ret);
				for (var i = 0; i < values.length; i++) {
					var record = values[i];
					var shortName = record.eevo.shortName;
					var newDiv = Ext.DomHelper.append(shortName + '_vizwrap', {
								tag : 'div',
								id : shortName + "_vis",
								style : 'width:' + Gemma.PLOT_SIZE + 'px;height:' + Gemma.PLOT_SIZE + 'px;'
							});

					Heatmap.draw($(shortName + "_vis"), record.profiles);
				}
			}
		});

Gemma.VisualizationWindow = Ext.extend(Ext.Window, {
	id : 'VisualizationWindow',
	closeAction : 'destroy',
	bodyStyle : "background:white",
	layout : 'border',
	constrainHeader : true,
	title : "Visualization",
	height : Gemma.ZOOM_PLOT_SIZE,
	width : 600,
	// autoHeight : true,

	listeners : {
		show : {
			fn : function(window) {
				this.onLegendClick = function(event, component) {
					var probeId = event.getTarget().id;
					var record = window.dv.getSelectedRecords()[0];
					var profiles = record.get("profiles");

					// FIXME make legend bold so there is a user clue as to the line being bolded.
					// My attempts failed.
					// Not a good way to do this cause next time click on legend the <b> element is returned.
					// component.innerHTML = "<b>" + component.innerHTML + "</b>";
					// component.update("<b>" + component.innerHTML + "</b>");
					// component.repaint(); This bombs, no repaint method, but works.

					// Try getting compoent via ext and changing the css class of div
					// doesn't work dom not getting updated...
					// var el = Ext.get(probeId);
					// console.log(el);
					// el.toggleClass("x-grid3-row-selected");
					// el.repaint();
					// console.log(el);

					for (var i = 0; i < profiles.size(); i++) {

						if (profiles[i].labelID == probeId) {
							if (profiles[i].selected) {
								profiles[i].lines.lineWidth = profiles[i].lines.lineWidth / Gemma.SELECTED;
								profiles[i].selected = false;
							} else {
								profiles[i].selected = true;
								profiles[i].lines.lineWidth = profiles[i].lines.lineWidth * Gemma.SELECTED;

							}
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
		// If there are any compile errors with the template the error will not
		// make its way to the console.
		// Tried every combination i could think of to get the profile to
		// display...
		// Can't seem to access the data even though its there...

		this.dv = new Ext.DataView({
			autoHeight : true,
			emptyText : 'No images to display',
			loadingText : 'Loading data ...',
			store : new Gemma.VisualizationStore(),

			tpl : new Gemma.ProfileTemplate(
					'<tpl for="."><tpl for="eevo">',
					'<div class="vizWrap" id ="{shortName}_vizwrap" style="float:left; padding: 10px"> {shortName} </div>',
					'</tpl></tpl>'),

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

				// Need to transform the cordinate data from an object to an
				// array for flotr
				// probe, genes
				var flotrData = [];
				var coordinateProfile = data.profiles;

				for (var i = 0; i < coordinateProfile.size(); i++) {
					var coordinateObject = coordinateProfile[i].points;

					var probeId = coordinateProfile[i].probe.id;
					var probe = coordinateProfile[i].probe.name;
					var genes = coordinateProfile[i].genes;
					var color = coordinateProfile[i].color;
					var factor = coordinateProfile[i].factor;

					var geneNames = genes[0].name;
					for (var k = 1; k < genes.size(); k++) {
						geneNames = geneNames + "," + genes[k].name;
					}
					if (factor == 2)
						geneNames = geneNames + "*";

					var oneProfile = [];

					for (var j = 0; j < coordinateObject.size(); j++) {
						var point = [coordinateObject[j].x, coordinateObject[j].y];
						oneProfile.push(point);
					}
					var plotConfig = {
						data : oneProfile,
						color : color,
						genes : genes,
						label : probe + "=>" + geneNames,
						labelID : probeId,
						factor : factor

					};

					flotrData.push(plotConfig);
				}

				data.profiles = flotrData;
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
					html : {
						id : 'zoomLegend',
						tag : 'div',
						style : 'width:' + Gemma.PLOT_SIZE + 'px;height:' + Gemma.PLOT_SIZE + 'px; float:left;'
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
					// stateful : false,
					listeners : {
						resize : {
							fn : function(component, adjWidth, adjHeight, rawWidth, rawHeight) {

								// Change the div so that it is the size of the panel surrounding it.
								zoomPanelDiv = Ext.get('graphzoompanel');
								zoomPanelDiv.setHeight(rawHeight - 27);
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
							var window = this.findParentByType(Gemma.VisualizationWindow)
							var record = window.dv.getSelectedRecords()[0];
							// This gets called because window gets resized at startup.
							if (record == null)
								return;
							profiles = record.get("profiles");
						}

						Flotr.draw($('graphzoompanel'), profiles, Gemma.GRAPH_ZOOM_CONFIG);

					},

					displayWindow : function(eevo, profiles) {

						this.setTitle("Visualization of probes in dataset:  " + eevo.shortName);

						if (!this.isVisible()) {
							this.setVisible(true);
							this.show();
						}

						// Change color to hot and cold variety
						var coldGeneName = profiles[0].genes[0].name;
						var coldIndex = 0, hotIndex = 0;

						var sortedHotProfile = [];

						for (var i = 0; i < profiles.size(); i++) {
							var fade = profiles[i].factor < 2;
							if (Gemma.geneContained(coldGeneName, profiles[i].genes)) {
								if (fade) {
									profiles[i].color = "#DDDDDD";
								} else {
									// profiles[i].color = Gemma.COLD_COLORS[coldIndex];
									profiles[i].color = "#000000";
								}

								coldIndex++;
							} else {
								if (fade) {
									profiles[i].color = "#FFDDDD";
								} else {
									// profiles[i].color = Gemma.HOT_COLORS[hotIndex];
									profiles[i].color = "#FF0000";
								}
								hotIndex++;
							}
							profiles[i].lines = {
								// lineWidth : Gemma.LINE_THICKNESS*profiles[i].factor
								lineWidth : Gemma.LINE_THICKNESS
							};

						}

						// sort array by gene
						profiles.sort(function(a, b) {

								//sorts data by importance 1st
								if (a.factor > b.factor )
										return 1;									
								else if (a.factor < b.factor)
										return -1;

								//if equal importance than sort by gene name
								else {
									if (a.genes[0].name > b.genes[0].name)
										return 1;
								    else if (a.genes[0].name < b.genes[0].name)
										return -1;
									else return (a.labelID > b.labelID);
									
								}
						});

						Flotr.draw($('graphzoompanel'), profiles, Gemma.GRAPH_ZOOM_CONFIG);

					}

				});

		Ext.apply(this, {
					items : [this.thumbnailPanel, this.zoomPanel]
				});

		Gemma.VisualizationWindow.superclass.initComponent.call(this);

	},

	displayWindow : function(eeIds, queryGene, coexpressedGene) {

		this.setTitle("Visualization of query gene: " + queryGene.officialSymbol + " with coexpressed gene <b> "
				+ coexpressedGene.officialSymbol + "</b>");

		this.thumbnailPanel.setTitle(queryGene.officialSymbol + " (red) with " + coexpressedGene.officialSymbol
				+ " (black)");

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

// -----------------------------------------------------
// FIXME: this could be abstracted out better resolving differences with a config object on creation
// Differences are: Read method (dwr method is different) calls getDEDVForVisualization not
// getDEDVForCoexpressionVisualization
// Calling load takes different paramaters (an array of genes ids not just two gene ids)
// Titles of thumbnail panel is different (only 1 gene not 2 in title)
// Don't need hot and cold colors differentiaion (just remove color and let flotr decide)
// -----------------------------------------------------

Gemma.VisualizationDifferentialWindow = Ext.extend(Ext.Window, {
	id : 'VisualizationDifferentialWindow',
	closeAction : 'destroy',
	bodyStyle : "background:white",
	layout : 'border',
	constrainHeader : true,
	stateful : false,
	title : "Visualization",
	height : Gemma.ZOOM_PLOT_SIZE,
	width : 600,
	// autoHeight : true,

	listeners : {
		show : {
			fn : function(window) {
				this.onLegendClick = function(event, component) {
					var probeId = event.getTarget().id;
					var record = window.dv.getSelectedRecords()[0];
					var profiles = record.get("profiles");

					// FIXME make legend bold so there is a user clue as to the line being bolded.
					// My attempts failed.
					// Not a good way to do this cause next time click on legend the <b> element is returned.
					// component.innerHTML = "<b>" + component.innerHTML + "</b>";
					// component.update("<b>" + component.innerHTML + "</b>");
					// component.repaint(); This bombs, no repaint method, but works.

					// Try getting compoent via ext and changing the css class of div
					// doesn't work dom not getting updated...
					// var el = Ext.get(probeId);
					// console.log(el);
					// el.toggleClass("x-grid3-row-selected");
					// el.repaint();
					// console.log(el);

					for (var i = 0; i < profiles.size(); i++) {

						if (profiles[i].labelID == probeId) {
							if (profiles[i].lines == null) {
								profiles[i].lines = {
									lineWidth : 5
								};
							} else {
								profiles[i].lines = null;
							}
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
		this.dv = new Ext.DataView({
			autoHeight : true,
			emptyText : 'No images to display',
			loadingText : 'Loading data ...',
			store : new Gemma.VisualizationStore({
						readMethod : DEDVController.getDEDVForDiffExVisualization
					}),

			tpl : new Gemma.ProfileTemplate(
					'<tpl for="."><tpl for="eevo">',
					'<div class="vizWrap" id ="{shortName}_vizwrap" style="float:left; padding: 10px"> {shortName} </div>',
					'</tpl></tpl>'),

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

				for (var i = 0; i < coordinateProfile.size(); i++) {
					var coordinateObject = coordinateProfile[i].points;

					var probeId = coordinateProfile[i].probe.id;
					var probe = coordinateProfile[i].probe.name;
					var genes = coordinateProfile[i].genes;
					var color = coordinateProfile[i].color;
					var factor = coordinateProfile[i].factor;

					var geneNames = genes[0].name;
					for (var k = 1; k < genes.size(); k++) {
						geneNames = geneNames + "," + genes[k].name;
					}

					var oneProfile = [];

					for (var j = 0; j < coordinateObject.size(); j++) {
						var point = [coordinateObject[j].x, coordinateObject[j].y];
						oneProfile.push(point);
					}
					var plotConfig = {
						data : oneProfile,
						color : color,
						genes : genes,
						label : probe + "=>" + geneNames,
						labelID : probeId,
						factor : factor
					};

					flotrData.push(plotConfig);
				}

				data.profiles = flotrData;
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
					html : {
						id : 'zoomLegend',
						tag : 'div',
						style : 'width:' + Gemma.PLOT_SIZE + 'px;height:' + Gemma.PLOT_SIZE + 'px; float:left;'
					}

				});

		this.zoomPanel = new Ext.Panel({

					region : 'center',
					split : true,
					width : Gemma.ZOOM_PLOT_SIZE,
					height : Gemma.ZOOM_PLOT_SIZE,
					stateful : false,
					id : 'visualization-zoom-window',
					closeAction : 'destroy',
					bodyStyle : "background:white",
					constrainHeader : true,
					layout : 'fit',
					title : "Click thumbnail to zoom in",
					// hidden : true,
					// stateful : false,
					listeners : {
						resize : {
							fn : function(component, adjWidth, adjHeight, rawWidth, rawHeight) {

								// Change the div so that it is the size of the panel surrounding it.
								zoomPanelDiv = Ext.get('graphzoompanel');
								zoomPanelDiv.setHeight(rawHeight - 27);
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
							var window = this.findParentByType(Gemma.VisualizationDifferentialWindow)
							var record = window.dv.getSelectedRecords()[0];
							// This gets called because window gets resized at startup.
							if (record == null)
								return;
							profiles = record.get("profiles");
						}

						Flotr.draw($('graphzoompanel'), profiles, Gemma.GRAPH_ZOOM_CONFIG);

					},

					displayWindow : function(eevo, profiles) {

						this.setTitle("Visualization of probes in dataset:  " + eevo.shortName);

						if (!this.isVisible()) {
							this.setVisible(true);
							this.show();
						}
						for (var i = 0; i < profiles.size(); i++) {
							var fade = profiles[i].factor < 2;
							if (fade) {
								profiles[i].color = "#FFDDDD";
							} else {
								profiles[i].color = "#FF0000";
							}

						}

						Flotr.draw($('graphzoompanel'), profiles, Gemma.GRAPH_ZOOM_CONFIG);

					}

				});

		Ext.apply(this, {
					items : [this.thumbnailPanel, this.zoomPanel]
				});

		Gemma.VisualizationDifferentialWindow.superclass.initComponent.call(this);

	},

	displayWindow : function(eeIds, gene, threshold, factorMap) {

		this.setTitle("Visualization of gene: " + gene.officialSymbol);

		this.thumbnailPanel.setTitle(gene.officialSymbol);

		var params = [];
		params.push(eeIds);
		params.push([gene.id]);
		params.push(threshold);
		params.push(factorMap);

		this.show();
		this.dv.store.load({
					params : params
				});

	}

});
