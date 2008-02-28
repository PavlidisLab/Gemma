Ext.namespace('Ext.Gemma');

Ext.onReady( function() {
	Ext.QuickTips.init();
	Ext.state.Manager.setProvider( new Ext.state.CookieProvider() );
	
	var admin = dwr.util.getValue("hasAdmin");
	
	var searchPanel = new Ext.Gemma.CoexpressionSearchForm( {
	} );
	searchPanel.render( "coexpression-form" );
	
	var summaryPanel;
	
	var knownGeneDatasetGrid = new Ext.Gemma.CoexpressionDatasetGrid( {
		renderTo : "coexpression-results"
	} );
	var knownGeneGrid = new Ext.Gemma.CoexpressionGrid( {
		renderTo : "coexpression-results",
		title : "Coexpressed genes",
		pageSize : 25
	} );
	var predictedGeneGrid;
	var probeAlignedGrid;
	if ( admin ) {
		var predictedGeneDatasetGrid = new Ext.Gemma.CoexpressionDatasetGrid( {
			renderTo : "coexpression-results",
			adjective : "predicted gene"
		} );
		predictedGeneGrid = new Ext.Gemma.CoexpressionGrid( {
			renderTo : "coexpression-results",
			title : "Coexpressed predicted genes",
			pageSize : 25,
			collapsed : true
		} );
		var probeAlignedDatasetGrid = new Ext.Gemma.CoexpressionDatasetGrid( {
			renderTo : "coexpression-results",
			adjective : "probe-aligned region"
		} );
		probeAlignedGrid = new Ext.Gemma.CoexpressionGrid( {
			renderTo : "coexpression-results",
			title : "Coexpressed probe-aligned regions",
			pageSize : 25,
			collapsed : true
		} );
	}
	
	searchPanel.on( "aftersearch", function ( panel, result ) {
		var eeMap = {};
		if ( result.datasets ) {
			for ( var i=0; i<result.datasets.length; ++i ) {
				var ee = result.datasets[i];
				eeMap[ee.id] = ee;
			}
		}
		
		if ( summaryPanel ) {
			// grid.destroy() seems to be broken...
			try {
				summaryPanel.destroy();
			} catch (e) {}
		}
		summaryPanel = new Ext.Gemma.CoexpressionSummaryGrid( {
			genes : result.queryGenes,
			summary : result.summary
		} );
		summaryPanel.render( "coexpression-summary" );
		
		// create expression experiment image map
		var imageMap = Ext.get( "eeMap" );
		if ( ! imageMap ) {
			imageMap = Ext.getBody().createChild( {
				tag: 'map',
				id: 'eeMap',
				name: 'eeMap'
			} );
		}
		Ext.Gemma.CoexpressionGrid.getBitImageMapTemplate().overwrite( imageMap, result.datasets );
		
		Ext.Gemma.CoexpressionDatasetGrid.updateDatasetInfo( result.knownGeneDatasets, eeMap );
		knownGeneDatasetGrid.loadData( result.isCannedAnalysis, result.queryGenes.length, result.knownGeneDatasets ) ;
		knownGeneGrid.loadData( result.isCannedAnalysis, result.queryGenes.length, result.knownGeneResults );
		
		knownGeneGrid.setTitle( String.format( "Coexpressed genes <a href='{0}'>(bookmarkable link)</a>", panel.getBookmarkableLink() ) );
		
		if ( admin ) {
			Ext.Gemma.CoexpressionDatasetGrid.updateDatasetInfo( result.predictedGeneDatasets, eeMap );
			predictedGeneDatasetGrid.loadData( result.isCannedAnalysis, result.queryGenes.length, result.predictedGeneDatasets ) ;
			predictedGeneGrid.loadData( result.isCannedAnalysis, result.queryGenes.length, result.predictedGeneResults );
			Ext.Gemma.CoexpressionDatasetGrid.updateDatasetInfo( result.probeAlignedRegionDatasets, eeMap );
			probeAlignedDatasetGrid.loadData( result.isCannedAnalysis, result.queryGenes.length, result.probeAlignedRegionDatasets ) ;
			probeAlignedGrid.loadData( result.isCannedAnalysis, result.queryGenes.length, result.probeAlignedRegionResults );
		}
	} );
	
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
		title : 'Search Summary',
		width : 250
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