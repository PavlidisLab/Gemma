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
				this.eeSearchField.setTaxon(taxon);
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
								width : 110,
								listeners : {
									'select' : {
										fn : function(combo, record, index) {
											var taxon = record.data;
											this.eeSearchField.setTaxon(taxon);
										},
										scope : this
									},
									'ready' : {
										fn : function(taxon) {
											this.eeSearchField.setTaxon(taxon);
										},
										scope : this
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
										if (this.grid) {
											this.grid.setTitle("Dataset locator");
										}
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

					/*
					 * This doesn't handle the case where the currently selected EESet taxon doesn't match this one -
					 * we're just matching on what is already in the targetGrid.
					 */
//					if (this.targetGrid.getStore().getCount() > 0) {
//						var taxonId = this.targetGrid.getStore().getAt(0).get('taxonId');
//
//						for (var i = 0; i < sels.length; i++) {
//							if (taxonId != sels[i].get('taxonId')) {
//								Ext.Msg
//										.alert("Invalid",
//												"You cannot mix data sets from different species in one group");
//								return;
//							}
//						}
//					}

					this.targetGrid.getStore().insert(0, sels);
					this.grid.getStore().remove(sels);
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
						}.createDelegate(this));
			}

		});
