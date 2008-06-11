Ext.namespace('Gemma');

/* Gemma.CoexpressionDatasetGrid constructor...
 * 	config is a hash with the following options:
 */
Gemma.CoexpressionDatasetGrid = function ( config ) {

	this.adjective = config.adjective; delete config.adjective;
	
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
		reader : new Ext.data.ListRangeReader( {}, Gemma.CoexpressionDatasetGrid.getRecord() ),
		groupField : 'queryGene',
		sortInfo : { field : 'coexpressionLinkCount', dir : 'DESC' }  
	} );
	
	superConfig.view = new Ext.grid.GroupingView( {
		hideGroupedColumn : true
	} );
	
	superConfig.cm = new Ext.grid.ColumnModel( [
		{ id: 'shortName', header: "Dataset", dataIndex: "shortName" , renderer: Gemma.CoexpressionDatasetGrid.getEEStyler() },
		{ id: 'name', header: "Name", dataIndex: "name" },
		{ id: 'queryGene', header: "Query Gene", dataIndex: "queryGene", hidden: true },
		{ header: "Raw Links", dataIndex: "rawCoexpressionLinkCount" },
		{ header: "Contributing Links", dataIndex: "coexpressionLinkCount" },
		{ header: "Specific Probe", dataIndex: "probeSpecificForQueryGene", type:"boolean", renderer: Gemma.CoexpressionDatasetGrid.getBooleanStyler()  },
		{ id: 'arrays', header: "Arrays", dataIndex: "arrayDesignCount" },
		{ id: 'assays', header: "Assays", dataIndex: "bioAssayCount", renderer: Gemma.CoexpressionDatasetGrid.getAssayCountStyler()  }
	] );
	superConfig.cm.defaultSortable = true;
	
	superConfig.autoExpandColumn = 'name';

	for ( property in config ) {
		superConfig[property] = config[property];
	}
	Gemma.CoexpressionDatasetGrid.superclass.constructor.call( this, superConfig );
	
	this.getStore().on( "load", function () {
		this.doLayout();
	}, this );
	
};

/* static methods
 */
 Gemma.CoexpressionDatasetGrid.getAssayCountStyler = function() {
	if ( Gemma.CoexpressionDatasetGrid.assayCountStyler === undefined ) {
		Gemma.CoexpressionDatasetGrid.assayCountStyler = function ( value, metadata, record, row, col, ds ) {
			return String.format(
				"{0}&nbsp;<a href='/Gemma/expressionExperiment/showBioAssaysFromExpressionExperiment.html?id={1}'><img src='/Gemma/images/icons/magnifier.png' height='10' width='10'/></a>", record.data.bioAssayCount, record.data.id );
		};
	}
	return Gemma.CoexpressionDatasetGrid.assayCountStyler;
};

Gemma.CoexpressionDatasetGrid.getRecord = function() {
	if ( Gemma.CoexpressionDatasetGrid.record === undefined ) {
		Gemma.CoexpressionDatasetGrid.record = Ext.data.Record.create( [
			{ name:"id", type:"int" },
			{ name:"shortName", type:"string"},
			{ name:"name", type:"string" },
			{ name:"rawCoexpressionLinkCount", type:"int" },
			{ name:"coexpressionLinkCount", type:"int" },
			{ name:"probeSpecificForQueryGene"},
			{ name:"arrayDesignCount", type:"int" },
			{ name:"externalUri", type:"string" },
			{ name:"bioAssayCount", type:"int"},
			{ name:"queryGene", type:"string" }
		] );
	}
	return Gemma.CoexpressionDatasetGrid.record;
};

Gemma.CoexpressionDatasetGrid.getEEStyler = function() {
	if ( Gemma.CoexpressionDatasetGrid.eeStyler === undefined ) {
		Gemma.CoexpressionDatasetGrid.eeTemplate = new Ext.Template(
			"<a target='_blank' href='/Gemma/expressionExperiment/showExpressionExperiment.html?id={id}' ext:qtip='{name}'>{shortName}</a>"
		);
		Gemma.CoexpressionDatasetGrid.eeStyler = function ( value, metadata, record, row, col, ds ) {
			return Gemma.CoexpressionDatasetGrid.eeTemplate.apply( record.data );
		};
	}
	return Gemma.CoexpressionDatasetGrid.eeStyler;
};

Gemma.CoexpressionDatasetGrid.getBooleanStyler = function() {
	if ( Gemma.CoexpressionDatasetGrid.booleanStyler === undefined ) {
		Gemma.CoexpressionDatasetGrid.booleanStyler = function ( value, metadata, record, row, col, ds ) {
			if ( value ) {
				return "<img src='/Gemma/images/icons/ok.png' height='10' width='10' />";
			} 
			return "";
		};
	}
	return Gemma.CoexpressionDatasetGrid.booleanStyler;
};

Gemma.CoexpressionDatasetGrid.updateDatasetInfo = function( datasets, eeMap ) {
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
Ext.extend( Gemma.CoexpressionDatasetGrid, Gemma.GemmaGridPanel, {

	loadData : function ( isCannedAnalysis, numQueryGenes, data ) {
		/* TODO get this in metadata somehow...
		 */
		var datasets = {}, numDatasets = 0;
		for ( var i=0; i<data.length; ++i ) {
			if ( ! datasets[ data[i].id ] ) {
				datasets[ data[i].id ] = 1;
				++numDatasets;
			}
		}
		
		this.show();
		this.setTitle( String.format( "{0} dataset{1} relevant{2} coexpression data",
			numDatasets,
			numDatasets == 1 ? " has" : "s have",
			this.adjective ? " " + this.adjective : ""
		) );

/*		
		var shortNameCol = this.getColumnModel().getColumnById( 'shortName' );
		var nameCol = this.getColumnModel().getColumnById( 'name' );
		var queryCol = this.getColumnModel().getColumnById( 'queryGene' );
		var arrayCol = this.getColumnModel().getColumnById( 'arrays' );
		var assayCol = this.getColumnModel().getColumnById( 'assays' );
		if ( numQueryGenes > 1 ) {
			shortNameCol.hidden = false;
			nameCol.hidden = false;
			queryCol.hidden = true;
			arrayCol.hidden = false;
			arrayCol.hidden = false;
			this.getStore().groupField = "queryGene";
		} else {
			shortNameCol.hidden = false;
			nameCol.hidden = false;
			queryCol.hidden = true;
			arrayCol.hidden = false;
			arrayCol.hidden = false;
			this.getStore().groupField = "";
		}
*/
		
		this.getStore().proxy.data = data;
		this.getStore().reload();
		this.getView().refresh( true );
	}
	
} );
