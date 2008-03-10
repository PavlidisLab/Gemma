/*
* Grid to display ExpressionExperiments.
* Author: Paul (based on Luke's CoexpressionDatasetGrid)
* $Id$
*/
Ext.namespace('Ext.Gemma.ExpressionExperimentGrid');


/* Ext.Gemma.ExpressionExperimentGrid constructor 
 */
Ext.Gemma.ExpressionExperimentGrid = function ( div, config ) {
 	this.readMethod = config.readMethod; delete config.readMethod;
	this.readParams = config.readParams; delete config.readParams;
	this.editable = config.editable; delete config.editable;
	this.pageSize = config.pageSize; delete config.pageSize;
	this.ddGroup = config.ddGroup; delete config.ddGroup;
	
	/* keep a reference to ourselves to avoid convoluted scope issues below...
	 */
	var thisGrid = this;
	 
	
	/* establish default config options.
	 */
	var superConfig = {
		renderTo : div,
		collapsible : false,
		header : true,
		collapsed : false,
		hidden : false
	};
	
	if ( this.pageSize ) {
		superConfig.ds = new Ext.Gemma.PagingDataStore( {
			proxy : new Ext.data.DWRProxy( this.readMethod ),
			reader : new Ext.data.ListRangeReader( {id:"id"}, Ext.Gemma.ExpressionExperimentGrid.getRecord() ),
			pageSize : this.pageSize
		} );
		superConfig.bbar = new Ext.Gemma.PagingToolbar( {
			pageSize : this.pageSize,
			store : superConfig.ds
		} );
	} else {
		superConfig.ds = new Ext.data.Store( {
			proxy : new Ext.data.DWRProxy( this.readMethod ),
			reader : new Ext.data.ListRangeReader( {id:"id"}, Ext.Gemma.ExpressionExperimentGrid.getRecord() )
		} );
	}
	
	superConfig.cm = new Ext.grid.ColumnModel( [
		{ id: 'shortName', header: "Dataset", dataIndex: "shortName" },
		{ id: 'name', header: "Name", dataIndex: "name" },
		{ id: 'arrays', header: "Arrays", dataIndex: "arrayDesignCount" },
		{ id: 'assays', header: "Assays", dataIndex: "bioAssayCount" }
	] );
	superConfig.cm.defaultSortable = true;
	
	superConfig.autoExpandColumn = 'shortName';

	
	for ( property in config ) {
		if (true) {
			superConfig[property] = config[property];
		}
	}
	
	//if (this.editable) {
	
	//}
	
	//var resizer = new Ext.Resizable(div, {
   // 	handles: "w,e",
   // 	pinned: true
	//});
//	resizer.on('resize', thisGrid.doLayout, thisGrid);
	
	Ext.Gemma.ExpressionExperimentGrid.superclass.constructor.call( this, superConfig );
	
	this.getStore().on( "load", function () {
		this.autoSizeColumns();
		this.doLayout();
	}, this );
	
};


/* static methods
 */
 Ext.Gemma.ExpressionExperimentGrid.getAssayCountStyler = function() {
	if ( Ext.Gemma.ExpressionExperimentGrid.assayCountStyler === undefined ) {
		Ext.Gemma.ExpressionExperimentGrid.assayCountStyler = function ( value, metadata, record, row, col, ds ) {
			return String.format(
				"{0}<a href='/Gemma/expressionExperiment/showBioAssaysFromExpressionExperiment.html?id={1}'><img src='/Gemma/images/magnifier.png' height=10 width=10/></a>", record.data.bioAssayCount, record.data.id );
		};
	}
	return Ext.Gemma.ExpressionExperimentGrid.foundGeneStyler;
};

Ext.Gemma.ExpressionExperimentGrid.updateDatasetInfo = function( datasets, eeMap ) {
	for ( var i=0; i<datasets.length; ++i ) {
		var ee = eeMap[ datasets[i].id ];
		if ( ee ) {
			datasets[i].shortName = ee.shortName;
			datasets[i].name = ee.name;
		}
	}
};

Ext.Gemma.ExpressionExperimentGrid.getRecord = function() {
	if ( Ext.Gemma.ExpressionExperimentGrid.record === undefined ) {
		Ext.Gemma.ExpressionExperimentGrid.record = Ext.data.Record.create( [
			{ name:"id", type:"int" },
			{ name:"shortName", type:"string" },
			{ name:"name", type:"string" },
			{ name:"arrayDesignCount", type:"int" },
			{ name:"bioAssayCount", type:"int", renderer: Ext.Gemma.ExpressionExperimentGrid.getAssayCountStyler() }
		] );
	}
	return Ext.Gemma.ExpressionExperimentGrid.record;
};

/*
* Type definition
*/
Ext.extend( Ext.Gemma.ExpressionExperimentGrid, Ext.Gemma.GemmaGridPanel, {
	getReadParams : function() {
		return ( typeof this.readParams == "function" ) ? this.readParams() : this.readParams;
	}  
    
});

Ext.Gemma.DatasetSearchToolBar = function ( grid, config ) {

	var bar = this;
	var thisGrid = grid;
	this.targetGrid = config.targetGrid;
	
	var eeSearchField = new Ext.Gemma.DatasetSearchField( {
		fieldLabel : "Experiment keywords"
	} );
	this.eeSearchField = eeSearchField;
	eeSearchField.on( 'aftersearch', function ( field, results ) {
		this.getStore().load( { params : [results] });
	}, grid );
	
	var taxonCombo = new Ext.Gemma.TaxonCombo( {
			emptyText : 'select a taxon',
			width : 150
		} );
		
	taxonCombo.on( "taxonchanged", function( combo, taxon ) {
		this.eeSearchField.taxonChanged(taxon, true);	 	
	}, this );

	Ext.Gemma.DatasetSearchToolBar.superclass.constructor.call( this, {
		autoHeight : true,
		renderTo : thisGrid.tbar
	} );		

	var grabber = new Ext.Button({text : "Grab >>", handler : function( button, ev ) {
		this.targetGrid.getStore().add( thisGrid.getSelectionModel().getSelections());
		this.targetGrid.getView().refresh();
	}, scope : this });
	
	this.addField( taxonCombo );
	this.addSpacer();
	this.addField( eeSearchField );
	this.addFill( );
	this.addButton( grabber );


};

Ext.extend(Ext.Gemma.DatasetSearchToolBar, Ext.Toolbar, {
});

 