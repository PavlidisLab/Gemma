Ext.namespace('Ext.Gemma');

/* Ext.Gemma.CoexpressionGrid constructor...
 * 	config is a hash with the following options:
 */
Ext.Gemma.CoexpressionGrid = function ( config ) {
	
	this.pageSize = config.pageSize; delete config.pageSize;
	
	/* keep a reference to ourselves to avoid convoluted scope issues below...
	 */
	var thisGrid = this;
	
	/* establish default config options...
	 */
	var superConfig = {
		collapsible : true,
		editable : false,
		style : "margin-bottom: 1em;"
	};
	
	if ( this.pageSize ) {
		superConfig.store = new Ext.Gemma.PagingDataStore( {
			proxy : new Ext.data.MemoryProxy( [] ),
			reader : new Ext.data.ListRangeReader( {id:"id"}, Ext.Gemma.CoexpressionGrid.getRecord() ),
			pageSize : this.pageSize
		} );
		superConfig.bbar = new Ext.Gemma.PagingToolbar( {
			pageSize : this.pageSize,
			store : superConfig.store
		} );
	} else {
		superConfig.ds = new Ext.data.Store( {
			proxy : new Ext.data.MemoryProxy( [] ),
			reader : new Ext.data.ListRangeReader( {id:"id"}, Ext.Gemma.CoexpressionGrid.getRecord() )
		} );
	}
	
	superConfig.cm = new Ext.grid.ColumnModel( [
		{ id: 'query', header: "Query Gene", dataIndex: "queryGene" },
		{ id: 'found', header: "Coexpressed Gene", dataIndex: "foundGene", renderer: Ext.Gemma.CoexpressionGrid.getFoundGeneStyler() },
		{ id: 'support', header: "Support", dataIndex: "supportKey", renderer: Ext.Gemma.CoexpressionGrid.getSupportStyler() },
		{ id: 'go', header: "GO Overlap", dataIndex: "goOverlap", renderer: Ext.Gemma.CoexpressionGrid.getGoStyler() },
		{ id: 'datasets', header: "Datasets", dataIndex: "supportingDatasetVector", renderer: Ext.Gemma.CoexpressionGrid.getBitImageStyler() }
	] );
	superConfig.cm.defaultSortable = true;
	superConfig.plugins = Ext.Gemma.CoexpressionGrid.getRowExpander();
	
	superConfig.autoExpandColumn = 'found';

	for ( property in config ) {
		superConfig[property] = config[property];
	}
	Ext.Gemma.CoexpressionGrid.superclass.constructor.call( this, superConfig );
	
	this.getStore().on( "load", function () {
		this.autoSizeColumns();
		this.doLayout();
	}, this );
	
};

/* static methods
 */
Ext.Gemma.CoexpressionGrid.getRecord = function() {
	if ( Ext.Gemma.CoexpressionGrid.record === undefined ) {
		Ext.Gemma.CoexpressionGrid.record = Ext.data.Record.create( [
			{ name:"queryGene", type:"string", convert: function( g ) { return g.officialSymbol } },
			{ name:"foundGene" },
			{ name:"supportKey", type:"int" },
			{ name:"positiveLinks", type:"int" },
			{ name:"negativeLinks", type:"int" },
			{ name:"numDatasetsLinkTestedIn", type:"int" },
			{ name:"nonSpecificPositiveLinks", type:"int" },
			{ name:"nonSpecificNegativeLinks", type:"int" },
			{ name:"hybridizesWithQueryGene", type:"boolean" },
			{ name:"goOverlap", type:"int" },
			{ name:"possibleOverlap", type:"int" },
			{ name:"testedDatasetVector" },
			{ name:"supportingDatasetVector" }
		] );
	}
	return Ext.Gemma.CoexpressionGrid.record;
};

Ext.Gemma.CoexpressionGrid.getFoundGeneStyler = function() {
	if ( Ext.Gemma.CoexpressionGrid.foundGeneStyler === undefined ) {
		Ext.Gemma.CoexpressionGrid.foundGeneStyler = function ( value, metadata, record, row, col, ds ) {
			var g = record.data.foundGene;
			return String.format( "<a href='/Gemma/gene/showGene.html?id={0}'>{1}</a> {2}", g.id, g.officialSymbol, g.officialName || "" );
		};
	}
	return Ext.Gemma.CoexpressionGrid.foundGeneStyler;
};

Ext.Gemma.CoexpressionGrid.getSupportStyler = function() {
	if ( Ext.Gemma.CoexpressionGrid.supportStyler === undefined ) {
		Ext.Gemma.CoexpressionGrid.supportStyler = function ( value, metadata, record, row, col, ds ) {
			var row = record.data;
			if ( row.positiveLinks || row.negativeLinks ) {
				var s = "";
				if ( row.positiveLinks ) {
					s = s + String.format( "<span class='positiveLink'>{0}{1}</span> ", row.positiveLinks, Ext.Gemma.CoexpressionGrid.getSpecificLinkString( row.positiveLinks, row.nonSpecificPositiveLinks ) );
				}
				if ( row.negativeLinks ) {
					s = s + String.format( "<span class='negativeLink'>{0}{1}</span> ", row.negativeLinks, Ext.Gemma.CoexpressionGrid.getSpecificLinkString( row.negativeLinks, row.nonSpecificNegativeLinks ) );
				}
				s = s + String.format( "{0}/ {1}", row.hybridizesWithQueryGene ? " *" : "", row.numDatasetsLinkTestedIn );
				return s;
			} else {
				return "-";
			}
		};
	}
	return Ext.Gemma.CoexpressionGrid.supportStyler;
};

Ext.Gemma.CoexpressionGrid.getSpecificLinkString = function( total, nonSpecific ) {
	return nonSpecific ? String.format( "<span class='specificLink'> ({0})</span>", total - nonSpecific ) : "";
};

Ext.Gemma.CoexpressionGrid.getGoStyler = function() {
	if ( Ext.Gemma.CoexpressionGrid.goStyler === undefined ) {
		Ext.Gemma.CoexpressionGrid.goStyler = function ( value, metadata, record, row, col, ds ) {
			var row = record.data;
			if ( row.goOverlap || row.possibleOverlap ) {
				return String.format( "{0}/{1}", row.goOverlap, row.possibleOverlap );
			} else {
				return "-";
			}
		};
	}
	return Ext.Gemma.CoexpressionGrid.goStyler;
};

Ext.Gemma.CoexpressionGrid.bitImageBarWidth = 2;
Ext.Gemma.CoexpressionGrid.bitImageBarHeight = 10;

Ext.Gemma.CoexpressionGrid.getBitImageMapTemplate = function() {
	if ( Ext.Gemma.CoexpressionGrid.bitImageMapTemplate === undefined ) {
		Ext.Gemma.CoexpressionGrid.bitImageMapTemplate = new Ext.XTemplate(
			'<tpl for=".">',
			'<area shape="rect" coords="{[ (xindex - 1) * this.barx ]},0,{[ xindex * this.barx ]},{[ this.bary ]}" ext:qtip="{name}" href="{externalUri}" />',
			'</tpl>',
			{
				barx : Ext.Gemma.CoexpressionGrid.bitImageBarWidth,
				bary : Ext.Gemma.CoexpressionGrid.bitImageBarHeight - 1
			}
		);
	}
	return Ext.Gemma.CoexpressionGrid.bitImageMapTemplate;
};

Ext.Gemma.CoexpressionGrid.getBitImageStyler = function() {
	if ( Ext.Gemma.CoexpressionGrid.bitImageStyler === undefined ) {
		Ext.Gemma.CoexpressionGrid.bitImageStyler = function ( value, metadata, record, row, col, ds ) {
			var bits = record.data.supportingDatasetVector;
			var width = Ext.Gemma.CoexpressionGrid.bitImageBarWidth * bits.length;
			var height = Ext.Gemma.CoexpressionGrid.bitImageBarHeight;
			var s = '<span style="background-color:#DDDDDD;">' +
				'<img src="/Gemma/spark?type=bar&width=' + width + '&height=' + height +
				'&color=black&spacing=0&data=';
			for ( var i=0; i<bits.length; ++i ) {
				if ( i>0 ) {
					s = s + ",";
				}
				s = s + ( bits[i] > 0 ? "20" : "0" );
			}
			s = s + '" usemap="eeMap" /></span>';
			return s;
		};
	}
	return Ext.Gemma.CoexpressionGrid.bitImageStyler;
};

Ext.Gemma.CoexpressionGrid.getRowExpander = function() {
	if ( Ext.Gemma.CoexpressionGrid.rowExpander === undefined ) {
		Ext.Gemma.CoexpressionGrid.rowExpander = new Ext.grid.RowExpander( {
			
		} );
	}
	return Ext.Gemma.CoexpressionGrid.rowExpander;
};

/* instance methods...
 */
Ext.extend( Ext.Gemma.CoexpressionGrid, Ext.Gemma.GemmaGridPanel, {

	loadData : function ( data ) {
		var queryGenes = {}, numQueryGenes = 0;
		for ( var i=0; i<data.length; ++i ) {
			var g = data[i].queryGene;
			if ( queryGenes[g.id] === undefined ) {
				queryGenes[g.id] = 1;
				++numQueryGenes;
			}
		}
		var queryCol = this.getColumnModel().getColumnById( 'query' );
		if ( numQueryGenes > 1 ) {
			queryCol.hidden = false;
		} else {
			queryCol.hidden = true;
		}
		this.getStore().proxy.data = data;
		this.refresh();
		this.getView().refresh( true );
	}
	
} );

/* Ext.Gemma.CoexpressionGridRowExpander constructor...
 */
Ext.Gemma.CoexpressionGridRowExpander = function ( config ) {
};

/* instance methods...
 */
Ext.extend( Ext.Gemma.CoexpressionGridRowExpander, Ext.grid.RowExpander, {
	
	generateBodyContent : function (record, rowIndex) {
    	
    }
	
} );