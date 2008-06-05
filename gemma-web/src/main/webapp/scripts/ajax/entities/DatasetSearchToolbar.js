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

	setTaxon : function(taxon) {
		this.taxonCombo.setValue(taxon);
	},

	afterRender : function() {
		Ext.Gemma.DatasetSearchToolBar.superclass.afterRender.call(this);

		if (this.taxonSearch) {
			this.taxonCombo = new Ext.Gemma.TaxonCombo({
				emptyText : 'Select a taxon',
				width : 125,
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
						this.fireEvent('after.tbsearch', results);
						if (this.grid) {
							this.grid.getStore().load({
								params : [results]
							});
						}
					}.createDelegate(this)
				}
			}
		});

		this.addField(this.eeSearchField);

	},

	updateDatasets : function() {
		if (this.eeSearchField.filtering) {
			this.eeSearchField.setFilterFrom(this.container.getEEIds());
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

			initComponent : function() {
				Ext.Gemma.DataSetSearchAndGrabToolbar.superclass.initComponent
						.call(this);
				this.addEvents("grabbed");

			},

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
							Ext.log("Grabbed " + sels.length);
							this.targetGrid.getStore().add(sels);
							this.targetGrid.getView().refresh();
							this.fireEvent("grabbed", sels);
						}
					},
					scope : this
				});

				this.add(grabber);
			}

		});
