

/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 * 
 */

Ext.namespace('Gemma');

/**
 * Interface with two panels: one for the groups, one for the group members.
 * 
 * @class Gemma.GeneGroupManager
 * @extends Ext.Panel
 */
	// if window is wider than 1000, give wider panel
	var availablePanelWidth = Ext.getBody().getViewSize().width * 0.9;
	var panelWidth = (availablePanelWidth > 1000)? availablePanelWidth : 1000;
	var westWidth = panelWidth*0.5;
	var eastWidth = panelWidth*0.5;
Gemma.GeneGroupManager = Ext.extend(Ext.Panel, {

			id : "gene-manager-panel",
			layout : 'border',
			height : 400,
			width : panelWidth,
			title : "Gene Group Manager",

			initComponent : function() {

				this.geneChooserPanel = new Gemma.GeneGroupMemberPanel({
							region : 'east',
							split : true,
							id : 'gene-chooser-panel',
							width: eastWidth,
							maxSize: 650,
							minSize: 500,
						});

				this.geneGroupPanel = new Gemma.GeneGroupPanel({
							id : 'gene-group-panel',
							region : 'center',
							tbar : new Gemma.GeneGroupEditToolbar(),
							width: westWidth,
						});

				Ext.apply(this.geneChooserPanel.getTopToolbar().taxonCombo, {
							stateId : "",
							stateful : false,
							stateEvents : []
						});

				// todo add widget for searching for gene groups (or go terms)

				Ext.apply(this, {
							items : [this.geneGroupPanel, this.geneChooserPanel]
						});

				Gemma.GeneGroupManager.superclass.initComponent.call(this);

				/*
				 * Remove a gene: update gene group record locally
				 */
				this.geneChooserPanel.getStore().on('remove', function(store, record, index) {
							this.dirtySet(store);
						}, this);

				/*
				 * Add a gene: update gene group record locally
				 */
				this.geneChooserPanel.getStore().on('add', function(store, records, index) {
							this.dirtySet(store);
							this.geneChooserPanel.resetKeepTaxon();
						}, this);

				/*
				 * After the gene panel loads, unmask
				 */
				this.geneChooserPanel.getStore().on('load', function() {
							this.getEl().unmask();
						}, this);

				this.geneGroupPanel.getSelectionModel().on('rowselect', function(model, rowindex, record) {
							// keep user from messing up interface while we load (e.g., switching rows)
							this.getEl().mask();
							if (record.get('geneIds').length === 0) {
								this.geneChooserPanel.getStore().removeAll();
								this.getEl().unmask();
							} else {
								if (!record.phantom) {
									this.geneChooserPanel.showGeneGroup(record);
								} else {
									this.geneChooserPanel.loadGenes(record.get('geneIds'));
								}
							}

						}, this);
			},

			/**
			 * Copy gene information from the gene grid over to the gene group.
			 * 
			 * @param store
			 *            the store from the gene list panel.
			 */
			dirtySet : function(store) {
				store.clearFilter(false);
				// collect current ids from store
				var currentIds = [];
				store.each(function(r) {
							currentIds.push(r.get('id'));
						}, this);

				var rec = this.geneGroupPanel.getSelectionModel().getSelected();

				if (rec) {
					rec.set('geneIds', currentIds);
					rec.set('size', currentIds.length);
				}
			}
		});

/**
 * Panel showing a list of genes in a gene group.
 * 
 * @class Gemma.GeneGroupMemberPanel
 * @extends Gemma.GeneGrid
 */
Gemma.GeneGroupMemberPanel = Ext.extend(Gemma.GeneGrid, {

			loadMask : true,
			showGeneGroup : function(groupRecord) {
				this.getEl().mask("Loading genes ...");
				GeneSetController.getGenesInGroup(groupRecord.get('id'), {
							callback : this.afterLoadGenes.createDelegate(this, [groupRecord], true),
							errorHandler : function(e) {
								this.getEl().unmask();
								Ext.Msg.alert('There was an error', e);
							}
						});
			},

			reset : function() {
				this.getTopToolbar().taxonCombo.reset();
				this.getTopToolbar().geneCombo.reset();
				this.getTopToolbar().taxonCombo.setDisabled(false);
				this.fireEvent("taxonchanged", null);
				this.loadGenes([]);
				this.currentGroupSize = 0;
			},
			/**
			 * functions the same as reset(), except the taxon combo box doesn't lose its value 
			 * and an event announcing that the taxon has been changed isn't fired
			 */
			resetKeepTaxon : function() {
				this.getTopToolbar().geneCombo.reset();
				this.getTopToolbar().taxonCombo.setDisabled(false);
				this.loadGenes([]);
				this.currentGroupSize = 0;
			},

			lockInTaxon : function(taxon) {
				this.getTopToolbar().taxonCombo.setTaxon(taxon);
				this.getTopToolbar().geneCombo.setTaxon(taxon);
				this.getTopToolbar().taxonCombo.setDisabled(true);
			},

			afterLoadGenes : function(geneValueObjs, groupRecord) {

				if (groupRecord.get('currentUserHasWritePermission')) {
					Ext.util.Observable.releaseCapture(this.getStore());
					this.getTopToolbar().setDisabled(false);
				} else {
					this.getTopToolbar().setDisabled(true);
					Ext.util.Observable.capture(this.getStore(), function(eventName, args) {
								/*
								 * Trap events that would modify an unmodifiable set. Basically 'remove' is the problem.
								 */
								if (eventName === 'add' || eventName === 'remove') {
									Ext.Msg.alert("Access denied", "You don't have permission to edit this set.");
									return false;
								}
								return true;
							}, this);
				}

				// If no genes in gene set, enable taxon
				// selection
				this.currentGroupId = groupRecord.get('id');

				if (!geneValueObjs || geneValueObjs.size() === 0) {
					this.reset();
				} else {

					this.currentGroupSize = geneValueObjs.size();

					var geneIds = [];
					var taxonId = geneValueObjs[0].taxonId;
					for (var i = 0; i < geneValueObjs.length; i++) {
						if (taxonId !== geneValueObjs[0].taxonId) {
							Ext.Msg.alert('Sorry',
									'Gene groups do not support mixed taxa. Please remove this gene group');
							break;
						}
						geneIds.push(geneValueObjs[i].id);
					}

					var groupTaxon = {
						id : taxonId,
						commonName : geneValueObjs[0].taxonName
					};
					this.lockInTaxon(groupTaxon);
					this.loadGenes(geneIds);
				}

				this.getEl().unmask();
			}

		});

/**
 * Toolbar for the GeneGroupPanel
 * 
 * @class Gemma.GeneGroupEditToolbar
 * @extends Ext.Toolbar
 */
Gemma.GeneGroupEditToolbar = Ext.extend(Ext.Toolbar, {

			getCurrentSetGeneIds : function() {
				return this.getCurrentSet().get("geneIds");
			},

			getCurrentSet : function() {
				var sm = this.ownerCt.getSelectionModel();
				return sm.getSelected();
			},

			getCurrentSetId : function() {
				return this.getCurrentSet().get("id");
			},

			/**
			 * Create a new one.
			 */
			getNewDetails : function() {
				if (!this.detailsWin) {
					this.detailsWin = new Gemma.GeneSetDetailsDialog({
								store : this.ownerCt.getStore()
							});
				}

				this.detailsWin.purgeListeners();

				this.detailsWin.on("commit", function(args) {
							var constr = this.ownerCt.getStore().record;
							var newRec = new constr({
										name : args.name,
										description : args.description,
										// id : -1, // maybe not important.
										currentUserHasWritePermission : true,
										geneIds : []
									});

							newRec.markDirty();

							/*
							 * Select it.
							 */
							this.ownerCt.getStore().add(newRec);
							this.ownerCt.getSelectionModel().selectRecords([newRec]);
							this.ownerCt.getView().focusRow(this.ownerCt.getStore().indexOf(newRec));

							this.commitBut.enable();
							this.deleteBut.enable();
							this.cloneBut.disable();
							this.resetBut.disable();

						}, this);

				this.detailsWin.name = '';
				this.detailsWin.description = '';
				this.detailsWin.show();
			},

			/**
			 * 
			 */
			afterRender : function() {
				Gemma.GeneGroupEditToolbar.superclass.afterRender.call(this);

				this.addButton(this.newBut);
				this.addButton(this.commitBut);
				this.addButton(this.cloneBut);
				this.addButton(this.resetBut);
				this.addButton(this.deleteBut);
				this.addButton(this.publicOrPrivateBut);
				this.addFill();
				// this.addButton(this.clearFilterBut);

			},

			/**
			 * 
			 */
			initComponent : function() {

				Gemma.GeneGroupEditToolbar.superclass.initComponent.call(this);
				this.newBut = new Ext.Button({
							handler : this.initNew,
							scope : this,
							icon : "/Gemma/images/icons/add.png",
							disabled : false, // if they are logged in.
							tooltip : "Create a new set (click 'commit' when you are done)"
						});

				this.commitBut = new Ext.Button({
							handler : this.commit,
							disabled : true,
							scope : this,
							icon : "/Gemma/images/icons/database_save.png",
							tooltip : "Commit all changes to the database"
						});

				this.cloneBut = new Ext.Button({
							handler : this.copy,
							scope : this,
							disabled : true,
							icon : "/Gemma/images/icons/arrow_branch.png",
							tooltip : "Clone as a new set (click 'save' afterwards)"
						});

				this.resetBut = new Ext.Button({
							handler : this.reset,
							scope : this,
							disabled : true,
							icon : "/Gemma/images/icons/arrow_undo.png",
							tooltip : "Reset selected set to stored version"
						});

				this.deleteBut = new Ext.Button({
							handler : this.remove,
							scope : this,
							disabled : true,
							icon : "/Gemma/images/icons/database_delete.png",
							tooltip : "Delete selected set"
						});

				this.publicOrPrivateBut = new Ext.Button({
							tooltip : "Show/hide public data",
							enableToggle : true,
							icon : "/Gemma/images/icons/world_add.png",
							handler : this.refreshData,
							pressed : true, // has to match default value in store.
							scope : this
						});

				// this.clearFilterBut = new Ext.Button({
				// text : "Show all",
				// handler : this.clearFilter,
				// scope : this,
				// disabled : true,
				// tooltip : "Clear filters"
				// });

			},

			/**
			 * 
			 * @param {}
			 *            ct
			 * @param {}
			 *            position
			 */
			onRender : function(ct, position) {
				Gemma.GeneGroupEditToolbar.superclass.onRender.apply(this, arguments);

				// owner isn't set until rendering...
				this.ownerCt.on('rowselect', function(selector, rowindex, record) {

							if (!record.phantom) {
								this.cloneBut.enable();
							}

							if (record.get('currentUserHasWritePermission')) {
								this.deleteBut.enable();

								if (record.isModified()) {
									this.resetBut.enable();
									this.commitBut.enable();
								}
							} else {
								this.deleteBut.disable();
								this.commitBut.disable();
								this.resetBut.disable();
							}

						}, this);

				// if (this.ownerCt.getStore().isFiltered()) {
				// this.clearFilterBut.enable();
				// } else {
				// this.clearFilterBut.disable();
				// }

				this.ownerCt.getStore().on('update', function(store, record, operation) {
							// if (store.isFiltered()) {
							// this.clearFilterBut.enable();
							// } else {
							// this.clearFilterBut.disable();
							// }

							if (this.getCurrentSet && this.getCurrentSet() && this.getCurrentSet().dirty) {
								this.cloneBut.enable();
								this.resetBut.enable();
								this.commitBut.enable();
							}
						}, this);

				this.ownerCt.on('afteredit', function(e) {
							this.resetBut.enable();
							this.commitBut.enable();
						}, this);

				this.ownerCt.getStore().on('write', function(store, action, data, records, options) {
							this.ownerCt.loadMask.hide();
							this.commitBut.disable();
						}, this);

				this.ownerCt.getStore().on('exception', function(proxy, type, action, options, response, arg) {
							this.ownerCt.loadMask.hide();
						}, this);

			},

			/**
			 * Handler
			 */
			initNew : function() {
				this.getNewDetails();
			},

			/**
			 * Handler. Remove a group. If it is persistent, you need to have permission to do this.
			 */
			remove : function() {
				var rec = this.getCurrentSet();
				if (rec) {
					Ext.Msg.confirm("Delete?", "Are you sure you want to delete this set? This cannot be undone.",
							function(but) {
								if (but === 'no') {
									return;
								}

								this.ownerCt.loadMask.show();

								if (rec.phantom) {
									// nonpersistent, go ahead.
									this.ownerCt.getStore().remove(rec);
									this.ownerCt.getStore().clearSelected();
									this.resetBut.disable();
									this.deleteBut.disable();
									this.commitBut.disable();
									this.ownerCt.loadMask.hide();
								} else {
									var callback = function(data) {
										if (data) {
											this.ownerCt.getStore().remove(rec);
											this.ownerCt.getStore().clearSelected();
											this.resetBut.disable();
											this.deleteBut.disable();
											this.commitBut.disable();
											this.ownerCt.loadMask.hide();
										}
									}.createDelegate(this);
									GeneSetController.remove([rec.data], callback);
								}
								this.fireEvent("delete-set", rec);
							}, this);
				}
			},

			/**
			 * Handler.
			 */
			clearFilter : function() {
				this.ownerCt.getStore().clearFilter();
			},

			/**
			 * Handler
			 */
			refreshData : function() {
				var showPrivateOnly = !this.publicOrPrivateBut.pressed;
				if (this.ownerCt.getStore().getModifiedRecords().length > 0) {
					Ext.Msg.show({
								title : 'Are you sure?',
								msg : 'You have unsaved changes which will be lost if you change modes.',
								buttons : Ext.Msg.YESNO,
								fn : function(btn, text) {
									if (btn === 'yes') {

										this.ownerCt.getStore().load({
													params : [showPrivateOnly, null]
												});
									}
								},
								scope : this
							});
				} else {
					this.ownerCt.getStore().load({
								params : [showPrivateOnly, null]
							});
				}
			},

			/**
			 * Handler.
			 */
			commit : function() {
				this.ownerCt.loadMask.show();
				this.ownerCt.getStore().save();
			},

			/**
			 * Handler. Clone (copy) an existing EESet
			 */
			copy : function() {
				var rec = this.getCurrentSet();
				var constr = this.ownerCt.getStore().record;
				var newRec = new constr({
							name : "Copy of " + rec.get("name"), // indicate they should edit it.
							description : rec.get("description"),
							size : rec.get("size"),
							geneIds : rec.get("geneIds"),
							currentUserHasWritePermission : true
						});

				this.ownerCt.getStore().add(newRec);
				this.ownerCt.getSelectionModel().selectRecords([newRec]);
				this.ownerCt.getView().focusRow(this.ownerCt.getStore().indexOf(newRec));

				this.deleteBut.enable();
				this.commitBut.enable();
				this.resetBut.disable();
				this.cloneBut.disable(); // until we change it.
			},

			/**
			 * Handler. Reset the currently selected Set
			 */
			reset : function() {
				if (this.getCurrentSet()) {
					this.getCurrentSet().reject();
					this.resetBut.disable();
					this.commitBut.disable();
					this.cloneBut.enable();
					this.ownerCt.getSelectionModel().fireEvent("rowselect", this.ownerCt.getSelectionModel(),
							this.ownerCt.getStore().indexOf(this.getCurrentSet()), this.getCurrentSet());
				}
			}

		});

/**
 * A list of gene groups.
 * 
 * @class Gemma.GeneGroupPanel
 * @extends Ext.grid.EditorGridPanel
 */
Gemma.GeneGroupPanel = Ext.extend(Ext.grid.EditorGridPanel, {

			selModel : new Ext.grid.RowSelectionModel({
						singleSelect : true
					}),

			name : 'geneGroupGridPanel',
			stripeRows : true,

			initComponent : function() {
				Gemma.GeneGroupPanel.superclass.initComponent.call(this);
				if (!this.store) {
					Ext.apply(this, {
								store : new Gemma.GeneGroupStore()
							});
				}
				this.addEvents({
							'dirty' : true
						});

				this.record = this.getStore().record;

			},

			afterRender : function() {

				Gemma.GeneGroupPanel.superclass.afterRender.call(this);

				this.loadMask = new Ext.LoadMask(this.body, {
							msg : 'Loading ...',
							store : this.store
						});

				this.relayEvents(this.getSelectionModel(), 'rowselect');
				this.relayEvents(this.getStore(), 'datachanged');

				this.getSelectionModel().on("rowselect", function(selmol, index, rec) {
							this.getStore().setSelected(rec);
						}, this);
			},

			columns : [{
						header : 'Name',
						dataIndex : 'name',
						width: 250,
						editable : true,
						sortable : true,
						editor : new Ext.form.TextField({
									allowBlank : false
								})
					}, {
						header : 'Description',
						dataIndex : 'description',
						width: 250,
						editable : true,
						sortable : true,
						editor : new Ext.form.TextField({
									allowBlank : false
								})
					}, {
						header : 'Size',
						sortable : true,
						dataIndex : 'size',
						editable : false,
						width : 40,
						tooltip : 'number of genes in group'
					}, {
						header : 'Flags',
						sortable : true,
						width: 100,
						renderer : function(value, metadata, record, rowIndex, colIndex, store) {
							var result = Gemma.SecurityManager.getSecurityLink(
									"ubic.gemma.model.genome.gene.GeneSetImpl", record.get('id'), record.get('publik'),
									record.get('shared'), record.get('currentUserHasWritePermission'));
							return result;
						},
						tooltip : 'Click to edit permissions'
					}

			],

			/**
			 * Called by outside when adding members.
			 * 
			 * @param {}
			 *            store
			 */
			updateMembers : function(store) {
				var rec = this.getSelectionModel().getSelected();

				if (!rec) {
					// if no set is currently selected.
					Ext.Msg.alert("Sorry", "You must select a set or create a new set before adding genes.",
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
				rec.set("geneIds", ids);
				rec.set("size", ids.length);
				this.fireEvent("dirty", rec);
			}
		});

/**
 * Dialog to ask user for information about a new set (or potentially modifications to an existing one)
 * 
 * @extends Ext.Window
 */
Gemma.GeneSetDetailsDialog = Ext.extend(Ext.Window, {
			width : 500,
			height : 300,
			closeAction : 'hide',
			title : "Provide or edit gene group details",
			shadow : true,
			modal : true,
			initComponent : function() {

				this.addEvents("commit");
				this.formId = Ext.id();
				
				Ext.apply(this, {
					items: new Ext.FormPanel({
						id: this.id + 'FormPanel',
						ref: 'formPanel',
						frame: false,
						labelAlign: 'left',
						height: 250,
						items: new Ext.form.FieldSet({
							id: this.id + 'FieldSet',
							ref: 'fieldSet',
							height: 240,
							items: [new Ext.form.TextField({
								ref: 'nameField',
								id: this.id + "Name",
								fieldLabel: 'Name',
								name: 'newSetName',
								minLength: 3,
								allowBlank: false,
								invalidText: "You must provide a name",
								msgTarget: 'side',
								width: 300
							}), new Ext.form.TextArea({
								ref: 'descField',
								id: this.id + 'Desc',
								fieldLabel: 'Description',
								name: 'newSetDescription',
								value: this.description,
								width: 300
							//value: this.suggestedDescription
							})			/*, new Ext.form.Radio({
			 fieldLabel : 'Private',
			 name : 'publicPrivate',
			 checked: true
			 }), new Ext.form.Radio({
			 fieldLabel : 'Public',
			 name : 'publicPrivate',
			 checked: false
			 })*/
							]
						})
					}),
					buttons: [{
						text: "Cancel",
						handler: this.hide.createDelegate(this, [])
					}, {
						text: "OK",
						scope:this,
						handler: function(){
							if (!this.formPanel.fieldSet.nameField.validate()) {
								return;
							}
							var values = this.formPanel.getForm().getValues();
							this.fireEvent("commit", {
								name: values.newSetName,
								description: values.newSetDescription
							});
							this.hide();
							return;
						}
					}]
				
				});

				Gemma.GeneSetDetailsDialog.superclass.initComponent.call(this);
				this.addEvents("commit");
			}
		});

/**
 * 
 * @param {}
 *            config
 */
Gemma.GeneGroupStore = function(config) {

	/*
	 * Leave this here so copies of records can be constructed.
	 */
	this.record = Ext.data.Record.create([{
				name : "id",
				type : "int"
			}, {
				name : "name",
				type : "string",
				convert : function(v, rec) {
					if (v.startsWith("GO")) {
						return rec.description;
					}
					return v;
				}
			}, {
				name : "description",
				type : "string",
				convert : function(v, rec) {
					if (rec.name.startsWith("GO")) {
						return rec.name;
					}
					return v;
				}

			}, {
				name : "publik",
				type : "boolean"
			}, {
				name : "size",
				type : "int"
			}, {
				name : "shared",
				type : 'boolean'
			}, {
				name : "currentUserHasWritePermission",
				type : 'boolean'
			}, {
				name : "geneIds"
			}]);

	// todo replace with JsonReader.
	this.reader = new Ext.data.ListRangeReader({
				id : "id"
			}, this.record);

	Gemma.GeneGroupStore.superclass.constructor.call(this, config);

};

/**
 * 
 * @class Gemma.GeneGroupStore
 * @extends Ext.data.Store
 */
Ext.extend(Gemma.GeneGroupStore, Ext.data.Store, {

			autoLoad : true,
			autoSave : false,
			selected : null,
			name : "geneGroupData-store",

			proxy : new Ext.data.DWRProxy({
						apiActionToHandlerMap : {
							read : {
								dwrFunction : GeneSetController.getUsersGeneGroups,
								getDwrArgsFunction : function(request) {
									if (request.params.length > 0) {
										return [request.params[0], request.params[1]];
									}
									return [false, null];
								}
							},
							create : {
								dwrFunction : GeneSetController.create
							},
							update : {
								dwrFunction : GeneSetController.update
							},
							destroy : {
								dwrFunction : GeneSetController.remove
							}
						}
					}),

			writer : new Ext.data.JsonWriter({
						writeAllFields : true
					}),

			getSelected : function() {
				return this.selected;
			},

			setSelected : function(rec) {
				this.previousSelection = this.getSelected();
				if (rec) {
					this.selected = rec;
				}
			},

			getPreviousSelection : function() {
				return this.previousSelection;
			},

			clearSelected : function() {
				this.selected = null;
				delete this.selected;
			},

			listeners : {
				write : function(store, action, result, res, rs) {
					// Ext.Msg.show({
					// title : "Saved",
					// msg : "Changes were saved",
					// icon : Ext.MessageBox.INFO
					// });
				},
				exception : function(proxy, type, action, options, res, arg) {
					console.log(res);
					if (type === 'remote') {
						Ext.Msg.show({
									title : 'Error',
									msg : res,
									icon : Ext.MessageBox.ERROR
								});
					} else {
						Ext.Msg.show({
									title : 'Error',
									msg : arg,
									icon : Ext.MessageBox.ERROR
								});
					}
				}
			

			}

		});
