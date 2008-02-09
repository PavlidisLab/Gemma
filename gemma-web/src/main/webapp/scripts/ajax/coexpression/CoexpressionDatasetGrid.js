Ext.namespace('Ext.Gemma');

/* Ext.Gemma.CoexpressionDatasetGrid constructor...
 * 	config is a hash with the following options:
 */
Ext.Gemma.CoexpressionDatasetGrid = function ( config ) {
	
	/* keep a reference to ourselves to avoid convoluted scope issues below...
	 */
	var thisGrid = this;
	
	/* establish default config options...
	 */
	var superConfig = {
		collapsible : true,
		editable : false,
		header : true,
		collapsed : true,
		hidden : true,
		style : "margin-top: 1em; margin-bottom: .5em;"
	};
	
	superConfig.ds = new Ext.data.GroupingStore( {
		proxy : new Ext.data.MemoryProxy( [] ),
		reader : new Ext.data.ListRangeReader( {id:"id"}, Ext.Gemma.CoexpressionDatasetGrid.getRecord() ),
		groupField : 'id',
		sortInfo : { field : 'coexpressionLinkCount', dir : 'DESC' }
	} );
	
	superConfig.view = new Ext.grid.GroupingView( {
		forceFit : true,
		groupTextTpl : [ '<dt>{[ values.rs[0].shortName ]} {[ values.rs[0].name ]}</dt>',
			'<dd>{[ values.rs[0].arrayDesignCount }] arrays; {[ values.rs[0].bioAssayCount }] assays ',
			'<a href="/Gemma/expressionExperiment/showBioAssaysFromExpressionExperiment.html?id={text}"><img src="/Gemma/images/magnifier.png" height=10 width=10/></a>'
		]
	} );
	
	superConfig.cm = new Ext.grid.ColumnModel( [
		{ id: 'shortName', header: "Dataset", dataIndex: "shortName" },
		{ id: 'name', header: "Name", dataIndex: "name" },
		{ id: 'queryGene', header: "Query Gene", dataIndex: "queryGene" },
		{ header: "Raw Links", dataIndex: "rawCoexpressionLinkCount" },
		{ header: "Contributing Links", dataIndex: "coexpressionLinkCount" },
		{ header: "Specific Probe", dataIndex: "probeSpecificForQueryGene" },
		{ id: 'arrays', header: "Arrays", dataIndex: "arrayDesignCount" },
		{ id: 'assays', header: "Assays", dataIndex: "bioAssayCount" }
	] );
	superConfig.cm.defaultSortable = true;
	
	superConfig.autoExpandColumn = 'queryGene';

	for ( property in config ) {
		superConfig[property] = config[property];
	}
	Ext.Gemma.CoexpressionDatasetGrid.superclass.constructor.call( this, superConfig );
	
	this.getStore().on( "load", function () {
		this.autoSizeColumns();
		this.doLayout();
	}, this );
	
};

/* static methods
 */
 Ext.Gemma.CoexpressionDatasetGrid.getAssayCountStyler = function() {
	if ( Ext.Gemma.CoexpressionDatasetGrid.assayCountStyler === undefined ) {
		Ext.Gemma.CoexpressionDatasetGrid.assayCountStyler = function ( value, metadata, record, row, col, ds ) {
			return String.format(
				"{0}<a href='/Gemma/expressionExperiment/showBioAssaysFromExpressionExperiment.html?id={1}'><img src='/Gemma/images/magnifier.png' height=10 width=10/></a>", record.data.bioAssayCount, record.data.id );
		};
	}
	return Ext.Gemma.CoexpressionDatasetGrid.foundGeneStyler;
};

Ext.Gemma.CoexpressionDatasetGrid.getRecord = function() {
	if ( Ext.Gemma.CoexpressionDatasetGrid.record === undefined ) {
		Ext.Gemma.CoexpressionDatasetGrid.record = Ext.data.Record.create( [
			{ name:"id", type:"int" },
			{ name:"shortName", type:"string" },
			{ name:"name", type:"string" },
			{ name:"rawCoexpressionLinkCount", type:"int" },
			{ name:"coexpressionLinkCount", type:"int" },
			{ name:"probeSpecificForQueryGene", type:"boolean" },
			{ name:"arrayDesignCount", type:"int" },
			{ name:"bioAssayCount", type:"int", renderer: Ext.Gemma.CoexpressionDatasetGrid.getAssayCountStyler() },
			{ name:"queryGene", type:"string" }
		] );
	}
	return Ext.Gemma.CoexpressionDatasetGrid.record;
};

Ext.Gemma.CoexpressionDatasetGrid.updateDatasetInfo = function( datasets, eeMap ) {
	for ( var i=0; i<datasets.length; ++i ) {
		var ee = eeMap[ datasets[i].id ];
		if ( ee ) {
			datasets[i].shortName = ee.shortName;
			datasets[i].name = ee.name;
		}
	}
};

/* instance methods...
 */
Ext.extend( Ext.Gemma.CoexpressionDatasetGrid, Ext.Gemma.GemmaGridPanel, {

	loadData : function ( data ) {
		/* TODO get this in metadata somehow...
		 */
		var queryGenes = {}, numQueryGenes = 0;
		var datasets = {}, numDatasets = 0;
		for ( var i=0; i<data.length; ++i ) {
			if ( ! queryGenes[ data[i].queryGene ] ) {
				queryGenes[ data[i].queryGene ] = 1;
				++numQueryGenes;
			}
			if ( ! datasets[ data[i].id ] ) {
				datasets[ data[i].id ] = 1;
				++numDatasets;
			}
		}
		
		this.show();
		this.setTitle( String.format( "{0} dataset{1} relevant coexpression data", numDatasets, numDatasets == 1 ? " has" : "s have" ) );
		
		var shortNameCol = this.getColumnModel().getColumnById( 'shortName' );
		var nameCol = this.getColumnModel().getColumnById( 'name' );
		var queryCol = this.getColumnModel().getColumnById( 'queryGene' );
		var arrayCol = this.getColumnModel().getColumnById( 'arrays' );
		var assayCol = this.getColumnModel().getColumnById( 'assays' );
		if ( numQueryGenes > 1 ) {
			shortNameCol.hidden = true;
			nameCol.hidden = true;
			queryCol.hidden = false;
			arrayCol.hidden = true;
			assayCol.hidden = true;
			this.getStore().groupField = "id";
		} else {
			shortNameCol.hidden = false;
			nameCol.hidden = false;
			queryCol.hidden = true;
			arrayCol.hidden = false;
			arrayCol.hidden = false;
			this.getStore().groupField = "";
		}
		
		this.getStore().proxy.data = data;
		this.refresh();
		this.getView().refresh( true );
	}
	
} );
