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
								isDisplayTaxonWithDatasets : true,
								width : 125,
								listeners : {
									'select' : {
										fn : function(combo, record, index) {
											var taxon = record.data;
											this.eeSearchField.taxonChanged(taxon);
										}.createDelegate(this, [], true)
									},
									'ready' : {
										fn : function(taxon) {
											this.eeSearchField.taxonChanged(taxon);
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
								'beforesearch' : {
									fn : function() {
										this.grid.setTitle("Dataset locator");
									}.createDelegate(this)
								},
								'aftersearch' : {
									fn : function(field, results) {
										this.fireEvent('after.tbsearch', results);
										if (this.grid) {
											this.grid.setTitle("Dataset locator - " + results.length + " found");
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

			addSelections : function(sels) {
				if (sels.length > 0) {
					this.targetGrid.getStore().insert(0, sels);
				}
			},

			afterRender : function() {
				Gemma.DataSetSearchAndGrabToolbar.superclass.afterRender.call(this);
				var grabber = new Ext.Button({
							id : 'grabber',
							disabled : false,
							text : "Grab >>",
							tooltip : "Transfer selected items to the set",
							handler : function(button, ev) {
								var selmo = this.grid.getSelectionModel();
								var sels = selmo.getSelections();
								this.addSelections(sels);
							}.createDelegate(this),
							scope : this
						});

				var allGrabber = new Ext.Button({
							id : 'all-grabber',
							disabled : false,
							text : "Grab All",
							tooltip : "Transfer all the results to the set",
							handler : function(button, ev) {
								var sels = this.grid.getStore().getRange();
								this.addSelections(sels);
							},
							scope : this
						});
				this.addSpacer();
				this.addSpacer();
				this.addSeparator();
				this.addSpacer();
				this.addSpacer();
				this.add(allGrabber);
				this.addSeparator();
				this.add(grabber);

				this.targetGrid.getStore().on("add", function(store, recs, index) {
							this.targetGrid.getView().refresh();
							this.fireEvent("grabbed", recs);
							if (this.targetGrid.getBottomToolBar && this.targetGrid.getBottomToolBar.changePage) {
								this.targetGrid.getBottomToolBar().changePage(1);
							}
						}.createDelegate(this));
			}

		});
