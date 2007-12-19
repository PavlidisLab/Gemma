Ext.onReady( function() {

	var eeId = dwr.util.getValue("eeId");

	/* here we reference the grid defined in ExpressionExperimentGrid.js.
	 */
	var toolbar = new Ext.Gemma.AnnotationToolBar( Ext.Gemma.ExpressionExperimentGrid.grid, {
		createHandler : function( characteristic, callback ) {
			OntologyService.saveExpressionExperimentStatement( characteristic, [eeId], callback );
		},
		deleteHandler : function( ids, callback ) {
			OntologyService.removeExpressionExperimentStatement( ids, [eeId], callback );
		}
	} );

} );