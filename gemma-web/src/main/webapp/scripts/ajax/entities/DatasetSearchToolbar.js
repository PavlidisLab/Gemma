Ext.namespace('Ext.Gemma');

/**
 * 
 * @class Ext.Gemma.DatasetSearchToolBar
 * @extends Ext.Toolbar
 */
Ext.Gemma.DatasetSearchToolBar = Ext.extend(Ext.Toolbar, {

	taxonSearch : true,

	initComponent : function() {
		Ext.Gemma.DatasetSearchToolBar.superclass.initComponent.call(this);
		this.addEvents("after.tbsearch");
	},

	afterRender : function() {
		Ext.Gemma.DatasetSearchToolBar.superclass.afterRender.call(this);

		if (this.taxonSearch) {
			this.taxonCombo = new Ext.Gemma.TaxonCombo({
				emptyText : 'select a taxon',
				width : 150,
				listeners : {
					'taxonchanged' : {
						fn : function(taxon) {
							this.eeSearchField.taxonChanged(taxon, false); // false:
							// don't
							// search for EE
							// sets right
							// away.
						}.createDelegate(this)
					}
				}
			});

			this.add(this.taxonCombo);
			this.addSpacer();
		}

		this.eeSearchField = new Ext.Gemma.DatasetSearchField({
			fieldLabel : "Experiment keywords",
			filtering : this.filtering,
			listeners : {
				'aftersearch' : {
					fn : function(field, results) {
						this.resetButton.enable();
						this.fireEvent('after.tbsearch', results);
					}.createDelegate(this)
				}
			}
		});

		this.resetButton = new Ext.Button({
			id : 'reset',
			text : "Reset",
			handler : this.reset,
			scope : this,
			disabled : true,
			tooltip : "Restore the display list of datasets for the analysis"
		});

		this.addField(this.eeSearchField);

		this.addFill();
		this.addField(this.resetButton);

	},

	updateDatasets : function() {
		if (this.eeSearchField.filtering) {
			this.eeSearchField.setFilterFrom(this.container.getEEIds());
		}
	},

	reset : function() {
		this.eeSearchField.reset();
		if (this.owningGrid.analysisId) {
			var callback = function(d) {
				// load the data sets.
				this.getStore().load({
					params : [d]
				});
			};
			// Go back to the server to get the ids of the experiments the
			// selected analysis' parent has.
			GeneLinkAnalysisManagerController.getExperimentIdsInAnalysis(
					this.owningGrid.analysisId, {
						callback : callback.createDelegate(this.owningGrid, [],
								true)
					});

		}
	}

});

/**
 * Adds a 'grab' button that can send records to another grid.
 * 
 * @class Ext.Gemma.DataSetSearchAndGrabToolbar
 * @extends Ext.Gemma.DatasetSearchToolBar
 */
Ext.Gemma.DataSetSearchAndGrabToolbar = Ext.extend(
		Ext.Gemma.DatasetSearchToolBar, {
			afterRender : function() {
				Ext.Gemma.DataSetSearchAndGrabToolbar.superclass.afterRender
						.call(this);
				var grabber = new Ext.Button({
					id : 'grabber',
					disabled : false,
					text : "Grab >>",
					handler : function(button, ev) {
						var selmo = this.grid.getSelectionModel();
						var sels = selmo.getSelections();
						if (sels.length > 0) {
							this.targetGrid.getStore().add(sels);
							this.targetGrid.getView().refresh();
						}
					},
					scope : this
				});

				this.add(grabber);
				// grid.store.on("load", function() {
				// Ext.getCmp('grab').enable();
				// }, this);

			}

		});
