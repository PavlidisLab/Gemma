Ext.namespace('Gemma');

/**
 * 
 * @class Gemma.DatasetSearchToolBar
 * @extends Ext.Toolbar
 */
Gemma.DatasetSearchToolBar = Ext.extend(Ext.Toolbar, {

	taxonSearch : true,

	initComponent : function() {
		Gemma.DatasetSearchToolBar.superclass.initComponent.call(this);
		this.addEvents("after.tbsearch");
	},

	setTaxon : function(taxon) {
		this.taxonCombo.setValue(taxon);
	},

	filterTaxon : function(taxon) {
		this.taxonCombo.filter(taxon);
	},

	afterRender : function() {
		Gemma.DatasetSearchToolBar.superclass.afterRender.call(this);

		if (this.taxonSearch) {
			this.taxonCombo = new Gemma.TaxonCombo({
				emptyText : 'Select a taxon',
				width : 125,
				listeners : {
					'select' : {
						fn : function(combo, record, index) {
							var taxon = record.data;
							this.eeSearchField.taxonChanged(taxon, false); // false:
							// don't
							// search for EE
							// sets right
							// away.
						}.createDelegate(this, [], true)
					},
					'ready' : {
						fn : function(taxon) {
							this.eeSearchField.taxonChanged(taxon, false); // false:
							// don't
							// search for EE
							// sets right
							// away.
						}.createDelegate(this, [], true)
					}
				}
			});

			this.add(this.taxonCombo);
			this.addSpacer();
		}

		this.eeSearchField = new Gemma.DatasetSearchField({
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
 * @class Gemma.DataSetSearchAndGrabToolbar
 * @extends Gemma.DatasetSearchToolBar
 */
Gemma.DataSetSearchAndGrabToolbar = Ext.extend(Gemma.DatasetSearchToolBar, {

	initComponent : function() {
		Gemma.DataSetSearchAndGrabToolbar.superclass.initComponent.call(this);
		this.addEvents("grabbed");

	},

	afterRender : function() {
		Gemma.DataSetSearchAndGrabToolbar.superclass.afterRender.call(this);
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
					this.fireEvent("grabbed", sels);
				}
			},
			scope : this
		});

		this.addFill();
		this.add(grabber);
	}

});
