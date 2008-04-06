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
	
	searchPanel.on( "aftersearch", function ( panel, result ){

		var diffExGrid = new Ext.Gemma.DifferentialExpressionGrid( {
    			geneId : result.data.foundGene.id,
    			threshold : 0.01,
    			renderTo : diffExGrid,
    			pageSize : 10,
    			width : 800
    		} );
    		
    		diffExGrid.render("diffExpression-results");
	});
		

	
} );