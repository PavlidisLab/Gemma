Ext.namespace('Gemma');
Ext.BLANK_IMAGE_URL = '/Gemma/images/default/s.gif';

Gemma.EE_REPORT_PAGE_SIZE = 100;

Ext.onReady(function() {
	Ext.QuickTips.init();
	Ext.state.Manager.setProvider(new Ext.state.CookieProvider());

		
	manager = new Gemma.EEManager({
				editable : true,
				id : 'eemanager'
			});

	if (Ext.get('updateAllReports-area')) {
		Ext.DomHelper.overwrite('updateAllReports-area', '<p>' + 'To update all reports click here: '
						+ '<a href="#" onClick="Ext.getCmp(\'eemanager\').updateAllEEReports(1)"><img '
						+ 'src="/Gemma/images/icons/arrow_refresh_small.png" title="refresh all reports" /> </a>'
						+ '</p>');
	}

	this.manager = manager;

	var store = new Gemma.PagingDataStore({
				proxy : new Ext.data.DWRProxy(ExpressionExperimentController.loadStatusSummaries),
				reader : new Ext.data.ListRangeReader({
							id : "id"
						}, manager.record),
				remoteSort : false,
				pageSize : Gemma.EE_REPORT_PAGE_SIZE,
				sortInfo : {
					field : 'dateCreated',
					direction : 'DESC'
				}
			});

	manager.on('done', function() {
				store.reload();
			});
	manager.on('tagsUpdated', function() {
				store.reload();
			});

	var dateRenderer = new Ext.util.Format.dateRenderer("y/M/d");

	var adminRenderer = function(value, metadata, record, rowIndex, colIndex, store) {
		var adminLink =  '<a href="#" onClick="Ext.getCmp(\'eemanager\').updateEEReport('
				+ value
				+ ')"><img src="/Gemma/images/icons/arrow_refresh_small.png" ext:qtip="Refresh statistics"  title="refresh"/></a>';
				// + '&nbsp;<a href="/Gemma/expressionExperiment/editExpressionExperiment.html?id='
				// + value
				// + '" target="_blank"><img src="/Gemma/images/icons/wrench.png" ext:qtip="Go to editor page for this
				// experiment" title="edit"/></a>'
								
				var isAdmin = Ext.get("hasAdmin").getValue() == 'true';
				if (isAdmin){
					adminLink = adminLink + '&nbsp;&nbsp;&nbsp;<a href="#" onClick="return Ext.getCmp(\'eemanager\').deleteExperiment('
					+ value
					+ ')"><img src="/Gemma/images/icons/cross.png" ext:qtip="Delete the experiment from the system" title="delete" /></a>&nbsp;';
				}
				return adminLink;
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

	var experimentalDesignEditRenderer = function(value, metadata, record, rowIndex, colIndex, store) {
		var id = record.get('id');
		var url = '<a target="_blank" href="/Gemma/experimentalDesign/showExperimentalDesign.html?eeid='
				+ id
				+ '"><img src="/Gemma/images/icons/pencil.png" alt="view/edit experimental design" title="view/edit experimental design"/></a>';
		return value + '&nbsp;' + url;
	};

	var experimentTaggerRenderer = function(value, metadata, record, rowIndex, colIndex, store) {
		var id = record.get('id');
		var url = '<a href="#" onClick="return Ext.getCmp(\'eemanager\').tagger(' + id
				+ ')"><img src="/Gemma/images/icons/pencil.png" alt="add tags" title="add tags"/></a>';
		return value + '&nbsp;' + url;
	};

	var linkAnalysisRenderer = function(value, metadata, record, rowIndex, colIndex, store) {
		var id = record.get('id');
		var runurl = '<a href="#" onClick="return Ext.getCmp(\'eemanager\').doLinks('
				+ id
				+ ')"><img src="/Gemma/images/icons/control_play_blue.png" ext:qtip="Run coexpression analysis"  alt="link analysis" /></a>';
		if (record.get('dateLinkAnalysis')) {
			var type = record.get('linkAnalysisEventType');
			var color = "#000";
			var suggestRun = true;
			var qtip = 'ext:qtip="OK"';
			if (type == 'FailedLinkAnalysisEventImpl') {
				color = 'red';
				qtip = 'ext:qtip="Failed"';
			} else if (type == 'TooSmallDatasetLinkAnalysisEventImpl') {
				color = '#CCC';
				qtip = 'ext:qtip="Too small to perform link analysis"';
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
		var runurl = '<a href="#" onClick="return Ext.getCmp(\'eemanager\').doMissingValues('
				+ id
				+ ')"><img src="/Gemma/images/icons/control_play_blue.png" ext:qtip="Run missing value analysis" alt="missing value computation"  /></a>';
		if (record.get('technologyType') != 'ONECOLOR' && record.get('hasBothIntensities')) {
			if (record.get('dateMissingValueAnalysis')) {
				var type = record.get('missingValueAnalysisEventType');
				var color = "#000";
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
			return '<span style="color:#CCF;" ext:qtip="Only relevant for two-channel microarray studies">NA</span>';
		}
	};

	var processedVectorCreateRenderer = function(value, metadata, record, rowIndex, colIndex, store) {
		var id = record.get('id');
		var runurl = '<a href="#" onClick="return Ext.getCmp(\'eemanager\').doProcessedVectors('
				+ id
				+ ')"><img src="/Gemma/images/icons/control_play_blue.png" ext:qtip="Run processed vector generation" alt="processed vector generation"/></a>';

		if (record.get('dateProcessedDataVectorComputation')) {
			var type = record.get('processedDataVectorComputationEventType');
			var color = "#000";

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
		var runurl = '<a href="#" onClick="return Ext.getCmp(\'eemanager\').doDifferential('
				+ id
				+ ')"><img src="/Gemma/images/icons/control_play_blue.png" alt="differential expression analysis" ext:qtip="Run differential expression analysis"/></a>';

		if (diffIsPossible(record)) {
			if (record.get('dateDifferentialAnalysis')) {
				var type = record.get('differentialAnalysisEventType');

				var color = "#000";
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
			return '<span style="color:#CCF;" ext:qtip="You must create at least one experimental factor to enable this analysis.">NA</span>';
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
				dataIndex : 'taxon',
				width : 40
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

	store.load({
				params : [null]
			});

	var tpl = new Ext.XTemplate('<tpl for="."><div class="itemwrap" id="{shortName}">',
			'<p>{id} {name} {shortName} {externalUri} {[this.log(values.id)]}</p>', "</div></tpl>", {
				log : function(id) {
					// console.log(id);
				}
			});

	var manager = new Gemma.EEReportPanel({
				renderTo : 'eemanage',
				store : store,
				loadMask : true,
				autoHeight : true,
				minHeight : 70,
				width : 1250,
				columns : columns,
				rowExpander : rowExpander,
				plugins : rowExpander

			});

});

/**
 * 
 * @class Gemma.EEReportPanel
 * @extends Ext.grid.GridPanel
 */
Gemma.EEReportPanel = Ext.extend(Ext.grid.GridPanel, {

			viewConfig : {
				autoFill : true,
				forceFit : false
			},

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
									}),
							bbar : new Gemma.PagingToolbar({
										pageSize : Gemma.EE_REPORT_PAGE_SIZE,
										store : this.store
									})
						});
				Gemma.EEReportPanel.superclass.initComponent.call(this);
			}
		});
