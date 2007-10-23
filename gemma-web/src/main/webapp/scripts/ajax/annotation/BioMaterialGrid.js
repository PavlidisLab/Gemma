Ext.namespace('Ext.Gemma.BioMaterialGrid');

Ext.onReady( function() {

	var bmId = dwr.util.getValue("bmId");
	var bmClass = dwr.util.getValue("bmClass");

	Ext.Gemma.BioMaterialGrid.grid = new Ext.Gemma.AnnotationGrid( "bmAnnotations", {
		readMethod : BioMaterialController.getAnnotation,
		readParams : [ { id:bmId, classDelegatingFor:bmClass } ]
	} );
	Ext.Gemma.BioMaterialGrid.grid.render();
	
} );