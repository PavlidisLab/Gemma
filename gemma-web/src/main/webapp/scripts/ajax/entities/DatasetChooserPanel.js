/*
 * Panel for choosing datasets based on search criteria, or on established "ExpressionExperimentSets".
 * 
 * At the top, the available ExpressionExperimentSets are shown. At the bottom left, searching for experiments; at the
 * bottom right, the experiments that are in the set. If the user is an admin, they can save new
 * ExpressionExperimentSets to the database; otherwise they are saved for the user in a cookie.
 * 
 * @author Paul
 * 
 * @version $Id$
 */
Ext.namespace('Gemma');
Ext.namespace('Gemma.ExpressionExperimentSetStore');

/**
 * 
 * @class Gemma.ExpressionExperimentSetPanel
 * @extends Ext.Panel
 */
Gemma.ExpressionExperimentSetPanel = Ext.extend(Ext.Panel, {

			layout : 'table',
			layoutConfig : {
				columns : 2
			},
			defaults : {
				bodyStyle : 'padding:3px'
			},
			border : false,
			autoWidth : true,
			isAdmin : false,
			stateful : false,

			setState : function(state) {
				if (this.ready) {
					this.selectById(state);
				} else {
					this.storedState = state;
				}
			},
			
			setStateByName : function(state) {
				if (this.ready) {
					this.selectByName(state);
				} else {
					this.storedNameState = state;
				}
			},

			restoreState : function() {
				if (this.storedState) {
					this.selectById(this.storedState);
					delete this.storedState;
				} else if (this.storedNameState) {
					this.selectByName(this.storedNameState);
					delete this.storedNameState;
				}
				this.ready = true;
			},

			getSelected : function() {
				return this.store.getSelected();
			},

			selectById : function(id) {
				this.combo.setValue(id);
				var rec = this.store.getById(id);
				if (!rec) {
					this.store.selectedId = id;
				}
				this.store.setSelected(rec);
				this.fireEvent("set-chosen", rec);
			},

			selectByName : function(name) {
				var index = this.store.findBy(function(record, i) {
							return record.get("name") == name;
						});
				var rec = this.store.getAt(index);
				this.combo.setValue(rec.get("id"));
				this.store.setSelected(rec);
				this.fireEvent("set-chosen", rec);
			},

			filterByTaxon : function(taxon) {
				// side effect: grid is filtered too.
				if (taxon) {
					this.combo.filterByTaxon(taxon.id);
				}
			},

			initComponent : function() {

				Gemma.ExpressionExperimentSetPanel.superclass.initComponent.call(this);

				if (!this.store) {
					this.store = new Gemma.ExpressionExperimentSetStore();
				}

				this.dcp = new Gemma.DatasetChooserPanel({
							isAdmin : this.isAdmin,
							modal : true,
							eeSetStore : this.store
						});

				this.combo = new Gemma.ExpressionExperimentSetCombo({
							width : 175,
							store : this.store
						});

				this.combo.on("select", function(combo, sel) {
							this.fireEvent('set-chosen', sel);
						}.createDelegate(this));

				this.combo.on("ready", function(combo, sel) {
							// keeps ready event from propogating??
							// this.fireEvent('set-chosen', sel);
						}.createDelegate(this));

				this.dcp.on("datasets-selected", function(sel) {
							if (sel) {
								this.fireEvent('set-chosen', sel);
								this.combo.setValue(sel.get("name"));
							}
						}.createDelegate(this));

				this.dcp.on("delete-set", function(rec) {
							if (this.store.getPreviousSelection()) {
								this.combo.setValue(this.store.getPreviousSelection().get("name"))
							} else {
								this.combo.setValue("");
							}
						}.createDelegate(this));

				this.dcp.on("commit", function(sel) {
							if (sel) {
								this.combo.setValue(sel.get("name"));
							}
							this.fireEvent('commit', sel);
						}.createDelegate(this));

				this.store.on("load", this.restoreState.createDelegate(this));

				this.addEvents("set-chosen", "commit");
			},

			onRender : function(ct, position) {
				Gemma.ExpressionExperimentSetPanel.superclass.onRender.call(this, ct, position);

				this.add(this.combo);

				this.add(new Ext.Button({
							text : "Edit",
							anchor : '',
							tooltip : "View dataset chooser interface to modify or create sets",
							handler : function() {
								this.dcp.show({
											selected : this.getSelected()
										});
							},
							scope : this
						}));

			}

		});

/**
 * ComboBox to show ExpressionExperimentSets. Configure this with a ExpressionExperimentSetStore.
 * 
 * @class Gemma.ExpressionExperimentSetCombo
 * @extends Ext.form.ComboBox
 */
Gemma.ExpressionExperimentSetCombo = Ext.extend(Ext.form.ComboBox, {

	displayField : 'name',
	valueField : 'id',
	editable : false,
	loadingText : "Loading ...",
	listWidth : 250,
	forceSelection : true,
	mode : 'local',
	triggerAction : 'all',
	lazyInit : false, // important!
	emptyText : 'Select a search scope',
	isReady : false,
	stateful : true,
	stateId : "Gemma.EESetCombo",
	stateEvents : ['select'],
	suppressFiltering : false,

	/**
	 * Custom cookie config. This method will be called by the state manager.
	 * 
	 * @param {}
	 *            state
	 */
	applyState : function(state) {
		// console.log("apply state");
		if (state && state.eeSet) {
			this.setState(state.eeSet);
		}
		Gemma.ExpressionExperimentSetCombo.superclass.applyState.call(this, state);
	},

	getState : function() {
		if (this.store.getSelected()) {
			return ({
				eeSet : this.store.getSelected().get("id")
			});
		}
	},

	setState : function(v) {
		// console.log("Set state=" + v);
		if (this.isReady) {
			this.selectById(v);
		} else {
			this.tmpState = v;
		}
	},

	restoreState : function() {
		if (this.tmpState) {
			this.selectById(this.tmpState);
			delete this.tmpState;
			this.isReady = true;
		}
		// console.log("Restore state");
		if (this.store.getSelected()) {

			this.fireEvent('ready', this.store.getSelected().data);
		} else {
			this.fireEvent('ready');
		}

		this.store.sort('name');

	},

	filterByTaxon : function(taxonId) {
		if (this.suppressFiltering) {
			return;
		}

		this.doQueryBy(function(record, id) {
					if (!record.get("taxonId")) {
						return true; // in case there is none.
					} else if (taxonId == record.get("taxonId")) {
						return true;
					} else {
						return false;
					}
				});

		if (this.store.getSelected() && this.store.getSelected().get("taxonId") != taxonId) {
			this.setValue("");
		}
	},

	/**
	 * Override.
	 */
	onTriggerClick : function() {
		if (this.disabled) {
			return;
		}
		if (this.isExpanded()) {
			this.collapse();
			this.el.focus();
		} else {
			this.onFocus();
			this.expand();
			this.el.focus();
		}
	},

	doQueryBy : function(fn) {
		this.store.clearFilter();
		this.store.filterBy(fn);
	},

	/**
	 * Fires the select event!
	 * 
	 * @param {}
	 *            id
	 */
	selectById : function(id) {
		// console.log("Selecting id:" + id);
		var index = this.store.find("id", id);
		if (index >= 0) {
			var rec = this.store.getAt(index);
			this.store.setSelected(rec);
			this.setValue(rec.get("name"));
			this.fireEvent("select", this, rec, index);
		}
	},

	initComponent : function() {
		Gemma.ExpressionExperimentSetCombo.superclass.initComponent.call(this);
		this.tpl = new Ext.XTemplate('<tpl for="."><div ext:qtip="{description} ({numExperiments} members)" class="x-combo-list-item">{name}{[ values.taxon ? " (" + values.taxon.scientificName + ")" : "" ]}</div></tpl>');
		this.tpl.compile();

		this.addEvents("ready");

		this.on("select", function(cb, rec, index) {
					// console.log("selected " + rec.get("id"));
					this.store.setSelected(rec);
				});

		this.store.on("ready", this.restoreState, this);

	}

});

/**
 * @class Gemma.ExpressionExperimentSetStore
 * @extends Ext.data.Store
 */
Gemma.ExpressionExperimentSetStore = function(config) {

	this.record = Ext.data.Record.create([{
				name : "id",
				type : "int"
			}, {
				name : "name"
			}, {
				name : "description"
			}, {
				name : "numExperiments",
				type : "int"
			}, {
				name : "expressionExperimentIds"
			}, {
				name : "taxonId",
				type : "int"
			}, {
				name : "taxonName"
			}]);

	this.readMethod = ExpressionExperimentSetController.getAvailableExpressionExperimentSets;

	this.proxy = new Ext.data.DWRProxy(this.readMethod);

	this.reader = new Ext.data.ListRangeReader({
				id : "id"
			}, this.record);

	Gemma.ExpressionExperimentSetStore.superclass.constructor.call(this, config);

	this.addEvents('ready');

	// this.on("load", this.addFromCookie, this);
	this.load({
				callback : this.addFromCookie
			});

};

/**
 * 
 * @class Gemma.ExpressionExperimentSetStore
 * @extends Ext.data.Store
 */
Ext.extend(Gemma.ExpressionExperimentSetStore, Ext.data.Store, {

			autoLoad : false,

			getSelected : function() {
				if (this.selected) {
					return this.selected;
				} else if (this.selectedId) {
					return this.getById(this.selectedId);
				} else {
					return null;
				}
			},

			setSelectedId : function(id) {
				this.selectedId = id;
			},

			setSelected : function(rec) {
				this.previousSelection = this.getSelected();
				if (rec) {
					this.selected = rec;
					this.selectedId = rec.get("id");
				}
			},

			getPreviousSelection : function() {
				return this.previousSelection;
			},

			clearSelected : function() {
				// console.log("clear");
				this.selected = null;
				this.selectedId = null;
				delete this.selected;
				delete this.selectedId;
			},

			addFromCookie : function() {
				var recs = this.cookieRetrieveEESets();
				if (recs && recs.length > 0) {
					this.add(recs);
				}
				this.isReady = true;
				this.fireEvent("ready");
			},

			cookieSaveOrUpdateEESet : function(rec) {

				var eeSets = this.cookieRetrieveEESets();
				var toBeUpdated = this.searchCookie(eeSets, rec);

				if (toBeUpdated) {
					// Ext.log("Modifying record");
					toBeUpdated.set("name", rec.get("name"));
					toBeUpdated.set("description", rec.get("description"));
					toBeUpdated.set("expressionExperimentIds", rec.get("expressionExperimentIds"));
					toBeUpdated.set("taxonId", rec.get("taxonId"));
					toBeUpdated.set("taxonName", rec.get("taxonName"));
					toBeUpdated.commit();
				} else {
					// Ext.log("Adding record");
					eeSets.push(rec);
				}
				this.cookieSaveEESets(eeSets);
			},

			/**
			 * See if the cookie already has an item to match the given one.
			 */
			searchCookie : function(storedSets, rec) {

				var recName = rec.get("name");

				for (var i = 0, len = storedSets.length; i < len; i++) {
					var s = storedSets[i];

					// Ext.log("Comparing " + s.get("name") + " to " + recName);
					if (s.get("name") == recName) {
						// Ext.log("Found existing set in cookie");
						return s;
					}
				}
				return null;
			},

			removeFromCookie : function(rec) {
				var eeSets = this.cookieRetrieveEESets();
				var updatedSets = [];
				for (var i = 0, len = eeSets.length; i < len; i++) {
					var s = eeSets[i];
					if (s.get("name") != rec.get("name")) {
						updatedSets.push(s);
					} else {
						// Ext.log("Remove " + s.get("name") + " from cookie");
					}
				}

				this.cookieSaveEESets(updatedSets);
			},

			/**
			 * 
			 * @param {}
			 *            eeSets [Records]
			 */
			cookieSaveEESets : function(eeSets) {
				var eeSetData = [];
				for (var i = 0, len = eeSets.length; i < len; i++) {
					eeSetData.push(eeSets[i].data);
				}
				Ext.state.Manager.set(Gemma.ExpressionExperimentSetStore.COOKIE_KEY, eeSetData);
				this.fireEvent("saveOrUpdate");
			},

			/**
			 * Retrieve EESets from the user's cookie.
			 * 
			 * @return {}
			 */
			cookieRetrieveEESets : function() {
				var storedSets = Ext.state.Manager.get(Gemma.ExpressionExperimentSetStore.COOKIE_KEY);
				var eeSets = [];
				if (storedSets && storedSets.length > 0) {
					for (var i = 0, len = storedSets.length; i < len; i++) {
						if (storedSets[i] && storedSets[i].name) {
							var rec = new this.record(storedSets[i]);
							if (rec && rec.data) { // make sure data aren't
								// corrupt.
								eeSets.push(rec);
							}
						}
					}
				}
				return eeSets;
			}

		});

Gemma.ExpressionExperimentSetStore.COOKIE_KEY = "eeSets";

/**
 * User interface for viewing, creating and editing ExpressionExperimentSets.
 * 
 * @class Gemma.DatasetChooserPanel
 * @extends Ext.Window
 */
Gemma.DatasetChooserPanel = Ext.extend(Ext.Window, {
			id : 'dataset-chooser',
			layout : 'border',
			width : 800,
			height : 500,
			closeAction : 'hide',
			constrainHeader : true,
			title : "Dataset set chooser",
			isAdmin : false,

			onCommit : function() {
				var rec = this.eeSetGrid.getStore().getSelected();

				/*
				 * If any are dirty, and if any of the modified records are saveable by this user, then prompt for save.
				 */
				var numModified = this.eeSetStore.getModifiedRecords().length;

				var canSave = this.isAdmin;
				for (var i = 0; i < numModified; i++) {
					if (this.eeSetStore.getModifiedRecords()[i].get("id") < 0) {
						// console.log("can save");
						canSave = true;
						break;
					}
				}

				if (numModified > 0 && canSave) {
					Ext.Msg.show({
								animEl : this.getEl(),
								title : 'Save Changes?',
								msg : 'You have unsaved changes. Would you like to save them?',
								buttons : {
									ok : 'Yes',
									cancel : 'No'
								},
								fn : function(btn, text) {
									if (btn == 'ok') {
										this.eeSetStore.commitChanges();
									}
									if (rec) {
										this.eeSetStore.setSelected(rec);
										this.fireEvent("datasets-selected", rec);
										this.fireEvent("commit", rec);
									}
									this.hide();
								}.createDelegate(this),
								scope : this,
								icon : Ext.MessageBox.QUESTION
							});
				} else {
					this.hide();
					if (rec) {
						this.eeSetStore.setSelected(rec);
						this.fireEvent("datasets-selected", rec);
					} else {
						// console.log("Nothing selected");
					}
					this.fireEvent("commit", rec);
				}

			},

			onHelp : function() {
				window.open('http://bioinformatics.ubc.ca/confluence/display/gemma/Dataset+chooser ',
						'DataSetChooserHelp');
			},

			initComponent : function() {

				Ext.apply(this, {
							buttons : [{
										id : 'done-selecting-button',
										text : "Done",
										handler : this.onCommit.createDelegate(this),
										scope : this
									}, {
										id : 'help-selecting-button',
										text : "Help",
										handler : this.onHelp.createDelegate(this),
										scope : this
									}]
						});

				Gemma.DatasetChooserPanel.superclass.initComponent.call(this);

				this.addEvents({
							"datasets-selected" : true,
							"commit" : true,
							'delete-set' : true
						});

			},

			show : function(config) {
				if (config && config.selected) {
					// Avoid adding handler multiple times.
					this.on("show", function() {
								this.eeSetGrid.getSelectionModel().selectRecords([config.selected]);
								this.eeSetGrid.getView().focusRow(this.eeSetGrid.getStore().indexOf(config.selected));
							}, this, {
								single : true
							}, config);
				}

				Gemma.DatasetChooserPanel.superclass.show.call(this);
			},

			onRender : function(ct, position) {
				Gemma.DatasetChooserPanel.superclass.onRender.call(this, ct, position);

				var admin = dwr.util.getValue("hasAdmin");

				/**
				 * Plain grid for displaying datasets in the current set. Editable.
				 */
				this.eeSetMembersGrid = new Gemma.ExpressionExperimentGrid({
							isAdmin : this.isAdmin,
							region : 'center',
							title : "Datasets in current set",
							pageSize : 15,
							showAnalysisInfo : true,
							loadMask : {
								msg : 'Loading datasets ...'
							},
							tbar : ['->', {
										text : "Delete selected",
										handler : this.removeSelectedFromEeSetMembersGrid.createDelegate(this)
									}],
							split : true,
							height : 200,
							width : 400,
							rowExpander : true
						});

				/**
				 * Datasets that can be added to the current set.
				 */
				this.sourceDatasetsGrid = new Gemma.ExpressionExperimentGrid({
							editable : false,
							isAdmin : this.isAdmin,
							title : "Dataset locator",
							region : 'west',
							split : true,
							// disabled : true, // enable after selecting a set
							showAnalysisInfo : true,
							pageSize : 15,
							height : 200,
							loadMask : {
								msg : 'Searching ...'
							},
							width : 400,
							rowExpander : true,
							tbar : new Gemma.DataSetSearchAndGrabToolbar({
										taxonSearch : true,
										targetGrid : this.eeSetMembersGrid
									})
						});

				/**
				 * Top grid for showing the EEsets
				 */
				this.eeSetGrid = new Gemma.ExpressionExperimentSetGrid({
							store : this.eeSetStore,
							region : 'north',
							layout : 'fit',
							split : true,
							collapsible : true,
							collapseMode : 'mini',
							loadMask : {
								msg : 'Loading'
							},
							height : 200,
							title : "Available expression experiment sets",
							displayGrid : this.eeSetMembersGrid,
							searchGrid : this.sourceDatasetsGrid,
							tbar : new Gemma.EditExpressionExperimentSetToolbar({
										userCanWriteToDB : this.isAdmin
									})

						});

				this.add(this.eeSetGrid);
				this.add(this.eeSetMembersGrid);
				this.add(this.sourceDatasetsGrid);

				this.eeSetGrid.getTopToolbar().on("delete-set", function(rec) {
							this.eeSetMembersGrid.setTitle('Set members');
							this.fireEvent('delete-set');
						}.createDelegate(this));

			},

			removeSelectedFromEeSetMembersGrid : function() {
				this.eeSetMembersGrid.removeSelected();
			}

		});

/**
 * 
 * @class Gemma.ExpressionExperimentSetGrid
 * @extends Ext.grid.GridPanel
 */
Gemma.ExpressionExperimentSetGrid = Ext.extend(Ext.grid.EditorGridPanel, {

			autoExpandColumn : 'description',
			selModel : new Ext.grid.RowSelectionModel({
						singleSelect : true
					}),
			stripeRows : true,
			viewConfig : {
				forceFit : true
			},
			autoExpandMax : 400,
			autoExpandColumn : "description",

			initComponent : function() {

				Gemma.ExpressionExperimentSetGrid.superclass.initComponent.call(this);

				if (!this.store) {
					Ext.apply(this, {
								store : new Gemma.ExpressionExperimentSetStore()
							});
				}

				this.addEvents({
							'loadExpressionExperimentSet' : true,
							'dirty' : true
						});

				this.record = this.getStore().record;

				this.getStore().on("update", function(store, record, operation) {
					if (operation == Ext.data.Record.COMMIT) {
						// console.log("update: " + record.get("name"));

						if (!record.get("expressionExperimentIds")
								|| record.get("expressionExperimentIds").length === 0) {
							Ext.Msg.alert("Cannot save",
									"You must add some experiments to the set before you can save it.");
						} else {
							this.getTopToolbar().update(record);
						}
					}
				}, this);

				this.on("dirty", this.getTopToolbar().editing, this.getTopToolbar());

			},

			afterRender : function() {
				Gemma.ExpressionExperimentSetGrid.superclass.afterRender.call(this);

				this.getTopToolbar().grid = this;

				this.getSelectionModel().on("selectionchange", function() {
							if (this.getCurrentSet && this.getCurrentSet() && this.getCurrentSet().dirty) {
								// console.log("Enabling");
								this.cloneBut.enable();
								this.resetBut.enable();
								this.commitBut.enable();
							}
						}, this.getTopToolbar());

				this.getStore().on("datachanged", function() {
							if (this.getCurrentSet && this.getCurrentSet() && this.getCurrentSet().dirty) {
								// console.log("Enabling");
								this.cloneBut.enable();
								this.resetBut.enable();
								this.commitBut.enable();
							}
						}, this.getTopToolbar());

				this.getSelectionModel().on("rowselect", function(selmol, index, rec) {
							if (this.displayGrid) {
								this.displayGrid.setTitle(rec.get("name"));
								this.displayGrid.getStore().load({
											params : [rec.get("expressionExperimentIds")]
										});
							}

							this.getStore().setSelected(rec);

							if (this.searchGrid && rec.get("taxonName")) {
								this.searchGrid.getTopToolbar().filterTaxon(rec.get("taxonName"));
							}
						}, this);

				/*
				 * Suppress updates while loading
				 */
				this.displayGrid.on("beforeload", function() {
							this.displayGrid.un("add", this.updateMembers);
							this.displayGrid.un("remove", this.updateMembers);
						});

				/*
				 * Update record. We add these listeners after the initial load so they aren't fired right away.
				 */
				this.displayGrid.getStore().on("load", function() {
					this.displayGrid.getStore().on("add", this.updateMembers.createDelegate(this),
							[this.displayGrid.getStore()]);
					this.displayGrid.getStore().on("remove", this.updateMembers.createDelegate(this),
							[this.displayGrid.getStore()]);

				}, this);
			},

			updateMembers : function(store) {
				var rec = this.getSelectionModel().getSelected();

				if (!rec) {
					// if no EEset is currently selected.
					Ext.Msg.alert("Sorry", "You must select a set or create a new set before adding experiments.",
							function() {
								store.un("remove", this.updateMembers);
								store.un("add", this.updateMembers);
								store.removeAll();
							});
					return;
				}

				var ids = [];
				store.each(function(rec) {
							ids.push(rec.get("id"));
						});
				rec.set("expressionExperimentIds", ids);
				rec.set("numExperiments", ids.length);

				this.fireEvent("dirty", rec);
			},

			display : function() {
				// Show the selected eeset members in the lower right-hand grid (or
				// empty)

				var rec = this.getSelectionModel().getSelected();
				if (rec) {
					this.displayGrid.getStore().removeAll();
					this.displayGrid.getStore().load({
								params : [rec.get("expressionExperimentIds")]
							});
					this.displayGrid.setTitle(rec.get("name"));
				}

			},

			clearDisplay : function() {
				// Show the selected eeset members in the lower right-hand grid (or
				// empty)
				var rec = this.getSelectionModel().getSelected();
				if (rec) {
					this.displayGrid.getStore().removeAll();
					this.displayGrid.setTitle(rec.get("name"));
				}
			},

			columns : [{
						id : 'name',
						header : "Name",
						dataIndex : "name",
						sortable : true,
						editor : new Ext.form.TextField({
									allowBlank : false
								})
					}, {
						id : 'description',
						header : "Description",
						dataIndex : "description",
						sortable : true,
						editor : new Ext.form.TextField({
									allowBlank : false
								})
					}, {
						id : 'datasets',
						header : "Num datasets",
						dataIndex : "numExperiments",
						sortable : true
					}, {
						id : 'taxon',
						header : "Taxon",
						dataIndex : "taxonName",
						sortable : true
						// ,renderer : function(v) {
					// if (v) {
					// return v.commonName;
					// } else {
					// return "";
					// }
					// }
				}]

		});

/**
 * Toolbar for creating/updating expressionExperimentSet. Attach to the virtualAnalysisGrid. Either save to the database
 * or to a cookie.
 */
Gemma.EditExpressionExperimentSetToolbar = Ext.extend(Ext.Toolbar, {

			userCanWriteToDB : false,

			display : function() {
				this.grid.display();
			},

			getCurrentSetEEIds : function() {
				return this.getCurrentSet().get("expressionExperimentIds");
			},

			getCurrentSet : function() {
				var sm = this.grid.getSelectionModel();
				return sm.getSelected();
			},

			getCurrentSetId : function() {
				return this.getCurrentSet().get("id");
			},

			getNewDetails : function() {

				if (!this.detailsWin) {
					this.detailsWin = new Gemma.DetailsWindow({
								store : this.grid.getStore()
							});
				}

				this.detailsWin.purgeListeners();
				this.detailsWin.on("commit", function(args) {
							// Ext.log("Add new record");
							var constr = this.grid.getStore().record;
							var newRec = new constr({
										name : args.name,
										description : args.description,
										id : -1,
										modifiable : true,
										expressionExperimentIds : [],
										numExperiments : 0,
										taxon : args.taxon
									}); // Ext creates the id.

							// make the new record dirty.
							newRec.set("description", "");
							newRec.set("description", args.description);

							this.grid.getStore().add(newRec);
							this.grid.getSelectionModel().selectRecords([newRec]);
							this.grid.getView().focusRow(this.grid.getStore().indexOf(newRec));

							this.grid.clearDisplay();
							this.fireEvent("taxonset", args.taxon);
							this.commitBut.enable();

						}, this);

				this.detailsWin.name = '';
				this.detailsWin.description = '';
				this.detailsWin.show();
			},

			afterRender : function() {
				Gemma.EditExpressionExperimentSetToolbar.superclass.afterRender.call(this);

				// this.addButton(this.displayBut);
				// this.addSeparator();
				this.addFill();
				this.addButton(this.newBut);
				this.addButton(this.commitBut);
				this.addButton(this.cloneBut);
				this.addButton(this.resetBut);
				this.addButton(this.deleteBut);
				this.addButton(this.clearFilterBut);

				this.on("disable", function() {
							// Ext.log("Someone disabled me!");
							this.enable();
						});

			},

			initComponent : function() {

				Gemma.EditExpressionExperimentSetToolbar.superclass.initComponent.call(this);

				this.newBut = new Ext.Button({
							id : 'newnew',
							text : "New",
							handler : this.initNew,
							scope : this,
							disabled : false,
							tooltip : "Start a new set (click update to save when you are done)"
						});

				this.commitBut = new Ext.Button({
							id : 'update',
							text : "Save",
							handler : function() {
								this.updateSelected();
							},
							disabled : false,
							scope : this,
							tooltip : "Save or update the set"
						});

				this.cloneBut = new Ext.Button({
							id : 'newsave',
							text : "Clone",
							handler : this.copy,
							scope : this,
							disabled : false,
							tooltip : "Create as new set (click 'save' afterwards)"
						});

				this.resetBut = new Ext.Button({
							id : 'reset',
							text : "Reset",
							handler : this.reset,
							scope : this,
							disabled : false,
							tooltip : "Reset selected set to stored version"
						});

				this.deleteBut = new Ext.Button({
							id : 'delete',
							text : "Delete",
							handler : this.remove,
							scope : this,
							disabled : false,
							tooltip : "Delete selected set"
						});

				this.clearFilterBut = new Ext.Button({
							id : 'clearFilt',
							text : "Show all",
							handler : this.clearFilter,
							scope : this,
							disabled : false,
							tooltip : "Clear filters"
						});

				this.addEvents('saveOrUpdate', 'taxonset', 'remove-set');
				this.on("saveOrUpdate", function() {
							this.commitBut.disable();
						});

				this.resetBut.on("enable", function() {
							// Ext.log("Attempt to enable resetBut");
						});
			},

			initNew : function() {
				// Ext.log("init");
				this.resetBut.disable();
				this.commitBut.disable();
				this.getNewDetails();
			},

			remove : function() {
				var rec = this.getCurrentSet();
				if (rec) {
					Ext.Msg.confirm("Delete?", "Are you sure you want to delete this set? This cannot be undone.",
							function(but) {

								if (but == 'no') {
									return;
								}

								if (rec.get("id") < 0) {
									this.grid.getStore().remove(rec);
									this.grid.getStore().removeFromCookie(rec);
									this.grid.getStore().clearSelected();
									this.fireEvent("delete-set", rec);
								} else {
									if (this.userCanWriteToDB) {
										this.fireEvent("delete-set", rec);
										var callback = function(data) {
											if (data) {
												this.grid.getStore().remove(rec);
												this.grid.getStore().clearSelected();
											}
										}.createDelegate(this);
										ExpressionExperimentSetController.remove(rec.data, callback);
									} else {
										Ext.Msg.alert("Permission denied", "Sorry, you can't delete this set.");
									}
								}
								this.deleteBut.enable();
							}, this);
				}
			},

			clearFilter : function() {
				this.grid.getStore().clearFilter();
			},

			updateSelected : function() {
				var rec = this.getCurrentSet();
				if (rec) {
					rec.commit();
				}
			},

			/**
			 * Method to get called by the 'update' event from the store. Save or update a record. If possible save it
			 * to the database; otherwise use a cookie store.
			 */
			update : function(rec) {

				if (!rec) {
					return;
				}

				this.resetBut.disable();
				this.commitBut.disable();

				if (rec.get("id") < 0) {
					if (this.userCanWriteToDB) {
						/*
						 * try to create it in the db.
						 */
						var callback = function(data) {
							if (data) {
								Ext.Msg.alert("Created", "The set was created in the database");
							} else {
								Ext.Msg.alert("Error", "Could not create. See the logs for details.");
							}
						}.createDelegate(this);
						var errorHandler = function(data) {
							Ext.Msg.alert("Error", "Could not create. See the logs for details. " + data);
							// FIXME: mark dirty.
						}
						ExpressionExperimentSetController.create(rec.data, {
									callback : callback,
									errorHandler : errorHandler
								});
					} else {
						this.grid.getStore().cookieSaveOrUpdateEESet(rec);
					}
				} else {
					if (this.userCanWriteToDB) {
						var updateCallback = function(data) {
							if (data) {
								Ext.Msg.alert("Created", "The set was updated in the database");
							} else {
								Ext.Msg.alert("Error", "Could not updated. See the logs for details.");
							}
						}.createDelegate(this);
						var updateErrorHandler = function(data) {
							Ext.Msg.alert("Error", "Could not update. See the logs for details.<br/>" + data);
							// FIXME: mark dirty.
						}
						ExpressionExperimentSetController.update(rec.data, {
									callback : updateCallback,
									errorHandler : updateErrorHandler
								});
					} else {
						Ext.Msg.alert("Permission denied",
								"Sorry, you can't edit this set. Try saving a clone instead.");
					}
				}

				this.cloneBut.enable();
			},

			copy : function() {
				var rec = this.getCurrentSet();
				var constr = this.grid.getStore().record;
				var newRec = new constr({
							name : rec.get("name") + "*", // indicate they should edit it.
							description : rec.get("description"),
							id : -1,
							modifiable : true,
							expressionExperimentIds : rec.get("expressionExperimentIds"),
							numExperiments : rec.get("numExperiments"),
							taxonId : rec.get("taxonId"),
							taxonName : rec.get("taxonName")
						}); // note that id is assigned by Ext.

				// ensure the new record is dirty.
				newRec.set("description", "");
				newRec.set("description", rec.get("description"));

				this.grid.getStore().add(newRec);
				this.grid.getSelectionModel().selectRecords([newRec]);
				this.grid.getView().focusRow(this.grid.getStore().indexOf(newRec));
				this.commitBut.enable();
				this.cloneBut.disable(); // until we change it.

			},

			reset : function() {
				if (this.getCurrentSet()) {
					this.getCurrentSet().reject();
					this.resetBut.disable();
					this.cloneBut.enable();
					this.display();
				}
			},

			editing : function() {
				this.cloneBut.enable();
				this.resetBut.enable();
				this.commitBut.enable();
				this.newBut.enable();
			}

		});

Gemma.DetailsWindow = Ext.extend(Ext.Window, {
			width : 500,
			height : 300,
			closeAction : 'hide',
			id : 'eeset-dialog',
			title : "Provide or edit expression experiment set details",
			shadow : true,
			modal : true,

			onCommit : function() {

				var values = Ext.getCmp('eeset-form').getForm().getValues();

				var taxon = Ext.getCmp('eesetTaxon').getTaxon().data;

				var name = values.eesetname;

				var indexOfExisting = this.store.findBy(function(record, id) {
							return record.get("name") == name;
						}, this);

				if (!this.nameField.validate()) {
					Ext.Msg.alert("Sorry", "You must provide a name for the set");
					return;
				} else if (indexOfExisting >= 0) {
					Ext.Msg.alert("Sorry", "Please provide a previously unused name for the set");
					return;
				} else {
					this.hide();
					return this.fireEvent("commit", {
								name : values.eesetname,
								description : values.eesetdescription,
								taxon : taxon
							});
				}
			},

			initComponent : function() {

				this.nameField = new Ext.form.TextField({
							fieldLabel : 'Name',
							value : this.name,
							id : 'eesetname',
							allowBlank : false,
							minLength : 3,
							invalidText : "You must provide a name",
							width : 300
						});

				Ext.apply(this, {
							items : new Ext.FormPanel({
										frame : true,
										labelAlign : 'left',
										id : 'eeset-form',
										height : 250,
										items : new Ext.form.FieldSet({
													height : 200,
													items : [new Gemma.TaxonCombo({
																		id : 'eesetTaxon',
																		fieldLabel : 'Taxon'
																	}), this.nameField, new Ext.form.TextArea({
																		fieldLabel : 'Description',
																		value : this.description,
																		id : 'eesetdescription',
																		width : 300
																	})]
												}),
										buttons : [{
													text : "Cancel",
													handler : this.hide.createDelegate(this, [])
												}, {
													text : "OK",
													handler : this.onCommit.createDelegate(this),
													scope : this,
													tooltip : "OK"
												}]

									})
						});

				Gemma.DetailsWindow.superclass.initComponent.call(this);

				this.addEvents("commit");
			}
		});
