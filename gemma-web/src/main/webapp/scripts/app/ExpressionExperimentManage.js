Ext.namespace('Gemma');
Ext.BLANK_IMAGE_URL = '/Gemma/images/default/s.gif';
Ext.onReady(function() {
	Ext.QuickTips.init();
	Ext.state.Manager.setProvider(new Ext.state.CookieProvider());

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
				name : "designElementDataVectorCount",
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
			}]);

	var dateRenderer = new Ext.util.Format.dateRenderer("y/M/d");

	var adminRenderer = function(value, metadata, record, rowIndex, colIndex, store) {
		return '<a href="#" onClick="return deleteExperiment('
				+ value
				+ ')"><img src="/Gemma/images/icons/delete.png" alt="delete" title="delete" /></a>&nbsp<a href="#" onClick="return updateEEReport('
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

	var linkAnalysisRenderer = function(value, metadata, record, rowIndex, colIndex, store) {
		if (record.get('dateLinkAnalysis')) {
			// TODO: Deal with failure, too small, etc.
			return dateRenderer(record.get('dateLinkAnalysis'));
		} else {
			return '<span style="color:#3A3;">Needed</span>&nbsp<a href="runit"><img src="/Gemma/images/icons/control_play_blue.png" alt="run" title="run"/></a>';
		}

	};

	var missingValueAnalysisRenderer = function(value, metadata, record, rowIndex, colIndex, store) {
		if (record.get('technologyType') != 'ONECOLOR' && record.get('hasBothIntensities')) {
			/*
			 * Determine if it was done..
			 */
			if (record.get('dateMissingValueAnalysis')) {
				// TODO
				return dateRenderer(record.get('dateMissingValueAnalysis'));
			} else {
				return '<span style="color:#3A3;">Needed</span>&nbsp<a href="runit"><img src="/Gemma/images/icons/control_play_blue.png" alt="run" title="run"/></a>';
			}

		} else {
			return '<span style="color:#CCF;">NA</span>';
		}
	};

	var processedVectorCreateRenderer = function(value, metadata, record, rowIndex, colIndex, store) {
		if (record.get('dateProcessedDataVectorComputation')) {
			// TODO
			return dateRenderer(record.get('dateProcessedDataVectorComputation'));
		} else {
			return '<span style="color:#3A3;">Needed</span>&nbsp<a href="runit"><img src="/Gemma/images/icons/control_play_blue.png" alt="run" title="run"/></a>';
		}
	};

	var differentialAnalysisRenderer = function(value, metadata, record, rowIndex, colIndex, store) {
		if (diffIsPossible(record)) {
			if (record.get('dateDifferentialAnalysis')) {
				// TODO
				return dateRenderer(record.get('dateDifferentialAnalysis'));
			} else {
				return '<span style="color:#3A3;">Needed</span>&nbsp<a href="runit"><img src="/Gemma/images/icons/control_play_blue.png" alt="run" title="run"/></a>';
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
		if (record.get('isPrivate')) {
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
				dataIndex : 'designElementDataVectorCount',
				width : 35
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
				items : [new Ext.grid.GridPanel({
							store : store,
							autoHeight : true,
							columns : columns,
							rowExpander : rowExpander,
							plugins : rowExpander
						})]

			});

});
