Ext.namespace('Ext.Gemma');

/* Ext.Gemma.DifferentialExpressionGrid constructor...
 * 	config is a hash with the following options:
 */
Ext.Gemma.DifferentialExpressionGrid = function ( config ) {
	Ext.QuickTips.init();
	
	this.pageSize = config.pageSize; delete config.pageSize;
	this.geneId = config.geneId; delete config.geneId;
	this.threshold = config.threshold; delete config.threshold;
	
	/* keep a reference to ourselves to avoid convoluted scope issues below...
	 */
	var thisGrid = this;
	
	/* establish default config options...
	 */
	var superConfig = {
		collapsible : true,
		editable : false,
		viewConfig : {
			emptyText : "no differential expression results available"
		}
	};
	
	if ( this.pageSize ) {
		superConfig.ds = new Ext.Gemma.PagingDataStore( {
			proxy : new Ext.data.DWRProxy( DifferentialExpressionSearchController.getDifferentialExpression ),
			reader : new Ext.data.ListRangeReader( {id:"id"}, Ext.Gemma.DifferentialExpressionGrid.getRecord() ),
			pageSize : this.pageSize
		} );
		superConfig.bbar = new Ext.Gemma.PagingToolbar( {
			pageSize : this.pageSize,
			store : superConfig.ds
		} );
	} else {
		superConfig.ds = new Ext.data.Store( {
			proxy : new Ext.data.DWRProxy( DifferentialExpressionSearchController.getDifferentialExpression ),
			reader : new Ext.data.ListRangeReader( {id:"id"}, Ext.Gemma.DifferentialExpressionGrid.getRecord() )
		} );
	}
	superConfig.ds.setDefaultSort( 'p' );
	superConfig.ds.load( { params: [ this.geneId, this.threshold ] } );
	
	superConfig.cm = new Ext.grid.ColumnModel( [
		{ id: 'ee', header: "Expression Experiment", dataIndex: "expressionExperiment", renderer: Ext.Gemma.DifferentialExpressionGrid.getEEStyler() },
		{ id: 'probe', header: "Probe", dataIndex: "probe" },
		{ id: 'efs', header: "Experimental Factors", dataIndex: "experimentalFactors", renderer: Ext.Gemma.DifferentialExpressionGrid.getEFStyler(), sortable: false },
		{ id: 'p', header: "Significance (FDR)", dataIndex: "p" },
	] );
	superConfig.cm.defaultSortable = true;
	
	superConfig.autoExpandColumn = 'ee';

	for ( property in config ) {
		superConfig[property] = config[property];
	}
	Ext.Gemma.DifferentialExpressionGrid.superclass.constructor.call( this, superConfig );
	
	this.getStore().on( "load", function () {
		this.autoSizeColumns();
		this.doLayout();
	}, this );
	
};

/* static methods
 */
Ext.Gemma.DifferentialExpressionGrid.getRecord = function() {
	if ( Ext.Gemma.DifferentialExpressionGrid.record === undefined ) {
		Ext.Gemma.DifferentialExpressionGrid.record = Ext.data.Record.create( [
			{ name:"expressionExperiment", sortType: function( ee ) { return ee.shortName } },
			{ name:"probe", type:"string" },
			{ name:"experimentalFactors" },
			{ name:"p", type:"float" }
		] );
	}
	return Ext.Gemma.DifferentialExpressionGrid.record;
};

Ext.Gemma.DifferentialExpressionGrid.getEEStyler = function() {
	if ( Ext.Gemma.DifferentialExpressionGrid.eeStyler === undefined ) {
		Ext.Gemma.DifferentialExpressionGrid.eeTemplate = new Ext.Template(
			"<a href='{externalUri}'>{shortName}</a> {name}"
		);
		Ext.Gemma.DifferentialExpressionGrid.eeStyler = function ( value, metadata, record, row, col, ds ) {
			var ee = record.data.expressionExperiment;
			return Ext.Gemma.DifferentialExpressionGrid.eeTemplate.apply( ee );
		};
	}
	return Ext.Gemma.DifferentialExpressionGrid.eeStyler;
};

Ext.Gemma.DifferentialExpressionGrid.getEFStyler = function() {
	if ( Ext.Gemma.DifferentialExpressionGrid.efStyler === undefined ) {
//		Ext.Gemma.DifferentialExpressionGrid.efTemplate = new Ext.XTemplate(
//			"<tpl for='.'>",
//				"{name} ({category})",
//			"</tpl>"
//		);
		Ext.Gemma.DifferentialExpressionGrid.efStyler = function ( value, metadata, record, row, col, ds ) {
			var efs = record.data.experimentalFactors;
			var names = [];
			for ( var i=0; i<efs.length; ++i ) {
				names.push( efs[i].name || "unnamed factor" );
			}
			return names.join( "," );
//			return Ext.Gemma.DifferentialExpressionGrid.efTemplate.apply( ef );
		};
	}
	return Ext.Gemma.DifferentialExpressionGrid.efStyler;
};

/* instance methods...
 */
Ext.extend( Ext.Gemma.DifferentialExpressionGrid, Ext.Gemma.GemmaGridPanel, {
	
} );