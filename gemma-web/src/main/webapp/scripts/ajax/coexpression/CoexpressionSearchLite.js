Ext.onReady( function() {
	Ext.QuickTips.init();
	Ext.state.Manager.setProvider( new Ext.state.CookieProvider() );
	
	var searchForm = new Ext.Gemma.CoexpressionSearchFormLite( {
	} );
	searchForm.render( "coexpression-form" );
} );