/**
* A grid to display the differential expression meta analysis results.
* @author keshav
* @version $Id$ 
*/
Ext.namespace('Ext.Gemma');

/* Ext.Gemma.DiffExpressionMetaAnalysisGrid constructor...
 * 	config is a hash with the following options:
 */
Ext.Gemma.DiffExpressionMetaAnalysisGrid = function ( config ) {
	Ext.QuickTips.init();
	
	this.pageSize = config.pageSize; delete config.pageSize;
	
	/* keep a reference to ourselves to avoid convoluted scope issues below...
	 */
	var thisGrid = this;
	
	/* establish default config options...
	 */
	var superConfig = {
		collapsible : true,
		editable : false,
		width : 350,
		height : 250,
		viewConfig : {
			emptyText : "no differential expression meta analysis results available"
		}
	};
	
	if ( this.pageSize ) {
		superConfig.ds = new Ext.Gemma.PagingDataStore( {
			proxy : new Ext.data.MemoryProxy( [] ),
			reader : new Ext.data.ListRangeReader( {id:"id"}, Ext.Gemma.DiffExpressionMetaAnalysisGrid.getRecord() ),
			//sortInfo : { field: 'sortKey', direction: 'ASC' },
			pageSize : this.pageSize
		} );
		superConfig.bbar = new Ext.Gemma.PagingToolbar( {
			pageSize : this.pageSize,
			store : superConfig.ds
		} );
	} else {
		superConfig.ds = new Ext.data.Store( {
			proxy : new Ext.data.MemoryProxy( [] ),
			reader : new Ext.data.ListRangeReader( {id:"id"}, Ext.Gemma.DiffExpressionMetaAnalysisGrid.getRecord() ),
			//sortInfo : { field: 'sortKey', direction: 'ASC' }
		} );
	}
	//superConfig.ds.setDefaultSort( 'p' );
	
	/*
	this.rowExpander = new Ext.Gemma.DifEEGridRowExpander( {
		tpl : ""
	} );
		
	superConfig.plugins = this.rowExpander;
	*/
	
	superConfig.cm = new Ext.grid.ColumnModel( [
		//this.rowExpander,
		{ id: 'gene', header: "Gene", dataIndex: "gene", width : 80},
		{ id: 'numSearchedDataSets', header: "Searched", dataIndex: "numSearchedDataSets", width : 80},
		{ id: 'numSupportingDataSets', header: "Support", dataIndex: "numSupportingDataSets", width : 80},
		{ id: 'fisherPValue', header: "Sig. (FDR)", dataIndex: "fisherPValue", renderer: function ( fisherPValue ) { return fisherPValue.toFixed(6); } }
		//{ id: 'probe', header: "Probe", dataIndex: "probe" },
	] );
	//superConfig.cm.defaultSortable = true;

	for ( property in config ) {
		superConfig[property] = config[property];
	}
	Ext.Gemma.DiffExpressionMetaAnalysisGrid.superclass.constructor.call( this, superConfig );
	
	this.originalTitle = this.title;
	
};

/* static methods
 */
Ext.Gemma.DiffExpressionMetaAnalysisGrid.getRecord = function() {
	if ( Ext.Gemma.DiffExpressionMetaAnalysisGrid.record === undefined ) {
		Ext.Gemma.DiffExpressionMetaAnalysisGrid.record = Ext.data.Record.create( [
		    //{ name:"id", type:"int"},
			{ name:"gene", type:"string", convert: function( gene ) { return gene.officialSymbol; } },
			{ name:"numSearchedDataSets", type:"int" },
			{ name:"numSupportingDataSets", type:"int" },
			{ name:"fisherPValue", type:"float" }
			//{ name:"expressionExperiment"},
			//{ name:"probe", type:"string" },
			//{ name:"experimentalFactors" },
		] );
	}
	return Ext.Gemma.DiffExpressionMetaAnalysisGrid.record;
};

/* instance methods...
 */
Ext.extend( Ext.Gemma.DiffExpressionMetaAnalysisGrid, Ext.Gemma.GemmaGridPanel, {

loadData : function (data) {

		this.getStore().proxy.data = data;
		this.getStore().reload( { resetPage : true } );
		this.getView().refresh( true ); // refresh column headers
	}
	
});