/**
 * Experimental design editor application.
 */
Ext.onReady(function() {

	Ext.QuickTips.init();

	var eeId = dwr.util.getValue("expressionExperimentID");
	var edId = dwr.util.getValue("experimentalDesignID");
	var admin = dwr.util.getValue("hasAdmin");
	var editable = admin ? true : false;

	/*
	 * TODO make sure we've got an edId and display a prominent error message if
	 * we don't...
	 */

	var bioMaterialEditor = new Gemma.BioMaterialEditor({
		title : "BioMaterials",
		renderTo : "bioMaterialsPanel",
		eeId : eeId,
		edId : edId,
		editable : editable
	});
	bioMaterialEditor.init();

//	var experimentalFactorGrid = new Gemma.ExperimentalFactorGrid({
//		title : "Experimental Factors",
//		renderTo : "experimentalFactorPanel",
//		edId : edId,
//		editable : editable
//	});
//	experimentalFactorGrid.on("experimentalfactorchange",
//			function(efgrid, efs) {
//				factorValueGrid.reloadExperimentalFactors();
//				bioMaterialEditor.init();
//			});
//
//	var factorValueGrid = new Gemma.FactorValueGrid({
//		title : "Factor Values",
//		renderTo : "factorValuePanel",
//		form : "factorValueForm",
//		edId : edId,
//		editable : editable
//	});
//
//	factorValueGrid.on("factorvaluecreate", function(fvgrid, fvs) {
//		bioMaterialEditor.grid.reloadFactorValues();
//	});
//	factorValueGrid.on("factorvaluechange", function(fvgrid, fvs) {
//		bioMaterialEditor.grid.reloadFactorValues();
//	});
//	factorValueGrid.on("factorvaluedelete", function(fvgrid, fvs) {
//		bioMaterialEditor.grid.reloadFactorValues();
//	});

});