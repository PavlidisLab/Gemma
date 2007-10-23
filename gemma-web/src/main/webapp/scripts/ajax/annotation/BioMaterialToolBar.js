Ext.onReady( function() {

	var bmId = dwr.util.getValue("bmId");
	
	var saveHandler = function( characteristic, callback ) {
		OntologyService.saveBioMaterialStatement( characteristic, [bmId], callback );
	}
	
	var deleteHandler = function( ids, callback ) {
		OntologyService.removeBioMaterialStatement( ids, [bmId], callback );
	}

	/* here we reference the grid defined in BioMaterialGrid.js.
	 */
	var toolbar = new Ext.Gemma.AnnotationToolBar( "bmAnnotator",
		Ext.Gemma.BioMaterialGrid.grid, saveHandler, deleteHandler );

} );