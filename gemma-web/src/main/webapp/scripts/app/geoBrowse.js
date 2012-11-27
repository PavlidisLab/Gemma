/**
 * 
 */
Ext.namespace('Gemma');

Gemma.GeoBrowseGrid = Ext
		.extend(
				Gemma.GemmaGridPanel,
				{
					collapsible : false,
					loadMask : true,
					defaults : {
						autoScroll : true
					},

					height : 500,
					width : 1300,
					autoScroll : true,
					loadMask : true,

					autoExpandColumn : 'description',

					record : Ext.data.Record.create([ {
						name : "usable",
						type : "boolean"
					}, {
						name : "geoAccession"
					}, {
						name : "numSamples",
						type : "int"
					}, {
						name : 'title'
					}, {
						name : 'correspondingExperiments'
					}, {
						name : "releaseDate",
						type : "date"
					}, {
						name : "organisms"
					}, {
						name : "inGemma",
						type : "boolean"
					}, {
						name : "usable",
						type : "boolean"
					}, {
						name : "previousClicks",
						type : "int"
					} ]),

					proceed : function(s) {
						// new start
						this.start = Number(s) > 0 ? this.start + Number(s)
								: (this.start + this.count); 

						this.store.load({
							params : [ this.start, this.count ]
						});

					},

					/**
					 * S is the skip.
					 * 
					 * @param s
					 */
					back : function(s) {

						// new start. Either go to the skip, or go back one
						// 'page', make sure greater than zero.
						this.start = Math.max(0, Number(s) > 0 ? this.start
								- Number(s) : this.start - this.count); 

						this.store.load({
							params : [ this.start, this.count ]
						});

					},

					// initial starting point
					start : 0,

					// page size
					count : 20,

					initComponent : function() {

						Ext
								.apply(
										this,
										{
											store : new Ext.data.Store(
													{
														proxy : new Ext.data.DWRProxy(
																{
																	apiActionToHandlerMap : {
																		read : {
																			dwrFunction : GeoRecordBrowserController.browse
																		}
																	},
																	getDwrArgsFunction : function(
																			request,
																			recordDataArray) {
																		return [
																				this.start,
																				this.count ];
																	}
																}),
														reader : new Ext.data.ListRangeReader(
																{
																	id : "id"
																}, this.record)
													})
										});

						Ext
								.apply(
										this,
										{
											columns : [
													{
														header : "Accession",
														dataIndex : "geoAccession",
														scope : this,
														renderer : this.geoAccessionRenderer
													},
													{
														header : "Title",
														dataIndex : "title",
														renderer : this.titleRenderer,
														width : 500
													},
													{
														header : "Release date",
														dataIndex : "releaseDate",
														renderer : new Ext.util.Format.dateRenderer(
																"M d, Y"),
														width : 76
													},
													{
														header : "numSamples",
														dataIndex : "numSamples",
														sortable : true,
														width : 40
													},
													{
														header : "In Gemma?",
														dataIndex : "inGemma",
														renderer : this.inGemmaRenderer,
														width : 40
													},
													{
														header : "taxa",
														dataIndex : "organisms",
														scope : this,
														renderer : this.taxonRenderer
													},
													{
														header : "Usable?",
														dataIndex : "usable",
														scope : this,
														width : 30,
														renderer : this.usableRenderer
													},
													{
														header : "Examined",
														dataIndex : "previousClicks",
														scope : this,
														width : 30,
														renderer : this.clicksRenderer
													} ]
										});

						Gemma.GeoBrowseGrid.superclass.initComponent.call(this);

						this.getStore().on(
								"load",
								function(store, records, options) {

									// there must be a better way of doing this.
									Ext.DomHelper.overwrite(Ext
											.getDom('numRecords'),
											"<span id='numRecords'>"
													+ records.length
													+ "</span>");

								}, this);

						this.getSelectionModel().on('rowselect',
								this.showDetails, this, {
									buffer : 100
								// keep from firing too many times at once
								});

						this.getStore().load({
							params : [ this.start, this.count ]
						});

					},

					/**
					 * Show some dots.
					 */
					clicksRenderer : function(value, metadata, record, row,
							col, ds) {
						var m = record.get('previousClicks');
						var result = "";
						for ( var i = 0; i < Math.min(m, 5); i++) {
							result = result + "&bull;";
						}
						return result;
					},

					geoAccessionRenderer : function(value, metadata, record,
							row, col, ds) {
						return "<a target='_blank' href='http://www.ncbi.nlm.nih.gov/geo/query/acc.cgi?acc="
								+ record.get('geoAccession')
								+ "'>"
								+ record.get('geoAccession') + "</a>";
					},

					titleRenderer : function(value, metadata, record, row, col,
							ds) {
						return record.get('title');
					},

					inGemmaRenderer : function(value, metadata, record, row,
							col, ds) {

						if (record.get('correspondingExperiments').length == 0) {
							return "<input type=\"button\" value=\"Load\" "
									+ "\" onClick=\"load('"
									+ record.get('geoAccession') + "')\" >";
						}

						var r = "";
						for ( var i = 0; i < record
								.get('correspondingExperiments').length; i++) {
							var ee = record.get('correspondingExperiments')[i];
							r = r
									+ "<a href=\"/Gemma/expressionExperiment/showExpressionExperiment.html?"
									+ "id=" + ee + "\">"
									+ record.get('geoAccession') + "</a>";

						}
						return r;

					},

					taxonRenderer : function(value, metadata, record, row, col,
							ds) {
						var r = "";
						for ( var i = 0; i < record.get('organisms').length; i++) {
							var ee = record.get('organisms')[i];
							r = r + "&nbsp;" + ee;

						}
						return r;
					},

					usableRenderer : function(value, metadata, record, row,
							col, ds) {
						if (record.get('correspondingExperiments').length > 0) {
							return "<img src=\"/Gemma/images/icons/gray-thumb.png\" width=\"16\" height=\"16\" alt=\"Already loaded\"/>";
						}
						if (record.get('usable')) {
							return "<span id=\""
									+ record.get('geoAccession')
									+ "-rating\"  onClick=\"toggleUsability('"
									+ record.get('geoAccession')
									+ "')\"><img src=\"/Gemma/images/icons/thumbsup.png\"  width=\"16\" height=\"16\"   alt=\"Usable, click to toggle\" /></span>";
						} else {
							return "<span id=\""
									+ record.get('geoAccession')
									+ "-rating\"  onClick=\"toggleUsability('"
									+ record.get('geoAccession')
									+ "')\"  ><img src=\"/Gemma/images/icons/thumbsdown-red.png\"  alt=\"Judged unusable, click to toggle\"  width=\"16\" height=\"16\"  /></span>";
						}
					},

					showDetails : function(model, rowindex, record) {
						var callParams = [];
						callParams.push(record.get('geoAccession'));

						var delegate = handleSuccess.createDelegate(this, [],
								true);
						var errorHandler = handleFailure.createDelegate(this,
								[], true);

						callParams.push({
							callback : delegate,
							errorHandler : errorHandler
						});

						GeoRecordBrowserController.getDetails.apply(this,
								callParams);
						Ext.DomHelper.overwrite("messages", {
							tag : 'img',
							src : '/Gemma/images/default/tree/loading.gif'
						});
						Ext.DomHelper.append("messages", {
							tag : 'span',
							html : "&nbsp;Please wait..."
						});

					}

				});

function handleSuccess(data) {
	Ext.DomHelper.overwrite("messages", {
		tag : 'div',
		html : data
	});
}

function handleUsabilitySuccess(data, accession) {

	if (data) {
		Ext.DomHelper.overwrite(accession + "-rating", {
			tag : 'img',
			src : '/Gemma/images/icons/thumbsup.png'
		});
	} else {
		Ext.DomHelper.overwrite(accession + "-rating", {
			tag : 'img',
			src : '/Gemma/images/icons/thumbsdown-red.png'
		});
	}

}

function handleFailure(data, e) {
	Ext.DomHelper.overwrite("taskId", "");
	Ext.DomHelper.overwrite("messages", {
		tag : 'img',
		src : '/Gemma/images/icons/warning.png'
	});
	Ext.DomHelper.append("messages", {
		tag : 'span',
		html : "&nbsp;There was an error: " + data
	});
}

function toggleUsability(accession) {
	var callParams = [];
	callParams.push(accession);

	var delegate = handleUsabilitySuccess.createDelegate(this, [ accession ],
			true);
	var errorHandler = handleFailure.createDelegate(this, [], true);

	callParams.push({
		callback : delegate,
		errorHandler : errorHandler
	});

	GeoRecordBrowserController.toggleUsability.apply(this, callParams);
	Ext.DomHelper.overwrite(accession + "-rating", {
		tag : 'img',
		src : '/Gemma/images/default/tree/loading.gif'
	});
}

function handleLoadSuccess(taskId) {
	try {
		Ext.DomHelper.overwrite("messages", "");

		var p = new Gemma.ProgressWindow({
			taskId : taskId,
			errorHandler : handleFailure,
			callback : function() {
				document.location.reload(true);
				Ext.DomHelper.overwrite("messages", "Successfully loaded");
			}
		});

		p.show('upload-button');

	} catch (e) {
		handleFailure(data, e);
		return;
	}

}

function load(accession) {

	var suppressMatching = "false";
	var loadPlatformOnly = "false";
	var arrayExpress = "false";
	var arrayDesign = "";

	var callParams = [];

	var commandObj = {
		accession : accession,
		suppressMatching : suppressMatching,
		loadPlatformOnly : loadPlatformOnly,
		arrayExpress : arrayExpress,
		arrayDesignName : arrayDesign
	};

	callParams.push(commandObj);

	var delegate = handleLoadSuccess.createDelegate(this, [], true);
	var errorHandler = handleFailure.createDelegate(this, [], true);

	callParams.push({
		callback : delegate,
		errorHandler : errorHandler
	});

	// this should return quickly, with the task id.
	Ext.DomHelper.overwrite("messages", {
		tag : 'img',
		src : '/Gemma/images/default/tree/loading.gif'
	});
	Ext.DomHelper.append("messages", "&nbsp;Submitting job...");
	ExpressionExperimentLoadController.load.apply(this, callParams);

}
