Ext.onReady( function() {

	var eeId = dwr.util.getValue("eeId");

	var saveHandler = function( characteristic, callback ) {
		OntologyService.saveExpressionExperimentStatement( characteristic, [eeId], callback );
	}
	
	var deleteHandler = function( ids, callback ) {
		OntologyService.removeExpressionExperimentStatement( ids, [eeId], callback );
	}

	/* here we reference the grid defined in ExpressionExperimentGrid.js.
	 */
	var toolbar = new Ext.Gemma.AnnotationToolBar( "eeAnnotator",
		Ext.Gemma.ExpressionExperimentGrid.grid, saveHandler, deleteHandler );

} );