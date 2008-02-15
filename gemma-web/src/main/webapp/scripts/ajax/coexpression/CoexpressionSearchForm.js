Ext.namespace('Ext.Gemma');

Ext.onReady( function() {
	Ext.QuickTips.init();
	
	var admin = dwr.util.getValue("hasAdmin");

	var searchPanel = new Ext.Gemma.CoexpressionSearchPanel( {
		style : "margin-bottom: 1em;"
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
	this.geneChooserPanel.taxonCombo.on( "taxonchanged", function ( combo, taxon ) {
		this.taxonChanged( taxon );
	}, this );
	
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
	analysisCombo.on( "analysisChanged", function ( combo, analysis ) {
		if ( analysis ) {
			if ( analysis.id < 0 ) { // custom analysis
				thisPanel.customAnalysis = true;
				customFs.show();
				thisPanel.updateDatasetsToBeSearched( eeSearchField.getEeIds() );
				eeSearchField.findDatasets();
			} else {
				thisPanel.customAnalysis = false;
				customFs.hide();
				thisPanel.taxonChanged( analysis.taxon );
				thisPanel.updateDatasetsToBeSearched( analysis.numDatasets );
			}
		} else {
			thisPanel.customAnalysis = false;
			customFs.hide();
			thisPanel.analysisFs.setTitle( "" );
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
		fieldLabel : "Experiment keywords"
	} );
	this.eeSearchField = eeSearchField;
	this.eeSearchField.on( 'aftersearch', function ( field, results ) {
		if ( thisPanel.customAnalysis ) {
			thisPanel.updateDatasetsToBeSearched( results );
		}
	} );
	
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

	initComponent : function() {
        Ext.Gemma.CoexpressionSearchPanel.superclass.initComponent.call(this);
        
        this.addEvents(
            'beforesearch',
            'aftersearch'
        );
    },

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
	},
	
	taxonChanged : function ( taxon ) {
		this.analysisCombo.taxonChanged( taxon );
		this.eeSearchField.taxonChanged( taxon );
		this.geneChooserPanel.taxonChanged( taxon );
	}
	
} );

/* Ext.Gemma.DatasetSearchField constructor...
 */
Ext.Gemma.DatasetSearchField = function ( config ) {

	this.loadMask = config.loadMask; delete config.loadMask;
	this.eeIds = [];

	Ext.Gemma.DatasetSearchField.superclass.constructor.call( this, config );
	
	this.on( 'beforesearch', function( field, query ) {
		if ( this.loadMask ) {
			this.loadMask.show();
		}
	} );
	this.on( 'aftersearch', function( field, results ) {
		if ( this.loadMask ) {
			this.loadMask.hide();
		}
	} );
};

/* other public methods...
 */
Ext.extend( Ext.Gemma.DatasetSearchField, Ext.form.TextField, {

	initComponent : function() {
        Ext.Gemma.DatasetSearchField.superclass.initComponent.call(this);
        
        this.addEvents(
            'beforesearch',
            'aftersearch'
        );
    },

	initEvents : function() {
		Ext.Gemma.DatasetSearchField.superclass.initEvents.call(this);
		
		var queryTask = new Ext.util.DelayedTask( this.findDatasets, this );
		this.el.on( "keyup", function( e ) { queryTask.delay( 500 ) } );
	},
	
	findDatasets : function () {
		var params = [ this.getValue(), this.taxon ? this.taxon.id : -1 ];
		if ( params == this.lastParams ) {
			return;
		}
		if ( this.fireEvent('beforesearch', this, params ) !== false ) {
			this.lastParams = params;
			ExtCoexpressionSearchController.findExpressionExperiments( params[0], params[1], this.foundDatasets.bind( this ) );
        }
	},
	
	foundDatasets : function ( results ) {
		this.eeIds = results;
		this.fireEvent( 'aftersearch', this, results );
	},
	
	getEeIds : function () {
		return this.eeIds;
	},
	
	taxonChanged : function ( taxon ) {
		this.taxon = taxon;
		this.findDatasets();
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
		lazyInit : false,
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

	initComponent : function() {
        Ext.Gemma.AnalysisCombo.superclass.initComponent.call(this);
        
        this.addEvents(
            'analysischanged'
        );
    },

	onSelect : function ( record, index ) {
		Ext.Gemma.AnalysisCombo.superclass.onSelect.call( this, record, index );
		
		if ( record.data != this.selectedAnalysis ) {
			this.selectedAnalysis = record.data;
			this.fireEvent( 'analysischanged', this, this.selectedAnalysis );
		}
	},
	
	reset : function() {
		Ext.Gemma.AnalysisCombo.superclass.reset.call(this);
		
		if ( this.selectedAnalysis != null ) {
			this.selectedAnalysis = null;
			this.fireEvent( 'analysischanged', this, this.selectedAnalysis );
		}
	},

	taxonChanged : function ( taxon ) {
		if ( this.selectedAnalysis && this.selectedAnalysis.taxon.id != taxon.id ) {
			this.reset();
		}
	}
	
} );

