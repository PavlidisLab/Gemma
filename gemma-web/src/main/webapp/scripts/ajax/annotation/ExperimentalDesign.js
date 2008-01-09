Ext.onReady( function() {
	
	Ext.QuickTips.init();

	var eeId = dwr.util.getValue("expressionExperimentID");
	var edId = dwr.util.getValue("experimentalDesignID");
	var admin = dwr.util.getValue("hasAdmin");

	/* TODO make sure we've got an edId and display a prominent error message if we don't...
	 */

/*
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
*/

	var experimentalFactorGrid = new Ext.Gemma.ExperimentalFactorGrid( {
		title : "Experimental Factors",
		renderTo : "experimentalFactorPanel",
		edId : edId
	} );
	experimentalFactorGrid.render();
	
	var factorValueGrid = new Ext.Gemma.FactorValueGrid( {
		title : "Factor Values",
		renderTo : "factorValuePanel",
		form : "factorValueForm",
		edId : edId
	} );
	factorValueGrid.render();
/*
	factorValueGrid.getStore().on( "load", function() {
		tabPanel.doLayout()
	} );
*/
	
	var bioMaterialEditor = new Ext.Gemma.BioMaterialEditor( {
		title : "BioMaterials",
		renderTo : "bioMaterialsPanel",
		eeId : eeId
	} );
	bioMaterialEditor.init();
	
	experimentalFactorGrid.onRefresh = function() {
		factorValueGrid.reloadExperimentalFactors();
		bioMaterialEditor.init();
	};
	
	factorValueGrid.onRefresh = function() {
		bioMaterialEditor.grid.reloadFactorValues();
	};
	
} );