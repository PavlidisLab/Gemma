/**
* @author keshav
* @version $Id$ 
*/
Ext.namespace('Ext.Gemma');

Ext.onReady( function() {
	Ext.QuickTips.init();
	Ext.state.Manager.setProvider( new Ext.state.CookieProvider() );
	
	var admin = dwr.util.getValue("hasAdmin");
	
	if ( Ext.isIE && ! Ext.isIE7 ) {
		Ext.DomHelper.append( 'diffExpression-form', {
			tag: 'p',
			cls: 'trouble',
			html: 'This page displays improperly in older versions of Internet Explorer.  Please upgrade to Internet Explorer 7.'
		} );
	}
	
	var searchPanel = new Ext.Gemma.DiffExpressionSearchForm( {
	} );
	searchPanel.render( "diffExpression-form" );
	
	var summaryPanel;
	
	var knownGeneDatasetGrid = new Ext.Gemma.DiffExpressionDatasetGrid( {
		renderTo : "diffExpression-results"
	} );
	var knownGeneGrid = new Ext.Gemma.DiffExpressionGrid( {
		renderTo : "diffExpression-results",
		title : "Differentially expressed genes",
		pageSize : 25
	} );
	
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
		summaryPanel = new Ext.Gemma.DiffExpressionSummaryGrid( {
			genes : result.queryGenes,
			summary : result.summary
		} );
		summaryPanel.render( "diffExpression-summary" );
		summaryPanel.autoSizeColumns();
		summaryPanel.getView().refresh();
		
		// create expression experiment image map
		var imageMap = Ext.get( "eeMap" );
		if ( ! imageMap ) {
			imageMap = Ext.getBody().createChild( {
				tag: 'map',
				id: 'eeMap',
				name: 'eeMap'
			} );
		}
		Ext.Gemma.DiffExpressionGrid.getBitImageMapTemplate().overwrite( imageMap, result.datasets );
		
		var link = panel.getBookmarkableLink();
		knownGeneGrid.setTitle( String.format( "Differentially Expressed genes <a href='{0}'>(bookmarkable link)</a> <a href='{0}&export'>(export as text)</a>", panel.getBookmarkableLink() ) );
		
		Ext.Gemma.DiffExpressionDatasetGrid.updateDatasetInfo( result.knownGeneDatasets, eeMap );
		knownGeneDatasetGrid.loadData( result.isCannedAnalysis, result.queryGenes.length, result.knownGeneDatasets ) ;
		knownGeneGrid.loadData( result.isCannedAnalysis, result.queryGenes.length, result.knownGeneResults, result.knownGeneDatasets );
		
		if ( admin ) {
			Ext.Gemma.DiffExpressionDatasetGrid.updateDatasetInfo( result.predictedGeneDatasets, eeMap );
			predictedGeneDatasetGrid.loadData( result.isCannedAnalysis, result.queryGenes.length, result.predictedGeneDatasets ) ;
			predictedGeneGrid.loadData( result.isCannedAnalysis, result.queryGenes.length, result.predictedGeneResults, result.predictedGeneDatasets );
			Ext.Gemma.DiffExpressionDatasetGrid.updateDatasetInfo( result.probeAlignedRegionDatasets, eeMap );
			probeAlignedDatasetGrid.loadData( result.isCannedAnalysis, result.queryGenes.length, result.probeAlignedRegionDatasets ) ;
			probeAlignedGrid.loadData( result.isCannedAnalysis, result.queryGenes.length, result.probeAlignedRegionResults, result.probeAlignedRegionDatasets );
		}
		
	} );
	
} );

/* instance methods...
 */
Ext.extend( Ext.Gemma.DiffExpressionSummaryGrid, Ext.Gemma.GemmaGridPanel, {
} );