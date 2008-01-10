Ext.onReady( function() {

	var bmId = dwr.util.getValue("bmId");
	
	/* here we reference the grid defined in BioMaterialGrid.js.
	 */
	var toolbar = new Ext.Gemma.AnnotationToolBar( Ext.Gemma.BioMaterialGrid.grid, {
		createHandler : function( characteristic, callback ) {
			OntologyService.saveBioMaterialStatement( characteristic, [bmId], callback );
		},
		deleteHandler : function( ids, callback ) {
			OntologyService.removeBioMaterialStatement( ids, [bmId], callback );
		},
		mgedTermKey : "factor"
	} );

} );