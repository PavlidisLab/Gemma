/**
 * Experimental design editor application.
 */

Ext.BLANK_IMAGE_URL = '/Gemma/images/default/s.gif';
Ext.onReady(function() {

	Ext.QuickTips.init();
	Ext.state.Manager.setProvider(new Ext.state.CookieProvider());

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
		height : 700,
		width : 1000,
		editable : editable
	});
	bioMaterialEditor.init();

	var experimentalFactorGrid = new Gemma.ExperimentalFactorGrid({
		title : "Experimental Factors",
		region : 'center',
		edId : edId,
		editable : editable,
		height : 200
	});

	var factorValueGrid = new Gemma.FactorValueGrid({
		title : "Factor Values",
		region : 'south',
		form : 'factorValueForm', // hack
		split : true,
		edId : edId,
		editable : editable,
		width : 1000,
		height : 500
	});

	var efPanel = new Ext.Panel({
		layout : 'border',
		height : 700,
		width : 1000,
		renderTo : "experimentalFactorPanel",
		items : [experimentalFactorGrid, factorValueGrid]
	})

	var tabPanel = new Ext.TabPanel({
		renderTo : "experimentalDesignPanel",
		height : 700,
		width : 1000,
		defaults : {
			layout : 'fit'
		},
		layoutOnTabChange : true,
		activeTab : 0,
		items : [{
			contentEl : "experimentalFactorPanel",
			title : "Experimental factors & values"
		}, {
			contentEl : "bioMaterialsPanel",
			title : "Biomaterials"
		}]
	});

	experimentalFactorGrid.on("experimentalfactorchange",
			function(efgrid, efs) {
				bioMaterialEditor.init(); // refresh available factors.
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