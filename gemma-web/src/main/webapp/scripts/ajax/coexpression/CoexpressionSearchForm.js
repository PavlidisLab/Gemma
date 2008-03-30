/* Ext.Gemma.CoexpressionSearchForm constructor...
 */
Ext.Gemma.CoexpressionSearchForm = function ( config ) {

	var thisPanel = this;
	
	/* establish default config options...
	 */
	var superConfig = {
		width : 470,
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
	
	
	/*
	 * Gene settings
	 */
	
	
	
	var geneChooserPanel = new Ext.Gemma.GeneChooserPanel( { 
		showTaxon : true 
	} );
	this.geneChooserPanel = geneChooserPanel;
	this.geneChooserPanel.taxonCombo.on( "taxonchanged", function ( combo, taxon ) {
		this.taxonChanged( taxon );
	}, this );
	
	var queryFs = new Ext.form.FieldSet( {
		title : 'Query gene(s)',
		autoHeight : true 
	} );
	queryFs.add( geneChooserPanel );
	
	
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
	Ext.Gemma.CoexpressionSearchForm.addToolTip( stringencyField, 
		"The minimum number of datasets that must show coexpression for a result to appear" );


	/*
	 * Analysis/datasets and stringency settings.
	 */
	
	var analysisCombo = new Ext.Gemma.AnalysisCombo( {
		fieldLabel : 'Limit search to',
		showCustomOption : true
	} );
	this.analysisCombo = analysisCombo;
	analysisCombo.on( "analysisChanged", function ( combo, analysis ) {
		if ( analysis ) {
			if ( analysis.id < 0 ) { // custom analysis
				thisPanel.customAnalysis = true;
				customFs.doLayout();
				customFs.show();
				thisPanel.updateDatasetsToBeSearched( eeSearchField.getEeIds() );
				eeSearchField.findDatasets();
			} else {
				thisPanel.customAnalysis = false;
				customFs.hide();
				thisPanel.taxonChanged( analysis.taxon );
				thisPanel.updateDatasetsToBeSearched( analysis.datasets );
			}
		} else {
			thisPanel.customAnalysis = false;
			customFs.hide();
			thisPanel.optionsPanel.setTitle( "Analysis options" );
		}
	} );
	Ext.Gemma.CoexpressionSearchForm.addToolTip( analysisCombo,
		"Restrict the list of datasets that will be searched for coexpression" );
	 	
	var eeSearchField = new Ext.Gemma.DatasetSearchField( {
		fieldLabel : "Experiment keywords" ,
		width : 150
	} );
	this.eeSearchField = eeSearchField;
	eeSearchField.on( 'aftersearch', function ( field, results ) {
		if ( thisPanel.customAnalysis ) {
			thisPanel.updateDatasetsToBeSearched( results );
		}
	} );
	Ext.Gemma.CoexpressionSearchForm.addToolTip( eeSearchField,
		"Search only datasets that match these keywords" );
	
	
	var customFs = new Ext.form.FieldSet( {
		title : 'Custom analysis options',
		hidden : true, 
		autoHeight: true,
		autoWidth:true,
		items :  [eeSearchField] 
	} );
	
	this.customFs = customFs;
	
	var queryGenesOnly = new Ext.form.Checkbox( {
		fieldLabel: 'Search among query genes only'
	} );
	this.queryGenesOnly = queryGenesOnly;
	
	var analysisFs = new Ext.form.FieldSet( {
		autoHeight : true, 
		border : false,
		defaults : {
			labelStyle : 'white-space: nowrap'
		},
		labelWidth : 180,
		items : [ stringencyField, queryGenesOnly, analysisCombo, customFs ]
	} );
	 


	var activeDatasetsWindow = new Ext.Window({
			el : 'coexpression-experiments',
			title : "Active datasets",
			modal : true,
			layout : 'fit',
			autoHeight : true,
			width : 600,
			closeAction:'hide',
			easing : 3, 
            buttons: [{ 
               text: 'Close',
               handler: function(){ 
                   activeDatasetsWindow.hide();  
               }
            }]
			
		});
		
		
	var activeDatasetsGrid = new Ext.Gemma.ExpressionExperimentGrid( activeDatasetsWindow.getEl(), {
			readMethod : ExpressionExperimentController.loadExpressionExperiments.bind( this ),
			editable : false,
			rowExpander : true,
			pageSize : 20 
		});
	
	this.showSelectedDatasets = function( ) {
		var eeids = this.getActiveEeIds();
		activeDatasetsGrid.getStore().removeAll();	 
		activeDatasetsGrid.expandedElements = [];
		activeDatasetsGrid.getStore().load( { params : [ eeids ] }); 
		activeDatasetsWindow.show();
	};
	

	var eeDetailsButton = new Ext.Button({ fieldLabel : 'Selected dataset details', id : 'selected-ds-button', cls:"x-btn-icon", icon : "/Gemma/images/icons/information.png", handler : this.showSelectedDatasets, scope : this, disabled : false, tooltip : "Show selected datasets" });
 
	var optionsPanel = new Ext.Panel({
		title : 'Analysis options',
		border : true,	
		layout : 'anchor',
		frame : true,
		items : [ analysisFs, eeDetailsButton ]
	});
	
	this.optionsPanel = optionsPanel;
	
	var submitButton = new Ext.Button( {
		text : "Find coexpressed genes",
		handler : function() {
			thisPanel.doSearch.call( thisPanel );
		}
	} );
	
	
	/*
	 * Build the form
	 */
	this.add( queryFs );
	this.add( optionsPanel );
	this.addButton( submitButton );

//	var stringencySpinner = new Ext.ux.form.Spinner({
//		renderTo : this.stringencyField.getEl(),
//		strategy: new Ext.ux.form.Spinner.NumberStrategy({
//			allowDecimals : false, minValue:2, maxValue:100 })
//	}); 
//	this.stringencySpinner = stringencySpinner; 

	Ext.Gemma.CoexpressionSearchForm.searchForGene = function( geneId ) {
		geneChooserPanel.setGene.call( geneChooserPanel, geneId, thisPanel.doSearch.bind( thisPanel ) );
	};
	
};

Ext.Gemma.CoexpressionSearchForm.MIN_STRINGENCY = 2;

Ext.Gemma.CoexpressionSearchForm.addToolTip = function( component, html ) {
	component.on( "render", function() {
		component.gemmaTooltip = new Ext.ToolTip( {
			target : component.getEl(),
			html : html
		} );
	} );
};

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
		if ( param.q ) {
			csc.queryGenesOnly = true;
		}
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
		if ( csc.queryGenesOnly ) {
			url += "&q";
		}
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
		this.clearError();
		var msg = this.validateSearch( csc );
		if ( msg.length === 0 ) {
			if ( this.fireEvent('beforesearch', this, csc ) !== false ) {
				this.loadMask.show();
				var errorHandler = this.handleError.createDelegate(this, [], true);
				ExtCoexpressionSearchController.doSearch( csc, {callback : this.returnFromSearch.bind( this ), errorHandler : errorHandler} );
			}
		} else {
			this.handleError(msg);
		}
	},
	
	handleError : function( msg, e ) {
		Ext.DomHelper.overwrite("coexpression-messages", {tag : 'img', src:'/Gemma/images/icons/warning.png' }); 
		Ext.DomHelper.append("coexpression-messages", {tag : 'span', html : "&nbsp;&nbsp;"  + msg });  
		this.loadMask.hide();
	},
	
	clearError : function () {
		Ext.DomHelper.overwrite("coexpression-messages", "");
	},
	
	validateSearch : function ( csc ) {
		if ( csc.queryGenesOnly && csc.geneIds.length < 2 ) { 
			return "You must select more than one query gene to use 'search among query genes only'";
		} else if ( csc.geneIds.length < 1 ) {
			return "We couldn't figure out which gene you want to query. Please use the search functionality to find genes.";
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
		if (datasets instanceof Array) {
			 this.eeIds = datasets;
		}
		this.optionsPanel.setTitle( String.format( "Analysis options (Up to {0} dataset{1} will be analyzed)", numDatasets, numDatasets != 1 ? "s" : "" ) );
	},
	
	taxonChanged : function ( taxon ) {
		this.analysisCombo.taxonChanged( taxon );
		this.eeSearchField.taxonChanged( taxon, this.customFs.hidden ? false : true );
		this.geneChooserPanel.taxonChanged( taxon );
	},
	
	getActiveEeIds : function() {
		return this.eeIds;
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
 
 	this.stringencyField = new Ext.form.Hidden ( {
		value : 3
 	});
 
	this.geneCombo = new Ext.Gemma.GeneCombo( {
		hiddenName : 'g',
		fieldLabel : 'Select a query gene'
	} );
	
	this.geneCombo.on("focus", this.clearMessages, this );
	
	this.analysisCombo = new Ext.Gemma.AnalysisCombo( {
		hiddenName : 'a',
		fieldLabel : 'Select search scope',
		showCustomOption : false
	} );
	
	this.analysisCombo.on( "analysischanged", function ( combo, analysis ) {
		this.clearMessages();
		if ( analysis && analysis.taxon ) {
			this.taxonChanged( analysis.taxon );
		}
	}, this );
	
	var submitButton = new Ext.Button( {
		text : "Find coexpressed genes",
		handler : function() {
			var msg = this.validateSearch( this.geneCombo.getValue(), this.analysisCombo.getValue() );
			if ( msg.length === 0 ) {
				document.location.href =
				String.format( "/Gemma/searchCoexpression.html?g={0}&a={1}&s={2}",
					this.geneCombo.getValue(), this.analysisCombo.getValue(), this.stringencyField.getValue());
			} else {
				this.handleError(msg);
			}
		}.bind( this )
	} );

	this.add( this.geneCombo );
	this.add( this.analysisCombo );
	this.add( this.stringencyField );
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
			analysisId : this.analysisCombo.getValue(),
			stringency : this.stringencyField.getValue()
		};
		return csc;
	},
	
	initializeFromCoexpressionSearchCommand : function ( csc ) {
		if ( csc.cannedAnalysisId > -1 ) {
			this.analysisCombo.setValue( csc.cannedAnalysisId );
		}
		if ( csc.stringency ) {
			this.stringencyField.setValue(csc.stringency);
		}
	},
	
	validateSearch : function ( gene, analysis ) {
		if ( !gene || gene.length == 0 ) {
			return "Please select a valid query gene";
		} else if ( !analysis ) {
			return "Please select an analysis";
		} else {
			return "";
		}
	},
	
	handleError : function( msg, e ) {
		Ext.DomHelper.overwrite("coexpression-messages", {tag : 'img', src:'/Gemma/images/icons/warning.png' }); 
		Ext.DomHelper.append("coexpression-messages", {tag : 'span', html : "&nbsp;&nbsp;"  + msg });  
		this.loadMask.hide();
	},
	
	clearMessages : function() {Ext.DomHelper.overwrite("coexpression-messages", {tag : 'h3', html : "Coexpression query"  });},
	
	taxonChanged : function ( taxon ) {
		this.geneCombo.setTaxon( taxon );
	}
	
} );