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

	uploadForm.on('start', function(taskId) {
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
					html : '<p>Experimental design submission works in two phases. ' +
							'First you must upload your design file (file format instructions' +
							' <a target="_blank" href="' +
							Gemma.HOST +
							'faculty/pavlidis/wiki/display/gemma/Experimental+Design+Upload">here</a>). ' +
							'Then click "submit". If your file format is invalid or does not match the properties of the ' +
							'experiment the design is intended for, you will see an error message.</p>'
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

				}
			});
};

Ext.onReady(function() {

			Ext.QuickTips.init();
			Ext.state.Manager.setProvider(new Ext.state.CookieProvider());

			var eeId = Ext.get("expressionExperimentID").getValue();
			var edId = Ext.get("experimentalDesignID").getValue();
			var editable = Ext.get('currentUserCanEdit').getValue() === 'true';

			/*
			 * TODO: load up the MGED terms, experimental design and factor values ahead of time. We end up doing it
			 * about 4 times each. Create the panel in the callback.
			 */

			/*
			 * If we init before the tab is rendered, then the scroll bars don't show up.
			 */
			var experimentalFactorGrid = new Gemma.ExperimentalFactorGrid({
						id : 'experimental-factor-grid',
						title : "Experimental Factors",
						region : 'north',
						edId : edId,
						editable : editable,
						split : true,
						// north items must have height.
						height : 200
					});

			var factorValueGrid = new Gemma.FactorValueGrid({
						title : "Factor Values",
						region : 'center',
						id : 'factor-value-grid',
						form : 'factorValueForm', // hack
						split : true,
						edId : edId,
						height : 470,
						editable : editable
					});

			var efPanel = new Ext.Panel({
						layout : 'border',
						height : 670,
						renderTo : "experimentalFactorPanel",
						items : [experimentalFactorGrid, factorValueGrid]
					});

			var bioMaterialEditor = new Gemma.BioMaterialEditor({
						renderTo : "bioMaterialsPanel",
						id : 'biomaterial-grid-panel',
						height : 670,
						eeId : eeId,
						edId : edId,
						viewConfig : {
							forceFit : true
						},
						editable : editable
					});

			var tabPanel = new Ext.TabPanel({
						renderTo : "experimentalDesignPanel",
						layoutOnTabChange : false,
						activeTab : 0,
						height : 700,
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
			var refreshNeeded = false;

			tabPanel.on('tabchange', function(panel, tab) {
						if (refreshNeeded || !bioMaterialEditor.firstInitDone && tab.contentEl == 'bioMaterialsPanel') {
							bioMaterialEditor.init();
							refreshNeeded = false;
						}
					});

			experimentalFactorGrid.on("experimentalfactorchange", function(efgrid, efs, factor) {
						factorValueGrid.getEl().unmask();
						if(factor.get("name") && factor.get("id")){
							factorValueGrid.setTitle("Factor values for : " + factor.get("name"));
							factorValueGrid.setExperimentalFactor(factor.get("id"));
						}
						refreshNeeded = true;
					});

			experimentalFactorGrid.on("experimentalfactorselected", function(factor) {

						if (factor.get("type") == "continuous") {
							factorValueGrid.getStore().removeAll();
							factorValueGrid
									.setTitle("Continuous values not displayed here, see the 'sample details' tab");
							factorValueGrid.getEl()
									.mask("Continuous values not displayed here, see the 'sample details' tab");

						} else {
							factorValueGrid.getEl().unmask();
							factorValueGrid.setTitle("Factor values for : " + factor.get("name"));
							factorValueGrid.setExperimentalFactor(factor.get("id"));

						}
					});

			factorValueGrid.on("factorvaluecreate", function(fvgrid, fvs) {
						refreshNeeded = true;
					});

			factorValueGrid.on("factorvaluechange", function(fvgrid, fvs) {
						refreshNeeded = true;
					});

			factorValueGrid.on("factorvaluedelete", function(fvgrid, fvs) {
						refreshNeeded = true;
					});

		});