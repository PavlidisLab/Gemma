Ext.namespace('Gemma');
Ext.BLANK_IMAGE_URL = '/Gemma/images/default/s.gif';
Ext.onReady(function() {
	Ext.QuickTips.init();
	Ext.state.Manager.setProvider(new Ext.state.CookieProvider());

	handleWait = function(taskId) {
		try {
			Ext.DomHelper.overwrite("messages", "");

			var p = new Gemma.ProgressWidget({
						taskId : taskId
					});

			var w = new Ext.Window({
						modal : true,
						closable : false,
						width : 500,
						items : [p]
					});

			p.on('done', function(payload) {
						w.destroy();
						p.destroy();
						store.reload();
					}.createDelegate(this));

			p.on('fail', function(message) {
						Ext.Msg.alert("Error", "There was an error:<br/>" + message);
						w.destroy();
						p.destroy();
					}.createDelegate(this));

			w.show();
			p.startProgress();
		} catch (e) {
			Ext.Msg.alert("Error", "There was an error:<br/>" + e);
			return;
		}
	};

	updateEEReport = function(id) {
		var callParams = [];
		callParams.push(id);
		callParams.push({
					callback : handleWait.createDelegate(this)
				});

		ExpressionExperimentController.updateReport.apply(this, callParams);
	};

	deleteExperiment = function(id) {
		// show confirmation dialog
		var dialog = new Ext.Window({
					title : "Confirm deletion",
					modal : true,
					layout : 'fit',
					autoHeight : true,
					width : 300,
					closeAction : 'hide',
					easing : 3,
					defaultType : 'textfield',
					items : [{
								xtype : 'label',
								text : "This cannot be undone"
							}],
					buttons : [{
								text : 'Cancel',
								handler : function() {
									dialog.hide();
								}
							}, {
								text : 'Confirm',
								handler : function() {
									dialog.hide();
									var callParams = []
									callParams.push(id);
									callParams.push({
												callback : handleWait.createDelegate(this)
											});
									ExpressionExperimentController.deleteById.apply(this, callParams);
								},
								scope : dialog
							}]
				});

		dialog.show();
	};

	var record = Ext.data.Record.create([{
				name : "id",
				type : "int"
			}, {
				name : "shortName"
			}, {
				name : "name"
			}, {
				name : "arrayDesignCount",
				type : "int"
			}, {
				name : "technologyType"
			}, {
				name : "hasBothIntensities",
				type : 'bool'
			}, {
				name : "bioAssayCount",
				type : "int"
			}, {
				name : "processedExpressionVectorCount",
				type : "int"
			}, {
				name : "externalUri"
			}, {
				name : "description"
			}, {
				name : "taxon"
			}, {
				name : "numAnnotations"
			}, {
				name : "numPopulatedFactors"
			}, {
				name : "isPublic"
			}, {
				name : "sourceExperiment"
			}, {
				name : "coexpressionLinkCount"
			}, {
				name : "validatedFlag"
			}, {
				name : "troubleFlag"
			}, {
				name : "missingValueAnalysisEventType"
			}, {
				name : "processedDataVectorComputationEventType"
			}, {
				name : "dateCreated",
				type : 'date'
			}, {
				name : "dateProcessedDataVectorComputation",
				type : 'date'
			}, {
				name : "dateMissingValueAnalysis",
				type : 'date'
			}, {
				name : "dateDifferentialAnalysis",
				type : 'date'
			}, {
				name : "dateLastUpdated",
				type : 'date'
			}, {
				name : "dateLinkAnalysis",
				type : 'date'
			}, {
				name : "linkAnalysisEventType"
			}, {
				name : "processedDataVectorComputationEventType"
			}, {
				name : "missingValueAnalysisEventType"
			}, {
				name : "differentialAnalysisEventType"
			}]);

	var dateRenderer = new Ext.util.Format.dateRenderer("y/M/d");

	var adminRenderer = function(value, metadata, record, rowIndex, colIndex, store) {
		return '<a href="#" onClick="return deleteExperiment('
				+ value
				+ ')"><img src="/Gemma/images/icons/delete.png" alt="delete" title="delete" /></a>&nbsp<a href="#" onClick="updateEEReport('
				+ value
				+ ')"><img src="/Gemma/images/icons/arrow_refresh_small.png" alt="refresh" title="refresh"/></a>'
				+ '&nbsp<a href="/Gemma/expressionExperiment/editExpressionExperiment.html?id=' + value
				+ '"  target="_blank"><img src="/Gemma/images/icons/pencil.png" alt="edit" title="edit"/></a>';
	};

	var shortNameRenderer = function(value, metadata, record, rowIndex, colIndex, store) {
		return '<a href="/Gemma/expressionExperiment/showExpressionExperiment.html?id=' + record.get("id")
				+ '" target="_blank">' + value + '</a>';
	};

	var rowExpander = new Gemma.EEGridRowExpander({
				tpl : ""
			});

	var diffIsPossible = function(record) {
		return record.get("numPopulatedFactors") > 0;
	};

	doLinks = function(id) {
		Ext.Msg.alert("Will run link analysis");
	};

	doMissingValues = function(id) {
		Ext.Msg.alert("Will run missing value analysis");
	};

	doProcessedVectors = function(id) {
		Ext.Msg.alert("Will compute processed vectors");
	};

	doDifferential = function(id) {
		Ext.Msg.alert("Will run differential expression analysis");
	};

	var linkAnalysisRenderer = function(value, metadata, record, rowIndex, colIndex, store) {
		if (record.get('dateLinkAnalysis')) {
			var type = record.get('linkAnalysisEventType');
			var color = "#3A3";
			var id = record.get('id');
			var suggestRun = true;
			var qtip = 'ext:qtip="OK"';
			if (type == 'FailedLinkAnalysisEventImpl') {
				color = 'red';
				qtip = 'ext:qtip="Failed"';
			} else if (type == 'TooSmallDatasetLinkAnalysisEventImpl') {
				color = '#CCC';
				qtip = 'ext:qtip="Too small"';
				suggestRun = false;
			}
			var runurl = '<a href="#" onClick="return doLinks('
					+ id
					+ ')"><img src="/Gemma/images/icons/control_play_blue.png" alt="link analysis" title="link analysis"/></a>';

			return '<span style="color:' + color + ';" ' + qtip + '>' + Ext.util.Format.date(value, 'y/M/d')
					+ (suggestRun ? runurl : '');
		} else {
			return '<span style="color:#3A3;">Needed</span>&nbsp' + runurl;
		}

	};

	var missingValueAnalysisRenderer = function(value, metadata, record, rowIndex, colIndex, store) {
		if (record.get('technologyType') != 'ONECOLOR' && record.get('hasBothIntensities')) {
			if (record.get('dateMissingValueAnalysis')) {
				var type = record.get('missingValueAnalysisEventType');
				var id = record.get('id');
				var color = "#3A3";
				var suggestRun = true;
				var qtip = 'ext:qtip="OK"';
				if (type == 'FailedMissingValueAnalysisEventImpl') {
					color = 'red';
					qtip = 'ext:qtip="Failed"';
				}

				var runurl = '<a href="#" onClick="return doMissingValues('
						+ id
						+ ')"><img src="/Gemma/images/icons/control_play_blue.png" alt="missing value computation" title="missing value computation"/></a>';

				return '<span style="color:' + color + ';" ' + qtip + '>' + Ext.util.Format.date(value, 'y/M/d')
						+ (suggestRun ? runurl : '');
			} else {
				return '<span style="color:#3A3;">Needed</span>&nbsp' + runurl;
			}

		} else {
			return '<span style="color:#CCF;">NA</span>';
		}
	};

	var processedVectorCreateRenderer = function(value, metadata, record, rowIndex, colIndex, store) {
		if (record.get('dateProcessedDataVectorComputation')) {
			var type = record.get('processedDataVectorComputationEventType');
			var color = "#3A3";
			var id = record.get('id');
			var suggestRun = true;
			var qtip = 'ext:qtip="OK"';
			if (type == 'FailedProcessedVectorComputationEventImpl') { // note: no such thing.
				color = 'red';
				qtip = 'ext:qtip="Failed"';
			}

			var runurl = '<a href="#" onClick="return doProcessedVectors('
					+ id
					+ ')"><img src="/Gemma/images/icons/control_play_blue.png" alt="processed vector computation" title="processed vector computation"/></a>';

			return '<span style="color:' + color + ';" ' + qtip + '>' + Ext.util.Format.date(value, 'y/M/d')
					+ (suggestRun ? runurl : '');
		} else {
			return '<span style="color:#3A3;">Needed</span>&nbsp' + runurl;
		}
	};

	var differentialAnalysisRenderer = function(value, metadata, record, rowIndex, colIndex, store) {
		if (diffIsPossible(record)) {
			if (record.get('dateDifferentialAnalysis')) {
				// TODO
				var type = record.get('differentialAnalysisEventType');
				var id = record.get('id');
				var color = "#3A3";
				var suggestRun = true;
				var qtip = 'ext:qtip="OK"';
				if (type == 'FailedDifferentialExpressionAnalysisEventImpl') { // note: no such thing.
					color = 'red';
					qtip = 'ext:qtip="Failed"';
				}

				var runurl = '<a href="#" onClick="return doDifferential('
						+ id
						+ ')"><img src="/Gemma/images/icons/control_play_blue.png" alt="differential expression analysis" title="differential expression analysis"/></a>';

				return '<span style="color:' + color + ';" ' + qtip + '>' + Ext.util.Format.date(value, 'y/M/d')
						+ (suggestRun ? runurl : '');
			} else {
				return '<span style="color:#3A3;">Needed</span>&nbsp' + runurl;
			}
		} else {
			return '<span style="color:#CCF;">NA</span>';
		}
	};

	var flagRenderer = function(value, metadata, record, rowIndex, colIndex, store) {
		var result = '';
		if (record.get('validatedFlag')) {
			result = result + '<img src="/Gemma/images/icons/emoticon_smile.png" alt="validated" title="validated"/>';
		}

		if (record.get('troubleFlag')) {

			result = result + '<img src="/Gemma/images/icons/error.png" alt="trouble" title="trouble"/>';
		}
		if (!record.get('isPublic')) {
			result = result + '<img src="/Gemma/images/icons/lock.png" alt="not public" title="not public"/>';
		}
		return result;

	};

	var columns = [rowExpander, {
				header : 'Short Name',
				sortable : true,
				dataIndex : 'shortName',
				renderer : shortNameRenderer
			}, {
				header : 'Name',
				sortable : true,
				dataIndex : 'name'
			}, {
				header : 'Taxon',
				sortable : true,
				dataIndex : 'taxon'
			}, {
				header : 'Flags',
				sortable : true,
				renderer : flagRenderer
			}, {
				header : '#ADs',
				sortable : true,
				dataIndex : 'arrayDesignCount',
				width : 35
			}, {
				header : '#BAs',
				sortable : true,
				dataIndex : 'bioAssayCount',
				width : 35
			}, {
				header : '#Vecs',
				sortable : true,
				dataIndex : 'processedExpressionVectorCount',
				width : 45
			}, {
				header : '#Facs',
				sortable : true,
				dataIndex : 'numPopulatedFactors',
				width : 35
			}, {
				header : '#anns',
				sortable : true,
				dataIndex : 'numAnnotations',
				width : 35
			}, {
				header : 'Created',
				sortable : true,
				dataIndex : 'dateCreated',
				renderer : dateRenderer
			}, {
				header : 'MissingVals',
				sortable : true,
				dataIndex : 'dateMissingValueAnalysis',
				renderer : missingValueAnalysisRenderer
			}, {
				header : 'ProcessVec',
				sortable : true,
				dataIndex : 'dateProcessedDataVectorComputation',
				renderer : processedVectorCreateRenderer
			}, {
				header : 'Diff',
				sortable : true,
				dataIndex : 'dateDifferentialAnalysis',
				renderer : differentialAnalysisRenderer
			}, {
				header : 'Links',
				sortable : true,
				dataIndex : 'dateLinkAnalysis',
				renderer : linkAnalysisRenderer
			}, {
				header : 'Admin',
				sortable : false,
				dataIndex : 'id',
				renderer : adminRenderer
			}];

	var store = new Ext.data.Store({
				proxy : new Ext.data.DWRProxy(ExpressionExperimentController.loadStatusSummaries),
				reader : new Ext.data.ListRangeReader({
							id : "id"
						}, record),
				remoteSort : false,
				sortInfo : {
					field : 'dateCreated',
					direction : 'DESC'
				}
			});
	store.load({
				params : [null]
			});

	var tpl = new Ext.XTemplate('<tpl for="."><div class="itemwrap" id="{shortName}">',
			'<p>{id} {name} {shortName} {externalUri} {[this.log(values.id)]}</p>', "</div></tpl>", {
				log : function(id) {
					console.log(id);
				}
			});

	var manager = new Ext.Panel({
				renderTo : 'eemanage',
				width : '100%',
				autoHeight : true,
				layout : 'fit',
				items : [new Gemma.EEReportPanel({
							store : store,
							loadMask : true,
							autoHeight : true,
							columns : columns,
							rowExpander : rowExpander,
							plugins : rowExpander

						})]

			});

});

/**
 * 
 * @class Gemma.EEReportPanel
 * @extends Ext.grid.GridPanel
 */
Gemma.EEReportPanel = Ext.extend(Ext.grid.GridPanel, {
			searchForText : function(button, keyev) {
				var text = this.searchInGridField.getValue();
				if (text.length < 2) {
					clearFilter();
					return;
				}
				this.getStore().filterBy(this.getSearchFun(text), this, 0);
			},

			clearFilter : function() {
				this.getStore().clearFilter();
			},

			getSearchFun : function(text) {
				var value = new RegExp(Ext.escapeRe(text), 'i');
				return function(r, id) {
					var obj = r.data;
					return value.match(obj.name) || value.match(obj.shortName);
				}
			},

			refresh : function() {
				this.store.reload();
			},

			initComponent : function() {
				this.searchInGridField = new Ext.form.TextField({
							enableKeyEvents : true,
							emptyText : 'Filter',
							tooltip : "Text typed here will ",
							listeners : {
								"keyup" : {
									fn : this.searchForText.createDelegate(this),
									scope : this,
									options : {
										delay : 100
									}
								}
							}
						});

				Ext.apply(this, {
							tbar : new Ext.Toolbar({
										items : [{
													xtype : 'button',
													minWidth : 20,
													cls : 'x-btn-icon',
													icon : '/Gemma/images/icons/arrow_refresh_small.png',
													handler : this.refresh,
													tooltip : "Refresh the table",
													scope : this
												}, '->', {
													xtype : 'button',
													handler : this.clearFilter.createDelegate(this),
													tooltip : "Show all",
													scope : this,
													cls : 'x-btn-text',
													text : 'Reset filter'
												}, ' ', this.searchInGridField]
									})
						});
				Gemma.EEReportPanel.superclass.initComponent.call(this);
			}
		});
