Ext.namespace('Gemma');

var m_profiles = []; // Data returned by server for visulization (alread
// processed by client)
var m_diffProfiles = [];
var m_eevo;
var m_geneIds;
var m_geneSymbols;
var m_myVizLoadMask;
var m_myDiffVizLoadMask;

var DEFAULT_LABEL = "[No gene]";

var THRESHOLD = 0.05;

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

/**
 */
Gemma.EEDetailsDiffExpressionVisualizationWindow = Ext
		.extend(
				Ext.Window,
				{

					// height :200,
					width : 320,

					constructor : function(factorDetails) {

						this.factorDetails = factorDetails;

						Gemma.EEDetailsDiffExpressionVisualizationWindow.superclass.constructor.call(this);

					},

					listeners : {
						resize : {
							fn : function(component, adjWidth, adjHeight, rawWidth, rawHeight) {

								// Change the div so that it is the size of the
								// panel surrounding it.
								vizDiv = Ext.get('vizDiffDiv');
								if (!vizDiv)
									return;
								vizDiv.setHeight(adjHeight);
								vizDiv.setWidth(adjWidth);
								vizDiv.repaint();

								component.vizPanel.refreshWindow();

							}.createDelegate(this)
						}
					},

					dedvCallback : function(data) {
						// Need to transform the coordinate data from an object
						// to an
						// array for flotr/HeatMap display

						// No data to visualize just
						if (!data || data.size() == 0) {
							m_myDiffVizLoadMask.hide();
							this.hide();
							Ext.Msg.alert('Status', 'No visualization data available');
							return;
						}

						var flotrData = [];
						var coordinateProfile = data[0].data.profiles;

						for ( var i = 0; i < coordinateProfile.size(); i++) {
							var coordinateObject = coordinateProfile[i].points;

							var probeId = coordinateProfile[i].probe.id;
							var probe = coordinateProfile[i].probe.name;
							var genes = coordinateProfile[i].genes;

							var geneNames = DEFAULT_LABEL;

							if (genes && genes.size() > 0 && genes[0]) {

								geneNames = genes[0].name;
								for ( var k = 1; k < genes.size(); k++) {
									// Put search gene in begining of list
									if (Gemma.geneContained(genes[k].name, m_geneSymbols)) {
										geneNames = genes[k].name + "," + geneNames;
									} else {
										geneNames = geneNames + "," + genes[k].name;
									}
								}
							}
							// turn data points into a structure usuable by
							// heatmap
							var oneProfile = [];
							for ( var j = 0; j < coordinateObject.size(); j++) {
								var point = [ coordinateObject[j].x, coordinateObject[j].y ];
								oneProfile.push(point);
							}

							var plotConfig = {
								data : oneProfile,
								genes : genes,
								label : " <a  href='/Gemma/compositeSequence/show.html?id=" + probeId
										+ "' target='_blank' ext:qtip= '" + probe + " (" + geneNames + ")" + "'> "
										+ Ext.util.Format.ellipsis(geneNames, Gemma.MAX_LABEL_LENGTH_CHAR) + "</a>",
								labelID : probeId,
								lines : {
									lineWidth : Gemma.LINE_THICKNESS
								},
								// Needs to be added so switching views work
								probe : {
									id : probeId,
									name : probe
								},
								points : coordinateObject

							};

							flotrData.push(plotConfig);
						}

						// flotrData.sort(Gemma.graphSort);
						m_diffProfiles = flotrData;
						m_eevo = data[0].data.eevo;

						this.setTitle("Top differentially expressed probes in " + m_eevo.shortName + " for "
								+ this.factorDetails.factorDetails);

						Heatmap.draw($('vizDiffDiv'), m_diffProfiles, HEATMAP_CONFIG);
						m_myDiffVizLoadMask.hide();

					},

					displayWindow : function(eeId, resultSetId) {

						var params = [];
						params.push(eeId);
						params.push(resultSetId)
						params.push(THRESHOLD);

						this.dv.store.load( {
							params : params,
							callback : this.dedvCallback.createDelegate(this)
						});

						this.show();

						m_myDiffVizLoadMask = new Ext.LoadMask(this.getEl(), {
							id : "heatmapLoadMask",
							msg : "Loading differentially expressed genes that met the threshold."
						});

						Ext.apply(this, {
							loadMask : m_myDiffVizLoadMask
						});

						m_myDiffVizLoadMask.show();

					},

					initComponent : function() {

						this.dv = new Ext.DataView( {
							autoHeight : true,
							emptyText : 'Unable to visualize missing data',
							loadingText : 'Loading data ...',
							store : new Gemma.VisualizationStore( {
								readMethod : DEDVController.getDEDVForDiffExVisualizationByThreshold
							})
						});

						this.vizPanel = new Ext.Panel(
								{

									id : 'visualizationWindow',
									closeAction : 'destroy',
									bodyStyle : "background:white",
									constrainHeader : true,
									layout : 'fit',
									// hidden : true,
									stateful : false,
									autoScroll : true,

									html : {
										id : 'vizDiffDiv',
										tag : 'div',
										style : 'width:' + 200 + 'px;height:' + 200 + 'px; margin:5px 2px 2px 5px;'
									},

									refreshWindow : function(data) {
										// Should redraw to fit current window
									// width and hight.
									if (data == null) {
										if (m_diffProfiles != null)
											data = m_diffProfiles;
										else
											return;
									}

									$('vizDiffDiv').innerHTML = '';

									Heatmap.draw($('vizDiffDiv'), data, HEATMAP_CONFIG);

								},

									displayWindow : function(eevo, data) {

										if (data == null)
											data = m_diffProfiles;

										if (eevo == null)
											eevo = m_eevo;

										this
												.setTitle("<a   target='_blank' href='/Gemma/expressionExperiment/showExpressionExperiment.html?id="
														+ eevo.id + " '> " + eevo.shortName + "</a>: " + eevo.name);

										if (!this.isVisible()) {
											this.setVisible(true);
											this.show();
										}

										$('vizDiffDiv').innerHTML = '';
										Heatmap.draw($('vizDiffDiv'), data, HEATMAP_CONFIG);

									}

								});

						Ext.apply(this, {
							items : [ this.vizPanel ]
						});

						Gemma.EEDetailsDiffExpressionVisualizationWindow.superclass.initComponent.call(this);

					}

				});

/**
 * Show either 'random' vectors or ones selected by user.
 */
Gemma.EEDetailsVisualizationWindow = Ext
		.extend(
				Ext.Window,
				{

					// height :200,
					width : 420,
					listeners : {
						resize : {
							fn : function(component, adjWidth, adjHeight, rawWidth, rawHeight) {

								// Change the div so that it is the size of the
								// panel surrounding it.
								vizDiv = Ext.get('vizDiv');
								if (!vizDiv)
									return;

								vizDiv.setHeight(adjHeight);
								vizDiv.setWidth(adjWidth);
								vizDiv.repaint();

								component.vizPanel.refreshWindow();

							}.createDelegate(this)
						}
					},

					dedvCallback : function(data) {
						// Need to transform the coordinate data from an object
						// to an
						// array for flotr/HeatMap display

						// No data to visualize. This could be because the data
						// are not processed yet, or the gene is not found.
						if (!data || data.size() == 0) {
							m_myVizLoadMask.hide();
							this.hide();
							Ext.Msg.alert('Status', 'No visualization data available');
							return;
						}

						var flotrData = [];
						var coordinateProfile = data[0].data.profiles;

						// Set the height of the window based on data

						for ( var i = 0; i < coordinateProfile.size(); i++) {
							var coordinateObject = coordinateProfile[i].points;

							var probeId = coordinateProfile[i].probe.id;
							var probe = coordinateProfile[i].probe.name;
							var genes = coordinateProfile[i].genes;

							var geneNames = DEFAULT_LABEL;

							if (genes && genes.size() > 0 && genes[0]) {

								geneNames = genes[0].name;
								for ( var k = 1; k < genes.size(); k++) {
									// Put search gene in begining of list
									if (Gemma.geneContained(genes[k].name, m_geneSymbols)) {
										geneNames = genes[k].name + "," + geneNames;
									} else {
										geneNames = geneNames + "," + genes[k].name;
									}
								}
							}
							// turn data points into a structure usuable by
							// heatmap
							var oneProfile = [];
							for ( var j = 0; j < coordinateObject.size(); j++) {
								var point = [ coordinateObject[j].x, coordinateObject[j].y ];
								oneProfile.push(point);
							}

							var plotConfig = {
								data : oneProfile,
								genes : genes,
								label : " <a  href='/Gemma/compositeSequence/show.html?id=" + probeId
										+ "' target='_blank' ext:qtip= '" + probe + " (" + geneNames + ")" + "'> "
										+ Ext.util.Format.ellipsis(geneNames, Gemma.MAX_LABEL_LENGTH_CHAR) + "</a>",
								labelID : probeId,
								lines : {
									lineWidth : Gemma.LINE_THICKNESS
								},
								// Needs to be added so switching views work
								probe : {
									id : probeId,
									name : probe
								},
								points : coordinateObject

							};

							flotrData.push(plotConfig);
						}

						flotrData.sort(Gemma.graphSort);
						m_profiles = flotrData;
						m_eevo = data[0].data.eevo;

						var downloadDedvLink = String
								.format(
										"<a ext:qtip='Download raw data in a tab delimted format'  target='_blank'  href='/Gemma/dedv/downloadDEDV.html?ee={0} &g={1}' > <img src='/Gemma/images/download.gif'/></a>",
										m_eevo.id, m_geneIds);

						if (m_geneSymbols) {
							this
									.setTitle(m_geneSymbols + " in " + m_eevo.shortName + "&nbsp;&nbsp;"
											+ downloadDedvLink);
						} else {
							this.setTitle("Sampling of data from " + m_eevo.shortName + "&nbsp;&nbsp;"
									+ downloadDedvLink);
						}

						Heatmap.draw(Ext.get('vizDiv'), m_profiles, HEATMAP_CONFIG);
						m_myVizLoadMask.hide();

					},

					displayWindow : function(eeId, genes) {

						var geneIds = [];
						var geneSymbols = [];

						for ( var i = 0; i < genes.size(); i++) {
							geneIds.push(genes[i].id);
							geneSymbols.push(genes[i].officialSymbol);
						}

						var params = [];
						params.push( [ eeId ]);
						params.push(geneIds);
						m_geneIds = geneIds;
						m_geneSymbols = geneSymbols;

						this.dv.store.load( {
							params : params,
							callback : this.dedvCallback.createDelegate(this)
						});

						this.show();

						m_myVizLoadMask = new Ext.LoadMask(this.getEl(), {
							id : "heatmapLoadMask",
							msg : "Accessing data ..."
						});

						Ext.apply(this, {
							loadMask : m_myVizLoadMask
						});

						m_myVizLoadMask.show();

					},

					initComponent : function() {

						this.dv = new Ext.DataView( {
							autoHeight : true,
							emptyText : 'Unavailable',
							loadingText : 'Loading data ...',
							store : new Gemma.VisualizationStore( {
								readMethod : DEDVController.getDEDVForVisualization
							})
						});

						this.vizPanel = new Ext.Panel(
								{

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
										style : 'width:' + 420 + 'px;height:' + 300 + 'px; margin:5px 2px 2px 5px;'
									},

									refreshWindow : function(data) {
										// Should redraw to fit current window
									// width and hight.

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

										this
												.setTitle("<a   target='_blank' href='/Gemma/expressionExperiment/showExpressionExperiment.html?id="
														+ eevo.id + " '> " + eevo.shortName + "</a>: " + eevo.name);

										if (!this.isVisible()) {
											this.setVisible(true);
											this.show();
										}

										$('vizDiv').innerHTML = '';
										Heatmap.draw($('vizDiv'), data, HEATMAP_CONFIG);

									}

								});

						Ext.apply(this, {
							items : [ this.vizPanel ]
						});

						Gemma.EEDetailsVisualizationWindow.superclass.initComponent.call(this);

					}

				});

Gemma.EEDetailsVisualizationWidget = Ext.extend(Ext.Panel, {

	layout : 'border',
	width : 390,
	height : 360,
	frame : true,

	constructor : function(taxon) {

		this.taxon = taxon;

		Gemma.EEDetailsVisualizationWidget.superclass.constructor.call(this);

	},

	visualizeHandler : function() {
		// Get expressionExperiment ID
	// Get Gene IDs
	// DedvController.
	// destroy if already open
	if (this.visWindow) {
		this.visWindow.close();
	}

	this.visWindow = new Gemma.EEDetailsVisualizationWindow( {});
	// this.visWindow.setHeight(100);
	// this.visWindow.setWidth(220);

	var geneList = this.geneChooserPanel.getGenes();

	// if (geneList.size() < 1){
	// Ext.Msg.alert('Status', 'Please select a gene first');
	// return;
	//
	// }
	var eeId = Ext.get("eeId").getValue();

	this.visWindow.displayWindow(eeId, geneList);
},

onRender : function() {
	Gemma.EEDetailsVisualizationWidget.superclass.onRender.apply(this, arguments);

},

initComponent : function() {

	this.geneChooserPanel = new Gemma.GeneChooserPanel( {
		height : 100,
		width : 400,
		region : 'center',
		id : 'gene-chooser-panel',
		extraButtons : [ new Ext.Button( {
			id : "visualizeButton",
			text : "Show",
			handler : this.visualizeHandler.createDelegate(this, [], false)
		}) ]
	});

	var allPanel = new Ext.Panel( {
		renderTo : 'visualization',
		layout : 'table',
		baseCls : 'x-plain-panel',
		autoHeight : true,
		width : 400,
		// layoutConfig : {
		// columns : 2
		// },
		items : [ this.geneChooserPanel ],
		// buttons : [],
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
			// cmp.setValue(false);
			cmp.disable();
		}
	}, this);

	/*
	 * This horrible mess. We listen to taxon ready event and filter the presets
	 * on the taxon.
	 */
	this.geneChooserPanel.toolbar.taxonCombo.on("ready", function(taxon) {

		var foundTaxon = this.geneChooserPanel.toolbar.taxonCombo.setTaxonByCommonName(this.taxon.taxon);
		this.geneChooserPanel.taxonChanged(foundTaxon, false);

	}.createDelegate(this), this);
}

}// initComponent

		);