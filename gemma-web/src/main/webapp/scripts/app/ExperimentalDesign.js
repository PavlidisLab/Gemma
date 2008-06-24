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
	 * TODO: load up the MGED terms, experimental design and factor values ahead
	 * of time. We end up doing it about 4 times each. Create the panel in the
	 * callback.
	 */

	/*
	 * TODO make sure we've got an edId and display a prominent error message if
	 * we don't...
	 */

	var bioMaterialEditor = new Gemma.BioMaterialEditor({
		renderTo : "bioMaterialsPanel",
		eeId : eeId,
		edId : edId,
		editable : editable,
		height : 700
	});

	var experimentalFactorGrid = new Gemma.ExperimentalFactorGrid({
		title : "Experimental Factors",
		region : 'center',
		edId : edId,
		editable : editable,
		height : 300
	});

	var factorValueGrid = new Gemma.FactorValueGrid({
		title : "Factor Values",
		region : 'south',
		split : true,
		// renderTo : "factorValuePanel",
		// form : "factorValueForm",
		edId : edId,
		editable : editable,
		height : 400
	});

	var efPanel = new Ext.Panel({
		layout : 'border',
		height : 700,
		renderTo : "experimentalFactorPanel",
		items : [experimentalFactorGrid, factorValueGrid]
	})

	var tabPanel = new Ext.TabPanel({
		renderTo : "experimentalDesignPanel",
		height : 700,
		width : 900,
		activeTab : 0,
		items : [{
			contentEl : "experimentalFactorPanel",
			title : "Experimental factors & values"
		}, {
			contentEl : "bioMaterialsPanel",
			title : "Biomaterials"
		}]
	});

	bioMaterialEditor.init();

	experimentalFactorGrid.on("experimentalfactorchange",
			function(efgrid, efs) {
				factorValueGrid.reloadExperimentalFactors();
				bioMaterialEditor.init();
			});

	experimentalFactorGrid.on("experimentalfactorselected", function(factor) {
		factorValueGrid.setTitle("Factor values for : " + factor.get("name"));
		factorValueGrid.setExperimentalFactor(factor.get("id"));
	});

	factorValueGrid.on("factorvaluecreate", function(fvgrid, fvs) {
		bioMaterialEditor.grid.reloadFactorValues();
	});

	factorValueGrid.on("factorvaluechange", function(fvgrid, fvs) {
		bioMaterialEditor.grid.reloadFactorValues();
	});

	factorValueGrid.on("factorvaluedelete", function(fvgrid, fvs) {
		bioMaterialEditor.grid.reloadFactorValues();
	});

});