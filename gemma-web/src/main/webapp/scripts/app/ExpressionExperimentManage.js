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

	tagger = function(id) {
		var anngrid = new Gemma.AnnotationGrid({
					minHeight : 90,
					readMethod : ExpressionExperimentController.getAnnotation,
					readParams : [{
								id : id,
								classDelegatingFor : "ubic.gemma.model.expression.experiment.ExpressionExperimentImpl"
							}],
					writeMethod : OntologyService.saveExpressionExperimentStatement,
					removeMethod : OntologyService.removeExpressionExperimentStatement,
					editable : true,
					mgedTermKey : "experiment",
					entId : id
				});

		var change = false;
		anngrid.on('refresh', function() {
					change = true;
				});
		var w = new Ext.Window({
					title : "Add tags",
					modal : true,
					layout : 'fit',
					items : [anngrid],
					buttons : [{
						text : 'Help',
						handler : function() {
							Ext.Msg
									.alert(
											"Help with tagging",
											"Select a 'category' for the term; then enter a term, "
													+ "choosing from existing terms if possible. "
													+ "Click 'create' to save it. You can also edit existing terms;"
													+ " click 'save' to make the change stick, or 'delete' to remove a selected tag.");
						}
					}, {
						text : 'Done',
						handler : function() {
							if (change) {
								store.reload();
							}
							w.hide();
						}
					}]

				});

		w.show();
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
		Ext.Msg.show({
					title : 'Really delete?',
					msg : 'Are you sure you want to delete the experiment? This cannot be undone.',
					buttons : Ext.Msg.YESNO,
					fn : function(btn, text) {
						if (btn == 'yes') {
							var callParams = []
							callParams.push(id);
							callParams.push({
										callback : handleWait.createDelegate(this)
									});
							ExpressionExperimentController.deleteById.apply(this, callParams);
						}
					},
					animEl : 'elId',
					icon : Ext.MessageBox.WARNING
				});
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
		return '<a href="#" onClick="updateEEReport('
				+ value
				+ ')"><img src="/Gemma/images/icons/arrow_refresh_small.png" ext:qtip="Refresh statistics"  title="refresh"/></a>'
				+ '&nbsp;<a href="/Gemma/expressionExperiment/editExpressionExperiment.html?id='
				+ value
				+ '"  target="_blank"><img src="/Gemma/images/icons/wrench.png" ext:qtip="Go to editor page for this experiment" title="edit"/></a><a href="#" onClick="return deleteExperiment('
				+ value
				+ ')"><img src="/Gemma/images/icons/cross.png" ext:qtip="Delete the experiment from the system" title="delete" /></a>&nbsp;';
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

	var experimentalDesignEditRenderer = function(value, metadata, record, rowIndex, colIndex, store) {
		var id = record.get('id');
		var url = '<a target="_blank" href="/Gemma/experimentalDesign/showExperimentalDesign.html?id='
				+ id
				+ '"><img src="/Gemma/images/icons/pencil.png" alt="view/edit experimental design" title="view/edit experimental design"/></a>';
		return value + '&nbsp;' + url;
	};

	var experimentTaggerRenderer = function(value, metadata, record, rowIndex, colIndex, store) {
		var id = record.get('id');
		var url = '<a href="#" onClick="return tagger(' + id
				+ ')"><img src="/Gemma/images/icons/pencil.png" alt="add tags" title="add tags"/></a>';
		return value + '&nbsp;' + url;
	};

	var linkAnalysisRenderer = function(value, metadata, record, rowIndex, colIndex, store) {
		var id = record.get('id');
		var runurl = '<a href="#" onClick="return doLinks('
				+ id
				+ ')"><img src="/Gemma/images/icons/control_play_blue.png" alt="link analysis" title="link analysis"/></a>';
		if (record.get('dateLinkAnalysis')) {
			var type = record.get('linkAnalysisEventType');
			var color = "#3A3";
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

			return '<span style="color:' + color + ';" ' + qtip + '>' + Ext.util.Format.date(value, 'y/M/d') + '&nbsp;'
					+ (suggestRun ? runurl : '');
		} else {
			return '<span style="color:#3A3;">Needed</span>&nbsp;' + runurl;
		}

	};

	var missingValueAnalysisRenderer = function(value, metadata, record, rowIndex, colIndex, store) {
		var id = record.get('id');
		var runurl = '<a href="#" onClick="return doMissingValues('
				+ id
				+ ')"><img src="/Gemma/images/icons/control_play_blue.png" alt="missing value computation" title="missing value computation"/></a>';
		if (record.get('technologyType') != 'ONECOLOR' && record.get('hasBothIntensities')) {
			if (record.get('dateMissingValueAnalysis')) {
				var type = record.get('missingValueAnalysisEventType');
				var color = "#3A3";
				var suggestRun = true;
				var qtip = 'ext:qtip="OK"';
				if (type == 'FailedMissingValueAnalysisEventImpl') {
					color = 'red';
					qtip = 'ext:qtip="Failed"';
				}

				return '<span style="color:' + color + ';" ' + qtip + '>' + Ext.util.Format.date(value, 'y/M/d')
						+ '&nbsp;' + (suggestRun ? runurl : '');
			} else {
				return '<span style="color:#3A3;">Needed</span>&nbsp;' + runurl;
			}

		} else {
			return '<span style="color:#CCF;">NA</span>';
		}
	};

	var processedVectorCreateRenderer = function(value, metadata, record, rowIndex, colIndex, store) {
		var id = record.get('id');
		var runurl = '<a href="#" onClick="return doProcessedVectors('
				+ id
				+ ')"><img src="/Gemma/images/icons/control_play_blue.png" alt="processed vector computation" title="processed vector computation"/></a>';

		if (record.get('dateProcessedDataVectorComputation')) {
			var type = record.get('processedDataVectorComputationEventType');
			var color = "#3A3";

			var suggestRun = true;
			var qtip = 'ext:qtip="OK"';
			if (type == 'FailedProcessedVectorComputationEventImpl') { // note: no such thing.
				color = 'red';
				qtip = 'ext:qtip="Failed"';
			}

			return '<span style="color:' + color + ';" ' + qtip + '>' + Ext.util.Format.date(value, 'y/M/d') + '&nbsp;'
					+ (suggestRun ? runurl : '');
		} else {
			return '<span style="color:#3A3;">Needed</span>&nbsp;' + runurl;
		}
	};

	var differentialAnalysisRenderer = function(value, metadata, record, rowIndex, colIndex, store) {
		var id = record.get('id');
		var runurl = '<a href="#" onClick="return doDifferential('
				+ id
				+ ')"><img src="/Gemma/images/icons/control_play_blue.png" alt="differential expression analysis" title="differential expression analysis"/></a>';

		if (diffIsPossible(record)) {
			if (record.get('dateDifferentialAnalysis')) {
				var type = record.get('differentialAnalysisEventType');

				var color = "#3A3";
				var suggestRun = true;
				var qtip = 'ext:qtip="OK"';
				if (type == 'FailedDifferentialExpressionAnalysisEventImpl') { // note: no such thing.
					color = 'red';
					qtip = 'ext:qtip="Failed"';
				}

				return '<span style="color:' + color + ';" ' + qtip + '>' + Ext.util.Format.date(value, 'y/M/d')
						+ '&nbsp;' + (suggestRun ? runurl : '');
			} else {
				return '<span style="color:#3A3;">Needed</span>&nbsp;' + runurl;
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

			result = result + '<img src="/Gemma/images/icons/stop.png" alt="trouble" title="trouble"/>';
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
				renderer : flagRenderer,
				tooltip : 'Status flags'
			}, {
				header : '#ADs',
				sortable : true,
				dataIndex : 'arrayDesignCount',
				tooltip : "The number of different array design platforms used in the study",
				width : 35
			}, {
				header : '#BAs',
				sortable : true,
				dataIndex : 'bioAssayCount',
				tooltip : 'The number of samples in the study',
				width : 35
			}, {
				header : '#Prof',
				sortable : true,
				dataIndex : 'processedExpressionVectorCount',
				tooltip : 'The number of expression profiles',
				width : 45
			}, {
				header : '#Facs',
				sortable : true,
				dataIndex : 'numPopulatedFactors',
				renderer : experimentalDesignEditRenderer,
				tooltip : 'The number of experimental factors (variables) defined for the study',
				width : 45
			}, {
				header : '#tags',
				sortable : true,
				dataIndex : 'numAnnotations',
				renderer : experimentTaggerRenderer,
				tooltip : 'The number of terms the experiment is tagged with',
				width : 45
			}, {
				header : 'Created',
				sortable : true,
				dataIndex : 'dateCreated',
				tooltip : 'Create date',
				renderer : dateRenderer
			}, {
				header : 'MissingVals',
				sortable : true,
				dataIndex : 'dateMissingValueAnalysis',
				tooltip : 'Status of missing value computation (two-channel studies only)',
				renderer : missingValueAnalysisRenderer
			}, {
				header : 'ProcProf',
				sortable : true,
				dataIndex : 'dateProcessedDataVectorComputation',
				tooltip : 'Status of processed expression profile configuration',
				renderer : processedVectorCreateRenderer
			}, {
				header : 'Diff',
				sortable : true,
				dataIndex : 'dateDifferentialAnalysis',
				tooltip : 'Status of differential expression analysis. Must have factors to enable',
				renderer : differentialAnalysisRenderer
			}, {
				header : 'Links',
				sortable : true,
				dataIndex : 'dateLinkAnalysis',
				tooltip : 'Status of coexpression analysis',
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
					this.clearFilter();
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
