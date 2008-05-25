/**
* @author keshav
* @version $Id$ 
*/
Ext.namespace('Ext.Gemma');

/* Ext.Gemma.DiffExpressionGrid constructor...
 * 	config is a hash with the following options:
 */
Ext.Gemma.DiffExpressionGrid = function ( config ) {
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
		style : "margin-bottom: 1em;",
		viewConfig : {
			emptyText : "no differential expression results available"
		}
	};
	
	if ( this.pageSize ) {
		superConfig.ds = new Ext.Gemma.PagingDataStore( {
			proxy : new Ext.data.MemoryProxy( [] ),
			reader : new Ext.data.ListRangeReader( {id:"id"}, Ext.Gemma.DiffExpressionGrid.getRecord() ),
			sortInfo : { field: 'sortKey', direction: 'ASC' },
			pageSize : this.pageSize
		} );
		superConfig.bbar = new Ext.Gemma.PagingToolbar( {
			pageSize : this.pageSize,
			store : superConfig.ds
		} );
	} else {
		superConfig.ds = new Ext.data.Store( {
			proxy : new Ext.data.MemoryProxy( [] ),
			reader : new Ext.data.ListRangeReader( {id:"id"}, Ext.Gemma.DiffExpressionGrid.getRecord() ),
			sortInfo : { field: 'sortKey', direction: 'ASC' }
		} );
	}
	//superConfig.ds.setDefaultSort( 'p' );
	
	this.rowExpander = new Ext.Gemma.DifEEGridRowExpander( {
		tpl : ""
	} );
		
	superConfig.plugins = this.rowExpander;

	superConfig.cm = new Ext.grid.ColumnModel( [
		//this.rowExpander,
		{ id: 'gene', header: "Gene", dataIndex: "gene", width : 80},
		{ id: 'active', header: "Active Datasets", dataIndex: "activeExperiments", renderer: Ext.Gemma.DiffExpressionGrid.getActiveExperimentsStyler() },
		{ id: 'supporting', header: "Supporting Datasets", dataIndex: "supportingExperiments", renderer: Ext.Gemma.DiffExpressionGrid.getSupportingExperimentsStyler()},
		{ id: 'fisherPValue', header: "Fisher P Value", dataIndex: "fisherPValue", renderer: function ( fisherPValue ) { return fisherPValue.toFixed(6); } }
		/*
		{ id: 'name', header: "Name", dataIndex: "expressionExperiment", renderer: Ext.Gemma.DiffExpressionGrid.getEENameStyler(), width : 120 },
		{ id: 'probe', header: "Probe", dataIndex: "probe" },
		{ id: 'efs', header: "Factor(s)", dataIndex: "experimentalFactors", renderer: Ext.Gemma.DiffExpressionGrid.getEFStyler(), sortable: false },
		{ id: 'p', header: "Sig. (FDR)", dataIndex: "p", renderer: function ( p ) { return p.toFixed(6); } }
		*/
	] );
	superConfig.cm.defaultSortable = true;
	
	superConfig.autoExpandColumn = 'gene';
	superConfig.autoExpandColumn = 'supporting';
	superConfig.autoExpandColumn = 'fisherPValue';

	for ( property in config ) {
		superConfig[property] = config[property];
	}
	Ext.Gemma.DiffExpressionGrid.superclass.constructor.call( this, superConfig );
	
	this.originalTitle = this.title;
};

/* static methods
 */
Ext.Gemma.DiffExpressionGrid.getRecord = function() {
	if ( Ext.Gemma.DiffExpressionGrid.record === undefined ) {
		Ext.Gemma.DiffExpressionGrid.record = Ext.data.Record.create( [
		    { name:"id", type:"int"},
			{ name:"gene", type:"string", convert: function( gene ) { return gene.officialSymbol; } },
			{ name:"activeExperiments" },
			{ name:"supportingExperiments" },			
			{ name:"fisherPValue", type:"float" }
			/*	
			{ name:"expressionExperiment"},
			{ name:"probe", type:"string" },
			{ name:"experimentalFactors" },
			{ name:"p", type:"float" }
			*/
		] );
	}
	return Ext.Gemma.DiffExpressionGrid.record;
};

Ext.Gemma.DiffExpressionGrid.getEEStyler = function() {
	if ( Ext.Gemma.DiffExpressionGrid.eeNameStyler === undefined ) {
		Ext.Gemma.DiffExpressionGrid.eeNameTemplate = new Ext.Template(
			"<a target='_blank' href='/Gemma/expressionExperiment/showExpressionExperiment.html?id={id}' ext:qtip='{name}'>{shortName}</a>"
		);
		Ext.Gemma.DiffExpressionGrid.eeNameStyler = function ( value, metadata, record, row, col, ds ) {
			var ee = record.data.expressionExperiment;
			return Ext.Gemma.DiffExpressionGrid.eeNameTemplate.apply( ee );
		};
	}
	return Ext.Gemma.DiffExpressionGrid.eeNameStyler;
};

Ext.Gemma.DiffExpressionGrid.getActiveExperimentsStyler = function() {
	if ( Ext.Gemma.DiffExpressionGrid.activeExperimentsStyler === undefined ) {
		Ext.Gemma.DiffExpressionGrid.activeExperimentsStyler = function ( value, metadata, record, row, col, ds ) {
			var ees = record.data.activeExperiments;
			return ees.size();
		};
	}
	return Ext.Gemma.DiffExpressionGrid.activeExperimentsStyler;
};

Ext.Gemma.DiffExpressionGrid.getSupportingExperimentsStyler = function() {
	if ( Ext.Gemma.DiffExpressionGrid.supportingExperimentsStyler === undefined ) {
		Ext.Gemma.DiffExpressionGrid.supportingExperimentsStyler = function ( value, metadata, record, row, col, ds ) {
			var ees = record.data.supportingExperiments;
			return ees.size();
		};
	}
	return Ext.Gemma.DiffExpressionGrid.supportingExperimentsStyler;
};

Ext.Gemma.DiffExpressionGrid.getEENameStyler = function() {
	if ( Ext.Gemma.DiffExpressionGrid.eeStyler === undefined ) {
		Ext.Gemma.DiffExpressionGrid.eeTemplate = new Ext.Template(
			"{name}"
		);
		Ext.Gemma.DiffExpressionGrid.eeStyler = function ( value, metadata, record, row, col, ds ) {
			var ee = record.data.expressionExperiment;
			return Ext.Gemma.DiffExpressionGrid.eeTemplate.apply( ee );
		};
	}
	return Ext.Gemma.DiffExpressionGrid.eeStyler;
};

Ext.Gemma.DiffExpressionGrid.getEFStyler = function() {
	if ( Ext.Gemma.DiffExpressionGrid.efStyler === undefined ) {
		Ext.Gemma.DiffExpressionGrid.efTemplate = new Ext.XTemplate(
			
			'<tpl for=".">',
				"<a target='_blank' ext:qtip='{factorValues}'>{name}</a>\n",
				//'<p><tooltip caption={factorValues} descr="factor values">{name}</tooltip></p>',
			'</tpl>'
				
		);
		Ext.Gemma.DiffExpressionGrid.efStyler = function ( value, metadata, record, row, col, ds ) {
			var efs = record.data.experimentalFactors;
			return Ext.Gemma.DiffExpressionGrid.efTemplate.apply( efs);
		};
	}
	return Ext.Gemma.DiffExpressionGrid.efStyler;
};

/* instance methods...
 */
Ext.extend( Ext.Gemma.DiffExpressionGrid, Ext.Gemma.GemmaGridPanel, {

loadData : function (data) {

		this.getStore().proxy.data = data;
		this.getStore().reload( { resetPage : true } );
		this.getView().refresh( true ); // refresh column headers
	}
	
});

//Needed to override row expander so that the id for EE would get used and not the id from the differentialExpressionValueObject
Ext.Gemma.DifEEGridRowExpander = function ( config ) {
 
	this.grid = config.grid;
	var superConfig = {
	};
	
	for ( property in config ) {
		superConfig[property] = config[property];
	}
	Ext.Gemma.DifEEGridRowExpander.superclass.constructor.call( this, superConfig );
	
};


Ext.extend( Ext.Gemma.DifEEGridRowExpander, Ext.Gemma.EEGridRowExpander, {

		beforeExpand : function (record, body, rowIndex) {		
				ExpressionExperimentController.getDescription(record.data.expressionExperiment.id, { callback : this.fillExpander.createDelegate(this, [body, rowIndex], true) } );
				return true;			
		}
	
});
