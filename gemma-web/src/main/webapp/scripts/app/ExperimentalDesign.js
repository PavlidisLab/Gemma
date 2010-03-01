/**
 * Experimental design editor application.
 */

Ext.BLANK_IMAGE_URL = '/Gemma/images/default/s.gif';

var serverFilePath = "";

var showDesignUploadForm = function() {

	var uploadForm = new Gemma.FileUploadForm({
				title : 'Select the design file',
				id : 'upload-design-form',
				style : 'margin : 5px',
				allowBlank : false
			});

	uploadForm.on('start', function(result) {
				Ext.getCmp('submit-design-button').disable();
			}.createDelegate(this));

	uploadForm.on('finish', function(result) {
				if (result.success) {
					Ext.getCmp('submit-design-button').enable();
					serverFilePath = result.localFile;
				}
			});

	var w = new Ext.Window({
		title : "Experimental design upload",
		closeAction : 'close',
		id : 'experimental-design-upload-form-window',
		width : 550,
		items : [{
			xtype : 'panel',
			collapsible : true,
			title : 'Instructions',
			collapsed : false,
			frame : false,
			border : true,
			html : '<p>Experimental design submission works in two phases. '
					+ 'First you must upload your design file (file format instructions'
					+ ' <a target="_blank" href="http://www.chibi.ubc.ca/faculty/pavlidis/wiki/display/gemma/Experimental+Design+Upload">here</a>). '
					+ 'Then click "submit". If your file format is invalid or does not match the properties of the '
					+ 'experiment the design is intended for, you will see an error message.</p>'
		}, uploadForm],
		buttons : [{
					id : 'submit-design-button',
					value : 'Submit',
					handler : submitDesign,
					text : "Submit dataset",
					disabled : true
				}, {
					value : 'Cancel',
					text : 'Cancel',
					enabled : true,
					handler : function() {
						w.close();
					}
				}]
	});

	w.show();
};

var submitDesign = function() {
	ExperimentalDesignController.createDesignFromFile(dwr.util.getValue("expressionExperimentID"), serverFilePath, {
				callback : function() {
					Ext.getCmp('experimental-design-upload-form-window').close();
					Ext.Msg.alert("Success", "Design imported.");
					Ext.getCmp('experimental-factor-grid').getStore().reload();
					// Ext.getCmp('factor-value-grid').getStore().reload(); // should have started out empty anyway.
					// biomaterial-grid-panel was not available to initialise gets initialised on tabchange
					// Ext.getCmp('biomaterial-grid-panel').init();

				}
			});
};

Ext.onReady(function() {

			Ext.QuickTips.init();
			Ext.state.Manager.setProvider(new Ext.state.CookieProvider());

			var eeId = dwr.util.getValue("expressionExperimentID");
			var edId = dwr.util.getValue("experimentalDesignID");
			var admin = dwr.util.getValue("hasAdmin");
			var editable = admin ? true : false;

			/*
			 * TODO: load up the MGED terms, experimental design and factor values ahead of time. We end up doing it
			 * about 4 times each. Create the panel in the callback.
			 */

			/*
			 * TODO make sure we've got an eeId and display a prominent error message if we don't...
			 */

			var bioMaterialEditor = new Gemma.BioMaterialEditor({
						renderTo : "bioMaterialsPanel",
						id : 'biomaterial-grid-panel',
						eeId : eeId,
						edId : edId,
						viewConfig : {
			// forceFit : true
						},
						height : 740,
						width : 1000,
						editable : editable
					});

			/*
			 * If we init before the tab is rendered, then the scroll bars don't show up.
			 */
			// bioMaterialEditor.init();
			var experimentalFactorGrid = new Gemma.ExperimentalFactorGrid({
						id : 'experimental-factor-grid',
						title : "Experimental Factors",
						region : 'center',
						edId : edId,
						editable : editable,
						height : 200
					});

			var factorValueGrid = new Gemma.FactorValueGrid({
						title : "Factor Values",
						region : 'south',
						id : 'factor-value-grid',
						form : 'factorValueForm', // hack
						split : true,
						edId : edId,
						editable : editable,
						width : 1000,
						height : 500
					});

			var efPanel = new Ext.Panel({
						layout : 'border',
						height : 750,
						width : 1000,
						renderTo : "experimentalFactorPanel",
						items : [experimentalFactorGrid, factorValueGrid]
					});

			var tabPanel = new Ext.TabPanel({
						renderTo : "experimentalDesignPanel",
						height : 800,
						width : 1000,
						defaults : {
							layout : 'fit'
						},
						layoutOnTabChange : false,
						activeTab : 0,
						items : [{
									contentEl : "experimentalFactorPanel",
									title : "Design setup"
								}, {
									contentEl : "bioMaterialsPanel",
									title : "Sample details"
								}]
					});

			/*
			 * Only initialize once we are viewing the tab to help ensure the scroll bars are rendered right away.
			 */
			tabPanel.on('tabchange', function(panel, tab) {

						if (!bioMaterialEditor.firstInitDone && tab.contentEl == 'bioMaterialsPanel') {
							bioMaterialEditor.init();
						}
					});

			experimentalFactorGrid.on("experimentalfactorchange", function(efgrid, efs) {
						factorValueGrid.enable();
						factorValueGrid.setTitle("Factor values");
						factorValueGrid.setExperimentalFactor(null);

					});

			experimentalFactorGrid.on("experimentalfactorselected", function(factor) {

						if (factor.get("type") == "Continuous") {
							factorValueGrid
									.setTitle("Continuous values not displayed here, see the 'sample details' tab");

							factorValueGrid.disable();
						} else {
							factorValueGrid.enable();
							factorValueGrid.setTitle("Factor values for : " + factor.get("name"));
							factorValueGrid.setExperimentalFactor(factor.get("id"));
						}
					});

			factorValueGrid.on("factorvaluecreate", function(fvgrid, fvs) {
						if (bioMaterialEditor.grid != null)
							bioMaterialEditor.grid.reloadFactorValues();
					});

			factorValueGrid.on("factorvaluechange", function(fvgrid, fvs) {
						if (bioMaterialEditor.grid != null)
							bioMaterialEditor.grid.reloadFactorValues();
					});

			factorValueGrid.on("factorvaluedelete", function(fvgrid, fvs) {
						if (bioMaterialEditor.grid != null)
							bioMaterialEditor.grid.reloadFactorValues();
					});

		});