Ext.namespace('Ext.Gemma.ExpressionExperimentAnnotGrid');

Ext.onReady(function() {
	var eeId = dwr.util.getValue("eeId");
	var eeClass = dwr.util.getValue("eeClass");
	var admin = dwr.util.getValue("hasAdmin");
	
	Ext.Gemma.ExpressionExperimentAnnotGrid.grid = new Ext.Gemma.AnnotationGrid("eeAnnotations", {
		readMethod : ExpressionExperimentController.getAnnotation,
		readParams : [{id:eeId, classDelegatingFor:eeClass}],
		editable : admin,
		mgedTermKey : "experiment"
	});
	Ext.Gemma.ExpressionExperimentAnnotGrid.grid.render();
});