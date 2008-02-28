/* Ext.Gemma.CoexpressionSearchForm constructor...
 */
Ext.Gemma.CoexpressionSearchForm = function ( config ) {

	var thisPanel = this;
	
	/* establish default config options...
	 */
	var superConfig = {
		autoWidth : true,
		autoHeight : true,
		frame : true,
		stateful : true,
		stateEvents : [ "beforesearch" ],
		stateId : "Ext.Gemma.CoexpressionSearch", // share state with main page...
		defaults : { }
	};
	
	/* apply user-defined config options and call the superclass constructor...
	 */
	for ( property in config ) {
		superConfig[property] = config[property];
	}
	Ext.Gemma.CoexpressionSearchForm.superclass.constructor.call( this, superConfig );
	
	var queryGenesOnly = new Ext.form.Checkbox( {
		fieldLabel: 'Search among query genes only'
	} );
	this.queryGenesOnly = queryGenesOnly;
	
	var geneChooserPanel = new Ext.Gemma.GeneChooserPanel( {
		showTaxon : true,
		bbar : [ queryGenesOnly, new Ext.Toolbar.Spacer(), new Ext.Toolbar.TextItem( queryGenesOnly.fieldLabel ) ]
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
		minValue : Ext.Gemma.CoexpressionSearchForm.MIN_STRINGENCY,
		maxValue : 999,
		fieldLabel : 'Stringency',
		invalidText : "Minimum stringency is " + Ext.Gemma.CoexpressionSearchForm.MIN_STRINGENCY,
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
	this.customFs = customFs;
	customFs.add( eeSearchField );
	
	var analysisFs = new Ext.form.FieldSet( {
		title : 'Analysis options',
		autoHeight : true,
		collapsible : true,
		defaults : {
			labelStyle : 'white-space: nowrap'
		},
		labelWidth : 150
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

	Ext.Gemma.CoexpressionSearchForm.searchForGene = function( geneId ) {
		geneChooserPanel.setGene.call( geneChooserPanel, geneId, thisPanel.doSearch.bind( thisPanel ) );
	};
	
};

Ext.Gemma.CoexpressionSearchForm.MIN_STRINGENCY = 2;

/* other public methods...
 */
Ext.extend( Ext.Gemma.CoexpressionSearchForm, Ext.FormPanel, {

	applyState : function( state, config ) {
		if ( state ) {
			this.csc = state;
		}
	},

	getState : function() {
		return this.getCoexpressionSearchCommand();
	},

	initComponent : function() {
        Ext.Gemma.CoexpressionSearchForm.superclass.initComponent.call(this);
        
        this.addEvents(
            'beforesearch',
            'aftersearch'
        );
    },
    
    render : function ( container, position ) {
		Ext.Gemma.CoexpressionSearchForm.superclass.render.apply(this, arguments);
    	
    	if ( ! this.loadMask ) {
			this.createLoadMask();
		}
		
		// initialize from state
		if ( this.csc ) {
			this.initializeFromCoexpressionSearchCommand( this.csc );
		}
		
		// intialize from URL (overrides state)
		var queryStart = document.URL.indexOf( "?" );
		if ( queryStart > -1 ) {
			this.initializeFromQueryString( document.URL.substr( queryStart + 1 ) );
		}
    },
	
	createLoadMask : function () {
		this.loadMask = new Ext.LoadMask( this.getEl() );
		this.eeSearchField.loadMask = this.loadMask;
	},

	getCoexpressionSearchCommand : function () {
		var csc = {
			geneIds : this.geneChooserPanel.getGeneIds(),
			stringency : this.stringencyField.getValue(),
			taxonId : this.geneChooserPanel.getTaxonId(),
			queryGenesOnly : this.queryGenesOnly.getValue()
		};
		var analysisId = this.analysisCombo.getValue();
		if ( analysisId < 0 ) {
			csc.eeIds = this.eeSearchField.getEeIds();
		} else {
			csc.cannedAnalysisId = analysisId;
		}
		return csc;
	},
	
	getCoexpressionSearchCommandFromQuery : function ( query ) {
		var param = Ext.urlDecode( query );
		var eeQuery = param.eeq || "";
		var ees; if ( param.ees ) { ees = param.ees.split( ',' ); }
		
		var csc = {
			geneIds : param.g ? param.g.split( ',' ) : [],
			stringency : param.s || Ext.Gemma.CoexpressionSearchForm.MIN_STRINGENCY,
			eeQuery : param.eeq
		};
		if ( param.ees ) {
			csc.eeIds = param.ees.split( ',' );
			csc.cannedAnalysisId = -1;
		} else {
			csc.cannedAnalysisId = param.a;
		}
		return csc;
	},
	
	initializeFromQueryString : function ( query ) {
		this.initializeFromCoexpressionSearchCommand(
			this.getCoexpressionSearchCommandFromQuery( query ), true
		);
	},
	
	initializeFromCoexpressionSearchCommand : function ( csc, doSearch ) {
		/* make the form look like it has the right values;
		 * this will happen asynchronously...
		 */
		if ( csc.taxonId ) {
			this.geneChooserPanel.taxonCombo.setValue( csc.taxonId );
		}
		if ( csc.geneIds.length > 1 ) {
			this.geneChooserPanel.loadGenes( csc.geneIds );
		} else {
			this.geneChooserPanel.setGene( csc.geneIds[0] );
		}
		if ( csc.cannedAnalysisId ) {
			this.analysisCombo.setValue( csc.cannedAnalysisId );
		}
		if ( csc.stringency ) {
			this.stringencyField.setValue( csc.stringency );
		}
		if ( csc.queryGenesOnly ) {
			this.queryGenesOnly.setValue( true );
		}
		if ( csc.cannedAnalysisId < 0 ) {
			this.customFs.show();
			this.eeSearchField.setValue( csc.eeQuery );
			this.updateDatasetsToBeSearched( csc.eeIds );
		} else {
			// TODO update the number of datasets to be searched...
		}
		
		/* perform the search with the specified values...
		 */
		if ( doSearch ) {
			this.doSearch( csc );
		}
	},
	
	getBookmarkableLink : function ( csc ) {
		if ( ! csc ) {
			csc = this.getCoexpressionSearchCommand();
		}
		var queryStart = document.URL.indexOf( "?" );
		var url = queryStart > -1 ? document.URL.substr( 0, queryStart ) : document.URL;
		url += String.format( "?g={0}&s={1}", csc.geneIds.join( "," ), csc.stringency );
		if ( csc.eeIds ) {
			url += String.format( "&ees={0}", csc.eeIds.join( "," ) );
		} else {
			url += String.format( "&a={0}", csc.cannedAnalysisId );
		}
		return url;
	},

	doSearch : function ( csc ) {
		if ( ! csc ) {
			csc = this.getCoexpressionSearchCommand();
		}
		var msg = this.validateSearch( csc );
		if ( msg.length === 0 ) {
			if ( this.fireEvent('beforesearch', this, csc ) !== false ) {
				this.loadMask.show();
				ExtCoexpressionSearchController.doSearch( csc, this.returnFromSearch.bind( this ) );
			}
		} else {
			Ext.MessageBox.show( {
				msg: msg,
				icon: Ext.MessageBox.ERROR
			} );
		}
	},
	
	validateSearch : function ( csc ) {
		if ( csc.geneIds.length < 1 ) {
			return "Please select at least one query gene";
		} else if ( csc.stringency < Ext.Gemma.CoexpressionSearchForm.MIN_STRINGENCY ) {
			return "Minimum stringency is " + Ext.Gemma.CoexpressionSearchForm.MIN_STRINGENCY;
		} else if ( csc.eeIds && csc.eeIds.length < 1 ) {
			return "There are no datasets that match your search terms";
		} else if ( !csc.eeIds && !csc.cannedAnalysisId ) {
			return "Please select an analysis";
		} else {
			return "";
		}
	},
	
	returnFromSearch : function ( result ) {
		this.loadMask.hide();
		this.fireEvent( 'aftersearch', this, result );
	},
	
	updateDatasetsToBeSearched : function ( datasets ) {
		var numDatasets = datasets instanceof Array ? datasets.length : datasets;
		this.stringencyField.maxValue = numDatasets;
		this.analysisFs.setTitle( String.format( "Analysis options ({0} dataset{1} will be analyzed)", numDatasets, numDatasets != 1 ? "s" : "" ) );
	},
	
	taxonChanged : function ( taxon ) {
		this.analysisCombo.taxonChanged( taxon );
		this.eeSearchField.taxonChanged( taxon, this.customFs.hidden ? false : true );
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
		this.el.on( "keyup", function( e ) { queryTask.delay( 500 ); } );
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
	
	taxonChanged : function ( taxon, doSearch ) {
		this.taxon = taxon;
		if ( doSearch ) {
			this.findDatasets();
		}
	}
	
} );

/* Ext.Gemma.AnalysisCombo constructor...
 */
Ext.Gemma.AnalysisCombo = function ( config ) {
	Ext.QuickTips.init();
	
	this.showCustomOption = config.showCustomOption; delete config.showCustomOption;
	this.isStoreLoaded = false;
	
	/* establish default config options...
	 */
	var superConfig = {
		displayField : 'name',
		valueField : 'id',
		disabled : true,
		editable : false,
		forceSelection : true,
		lazyInit : false,
		lazyRender : false,
		mode : 'local',
		selectOnFocus : true,
		triggerAction : 'all',
		store : new Ext.data.Store( {
			proxy : new Ext.data.DWRProxy( ExtCoexpressionSearchController.getCannedAnalyses ),
			reader : new Ext.data.ListRangeReader( {id:"id"}, Ext.Gemma.AnalysisCombo.getRecord() ),
			remoteSort : true
		} ),
		tpl : Ext.Gemma.AnalysisCombo.getTemplate()
	};
	var options = { params : [] };
	if ( this.showCustomOption ) {
		options.callback = function () {
			var Constructor = Ext.Gemma.AnalysisCombo.getRecord();
			var record = new Constructor( {
				id : -1,
				name : "Custom analysis",
				description : "Select specific datasets to search against"
			} );
			superConfig.store.add( record );
			this.storeLoaded();
		}.bind( this );
	} else {
		options.callback = function () {
			this.storeLoaded();
		}.bind( this );
	}
	superConfig.store.load( options );
	
	/* apply user-defined config options and call the superclass constructor...
	 */
	for ( var property in config ) {
		superConfig[property] = config[property];
	}
	Ext.Gemma.AnalysisCombo.superclass.constructor.call( this, superConfig );
	
	// call doQuery or the record filtering done in taxonChanged() below doesn't work...
	this.doQuery();
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
			'<tpl for="."><div ext:qtip="{description}" class="x-combo-list-item">{name}{[ values.taxon ? " (" + values.taxon.scientificName + ")" : "" ]}</div></tpl>'
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
    
    render : function ( container, position ) {
		Ext.Gemma.AnalysisCombo.superclass.render.apply(this, arguments);
    	
		if ( ! this.isStoreLoaded ) {
			this.setRawValue( "Loading..." );
		}
	},

	onSelect : function ( record, index ) {
		Ext.Gemma.AnalysisCombo.superclass.onSelect.call( this, record, index );
		
		if ( record.data != this.selectedAnalysis ) {
			this.analysisChanged( record.data );
		}
	},
	
	reset : function() {
		Ext.Gemma.AnalysisCombo.superclass.reset.call(this);
		
		if ( this.selectedAnalysis !== null ) {
			this.analysisChanged( null );
		}
	},
	
	setValue : function( v ) {
		if ( this.isStoreLoaded ) {
			Ext.Gemma.AnalysisCombo.superclass.setValue.call( this, v );
			
			var r = this.findRecord( this.valueField, v );
			this.analysisChanged( r ? r.data : null );
		} else {
			this.delayedSetValue = v;
		}
	},
	
	storeLoaded : function() {
		this.isStoreLoaded = true;
		if ( this.delayedSetValue !== undefined && this.delayedSetValue !== "" ) {
			this.setValue( this.delayedSetValue );
		} else {
			this.reset(); // clear the loading message...
		}
		this.enable();
	},
	
	analysisChanged : function ( analysis ) {
		this.selectedAnalysis = analysis;
		this.fireEvent( 'analysischanged', this, this.selectedAnalysis );
	},

	taxonChanged : function ( taxon ) {
		if ( this.selectedAnalysis && this.selectedAnalysis.taxon.id != taxon.id ) {
			this.reset();
		}
		this.store.filterBy( function( record, id ) {
			if ( ! record.data.taxon ) {
				return true; 
			} else if ( record.data.taxon.id == taxon.id ) {
				return true;
			}
		} );
	}
	
} );

/* Ext.Gemma.CoexpressionSummaryGrid constructor...
 * 	config is a hash with the following options:
 */
Ext.Gemma.CoexpressionSummaryGrid = function ( config ) {
	
	var genes = config.genes; delete config.genes;
	var summary = config.summary; delete config.summary;
	
	/* keep a reference to ourselves to avoid convoluted scope issues below...
	 */
	var thisGrid = this;
	
	/* establish default config options...
	 */
	var superConfig = {
		editable : false,
		title : 'Search Summary'
	};
	
	var fields = [
		{ name: 'sort', type: 'int' },
		{ name: 'group', type: 'string' },
		{ name: 'key', type: 'string' }
	];
	for ( var i=0; i<genes.length; ++i ) {
		fields.push( { name: genes[i].officialSymbol, type: 'int' } );
	}
	superConfig.store = new Ext.data.GroupingStore( {
		reader: new Ext.data.ArrayReader( {}, fields ),
		groupField: 'group',
		data: Ext.Gemma.CoexpressionSummaryGrid.transformData( genes, summary ),
		sortInfo: { field: 'sort', direction: 'ASC' }
	} );
	
	var columns = [
		{ header: 'Group', dataIndex: 'group' },
		{ id: 'key', header: '', dataIndex: 'key', align: 'right' }
	];
	for ( var i=0; i<genes.length; ++i ) {
		columns.push( { header: genes[i].officialSymbol, dataIndex: genes[i].officialSymbol, align: 'right' } );
	}
	superConfig.cm = new Ext.grid.ColumnModel( columns );
	superConfig.autoExpandColumn = 'key';
	
	superConfig.view = new Ext.grid.GroupingView( {
		enableGroupingMenu : false,
		enableNoGroups : false,
		hideGroupedColumn : true,
		showGroupName : false
	} );
	
	for ( var property in config ) {
		superConfig[property] = config[property];
	}
	Ext.Gemma.CoexpressionSummaryGrid.superclass.constructor.call( this, superConfig );
};

/* static methods...
 */
Ext.Gemma.CoexpressionSummaryGrid.transformData = function ( genes, summary ) {
	
	var datasetsAvailable = [ 0, "Datasets", "Available" ];
	var datasetsTested = [ 1, "Datasets", "Query gene testable" ];
	var linksFound = [ 2, "Links", "Found" ];
	var linksPositive = [ 3, "Links", "Met stringency (+)" ];
	var linksNegative = [ 4, "Links", "Met stringency (-)" ];
	
	for ( var i=0; i<genes.length; ++i ) {
		var thisSummary = summary[ genes[i].officialSymbol ] || {};
		datasetsAvailable.push( thisSummary.datasetsAvailable );
		datasetsTested.push( thisSummary.datasetsTested );
		linksFound.push( thisSummary.linksFound );
		linksPositive.push( thisSummary.linksMetPositiveStringency );
		linksNegative.push( thisSummary.linksMetNegativeStringency );
	}

	return [ datasetsAvailable, datasetsTested, linksFound, linksPositive, linksNegative ];
};


/* instance methods...
 */
Ext.extend( Ext.Gemma.CoexpressionSummaryGrid, Ext.Gemma.GemmaGridPanel, {
} );

/* Ext.Gemma.CoexpressionSearchFormLite constructor...
 */
Ext.Gemma.CoexpressionSearchFormLite = function ( config ) {

	/* establish default config options...
	 */
	var superConfig = {
		autoHeight : true,
		frame : true,
		stateful : true,
		stateEvents : [ "beforesearch" ],
		stateId : "Ext.Gemma.CoexpressionSearch", // share state with complex form...
		labelAlign : "top",
		defaults : { width: 185 }
	};
	
	/* apply user-defined config options and call the superclass constructor...
	 */
	for ( var property in config ) {
		superConfig[property] = config[property];
	}
	Ext.Gemma.CoexpressionSearchFormLite.superclass.constructor.call( this, superConfig );
	
	this.geneCombo = new Ext.Gemma.GeneCombo( {
		hiddenName : 'g',
		fieldLabel : 'Select a query gene'
	} );
	
	this.analysisCombo = new Ext.Gemma.AnalysisCombo( {
		hiddenName : 'a',
		fieldLabel : 'Select search scope',
		showCustomOption : false
	} );
	this.analysisCombo.on( "analysischanged", function ( combo, analysis ) {
		if ( analysis && analysis.taxon ) {
			this.taxonChanged( analysis.taxon );
		}
	}, this );
	
	var submitButton = new Ext.Button( {
		text : "Find coexpressed genes",
		handler : function() {
			document.location.href =
				String.format( "/Gemma/searchCoexpressionExt.html?g={0}&a={1}",
					this.geneCombo.getValue(), this.analysisCombo.getValue() );
		}.bind( this )
	} );

	this.add( this.geneCombo );
	this.add( this.analysisCombo );
	this.addButton( submitButton );
};

/* instance methods...
 */
Ext.extend( Ext.Gemma.CoexpressionSearchFormLite, Ext.FormPanel, {

	applyState : function( state, config ) {
		if ( state ) {
			this.csc = state;
		}
	},

	getState : function() {
		return this.getCoexpressionSearchCommand();
	},
    
    render : function ( container, position ) {
		Ext.Gemma.CoexpressionSearchFormLite.superclass.render.apply(this, arguments);
    	
    	// initialize from state
		if ( this.csc ) {
			this.initializeFromCoexpressionSearchCommand( this.csc );
		}
    },

	getCoexpressionSearchCommand : function () {
		var csc = {
			geneIds : [ this.geneCombo.getValue() ],
			analysisId : this.analysisCombo.getValue()
		};
		return csc;
	},
	
	initializeFromCoexpressionSearchCommand : function ( csc ) {
		if ( csc.cannedAnalysisId > -1 ) {
			this.analysisCombo.setValue( csc.cannedAnalysisId );
		}
	},
	taxonChanged : function ( taxon ) {
		this.geneCombo.setTaxon( taxon );
	}
	
} );