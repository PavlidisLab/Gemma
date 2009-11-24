/*
 * The Gemma project
 * 
 * Copyright (c) 2009 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 * 
 */
Ext.namespace('Gemma');

Gemma.ZOOM_PLOT_SIZE = 400;
Gemma.DIFFEXVIS_QVALUE_THRESHOLD = 0.05;

// Multiply the line thickness by this factor when it is
// selected in the legend
Gemma.SELECTED = 2;
Gemma.LINE_THICKNESS = 1;
Gemma.ZOOM_LINE_THICKNESS = 2;
Gemma.THUMBNAIL_PLOT_SIZE = 120;

Gemma.HOT_FADE_COLOR = "#FFDDDD";
// Gemma.HOT_FADE_SELECTED_COLOR = "#FFBBBB";
Gemma.COLD_FADE_COLOR = "#DDDDDD";
// Gemma.COLD_FADE_SELECTED_COLOR = "#BBBBBB";

Gemma.MAX_LABEL_LENGTH_CHAR = 12;
Gemma.MAX_THUMBNAILLABEL_LENGTH_CHAR = 30;
Gemma.MAX_EE_NAME_LENGTH = 40;

/**
 * Vertical strip of plots, with a legend. Supports little heatmaps or little linecharts.
 */
Gemma.DataVectorThumbnailsView = Ext.extend(Ext.DataView, {
			autoHeight : true,
			emptyText : 'No data',
			loadingText : 'Loading data ...',
			name : "vectorDV",

			singleSelect : true,

			itemSelector : 'div.vizWrap',

			/**
			 * The data get from the server is not compatible with flotr out-of-the box. A little transformation is
			 * needed.
			 * 
			 * @param {}
			 *            data for one record.
			 * @return {}
			 * @overrride
			 */
			prepareData : function(data) {
				return Gemma.prepareProfiles(data);
			},

			/**
			 * Gets the selected node's record; or, if no node is selected, returns the first record; or null if there
			 * are no nodes.
			 * 
			 * @return {record}
			 */
			getSelectedOrFirst : function() {
				if (this.getSelectionCount() > 0) {
					return this.getSelectedRecords()[0];
				} else {
					var node = this.getNode(0);
					if (node) {
						return this.getRecord(node);
					}
				}
				return null;
			},

			/*
			 * Used to switch between heatmap and line plots.
			 */
			setTemplate : function(tpl) {
				var k = this.getSelectedNodes();
				this.tpl = tpl;

				/*
				 * Check that we're rendered already.
				 */
				if (this.el) {
					this.refresh();
					this.select(k);
				}
			}

		});

/**
 * Takes a collection of VisualizationValueObjects, which in turn each contain a collection of GeneExpressionProfiles.
 * 
 * @param {}
 *            data
 * @return {}
 */
Gemma.prepareProfiles = function(data) {
	var preparedData = [];
	var geneExpressionProfile = data.profiles;

	for (var i = 0; i < geneExpressionProfile.size(); i++) {
		var profile = geneExpressionProfile[i].profile;

		var probeId = geneExpressionProfile[i].probe.id;
		var probe = geneExpressionProfile[i].probe.name;
		var genes = geneExpressionProfile[i].genes;
		var color = geneExpressionProfile[i].color;
		var factor = geneExpressionProfile[i].factor;
		var pvalue = geneExpressionProfile[i].PValue; // yes, it's PValue, not pValue.

		var fade = factor < 2;

		if (fade) {
			color = color == 'red' ? Gemma.HOT_FADE_COLOR : Gemma.COLD_FADE_COLOR;
		}

		/*
		 * Format the gene symbols and names into strings that will be displayed in the legend.
		 */

		var geneSymbols = "Unmapped"; // was 'no gene'...
		var geneNames = "";
		if (genes !== undefined && genes.length > 0) {
			geneSymbols = genes[0].name;
			var k, gene;
			for (k = 1; k < genes.size(); k++) {
				gene = genes[k].name;

				// put the query gene first.
				if (this.queryGene && gene == this.queryGene) {
					geneSymbols = gene + ", " + geneSymbols;
				} else {
					geneSymbols = geneSymbols + "," + gene;
				}
			}

			geneNames = genes[0].officialName;
			for (k = 1; k < genes.size(); k++) {
				gene = genes[k].name;
				var genen = genes[k].officialName;

				// put the query gene first.
				if (this.queryGene && gene == this.queryGene) {
					geneNames = genen + ", " + geneNames;
				} else {
					geneNames = geneNames + "," + genen;
				}
			}
		}

		/*
		 * Turn a flat vector into an array of points (that's what flotr needs)
		 */
		var points = [];
		for (var j = 0; j < profile.size(); j++) {
			var point = [j, profile[j]];
			points.push(point);
		}

		// Label for the thumbnail legend.
		var pvalueLabel = (pvalue && pvalue != 1) ? (sprintf("%.2e", pvalue) + ": ") : "";

		var labelStyle = '';
		var qtip = 'Probe: ' + probe + ' (' + geneSymbols + ') ';
		if (factor && factor < 2) {
			labelStyle = "font-style:italic";
			qtip = qtip + " [Not significant]"; // FIXME this might not always be appropriate.
		}

		/*
		 * Note: flotr requires that the data be called 'data'.
		 */
		var plotConfig = {
			profile : profile,
			data : points, // this is what gets plotted. Flotr wants this name.
			color : color,
			genes : genes,
			rawLabel : pvalueLabel + " " + geneSymbols + " " + geneNames,
			label : pvalueLabel + "<span style='" + labelStyle + "'><a  href='/Gemma/compositeSequence/show.html?id="
					+ probeId + "' target='_blank' ext:qtip= '" + qtip + "'>"
					+ Ext.util.Format.ellipsis(geneSymbols, Gemma.MAX_THUMBNAILLABEL_LENGTH_CHAR) + "</a> " + geneNames
					+ "</span>",
			lines : {
				lineWidth : Gemma.LINE_THICKNESS
			},

			labelID : probeId,
			factor : factor,
			probe : {
				id : probeId,
				name : probe
			},
			PValue : pvalue, // yes, it's PValue, not pValue.
			smoothed : false

		};

		preparedData.push(plotConfig);
	}

	/*
	 * The prepared data is augmented with the 'data' field and formatted labels.
	 */
	data.profiles = preparedData;

	data.profiles.sort(Gemma.sortByImportance);

	return data;
}

/**
 * Used for thumbnails.
 */
Gemma.ProfileTemplate = Ext.extend(Ext.XTemplate, {

			graphConfig : {
				lines : {
					lineWidth : 1
				},
				bars : {
					fill : false
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
				},
				forceFit : true
			},

			overwrite : function(el, values, ret) {

				Gemma.ProfileTemplate.superclass.overwrite.call(this, el, values, ret);

				// if (this.smooth) {
				// this.graphConfig.smoothLineGraphs = true;
				// } else {
				// this.graphConfig.smoothLineGraphs = false;
				// }

				for (var i = 0; i < values.length; i++) {
					var record = values[i];
					var shortName = record.eevo.shortName;
					Ext.DomHelper.append(shortName + '_vizwrap', {
								tag : 'div',
								id : shortName + "_vis",
								style : 'width:' + Gemma.THUMBNAIL_PLOT_SIZE + 'px;height:' + Gemma.THUMBNAIL_PLOT_SIZE
										+ 'px;'
							});

					/*
					 * Note: passing in 'newDiv' works in FF but not in IE. (flotr, anyway)
					 */
					LinePlot.draw($(shortName + "_vis"), record.profiles, this.graphConfig); // no sample names
				}
			}
		});

/**
 * Used for thumbnails.
 */
Gemma.HeatmapTemplate = Ext.extend(Ext.XTemplate, {

			graphConfig : {
				label : false, // shows labels at end of row, no for thumbnails
				forceFit : true,
				maxBoxHeight : 3,
				allowTargetSizeAdjust : true
				// make the rows smaller in the thumbnails
			},

			overwrite : function(el, values, ret) {
				Gemma.HeatmapTemplate.superclass.overwrite.call(this, el, values, ret);

				for (var i = 0; i < values.length; i++) {
					var record = values[i];
					var shortName = record.eevo.shortName;
					Ext.DomHelper.append(shortName + '_vizwrap', {
								tag : 'div',
								id : shortName + "_vis",
								style : 'width:' + Gemma.THUMBNAIL_PLOT_SIZE + 'px;height:' + Gemma.THUMBNAIL_PLOT_SIZE
										+ 'px;'
							});
					/*
					 * Note: 'newDiv' works in FF but not in IE.
					 */
					Heatmap.draw($(shortName + "_vis"), record.profiles, this.graphConfig); // no sample names
				}
			}
		});

/**
 * Pick the appropriate template
 * 
 * @param {}
 *            heatmap
 * @param {}
 *            havePvalues
 * @return {}
 */
Gemma.getProfileThumbnailTemplate = function(heatmap, havePvalues, smooth) {

	var pvalueString = "";
	if (havePvalues) {
		// yes, minPvalue, from EEVO pValue. The best pvalue of any of the profiles.
		pvalueString = '{[(values.minPvalue < 1) ? sprintf("<br/><span style=\'font-size:smaller\'>p=%.2e</span>", values.minPvalue) : ""]}';
	}

	if (heatmap) {
		return new Gemma.HeatmapTemplate(
				'<tpl for="."><tpl for="eevo">',
				'<div class="vizWrap" ext.qtip="{values.name}; Click to zoom" id ="{shortName}_vizwrap" style="cursor:pointer;float:left;padding: 10px"> <strong>{shortName}</strong>: <small> {[Ext.util.Format.ellipsis( values.name, Gemma.MAX_EE_NAME_LENGTH)]} </small> &nbsp;&nbsp;<i>'
						+ pvalueString + '</i></div>', '</tpl></tpl>');
	} else {
		return new Gemma.ProfileTemplate(
				'<tpl for="."><tpl for="eevo">',
				'<div class="vizWrap" ext.qtip="{values.name}; Click to zoom" id ="{shortName}_vizwrap" style="cursor:pointer;float:left;padding: 10px"> <strong> {shortName}</strong>: <small> {[Ext.util.Format.ellipsis( values.name, Gemma.MAX_EE_NAME_LENGTH)]} </small> &nbsp;&nbsp; <i>'
						+ pvalueString + '</i></div>', '</tpl></tpl>', {
					smooth : smooth
				});
	}

};

/**
 * Zoom panel.
 */
Gemma.VisualizationZoomPanel = Ext.extend(Ext.Panel, {

	region : 'center',
	split : true,
	width : Gemma.ZOOM_PLOT_SIZE + 100,
	height : Gemma.ZOOM_PLOT_SIZE + 100,
	stateful : false,
	autoScroll : false,
	closeAction : 'destroy',
	bodyStyle : "background:white",
	layout : 'fit',
	title : "", // dont show a title.
	name : "vizZoom",

	plugins : [new Ext.ux.plugins.ContainerMask({
				msg : 'Loading ... <img src="/Gemma/images/loading.gif" />',
				masked : true
			})],

	/*
	 * The following are only used if we don't have a parent container, or on initialization.
	 */
	heatmapMode : false,
	forceFitPlots : false,
	smoothLineGraphs : false,
	showLegend : true,

	initComponent : function() {

		Gemma.VisualizationZoomPanel.superclass.initComponent.call(this);

		this.on('resize', function(component, width, height) {
					component.update();
				}.createDelegate(this));
	},

	html : {
		tag : 'div',
		id : 'inner-zoom-html-' + Ext.id(),
		style : 'overflow:auto;width:' + Gemma.ZOOM_PLOT_SIZE + 'px;height:' + Gemma.ZOOM_PLOT_SIZE
				+ 'px; margin:5px 2px 2px 5px;'
	},

	/**
	 * Zoom panel update
	 */
	update : function(eevo, profiles, sampleNames) {
		if (profiles === undefined || profiles === null) {
			var record = this.dv.getSelectedOrFirst();

			if (record === null || record === undefined) {
				// can happen at startup.
				return;
			}

			profiles = record.get("profiles");
			if (profiles === undefined) {
				// hopefully this doesn't happen.
				return;
			}

			sampleNames = record.get("sampleNames");
		}

		if (profiles === undefined) {
			throw "No profiles!!";
		}

		if (eevo) {

			var eeInfoTitle = "<a ext.qtip='Click for details on experiment (opens in new window)' target='_blank'  href='/Gemma/expressionExperiment/showExpressionExperiment.html?id="
					+ eevo.id + " '>" + eevo.shortName + "</a> (" + Ext.util.Format.ellipsis(eevo.name, 35) + ")";

			if (this.ownerCt && this.ownerCt.originalTitle) {
				this.ownerCt.setTitle(this.ownerCt.originalTitle + "&nbsp;In:&nbsp;" + eeInfoTitle);
			} else {
				this.setTitle(eeInfoTitle);
			}
		}

		var forceFit = this.ownerCt ? this.ownerCt.forceFitPlots : this.forceFitPlots;

		var smooth = this.ownerCt ? this.ownerCt.smoothLineGraphs : this.smoothLineGraphs;

		var graphConfig = {
			xaxis : {
				noTicks : 0
			},
			yaxis : {
				noTicks : 0
			},
			grid : {
				labelMargin : 0,
				marginColor : "white"
			},
			shadowSize : 0,
			forceFit : forceFit,
			smoothLineGraphs : smooth,

			legend : {
				show : this.showLegend || this.heatmapMode,
				// container : this.legendDiv ? this.legendDiv : this.body.id,
				labelFormatter : function(s) {
					// assume we only have one link defined...
					var k = s.split("</a>", 2);

					return k[0] + "</a>" + Ext.util.Format.ellipsis(k[1], Gemma.MAX_THUMBNAILLABEL_LENGTH_CHAR);
				},
				position : "sw" // best to be west, if we're expanded...applies to linecharts.
			},
			label : true

		};

		Ext.DomHelper.overwrite(this.body.id, '');

		var doHeatmap = this.ownerCt ? this.ownerCt.heatmapMode : this.heatmapMode;
		this.showMask();

		if (doHeatmap) {
			graphConfig.legend.container = this.legendDiv ? this.legendDiv : this.body.id;
			profiles.sort(Gemma.sortByImportance);
			Heatmap.draw($(this.body.id), profiles, graphConfig, sampleNames, Gemma.sortByImportance);
		} else {
			profiles.sort(Gemma.sortByImportance);

			// clear the heatmap legend, if it's there
			if (this.legendDiv && this.legendDiv != this.body.id) {
				Ext.DomHelper.overwrite(this.legendDiv, '');
			}

			LinePlot.draw($(this.body.id), profiles, graphConfig, sampleNames);

			// make the line chart legend clickable. Selector is based on flotr's output.
			legend = Ext.DomQuery.select("div.flotr-legend", this.el.dom);

			if (legend && legend[0]) {

				var onLegendClick = function(event, component) {

					var probeId = event.getTarget().id;

					var record;
					if (this.dv.getSelectionCount() > 0) {
						record = this.dv.getSelectedRecords()[0];
					} else {
						record = this.dv.getStore().getRange(0, 0)[0];
					}

					var eevo = record.get("eevo");
					var profiles = record.get("profiles");
					var sampleNames = record.get("sampleNames");

					for (var i = 0; i < profiles.size(); i++) {

						if (profiles[i].labelID == probeId) {
							if (profiles[i].selected) {
								profiles[i].lines.lineWidth = (profiles[i].lines.lineWidth / Gemma.SELECTED);
								// profiles[i].lines.color = profile[i].lines.color; // put it back...
								Ext.DomHelper.applyStyles(event.getTarget(), "border:black 2px");
								profiles[i].selected = false;
							} else {
								profiles[i].selected = true;
								profiles[i].lines.lineWidth = profiles[i].lines.lineWidth * Gemma.SELECTED;
								// profiles[i].lines.color = profile[i].lines.color; // make it selected

								Ext.DomHelper.applyStyles(event.getTarget(), "border:green 2px");

							}
							break;
						}
					}
					this.update(eevo, profiles, sampleNames);

				};

				var el = new Ext.Element(legend[0]);
				el.on('click', onLegendClick.createDelegate(this));
			}

		}

		this.hideMask();

	}

}); // zoom panel.

/**
 * Two-part panel with thumbnails on the left, zoom view on the right.
 */
Gemma.VisualizationWithThumbsWindow = Ext.extend(Ext.Window, {
	closeAction : 'destroy',
	bodyStyle : "background:white",
	layout : 'border',
	title : "Visualization",
	maximizable : true,
	height : Gemma.ZOOM_PLOT_SIZE,
	width : 600,
	name : "VizWin",

	thumbnails : true,

	showThumbNails : true,

	heatmapSortMethod : Gemma.sortByImportance,

	stateful : true,
	stateId : "visualization-window",
	stateEvents : ['destroy'],

	heatmapMode : false, // must start this way.
	forceFitPlots : false,
	smoothLineGraphs : false,
	havePvalues : false,
	showLegend : true,

	toggleViewBtnId : 'toggleViewBtn-' + Ext.id(),

	forceFitBtnId : 'forceFitbtn-' + Ext.id(),

	smoothBtnId : 'smoothbtn-' + Ext.id(),

	toggleLegendBtnId : 'toggleLegendBtn-' + Ext.id(),

	downloadDataBtnId : 'downloadDataBtn-' + Ext.id(),

	getState : function() {
		return Ext.apply(Ext.Window.superclass.getState.call(this) || {}, {
					heatmapMode : this.heatmapMode,
					forceFitPlots : this.forceFitPlots,
					smoothLineGraphs : this.smoothLineGraphs,
					showLegend : this.showLegend
				});
	},

	loadcallback : function(records, options, success) {
		if (!success || records.length === 0) {
			Ext.Msg.alert("No data", "Sorry, no data were available", function() {
						this.close();
						this.destroy();
					}.createDelegate(this));
			return;
		}
		this.zoom(records[0], this.body.id);
	},

	setHeatmapMode : function(b) {
		this.heatmapMode = b;
		if (this.zoomPanel) {
			this.zoomPanel.heatmapMode = b;
		}
	},

	/**
	 * 
	 * @param {}
	 *            record
	 */
	zoom : function(record) {
		if (!record)
			return;
		var eevo = record.get("eevo");
		var profiles = record.get("profiles");
		var sampleNames = record.get("sampleNames");

		this.zoomPanel.update(eevo, profiles, sampleNames);
	},

	/**
	 * handler
	 * 
	 * @param {}
	 *            btn
	 */
	toggleForceFit : function(btn) {
		if (this.forceFitPlots) {
			this.forceFitPlots = false;
			this.zoomPanel.forceFitPlots = false;
			btn.setText("Fit width");
		} else {
			this.forceFitPlots = true;
			this.zoomPanel.forceFitPlots = true;
			btn.setText("Expand");
		}

		// force a refresh of the zoom.
		var record = this.dv.getSelectedOrFirst();

		this.zoom(record);
	},

	/**
	 * handler
	 * 
	 * @param {}F
	 *            btn
	 */
	toggleLegend : function(btn) {
		if (this.heatmapMode) {
			return;
		}

		if (this.showLegend) {
			this.showLegend = false;
			this.zoomPanel.showLegend = false;
			btn.setText("Show legend");
		} else {
			this.showLegend = true;
			this.zoomPanel.showLegend = true;
			btn.setText("Hide legend");
		}

		// force a refresh of the thumbnails.
		var template = Gemma.getProfileThumbnailTemplate(this.heatmapMode, this.havePvalues, /* this.smoothLineGraphs */
				false);
		this.dv.setTemplate(template);

		// force a refresh of the zoom.
		var record = this.dv.getSelectedOrFirst();

		this.zoom(record);
	},

	/**
	 * Handler
	 * 
	 * @param {}
	 *            btn
	 */
	downloadData : function(btn) {
		if (this.downloadLink) {
			window.open(this.downloadLink);
		}
	},

	/**
	 * handler
	 * 
	 * @param {}
	 *            btn
	 */
	toggleSmooth : function(btn) {
		// if (this.heatmapMode) {
		// return;
		// }
		//
		// if (this.smoothLineGraphs) {
		// this.smoothLineGraphs = false;
		// this.zoomPanel.smoothLineGraphs = false;
		// btn.setText("Smooth");
		// } else {
		// this.smoothLineGraphs = true;
		// this.zoomPanel.smoothLineGraphs = true;
		// btn.setText("Unsmooth");
		// }
		//
		// // force a refresh of the thumbnails.
		// var template = Gemma.getProfileThumbnailTemplate(this.heatmapMode, this.havePvalues, this.smoothLineGraphs);
		// this.dv.setTemplate(template);
		//
		// // force a refresh of the zoom.
		// var record = this.dv.getSelectedOrFirst();
		//
		// this.zoom(record);
	},

	updateTemplate : function() {
		var template = Gemma.getProfileThumbnailTemplate(this.heatmapMode, this.havePvalues, /* this.smoothLineGraphs */
				false);
		this.dv.setTemplate(template); // causes update of thumbnails.
	},

	/**
	 * handler
	 * 
	 * @param {}
	 *            btn
	 */
	switchView : function(btn) {
		// var smoothBtn = Ext.getCmp(this.smoothBtnId);
		var toggleLegendBtn = Ext.getCmp(this.toggleLegendBtnId);
		if (this.heatmapMode) {
			this.setHeatmapMode(false);
			btn.setText("Switch to heatmap");

			// if (smoothBtn) {
			// smoothBtn.setVisible(true);
			// }

			if (toggleLegendBtn) {
				toggleLegendBtn.setVisible(true);
			}

		} else {
			this.setHeatmapMode(true);

			// if (smoothBtn) {
			// smoothBtn.setVisible(false);
			// }

			if (toggleLegendBtn) {
				toggleLegendBtn.setVisible(false);
			}

			var zoomLegendDiv = $(this.zoomLegendId);
			if (zoomLegendDiv) {
				zoomLegendDiv.innerHTML = '';
			}
			btn.setText("Switch to line plots");
		}

		this.updateTemplate();

		// force a refresh of the zoom.
		var record = this.dv.getSelectedOrFirst();;
		this.zoom(record);

	},

	show : function(config) {
		Gemma.VisualizationWithThumbsWindow.superclass.show.call(this);

		var params = config.params || [];

		this.dv.store.load({
					params : params,
					callback : this.loadcallback.createDelegate(this),
					scope : this
				});

	},

	initComponent : function() {

		if (this.title) {
			this.originalTitle = this.title;
		}

		this.zoomLegendId = 'zoomLegend-' + Ext.id();

		this.store = new Gemma.VisualizationStore({
					readMethod : this.readMethod
				});

		this.dv = new Gemma.DataVectorThumbnailsView({
					tpl : this.tpl,
					store : this.store,
					heatmapSortMethod : this.heatmapSortMethod
				});

		this.zoomPanel = new Gemma.VisualizationZoomPanel({
					store : this.store,
					legendDiv : this.zoomLegendId,
					dv : this.dv, // perhaps give store?
					heatmapSortMethod : this.heatmapSortMethod
				});

		this.thumbnailPanel = new Ext.Panel({
					region : 'west',
					split : true,
					width : Gemma.THUMBNAIL_PLOT_SIZE + 50, // little extra..
					collapsible : true,
					title : "Thumbnails",
					stateful : false,
					margins : '3 0 3 3',
					items : this.dv,
					autoScroll : true,
					zoomPanel : this.zoomPanel,
					legendDiv : this.zoomLegendId,
					hidden : !this.thumbnails,

					/*
					 * legend div FIXME make this go to the TOP. Might need to make it a separate div in this panel
					 * instead.
					 */
					html : {
						id : this.zoomLegendId,
						tag : 'div',
						style : 'width:' + (Gemma.THUMBNAIL_PLOT_SIZE + 50) + 'px;height:' + Gemma.THUMBNAIL_PLOT_SIZE
								+ 'px; float:left;'
					}
				});
		var items = [this.thumbnailPanel, this.zoomPanel]

		var browserWarning = "";
		if (Ext.isIE) {
			browserWarning = "<span ext:qtip='Plots use a feature of HTML 5 that runs in IE via emulation unless you have Chrome Frame installed. Firefox, Chrome, Safari and Opera will be faster too.'>"
					+ "Too slow in Explorer? Try <a href='http://www.google.com/chromeframe/' target='_blank'>Chrome Frame</a></span>";
		}

		Ext.apply(this, {
					items : items,
					bbar : new Ext.Toolbar({
								items : [{
											xtype : 'tbbutton',
											id : this.downloadDataBtnId,
											icon : '../images/download.gif',
											cls : 'x-btn-text-icon',
											disabled : true, // enabled after load.
											tooltip : "Download displayed data in a tab-delimited format",
											handler : this.downloadData.createDelegate(this)
										}, browserWarning, '->', {
											xtype : 'tbbutton', // in ext3, use button; tbbutton is deprecated.
											text : this.showLegend ? "Hide legend" : "Show legend",
											id : this.toggleLegendBtnId,
											handler : this.toggleLegend.createDelegate(this),
											tooltip : "Toggle display of the plot legend",
											disabled : true,
											hidden : this.heatmapMode
										},

										// {
										// xtype : 'tbbutton',
										// text : this.smoothLineGraphs ? "Unsmooth" : "Smooth",
										// id : this.smoothBtnId,
										// handler : this.toggleSmooth.createDelegate(this),
										// disabled : true,
										// hidden : true
										// },
										//											
										{
											xtype : 'tbbutton',
											text : this.forceFitPlots ? "Expand" : "Fit width",
											id : this.forceFitBtnId,
											handler : this.toggleForceFit.createDelegate(this),
											tooltip : "Toggle forcing of the plot to fit in the width of the window",
											disabled : true,
											hidden : false
										}, {
											xtype : 'tbbutton',
											text : this.heatmapMode ? "Switch to line plot" : "Switch to heatmap",
											id : this.toggleViewBtnId,
											disabled : true,
											handler : this.switchView.createDelegate(this)
										} /* todo: add standarization on/off option. */]
							})
				});

		Gemma.VisualizationWithThumbsWindow.superclass.initComponent.call(this);

		this.dv.getStore().on('load', function(s, records, options) {

					Ext.getCmp(this.toggleViewBtnId).enable();
					// Ext.getCmp(this.smoothBtnId).enable();
					Ext.getCmp(this.forceFitBtnId).enable();
					Ext.getCmp(this.toggleLegendBtnId).enable();
					Ext.getCmp(this.downloadDataBtnId).enable();

					// So initial state is sure to be okay, after restore from cookie
					Ext.getCmp(this.toggleViewBtnId).setText(this.heatmapMode
							? "Switch to line plot"
							: "Switch to heatmap");
					Ext.getCmp(this.forceFitBtnId).setText(this.forceFitPlots ? "Expand" : "Fit width");
					Ext.getCmp(this.toggleLegendBtnId).setText(this.showLegend ? "Hide legend" : "Show legend");
					// Ext.getCmp(this.smoothBtnId).setText(this.smoothLineGraphs ? "Unsmooth" : "Smooth");

					if (this.heatmapMode) {
						// Ext.getCmp(this.smoothBtnId).hide();
						Ext.getCmp(this.toggleLegendBtnId).hide();
					} else {
						// Ext.getCmp(this.smoothBtnId).show();
						Ext.getCmp(this.toggleLegendBtnId).show();
					}

				}, this);

		this.dv.getStore().on('loadexception', function() {
					Ext.Msg.alert("No data", "Sorry, no data were available", function() {
								this.close();
								this.destroy();
							}.createDelegate(this));
				}, this);

		this.dv.on('selectionchange', function(dv, selections) {
					if (selections.length > 0) {
						var record = dv.getRecord(selections[0]);
						if (!record || record === undefined) {
							return;
						}
						this.zoom(record);
					}
				}.createDelegate(this), this);

		this.on('staterestore', function(w, state) {
					this.zoomPanel.heatmapMode = this.heatmapMode;
					this.zoomPanel.forceFitPlots = this.forceFitPlots;
					// this.zoomPanel.smoothLineGraphs = this.smoothLineGraphs;
					this.zoomPanel.showLegend = this.showLegend;
					this.updateTemplate();
				}, this);

		/*
		 * Tell thumbnails where to put the legend. Currently it's in the body of the graph.
		 */
		// this.on('show', function(cmp) {
		// console.log("yay");
		// cmp.zoomLegendId = cmp.getBottomToolbar().id;
		// cmp.zoomPanel.legendDiv = cmp.zoomLegendId;
		// cmp.thumbnailPanel.legendDiv = cmp.zoomLegendId;
		// }.createDelegate(this), this);
	}

});

/**
 * Specialization to show differentially expressed genes.
 */
Gemma.VisualizationDifferentialWindow = Ext.extend(Gemma.VisualizationWithThumbsWindow, {

			havePvalues : true,

			initComponent : function() {
				this.tpl = Gemma.getProfileThumbnailTemplate(false, true);
				Gemma.VisualizationDifferentialWindow.superclass.initComponent.call(this);

			},

			readMethod : DEDVController.getDEDVForDiffExVisualization
		});

/**
 * Specialization for coexpression display
 * 
 * @class Gemma.CoexpressionVisualizationWindow
 * @extends Gemma.VisualizationWithThumbsWindow
 */
Gemma.CoexpressionVisualizationWindow = Ext.extend(Gemma.VisualizationWithThumbsWindow, {

			/**
			 * config : {}
			 */
			initComponent : function() {
				this.tpl = Gemma.getProfileThumbnailTemplate(false, false);
				Gemma.VisualizationDifferentialWindow.superclass.initComponent.call(this);

			},
			readMethod : DEDVController.getDEDVForCoexpressionVisualization
		});

// ////////////////////////////////////////////////////////////////////////////////////////

/**
 * Represents a VisualizationValueObject.
 * 
 * @extends Ext.data.Store
 */
Gemma.VisualizationStore = function(config) {

	this.record = Ext.data.Record.create([{
				name : "id",
				type : "int"
			}, {
				// EE value object
				name : "eevo"
			}, {
				// GeneExpressionProfile value object.
				name : "profiles"
			}, {
				// not used yet.
				name : "factorProfiles"
			}, {
				name : "sampleNames"
			}]);

	if (config && config.readMethod) {
		this.readMethod = config.readMethod;
	} else {
		this.readMethod = DEDVController.getDEDVForVisualization; // takes eeids,geneids.
	}

	this.proxy = new Ext.data.DWRProxy(this.readMethod);

	this.reader = new Ext.data.ListRangeReader({
				id : "id"
			}, this.record);

	Gemma.VisualizationStore.superclass.constructor.call(this, config);

	this.relayEvents(this.proxy, ['loadexception']);

};

/**
 * 
 * @class Gemma.VisualizationStore
 * @extends Ext.data.Store
 */

Ext.extend(Gemma.VisualizationStore, Ext.data.Store, {});

// //////////////////////////////////////////////////////////////////////////////////

// Utility functions for visualization

/**
 * Sort in this order: 1. Query probes that show coexpression or sig. diff ex., ordered by pvalue 2. Target probes that
 * show coexp or sig diff ex. 3. Query probes that do not show coexpression or sig. diff ex. (faded) 4. Target probes
 * that do not show coexp or sig diff ex.
 * 
 * @param {}
 *            a
 * @param {}
 *            b
 * @return {Number}
 */
Gemma.sortByImportance = function(a, b) {

	// first level sort: by pvalue, if present.
	if (a.PValue !== null && a.PValue !== undefined && b.PValue !== null && b.PValue !== undefined) {
		return Math.log(a.PValue) - Math.log(b.PValue); // log to avoid roundoff trouble.
	}

	// Second level sort: by factor > 1 means 'involved in sig. coexpression' or "query gene".
	if (a.factor > b.factor) {
		return -1;
	}
	if (a.factor < b.factor) {
		return 1;
	}

	if ((!a.genes || a.genes.size() < 1) && (!b.genes || b.genes.size() < 1)) {
		return a.labelID > b.labelID;
	}

	if (!a.genes || a.genes.size() < 1) {
		return -1;
	}
	if (!b.genes || b.genes.size() < 1) {
		return 1;
	}

	if (a.genes[0].name > b.genes[0].name) {
		return 1;
	} else if (a.genes[0].name < b.genes[0].name) {
		return -1;
	} else {
		return a.labelID > b.labelID;
	}

};
