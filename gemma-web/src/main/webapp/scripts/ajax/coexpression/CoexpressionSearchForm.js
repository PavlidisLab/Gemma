Ext.namespace('Ext.Gemma');

Ext.onReady( function() {
	Ext.QuickTips.init();
	
	var admin = dwr.util.getValue("hasAdmin");

	var searchPanel = new Ext.Gemma.CoexpressionSearchPanel( {
	} );
	searchPanel.render( "coexpression-form" );
	
	var knownGeneDatasetGrid = new Ext.Gemma.CoexpressionDatasetGrid( {
		renderTo : "coexpression-results"
	} );
	var knownGeneGrid = new Ext.Gemma.CoexpressionGrid( {
		renderTo : "coexpression-results",
		title : "Coexpressed genes",
		pageSize : 25
	} );
	var predictedGeneGrid;
	var probeAlignedGrid;
	if ( admin ) {
		predictedGeneGrid = new Ext.Gemma.CoexpressionGrid( {
			renderTo : "coexpression-results",
			title : "Coexpressed predicted genes",
			pageSize : 25,
			collapsed : true
		} );
		probeAlignedGrid = new Ext.Gemma.CoexpressionGrid( {
			renderTo : "coexpression-results",
			title : "Coexpressed probe-aligned regions",
			pageSize : 25,
			collapsed : true
		} );
	}
	
	searchPanel.processSearchResults = function ( result ) {
		var eeMap = {};
		if ( result.datasets ) {
			for ( var i=0; i<result.datasets.length; ++i ) {
				var ee = result.datasets[i];
				eeMap[ee.id] = ee;
			}
		}
		
		Ext.Gemma.CoexpressionDatasetGrid.updateDatasetInfo( result.knownGeneDatasets, eeMap );
		knownGeneDatasetGrid.loadData( result.knownGeneDatasets ) ;
		knownGeneGrid.loadData( result.knownGeneResults );
		
		if ( admin ) {
			Ext.Gemma.CoexpressionDatasetGrid.updateDatasetInfo( result.predictedGeneDatasets, eeMap );
			//predictedGeneDatasetGrid.loadData( result.knownGeneDatasets ) ;
			predictedGeneGrid.loadData( result.predictedGeneResults );
			Ext.Gemma.CoexpressionDatasetGrid.updateDatasetInfo( result.probeAlignedRegionDatasets, eeMap );
			//probeAlignedDatasetGrid.loadData( result.knownGeneDatasets ) ;
			probeAlignedGrid.loadData( result.probeAlignedRegionResults );
		}
	};
	
} );

/* Ext.Gemma.CoexpressionSearchPanel constructor...
 */
Ext.Gemma.CoexpressionSearchPanel = function ( config ) {

	var thisPanel = this;
	
	/* establish default config options...
	 */
	var superConfig = {
		autoWidth : true,
		autoHeight : true,
		frame : true,
		defaults : { }
	};
	
	/* apply user-defined config options and call the superclass constructor...
	 */
	for ( property in config ) {
		superConfig[property] = config[property];
	}
	Ext.Gemma.CoexpressionSearchPanel.superclass.constructor.call( this, superConfig );
	
	var geneChooserPanel = new Ext.Gemma.GeneChooserPanel( {
		showTaxon : true
	} );
	this.geneChooserPanel = geneChooserPanel;
	
	var queryFs = new Ext.form.FieldSet( {
		title : 'Query gene(s)',
		autoHeight : true,
		collapsible : true
	} );
	queryFs.add( geneChooserPanel );
	
	var analysisCombo = new Ext.Gemma.AnalysisCombo( {
		fieldLabel : 'Limit search to',
		showCustomOption : true
	} );
	this.analysisCombo = analysisCombo;
	analysisCombo.on( "select", function ( combo, record, index ) {
		if ( record.data.id < 0 ) {
			customFs.show();
			thisPanel.updateDatasetsToBeSearched( 0 );
		} else {
			customFs.hide();
			geneChooserPanel.setTaxon( record.data.taxon );
			thisPanel.updateDatasetsToBeSearched( record.data.numDatasets );
		}
	} );
	
	var stringencyField = new Ext.form.NumberField( {
		allowBlank : false,
		allowDecimals : false,
		allowNegative : false,
		fieldLabel : 'Stringency',
		value : 2,
		width : 25
	} );
	this.stringencyField = stringencyField;
	
	var eeSearchField = new Ext.Gemma.DatasetSearchField( {
		fieldLabel : "Experiment keywords",
		callback : this.updateDatasetsToBeSearched.bind( this )
	} );
	this.eeSearchField = eeSearchField;
	
	var customFs = new Ext.form.FieldSet( {
		title : 'Custom analysis options',
		hidden : true
	} );
	customFs.add( eeSearchField );
	
	var analysisFs = new Ext.form.FieldSet( {
		title : 'Analysis options',
		autoHeight : true,
		collapsible : true,
		defaults : {
			labelStyle : 'white-space: nowrap'
		},
		labelWidth : 150,
	} );
	this.analysisFs = analysisFs;
	analysisFs.add( stringencyField );
	analysisFs.add( analysisCombo );
	analysisFs.add( customFs );
	
	var submitButton = new Ext.Button( {
		text : "Find coexpressed genes",
		handler : function() {
			thisPanel.doSearch.call( thisPanel );
		}
	} );
	
	this.add( queryFs );
	this.add( analysisFs );
	this.addButton( submitButton );

};

/* other public methods...
 */
Ext.extend( Ext.Gemma.CoexpressionSearchPanel, Ext.FormPanel, {

	onRender : function ( ct, position ) {
		Ext.Gemma.CoexpressionSearchPanel.superclass.onRender.apply(this, arguments);
		
		if ( ! this.loadMask ) {
			this.createLoadMask();
		}
	},
	
	createLoadMask : function () {
		this.loadMask = new Ext.LoadMask( this.getEl() );
		this.eeSearchField.loadMask = this.loadMask;
	},

	doSearch : function () {
		this.loadMask.show();
		ExtCoexpressionSearchController.doSearch(
			this.getCoexpressionSearchCommand(),
			this.returnFromSearch.bind( this )
		);
	},

	getCoexpressionSearchCommand : function () {
		var csc = {
			geneIds : this.geneChooserPanel.getGeneIds(),
			stringency : this.stringencyField.getValue()
		};
		var analysisId = this.analysisCombo.getValue();
		if ( analysisId < 0 ) {
			csc.eeIds = this.eeSearchField.getEeIds();
		} else {
			csc.cannedAnalysisId = analysisId;
		}
		return csc;
	},
	
	returnFromSearch : function ( result ) {
		this.processSearchResults( result );
		this.loadMask.hide();
	},

	processSearchResults : function ( result ) {
		// this is going to be overridden in the main method above; this should be cleaned up...
	},
	
	updateDatasetsToBeSearched : function ( datasets ) {
		var numDatasets = datasets instanceof Array ? datasets.length : datasets;
		this.analysisFs.setTitle( String.format( "Analysis options ({0} dataset{1} will be analyzed)", numDatasets, numDatasets != 1 ? "s" : "" ) );
	}
	
} );

/* Ext.Gemma.DatasetSearchField constructor...
 */
Ext.Gemma.DatasetSearchField = function ( config ) {

	this.callback = config.callback; delete config.callback;
	this.loadMask = config.loadMask; delete config.loadMask;
	this.eeIds = [];

	Ext.Gemma.DatasetSearchField.superclass.constructor.call( this, config );
};

/* other public methods...
 */
Ext.extend( Ext.Gemma.DatasetSearchField, Ext.form.TextField, {

	initEvents : function() {
		Ext.Gemma.DatasetSearchField.superclass.initEvents.call(this);
		var queryTask = new Ext.util.DelayedTask( this.findDatasets, this );
		this.el.on( "keyup", function( e ) { queryTask.delay( 500 ) } );
	},
	
	findDatasets : function () {
		var query = this.getValue();
		if ( query == this.lastQuery ) {
			return;
		} else if ( query == "" ) {
			this.foundDatasets( [] );
		} else {
			this.lastQuery = query;
			if ( this.loadMask ) {
//				this.loadMask.enable();
				this.loadMask.show();
			}
			ExtCoexpressionSearchController.findExpressionExperiments( query, this.foundDatasets.bind( this ) );
		}
	},
	
	foundDatasets : function ( results ) {
		this.eeIds = results;
		if ( this.callback instanceof Function ) {
			this.callback( results );
		}
		if ( this.loadMask ) {
			this.loadMask.hide();
//			this.loadMask.disable();
		}
	},
	
	getEeIds : function () {
		return this.eeIds;
	}
	
} );

/* Ext.Gemma.AnalysisCombo constructor...
 */
Ext.Gemma.AnalysisCombo = function ( config ) {
	
	this.showCustomOption = config.showCustomOption; delete config.showCustomOption;
	
	/* establish default config options...
	 */
	var superConfig = {
		displayField : 'name',
		valueField : 'id',
		editable : false,
		mode : 'local',
		selectOnFocus : true,
		triggerAction : 'all',
		store : new Ext.data.Store( {
			proxy : new Ext.data.DWRProxy( ExtCoexpressionSearchController.getCannedAnalyses ),
			reader : new Ext.data.ListRangeReader( {id:"id"}, Ext.Gemma.AnalysisCombo.getRecord() ),
			remoteSort : true
		} )
	};
	var options = { params : [] };
	if ( this.showCustomOption ) {
		options.callback = function () {
			var constructor = Ext.Gemma.AnalysisCombo.getRecord();
			var record = new constructor( {
				id : -1,
				name : "Custom analysis",
				description : ""
			} );
			superConfig.store.add( record );
		};
	}
	superConfig.store.load( options );
	
	/* apply user-defined config options and call the superclass constructor...
	 */
	for ( var property in config ) {
		superConfig[property] = config[property];
	}
	Ext.Gemma.AnalysisCombo.superclass.constructor.call( this, superConfig );
};

/* static methods
 */
Ext.Gemma.AnalysisCombo.getRecord = function() {
	if ( Ext.Gemma.AnalysisCombo.record === undefined ) {
		Ext.Gemma.AnalysisCombo.record = Ext.data.Record.create( [
			{ name: "id", type: "int" },
			{ name: "name", type: "string" },
			{ name: "description", type: "string" },
			{ name: "taxon" },
			{ name: "numDatasets", type: "int" }
		] );
	}
	return Ext.Gemma.AnalysisCombo.record;
};

Ext.Gemma.AnalysisCombo.getTemplate = function() {
	if ( Ext.Gemma.AnalysisCombo.template === undefined ) {
		Ext.Gemma.AnalysisCombo.template = new Ext.XTemplate(
			'<tpl for="."><div class="x-combo-list-item">{name}</div></tpl>'
		);
	}
	return Ext.Gemma.AnalysisCombo.template;
};

/* other public methods...
 */
Ext.extend( Ext.Gemma.AnalysisCombo, Ext.form.ComboBox, {
	
} );

