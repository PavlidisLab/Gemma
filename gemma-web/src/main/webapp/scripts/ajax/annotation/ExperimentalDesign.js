Ext.onReady( function() {
	
	Ext.QuickTips.init();

	var eeId = dwr.util.getValue("expressionExperimentID");
	var edId = dwr.util.getValue("experimentalDesignID");
	var admin = dwr.util.getValue("hasAdmin");
	var editable = admin ? true : false;

	/* TODO make sure we've got an edId and display a prominent error message if we don't...
	 */

	var experimentalFactorGrid = new Ext.Gemma.ExperimentalFactorGrid( {
		title : "Experimental Factors",
		renderTo : "experimentalFactorPanel",
		edId : edId,
		editable : editable
	} );
	experimentalFactorGrid.render();
	experimentalFactorGrid.on( "experimentalfactorchange", function( efgrid, efs ) {
		factorValueGrid.reloadExperimentalFactors();
		bioMaterialEditor.init();
	} );
	
	var factorValueGrid = new Ext.Gemma.FactorValueGrid( {
		title : "Factor Values",
		renderTo : "factorValuePanel",
		form : "factorValueForm",
		edId : edId,
		editable : editable
	} );
	factorValueGrid.render();
	factorValueGrid.on( "factorvaluecreate", function( fvgrid, fvs ) {
		bioMaterialEditor.grid.reloadFactorValues();
	} );
	factorValueGrid.on( "factorvaluechange", function( fvgrid, fvs ) {
		bioMaterialEditor.grid.reloadFactorValues();
	} );
	factorValueGrid.on( "factorvaluedelete", function( fvgrid, fvs ) {
		bioMaterialEditor.grid.reloadFactorValues();
	} );
	
	var bioMaterialEditor = new Ext.Gemma.BioMaterialEditor( {
		title : "BioMaterials",
		renderTo : "bioMaterialsPanel",
		eeId : eeId,
		editable : editable
	} );
	bioMaterialEditor.init();

/* tabPanel doesn't work with the grids in 2.0; try it in the next version...
	var tabPanel = new Ext.TabPanel( {
		renderTo : "tabPanel",
		activeTab : 0,
		autoHeight : true
		autoWidth : true,
		autoScroll : true,
//		deferredRender : false,
//		layoutOnTabChange : true,
		items : [
			{ title: "Experimental Factors", contentEl: "experimentalFactorPanel" },
			{ title: "Factor Values", contentEl: "factorValuePanel" }
		]
	} );
	factorValueGrid.getStore().on( "load", function() {
		tabPanel.doLayout()
	} );
*/
	
} );