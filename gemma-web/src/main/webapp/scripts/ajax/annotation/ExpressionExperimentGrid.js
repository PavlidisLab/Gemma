Ext.namespace('Ext.Gemma.ExpressionExperimentGrid');

Ext.onReady( function() {

	var eeId = dwr.util.getValue("eeId");
	var eeClass = dwr.util.getValue("eeClass");

	Ext.Gemma.ExpressionExperimentGrid.grid = new Ext.Gemma.AnnotationGrid( "eeAnnotations", {
		readMethod : ExpressionExperimentController.getAnnotation,
		readParams : [ { id:eeId, classDelegatingFor:eeClass } ]
	} );
	Ext.Gemma.ExpressionExperimentGrid.grid.render();
	
} );