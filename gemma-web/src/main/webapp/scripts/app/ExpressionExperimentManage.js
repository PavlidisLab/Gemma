Ext.namespace('Gemma');
Ext.BLANK_IMAGE_URL = '/Gemma/images/default/s.gif';

/**
 * Show table of multiple experiments
 * 
 * @see EEManager
 */
Ext.onReady(function() {
	Ext.QuickTips.init();
	Ext.state.Manager.setProvider(new Ext.state.CookieProvider());

	var manager = new Gemma.EEManager({
				editable : true,
				id : 'eemanager'
			});

	if (Ext.get('updateAllReports-area')) {
		Ext.DomHelper.overwrite('updateAllReports-area', '<p>' + 'To update all reports click here: '
						+ '<span class="link" onClick="Ext.getCmp(\'eemanager\').updateAllEEReports(1)"><img '
						+ 'src="/Gemma/images/icons/arrow_refresh_small.png" title="refresh all reports" /> </span>'
						+ '</p>');
	}

	this.manager = manager;

	var store = new Gemma.PagingDataStore({

				proxy : new Ext.data.DWRProxy(ExpressionExperimentController.loadStatusSummaries),

				reader : new Ext.data.ListRangeReader({
							id : "id"
						}, manager.record),
				remoteSort : false,
				sortInfo : {
					field : 'dateCreated',
					direction : 'DESC'
				},
				sort : function(fieldName, dir) {
					store.fireEvent('beforesort');
					/*
					 * Sorting this table is slooow. We need to pause to allow time for the loadmask to display.
					 */
					var t = new Ext.util.DelayedTask(function() {
								Gemma.PagingDataStore.superclass.sort.call(store, fieldName, dir);
								store.fireEvent('aftersort');
							});
					t.delay(100);

				}
			});

	store.addEvents({
				'beforesort' : true,
				'aftersort' : true
			});

	manager.on('done', function() {
				store.reload();
			});

	manager.on('tagsUpdated', function() {
				store.reload();
			});

	var dateRenderer = new Ext.util.Format.dateRenderer("y/M/d");

	var adminRenderer = function(value, metadata, record, rowIndex, colIndex, store) {

		if (record.get("currentUserHasWritePermission")) {
			var adminLink = '<span class="link"  onClick="Ext.getCmp(\'eemanager\').updateEEReport('
					+ value
					+ ')"><img src="/Gemma/images/icons/arrow_refresh_small.png" ext:qtip="Refresh statistics"  ext:qtip="refresh"/></span>';

			var isAdmin = Ext.get("hasAdmin").getValue() == 'true';
			if (isAdmin) {
				adminLink = adminLink
						+ '&nbsp;&nbsp;&nbsp;<span class="link" onClick="return Ext.getCmp(\'eemanager\').deleteExperiment('
						+ value
						+ ')"><img src="/Gemma/images/icons/cross.png" ext:qtip="Delete the experiment from the system" ext:qtip="delete" /></span>&nbsp;';
			}
			return adminLink;
		}
		return "(no permission)";

	};

	var shortNameRenderer = function(value, metadata, record, rowIndex, colIndex, store) {
		return '<a href="/Gemma/expressionExperiment/showExpressionExperiment.html?id='
				+ (record.get("sourceExperiment") ? record.get("sourceExperiment") : record.get("id"))
				+ '" target="_blank">' + value + '</a>';
	};

	// var rowExpander = new Gemma.EEGridRowExpander({
	// tpl : ""
	// });

	var diffIsPossible = function(record) {
		return record.get("numPopulatedFactors") > 0 && record.get("currentUserHasWritePermission");
	};

	var experimentalDesignEditRenderer = function(value, metadata, record, rowIndex, colIndex, store) {
		var id = record.get('id');
		var url = '<a target="_blank" href="/Gemma/experimentalDesign/showExperimentalDesign.html?eeid='
				+ id
				+ '"><img src="/Gemma/images/icons/pencil.png" alt="view/edit experimental design" ext:qtip="view/edit experimental design"/></a>';
		return value + '&nbsp;' + url;
	};

	var experimentTaggerRenderer = function(value, metadata, record, rowIndex, colIndex, store) {
		var id = record.get('id');
		var taxonId = record.get('taxonId');

		var url = '<span class="link" onClick="return Ext.getCmp(\'eemanager\').tagger(' + id + ',' + taxonId + ','
				+ record.get("currentUserHasWritePermission") + ',' + (record.get("validatedAnnotations") !== null)
				+ ')"><img src="/Gemma/images/icons/pencil.png" alt="view tags" ext:qtip="add/view tags"/></span>';
		value = value + '&nbsp;' + url;

		if (record.get("currentUserHasWritePermission")) {
			var turl;
			if (record.get('autoTagDate')) {
				var icon = "/Gemma/images/icons/wand.png";
				turl = '<span class="link"  onClick="return Ext.getCmp(\'eemanager\').autoTag(' + id + ')"><img src="'
						+ icon + '" alt="run auto-tagger" ext:qtip="tagger was run on '
						+ Ext.util.Format.date(record.get('autoTagDate'), 'y/M/d') + '; click to re-run"/></span>';
			} else {
				var icon = "/Gemma/images/icons/wand--plus.png";
				turl = '<span class="link"  onClick="return Ext.getCmp(\'eemanager\').autoTag(' + id + ')"><img src="'
						+ icon + '" alt="run auto-tagger" ext:qtip="add tags automatically"/></span>';
			}
			value = value + '&nbsp;' + turl;
		}

		return value;
	};

	var BIG_ENOUGH_FOR_LINKS = 7; // FIXME externalize this! And it should be based on biomaterials. and it should
	// come from the server side.

	var linkAnalysisRenderer = function(value, metadata, record, rowIndex, colIndex, store) {
		var id = record.get('id');
		var runurl = "";
		if (record.get("currentUserHasWritePermission")) {
			runurl = '<span class="link" onClick="return Ext.getCmp(\'eemanager\').doLinks('
					+ id
					+ ')"><img src="/Gemma/images/icons/control_play_blue.png" ext:qtip="Run coexpression analysis"  alt="link analysis" /></span>';
		}

		if (record.get('bioAssayCount') < BIG_ENOUGH_FOR_LINKS) {
			return '<span style="color:#CCC;">Too small</span>&nbsp;';
		}

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

		var runurl = "";
		if (record.get("currentUserHasWritePermission")) {
			runurl = '<span class="link" onClick="return Ext.getCmp(\'eemanager\').doMissingValues('
					+ id
					+ ')"><img src="/Gemma/images/icons/control_play_blue.png" ext:qtip="Run missing value analysis" alt="missing value computation"  /></span>';
		}

		/*
		 * Offer missing value analysis if it's possible (this might need tweaking).
		 */
		if (record.get('technologyType') != 'ONECOLOR' && record.get('hasEitherIntensity')) {
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
			return '<span style="color:#CCF;" ext:qtip="Only relevant for two-channel microarray studies with intensity data available.">NA</span>';
		}
	};

	var processedVectorCreateRenderer = function(value, metadata, record, rowIndex, colIndex, store) {
		var id = record.get('id');
		var runurl = "";
		if (record.get("currentUserHasWritePermission")) {
			runurl = '<span class="link" onClick="return Ext.getCmp(\'eemanager\').doProcessedVectors('
					+ id
					+ ')"><img src="/Gemma/images/icons/control_play_blue.png" ext:qtip="Run processed vector generation" alt="processed vector generation"/></span>';
		}

		if (record.get('dateProcessedDataVectorComputation')) {
			var type = record.get('processedDataVectorComputationEventType');
			var color = "#000";

			var suggestRun = true;
			var qtip = 'ext:qtip="OK"';
			if (type == 'FailedProcessedVectorComputationEventImpl') {
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

		var runurl = "";
		if (record.get("currentUserHasWritePermission")) {
			runurl = '<span class="link" onClick="return Ext.getCmp(\'eemanager\').doDifferential('
					+ id
					+ ')"><img src="/Gemma/images/icons/control_play_blue.png" alt="differential expression analysis" ext:qtip="Run differential expression analysis"/></span>';
		}

		if (diffIsPossible(record)) {
			if (record.get('dateDifferentialAnalysis')) {
				var type = record.get('differentialAnalysisEventType');

				var color = "#000";
				var suggestRun = true;
				var qtip = 'ext:qtip="OK"';
				if (type == 'FailedDifferentialExpressionAnalysisEventImpl') {
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
		var id = record.get('id');
		var result = '';
		if (record.get('validatedFlag')) {
			result = result
					+ '<img src="/Gemma/images/icons/emoticon_smile.png" alt="validated" ext:qtip="validated"/>';
		}

		if (record.get('troubleFlag')) {
			result = result + '<img src="/Gemma/images/icons/stop.png" alt="trouble" ext:qtip="trouble: '
					+ record.get('troubleFlag').note + '"/>';
		}

		result = result
				+ Gemma.SecurityManager.getSecurityLink(
						'ubic.gemma.model.expression.experiment.ExpressionExperimentImpl', id, record.get('isPublic'),
						record.get('isShared'), record.get('currentUserHasWritePermission'));

		return result;

	};

	var detailsmask = null;

	var showEEDetails = function(model, rowindex, record) {

		if (detailsmask == null) {
			detailsmask = new Ext.LoadMask(dataSetDetailsPanel.body, {
						msg : "Loading details ..."
					});
		}

		detailsmask.show();
		ExpressionExperimentController.getDescription(record.id, {
					callback : function(data) {
						Ext.DomHelper
								.overwrite(
										dataSetDetailsPanel.body,
										'<span class="big">'
												+ shortNameRenderer(record.get('shortName'), null, record)
												+ '</span>&nbsp;&nbsp;<span class="medium">'
												+ record.get('name')
												+ "</span><p>"
												+ data
												+ "</p>"
												+ '<span class="link" onClick="Ext.getCmp(\'eemanager\').showAuditWindow('
												+ record.id
												+ ');" ><img ext:qtip="Show history" src="/Gemma/images/icons/pencil.png" /></span>');
						detailsmask.hide();
					}.createDelegate(this)
				});

	};

	var columns = [{
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
				tooltip : 'Status flags',
				width : 40
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
				width : 60
			}, {
				header : 'Created',
				sortable : true,
				dataIndex : 'dateCreated',
				tooltip : 'Create date',
				renderer : dateRenderer,
				width : 80
			}, {
				header : 'Updated',
				sortable : true,
				dataIndex : 'dateLastUpdated',
				tooltip : 'Update date; not all possible types of updates are considered.',
				renderer : dateRenderer,
				width : 80
			}, {
				header : 'MissingVals',
				sortable : true,
				dataIndex : 'dateMissingValueAnalysis',
				tooltip : 'Status of missing value computation (two-channel studies only)',
				renderer : missingValueAnalysisRenderer,
				width : 80
			}, {
				header : 'ProcProf',
				sortable : true,
				dataIndex : 'dateProcessedDataVectorComputation',
				tooltip : 'Status of processed expression profile configuration',
				renderer : processedVectorCreateRenderer,
				width : 80
			}, {
				header : 'Diff',
				sortable : true,
				dataIndex : 'dateDifferentialAnalysis',
				tooltip : 'Status of differential expression analysis. Must have factors to enable',
				renderer : differentialAnalysisRenderer,
				width : 90
			}, {
				header : 'Links',
				sortable : true,
				dataIndex : 'dateLinkAnalysis',
				tooltip : 'Status of coexpression analysis',
				renderer : linkAnalysisRenderer,
				width : 90
			}, {
				header : 'Admin',
				sortable : false,
				dataIndex : 'id',
				renderer : adminRenderer,
				width : 60
			}];

	var limit = 100;

	var tpl = new Ext.XTemplate('<tpl for="."><div class="itemwrap" id="{shortName}">',
			'<p>{id} {name} {shortName} {externalUri} {[this.log(values.id)]}</p>', "</div></tpl>", {
				log : function(id) {
					// console.log(id);
				}
			});

	/*
	 * If the URL contains a list of IDs, limit ourselves to that.
	 */
	var queryStart = document.URL.indexOf("?");
	var ids = null;
	var taxonid = null;
	var filterMode = null;
	if (queryStart > -1) {
		var urlParams = Ext.urlDecode(document.URL.substr(queryStart + 1));
		ids = urlParams.ids ? urlParams.ids.split(',') : null;
		taxonid = urlParams.taxon ? urlParams.taxon : null;
		limit = urlParams.taxon ? urlParams.limit : 100;
		filterMode = urlParams.filter ? urlParams.filter : null;
	}

	var reportGrid = new Gemma.EEReportPanel({
		store : store,
		title : 'Experiment manager',
		region : 'center',
		loadMask : true,
		height : 500,
		taxonid : taxonid,
		limit : limit,
		filterMode : filterMode,
		ids : ids,
		colModel : new Ext.grid.ColumnModel(columns)
			// too buggy
			// ,colModel : new Ext.ux.grid.LockingColumnModel(columns),
			// view : new Ext.ux.grid.LockingGridView({
			// syncHeights : true
			// })
		});

	reportGrid.getSelectionModel().on('rowselect', showEEDetails, this, {
		buffer : 100
			// keep from firing too many times at once
		});

	var dataSetDetailsPanel = new Ext.Panel({
				id: 'dataSetDetailsPanel',
				region : 'south',
				split : true,
				bodyStyle : 'padding:8px',
				height : 200,
				autoScroll : true
			});

	// if window is wider than 1100, give wider panel
	var availablePanelWidth = Ext.getBody().getViewSize().width * 0.9;
	var minPanelWidth = 1100;
	var pan = new Ext.Panel({
				layout : 'border',
				renderTo : 'eemanage',
				width : (availablePanelWidth > minPanelWidth)? availablePanelWidth : minPanelWidth,
				height : 700,
				items : [reportGrid, dataSetDetailsPanel]
			});

	store.on('beforesort', function() {
				reportGrid.loadMask.show();
			});

	store.on('aftersort', function() {
				reportGrid.loadMask.hide()
			});

	store.load({
				params : [taxonid, ids, limit, filterMode]
			});

	store.on('load', function(store, records, options) {
				reportGrid.setTitle("Showing " + records.length + " records.");
			});

	store.on("exception", function(scope, args, data, e) {
				Ext.Msg.alert('There was an error', e + ".  \nPlease try again.");
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

			filterType : null,

			filterByNeed : function(box, record, index) {
				this.filterType = record.get('filterType');
				this.store.load({
							params : [this.taxonid, this.ids, this.limit, this.filterType]
						});
			},

			filterByTaxon : function(box, record, index) {
				
				// if user selected 'All taxa', load all datasets
				if(record.get('commonName') == "All taxa"){
					this.taxonid= null;
				}else{
					this.taxonid = record.get('id')
				}
				
				this.store.load({
					params : [this.taxonid, this.ids, this.limit, this.filterType]
				});
				
			},

			filterByLimit : function(box, record, index) {
				this.limit = record.get('count');
				this.store.load({
							params : [this.taxonid, this.ids, this.limit, this.filterType]
						});
			},

			downloadAsText : function() {
				alert("Sorry, coming soon");
			},

			getBookmark : function() {
				var url = Gemma.BASEURL + "expressionExperiment/showAllExpressionExperimentLinkSummaries.html?";
				if (this.ids) {
					url += "&ids=" + this.ids.join(",");
				}
				if (this.taxonid) {
					url += "&taxon=" + this.taxonid;
				}
				if (this.limit) {
					url += "&limit=" + this.limit;
				}
				if (this.filterType) {
					url += "&filter=" + this.filterType;
				}

				Ext.Msg.alert("Copy this URL", url);
			},

			initComponent : function() {
				this.searchInGridField = new Ext.form.TextField({
							enableKeyEvents : true,
							emptyText : 'Search',
							tooltip : "Text typed here will act as a filter.",
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

				this.filterCombo = new Ext.form.ComboBox({
							typeAhead : true,
							triggerAction : 'all',
							lazyRender : true,
							mode : 'local',
							store : new Ext.data.ArrayStore({
										id : 0,
										fields : ['filterType', 'displayText'],
										data : [[0, 'No filter'], [1, 'Need diff'], [2, 'Need coex'], [3, 'Has diff'],
												[4, 'Has coex'], [5, 'Troubled'], [6, 'No factors'], [7, 'No tags']]
									}),
							valueField : 'filterType',
							displayField : 'displayText',
							listeners : {
								scope : this,
								'select' : this.filterByNeed
							}

						});

				this.taxonCombo = new Gemma.TaxonCombo({
							isDisplayTaxonWithDatasets : true,
							stateId: null, //don't remember taxon value if user navigates away then comes back
							listeners : {
								scope : this,
								'select' : this.filterByTaxon
							}
						});

				this.limitCombo = new Ext.form.ComboBox({
							typeAhead : true,
							width : 150,
							triggerAction : 'all',
							lazyRender : true,
							mode : 'local',
							store : new Ext.data.ArrayStore({
										id : 0,
										fields : ['count', 'displayText'],
										data : [[50, '50 recently updated'], [100, '100 recently updated'],
												[200, '200 recently updated'], [300, '300 recently updated'],
												[500, '500 recently updated'], [-50, '50 oldest updates'],
												[-100, '100 oldest updates'], [-200, '200 oldest updates'],
												[-300, '300 oldest updates'], [-500, '500 oldest updates']]
									}),
							valueField : 'count',
							displayField : 'displayText',
							listeners : {
								scope : this,
								'select' : this.filterByLimit
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
												}, this.filterCombo, this.taxonCombo, this.limitCombo, '->', {
													xtype : 'button',
													minWidth : 20,
													cls : 'x-btn-icon',
													icon : '/Gemma/images/icons/link.png',
													handler : this.getBookmark,
													tooltip : "Bookmarkable link",
													scope : this
												}, {
													xtype : 'button',
													minWidth : 20,
													cls : 'x-btn-icon',
													icon : '/Gemma/images/icons/disk.png',
													handler : this.downloadAsText,
													tooltip : "Download as text",
													scope : this
												}]
									}),
							bbar : new Ext.Toolbar({
										items : ['->', {
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
