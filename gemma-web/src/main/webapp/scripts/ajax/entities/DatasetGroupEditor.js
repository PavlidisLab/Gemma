/**
 * Main user interface for viewing, creating and editing DatasetGroups. Three panes.
 * 
 * At the top, the available ExpressionExperimentSets are shown. At the bottom left, searching for experiments; at the
 * bottom right, the experiments that are in the set. If the user is authenticated, they can save new DatasetGroups to
 * the database.
 * 
 * @author Paul
 * 
 * @version $Id$
 * 
 * 
 * @class Gemma.DatasetGroupEditor
 * @extends Ext.Window
 */

/* 
 * get width and height of viewport to use for sizing the parent and children panels 
 */
var windowWidth = Ext.getBody().getViewSize().width * 0.9;
var westWidth = windowWidth* 0.33;
var eastWidth = windowWidth* 0.33;
var centreWidth = windowWidth-westWidth-eastWidth;


Gemma.DatasetGroupEditor = Ext.extend(Ext.Window, {

			id : 'dataset-chooser',
			name : 'datasetchooser',
			layout : 'border',
			width : windowWidth,
			height : 500,
			closeAction : 'hide',
			constrainHeader : false,
			title : "Dataset Group Editor ",
			isLoggedIn : false,
			maximizable : false,

			/**
			 * 
			 */
			initComponent : function() {

				Ext.apply(this, {
							buttons : [{
										id : 'done-selecting-button',
										text : "Done",
										handler : this.onCommit,
										scope : this
									}, {
										id : 'help-selecting-button',
										text : "Help",
										handler : this.onHelp,
										scope : this
									}]
						});

				Gemma.DatasetGroupEditor.superclass.initComponent.call(this);

				this.isLoggedIn = Ext.get('loggedIn').getValue();

				/**
				 * Plain grid for displaying datasets in the current set. Editable.
				 */
				this.datasetGroupMembersGrid = new Gemma.ExpressionExperimentGrid({
							isLoggedIn : this.isLoggedIn,
							region : 'east',
							title : "Datasets in current set",
							showAnalysisInfo : true,
							loadMask : {
								msg : 'Loading datasets ...'
							},
							tbar : ['->', {
										text : "Delete selected",
										handler : this.removeSelectedFromdatasetGroupMembersGrid,
										scope : this
									}],
							split : true,
							height : 200,
							width : eastWidth,
							minWidth : 200
						});

				/*
				 * Space to show the details about the currently selected expression experiment
				 */
				this.dataSetDetailsPanel = new Ext.Panel({
							region : 'south',
							split : true,
							bodyStyle : 'padding:8px',
							height : 200
						});

				/**
				 * Datasets that can be added to the current set.
				 */
				this.sourceDatasetsGrid = new Gemma.ExpressionExperimentGrid({
							editable : false,
							isLoggedIn : this.isLoggedIn,
							title : "Dataset locator",
							region : 'center',
							split : true,
							showAnalysisInfo : true,
							height : 200,
							loadMask : {
								msg : 'Searching ...'
							},
							minWidth : centreWidth,
							tbar : new Gemma.DataSetSearchAndGrabToolbar({
										taxonSearch : true,
										targetGrid : this.datasetGroupMembersGrid

									})

						});

				/*
				 * Top grid for showing the datasetGroups
				 */
				this.datasetGroupGrid = new Gemma.DatasetGroupGridPanel({
							store : this.datasetGroupStore,
							region : 'west',
							layout : 'fit',
							split : true,
							collapsible : true,
							collapseMode : 'mini',
							height : 200,
							width : westWidth,
							title : "Available expression experiment sets",
							displayGrid : this.datasetGroupMembersGrid,
							tbar : new Gemma.DatasetGroupEditToolbar()
						});

				this.add(this.datasetGroupGrid);
				this.add(this.datasetGroupMembersGrid);
				this.add(this.sourceDatasetsGrid);
				this.add(this.dataSetDetailsPanel);

				/*
				 * filter so we only see data sets that are NOT in the GroupMembersGrid - Remove datasets that are in
				 * the other grid.
				 */
				this.sourceDatasetsGrid.getStore().on('load', function(idsFound) {
					this.sourceDatasetsGrid.getStore().filterBy(function(record, id) {
								var rid = record.get('id');
								return this.datasetGroupMembersGrid.getStore().find('id', rid) < 0;
							}, this);

					this.sourceDatasetsGrid.setTitle(this.sourceDatasetsGrid.title + ", " +
							this.sourceDatasetsGrid.getStore().getCount() + " addable");

				}.createDelegate(this));

				this.datasetGroupGrid.getTopToolbar().on("delete-set", function(rec) {
							this.clearDisplay();
							this.fireEvent('delete-set');
						}.createDelegate(this));

				this.datasetGroupMembersGrid.getStore().on('remove', function(store, record, index) {
							this.dirtySet(store);
						}, this);
				this.datasetGroupMembersGrid.getStore().on('add', function(store, records, index) {
							this.dirtySet(store);
						}, this);

				this.datasetGroupGrid.getSelectionModel().on('rowselect', function(model, rowindex, record) {
							this.display(record);
						}, this);

				this.sourceDatasetsGrid.getSelectionModel().on('rowselect', this.showEEDetails, this, {
					buffer : 100
						// keep from firing too many times at once
					});
				this.datasetGroupMembersGrid.getSelectionModel().on('rowselect', this.showEEDetails, this, {
					buffer : 100
						// keep from firing too many times at once
					});

				this.on('show', function(panel) {
							var r = this.datasetGroupGrid.getStore().getSelected();
							if (r) {
								this.datasetGroupGrid.getSelectionModel().selectRecords([r], false);
								this.datasetGroupGrid.getView().focusRow(this.datasetGroupGrid.getStore().indexOf(r));
							}

						}, this, {
							delay : 100,
							single : true
						});

				this.addEvents({
							"select" : true,
							"commit" : true,
							'delete-set' : true
						});

			},

			showEEDetails : function(model, rowindex, record) {

				if (this.detailsmask === null) {
					this.detailsmask = new Ext.LoadMask(this.dataSetDetailsPanel.body, {
								msg : "Loading details ..."
							});
				}

				this.detailsmask.show();
				ExpressionExperimentController.getDescription(record.id, {
							callback : function(data) {
								Ext.DomHelper.overwrite(this.dataSetDetailsPanel.body, "<h1>" +
												record.get('shortName') + "</h1><h2>" + record.get('name') +
												"</h2><p>" + data + "</p>");
								this.detailsmask.hide();
							}.createDelegate(this)
						});

			},

			dirtySet : function(store) {
				store.clearFilter(false);
				// collect current ids from store
				var currentIds = [];
				store.each(function(r) {
							currentIds.push(r.get('id'));
						}, this);

				var rec = this.datasetGroupGrid.getSelectionModel().getSelected();

				if (rec) {
					rec.set('expressionExperimentIds', currentIds);
					rec.set('numExperiments', currentIds.length);
				}
			},

			/**
			 * Show the selected eeset members in the lower right-hand grid, if it exists
			 */

			display : function(record) {
				if (record && this.datasetGroupMembersGrid) {
					this.datasetGroupMembersGrid.getStore().removeAll();
					this.datasetGroupMembersGrid.getStore().load({
								params : [record.get("expressionExperimentIds")]
							});
					this.datasetGroupMembersGrid.setTitle(record.get("name"));
				}

				if (record && this.sourceDatasetsGrid) {
					this.sourceDatasetsGrid.getTopToolbar().setTaxon(record.get("taxonId"));
					this.sourceDatasetsGrid.getTopToolbar().taxonCombo.disable();
				}
			},

			/**
			 * Clear the lower right grid
			 */
			clearDisplay : function() {
				this.datasetGroupMembersGrid.getStore().removeAll();
				this.datasetGroupMembersGrid.setTitle('Set members');
				this.sourceDatasetsGrid.getTopToolbar().taxonCombo.enable();
			},

			removeSelectedFromdatasetGroupMembersGrid : function() {
				this.datasetGroupMembersGrid.removeSelected();
			},

			/**
			 * When a edit is completed and we're closing the window.
			 */
			onCommit : function() {
				var rec = this.datasetGroupGrid.getStore().getSelected();

				/*
				 * If any are dirty, and if any of the modified records are saveable by this user, then prompt for save.
				 */
				var numModified = this.datasetGroupStore.getModifiedRecords().length;

				var canSave = this.isLoggedIn;
				for (var i = 0; i < numModified; i++) {
					var r = this.datasetGroupStore.getModifiedRecords()[i];
					if (r.get('currentUserHasWritePermission')) {
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
									if (btn === 'ok') {
										this.datasetGroupStore.commitChanges();
									}
									if (rec) {
										this.datasetGroupStore.setSelected(rec);
										this.fireEvent("select", rec);
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
						this.datasetGroupStore.setSelected(rec);
						this.fireEvent("select", rec);
					}
					this.fireEvent("commit", rec);
				}

			},

			onHelp : function() {
				window.open(Gemma.HOST + 'faculty/pavlidis/wiki/display/gemma/Dataset+chooser', 'DataSetChooserHelp');
			}

		});