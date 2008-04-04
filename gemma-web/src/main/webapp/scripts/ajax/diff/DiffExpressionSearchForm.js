/*
 * The input for differential expression searches. This form has two main parts: a GeneChooserPanel, and the differential expression search parameters.
 * 
 * Differential expression search has three main settings: Threshold, "My genes only" checkbox, and the "Search scope".
 * 
 * If scope=custom, a DatasetSearchField is shown.
 * 
 * @author keshav
 * @version $Id$
 */
Ext.Gemma.DiffExpressionSearchForm = function ( config ) {

	var thisPanel = this;
	
	/* establish default config options...
	 */
	var superConfig = {
		width : 550,
		frame : true,
		stateful : true,
		stateEvents : [ "beforesearch" ],
		stateId : "Ext.Gemma.DiffExpressionSearch", // share state with main page...
		defaults : { }
	};
	
	/* apply user-defined config options and call the superclass constructor...
	 */
	for ( property in config ) {
		superConfig[property] = config[property];
	}
	Ext.Gemma.DiffExpressionSearchForm.superclass.constructor.call( this, superConfig );
	
	
	/*
	 * Gene settings
	 */
	this.geneChooserPanel = new Ext.Gemma.GeneChooserPanel( { 
		showTaxon : true 
	} );
	this.geneChooserPanel.taxonCombo.on( "taxonchanged", function ( combo, taxon ) {
		this.taxonChanged( taxon );
	}, this );
	
	var queryFs = new Ext.form.FieldSet( {
		title : 'Query gene(s)',
		autoHeight : true 
	} );
	queryFs.add( this.geneChooserPanel );
	
	// Window shown when the user wants to see the experiments that are 'in play'.
	var activeDatasetsWindow = new Ext.Window({
			el : 'diffExpression-experiments',
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
 
	var submitButton = new Ext.Button( {
		text : "Find diff expressed genes",
		handler : function() {
			thisPanel.doSearch.call( thisPanel );
		}
	} );
	
	
	/*
	 * Build the form
	 */
	this.add( queryFs );
	this.addButton( submitButton );

	Ext.Gemma.DiffExpressionSearchForm.searchForGene = function( geneId ) {
		geneChooserPanel.setGene.call( geneChooserPanel, geneId, thisPanel.doSearch.bind( thisPanel ) );
	};
	
};

Ext.Gemma.DiffExpressionSearchForm.addToolTip = function( component, html ) {
	component.on( "render", function() {
		component.gemmaTooltip = new Ext.ToolTip( {
			target : component.getEl(),
			html : html
		} );
	} );
};

this.thresholdField = new Ext.form.NumberField( {
		allowBlank : false,
		allowDecimals : false,
		allowNegative : false,
		minValue : Ext.Gemma.DiffExpressionSearchForm.MIN_THRESHOLD,
		maxValue : 999,
		fieldLabel : 'Threshold',
		invalidText : "Minimum threshold is " + Ext.Gemma.DiffExpressionSearchForm.MIN_THRESHOLD,
		value : 2,
		width : 60
	} ); 
	Ext.Gemma.DiffExpressionSearchForm.addToolTip( this.thresholdField, 
		"Only genes with a qvalue less than this threshold are returned." );

/* other public methods...
 */
Ext.extend( Ext.Gemma.DiffExpressionSearchForm, Ext.FormPanel, {

	applyState : function( state, config ) {
		if ( state ) {
			this.dsc = state;
		}
	},

	getState : function() {
		return this.getDiffExpressionSearchCommand();
	},

	initComponent : function() {
        Ext.Gemma.DiffExpressionSearchForm.superclass.initComponent.call(this);
        
        this.addEvents(
            'beforesearch',
            'aftersearch'
        );
    },
    
    render : function ( container, position ) {
		Ext.Gemma.DiffExpressionSearchForm.superclass.render.apply(this, arguments);
    	
    	if ( ! this.loadMask ) {
			this.createLoadMask();
		}
		
		// initialize from state
		if ( this.dsc ) {
			this.initializeFromDiffExpressionSearchCommand( this.dsc );
		}
		
    },
	
	createLoadMask : function () {
		this.loadMask = new Ext.LoadMask( this.getEl() );
	},

	getDiffExpressionSearchCommand : function () {
		var dsc = {
			geneIds : this.geneChooserPanel.getGeneIds(),
			threshold : this.threshold.getValue(),
			taxonId : this.geneChooserPanel.getTaxonId(),
		};
		
		return dsc;
	},
	
	initializeFromDiffExpressionSearchCommand : function ( dsc, doSearch ) {
		/* make the form look like it has the right values;
		 * this will happen asynchronously...
		 */
		if ( dsc.taxonId ) {
			this.geneChooserPanel.taxonCombo.setValue( dsc.taxonId );
		}
		if ( dsc.geneIds.length > 1 ) {
			this.geneChooserPanel.loadGenes( dsc.geneIds );
		} else {
			this.geneChooserPanel.setGene( dsc.geneIds[0] );
		}
		if ( dsc.threshold ) {
			this.thresholdField.setValue( dsc.threshold );
		}
		
		/* perform the search with the specified values...
		 */
		if ( doSearch ) {
			this.doSearch( dsc );
		}
	},
	
	getBookmarkableLink : function ( dsc ) {
		if ( ! dsc ) {
			dsc = this.getDiffExpressionSearchCommand();
		}
		var queryStart = document.URL.indexOf( "?" );
		var url = queryStart > -1 ? document.URL.substr( 0, queryStart ) : document.URL;
		url += String.format( "?g={0}&s={1}", dsc.geneIds.join( "," ), dsc.stringency );
		if ( dsc.queryGenesOnly ) {
			url += "&q";
		}
		if ( dsc.eeIds ) {
			url += String.format( "&ees={0}", dsc.eeIds.join( "," ) );
		} else {
			url += String.format( "&a={0}", dsc.cannedAnalysisId );
		}
		
		if (dsc.eeQuery) {
			url += "&eeq=" + dsc.eeQuery;
		}
		
		return url;
	},

	doSearch : function ( dsc ) {
		if ( ! dsc ) {
			dsc = this.getDiffExpressionSearchCommand();
		}
		this.clearError();
		var msg = this.validateSearch( dsc );
		if ( msg.length === 0 ) {
			if ( this.fireEvent('beforesearch', this, dsc ) !== false ) {
				this.loadMask.show();
				var errorHandler = this.handleError.createDelegate(this, [], true);
				ExtDiffExpressionSearchController.doSearch( dsc, {callback : this.returnFromSearch.bind( this ), errorHandler : errorHandler} );
			}
		} else {
			this.handleError(msg);
		}
	},
	
	handleError : function( msg, e ) {
		Ext.DomHelper.overwrite("diffExpression-messages", {tag : 'img', src:'/Gemma/images/icons/warning.png' }); 
		Ext.DomHelper.append("diffExpression-messages", {tag : 'span', html : "&nbsp;&nbsp;"  + msg });  
		this.loadMask.hide();
	},
	
	clearError : function () {
		Ext.DomHelper.overwrite("diffExpression-messages", "");
	},
	
	validateSearch : function ( dsc ) {
		if ( dsc.queryGenesOnly && dsc.geneIds.length < 2 ) { 
			return "You must select more than one query gene to use 'search among query genes only'";
		} else if ( dsc.geneIds.length < 1 ) {
			return "We couldn't figure out which gene you want to query. Please use the search functionality to find genes.";
		} else if ( dsc.stringency < Ext.Gemma.DiffExpressionSearchForm.MIN_THRESHOLD ) {
			return "Minimum threshold is " + Ext.Gemma.DiffExpressionSearchForm.MIN_THRESHOLD;
		} else if ( dsc.eeIds && dsc.eeIds.length < 1 ) {
			return "There are no datasets that match your search terms";
		} else if ( !dsc.eeIds && !dsc.cannedAnalysisId ) {
			return "Please select an analysis";
		} else {
			return "";
		}
	},
	
	returnFromSearch : function ( result ) {
		this.loadMask.hide();
		this.fireEvent( 'aftersearch', this, result );
	},
	
	taxonChanged : function ( taxon ) {
		this.geneChooserPanel.taxonChanged( taxon );
	},
	
	getActiveEeIds : function() {
		return this.eeIds;
	}
	
} );

Ext.Gemma.DiffExpressionSearchForm.MIN_STRINGENCY = 0.0;


/* Ext.Gemma.DiffExpressionSummaryGrid constructor...
 * 	config is a hash with the following options:
 */
Ext.Gemma.DiffExpressionSummaryGrid = function ( config ) {
	
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
		data: Ext.Gemma.DiffExpressionSummaryGrid.transformData( genes, summary ),
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
	Ext.Gemma.DiffExpressionSummaryGrid.superclass.constructor.call( this, superConfig );
};

/* static methods...
 */
 
Ext.Gemma.DiffExpressionSummaryGrid.transformData = function ( genes, summary ) {
	/*
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
	*/
};

/* instance methods...
 */
Ext.extend( Ext.Gemma.DiffExpressionSummaryGrid, Ext.Gemma.GemmaGridPanel, {
} );