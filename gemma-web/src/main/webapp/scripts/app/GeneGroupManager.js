Ext.namespace('Gemma');
Ext.BLANK_IMAGE_URL = '/Gemma/images/default/s.gif';

/**
 * Provides functionallity for creating and managing Gene groups inside of
 * Gemma.
 * 
 * @author klc
 * @version $Id$
 */
Ext.onReady(function() {

			Ext.QuickTips.init();
			Ext.state.Manager.setProvider(new Ext.state.CookieProvider());

			/*
			 * The GUI
			 */

			var geneGroupImporter = new Gemma.GeneGroupImporter({
						renderTo : 'genesetCreation-div',
						width : "100%"
					});

		});

// Displays the current users Gene Groups
Gemma.GeneGroupPanel = Ext.extend(Ext.grid.EditorGridPanel, {

	loadMask : true,
	stateful : false,
	selModel : new Ext.grid.RowSelectionModel({
		singleSelect : true,
		listeners : {
			'selectionChange' : {
				fn : function(selmod) {

					var sel = selmod.getSelected();

					if (!sel) {
						return;
					}
					var selectedGeneGroupId = sel.data.entityId;
					SecurityController.getGenesInGroup(selectedGeneGroupId,
							function(geneValueObjs) {

								var genePanel = Ext
										.getCmp('gene-chooser-panel');
								// If no genes in gene set, enable taxon
								// selction
								if (!geneValueObjs
										|| geneValueObjs.size() === 0) {
									genePanel.getTopToolbar().taxonCombo
											.reset();
									genePanel.getTopToolbar().geneCombo.reset();
									genePanel.getTopToolbar().taxonCombo
											.setDisabled(false);
									genePanel.fireEvent("taxonchanged", null);
									genePanel.loadGenes([]);
									return;
								}

								var geneIds = [];
								var taxonId = geneValueObjs[0].taxonId;
								for (var i = 0; i < geneValueObjs.length; i++) {
									if (taxonId != geneValueObjs[0].taxonId) {
										Ext.Msg
												.alert(
														'Sorry.  Gene groups do not support mixed taxa. Please remove this gene group',
														response);
									}
									geneIds.push(geneValueObjs[i].id);
								}

								var groupTaxon = {
									id : taxonId,
									commonName : geneValueObjs[0].taxonName
								};
								genePanel.getTopToolbar().taxonCombo
										.setTaxon(groupTaxon);
								genePanel.getTopToolbar().geneCombo
										.setTaxon(groupTaxon);
								genePanel.getTopToolbar().taxonCombo
										.setDisabled(true);

								Ext.getCmp('gene-chooser-panel')
										.loadGenes(geneIds);
							});
				}
			}
		}
	}),
	store : new Ext.data.Store({
		autoLoad : false,
		name : "geneGroupData-store",
		baseParams : {
			params : [true]
		},
		proxy : new Ext.data.DWRProxy(SecurityController.getUsersGeneGroups),
		reader : new Ext.data.ListRangeReader({}, Ext.data.Record.create([{
							name : "entityClazz",
							type : "string"
						}, {
							name : "entityId",
							type : "int"
						}, {
							name : "entityName",
							type : "string"
						}, {
							name : "entityShortName",
							type : "string"
						}, {
							name : "owner"
						}, {
							name : "isPubliclyReadable",
							type : "boolean"
						}, {
							name : "isShared",
							type : "boolean"
						}])),

		listeners : {
			"exception" : function(proxy, type, action, options, response, arg) {
				Ext.Msg.alert('Sorry', response);
			}
		}
	}),
	columns : [{
		header : 'Type',
		dataIndex : 'entityClazz',
		hidden : true,
		groupable : true,
		editable : false,
		sortable : true,
		renderer : function(value, metaData, record, rowIndex, colIndex, store) {
			return value.replace(/.*\./, '');
		}
	}, {
		header : 'ShortName',
		dataIndex : 'entityShortName',
		hidden : true,
		editable : false,
		groupable : false,
		sortable : true
	}, {
		header : 'Name',
		dataIndex : 'entityName',
		editable : false,
		groupable : false,
		sortable : true
	}, {
		header : 'Owner',
		tooltip : 'Who owns the data',
		dataIndex : 'owner',
		groupable : true,
		sortable : true,
		renderer : function(value, metaData, record, rowIndex, colIndex, store) {
			return value.authority ? value.authority : value;
		},
		editor : new Ext.form.ComboBox({
			typeAhead : true,
			displayField : "authority",
			triggerAction : 'all',
			lazyRender : true,
			store : new Ext.data.Store({
				proxy : new Ext.data.DWRProxy(SecurityController.getAvailablePrincipalSids),
				reader : new Ext.data.ListRangeReader({}, Ext.data.Record
								.create([{
											name : "authority"
										}, {
											name : "principal",
											type : "boolean"
										}])),
				listeners : {
					"exception" : function(proxy, type, action, options,
							response, arg) {
						Ext.Msg.alert('Sorry', response);
					}
				}
			})
		})
	}, {
		header : 'Flags',
		sortable : true,
		renderer : function(value, metadata, record, rowIndex, colIndex, store) {
			var id = record.get('entityId');
			var clazz = record.get('entityClazz');
			var isPub = record.get('isPubliclyReadable');
			var isShare = record.get('isShared');
			
			var result =  Gemma.SecurityManager.getSecurityLink(clazz, id,isPub, isShare);
			return result;

		},
		tooltip : 'Click to edit permissions'
	}

	]
});

// Shows the genes that are currently in a given group and allows genes to be
// added or removed from the selected group
Gemma.GeneGroupImporter = Ext.extend(Ext.Panel, {

	initComponent : function() {

		var refreshGeneGroupData = function() {
			var showPrivateOnly = !Ext.getCmp("geneGroupData-show-public").pressed;
			Ext.getCmp('gene-group-panel').getStore().load({
						params : [showPrivateOnly]
					});
		};

		this.geneChooserPanel = new Gemma.GeneGrid({
					region : 'east',
					id : 'gene-chooser-panel'
				});

		this.geneGroupPanel = new Gemma.GeneGroupPanel({
					id : 'gene-group-panel',
					region : 'center'
				});

		Ext.apply(this.geneChooserPanel.getTopToolbar().taxonCombo, {
					stateId : "",
					stateful : false,
					stateEvents : []
				});

		Ext.apply(this, {
			layout : 'border',
			width : "100%",
			height : 400,
			title : "Gene Group Manager",
			tbar : {
				items : [{
					tooltip : "Create New Group",
					icon : "/Gemma/images/icons/group_add.png",
					id : 'geneimportgroup-save-btn',
					handler : function(b, e) {

						Ext.Msg.prompt('New Group',
								'Please enter the group name:', function(btn,
										text) {
									if (btn == 'ok') {

										var geneIds = Ext
												.getCmp("gene-chooser-panel")
												.getGeneIds();
										// TODO add checks for no/bad genes

										SecurityController.createGeneGroup(
												text, geneIds, {
													callback : function(
															groupname) {

														refreshGeneGroupData();

													},
													errorHandler : function(e) {
														Ext.Msg.alert('Sorry',
																e);
													}
												})
									}
								});

					}
				}, {
					tooltip : "Save changes",
					icon : "/Gemma/images/icons/database_save.png",
					id : 'manager-data-panel-save-btn',
					handler : function(b, e) {

						var rec = Ext.getCmp("gene-group-panel")
								.getSelectionModel().getSelected();
						if (rec) {

							// Update the genes incase they changed also. Can
							// only update the genes for 1 list.
							var geneIds = Ext.getCmp("gene-chooser-panel")
									.getGeneIds();

							SecurityController.updateGeneGroup(
									rec.data.entityId, geneIds, {
										callback : function(d) {
											refreshGeneGroupData();
										},
										errorHandler : function(e) {
											Ext.Msg.alert('Sorry', e);
										}
									});

						}

					}
				}, {
					tooltip : "Refresh from the database",
					icon : "/Gemma/images/icons/arrow_refresh_small.png",
					handler : function() {
						refreshGeneGroupData();
					}
				}, {
					tooltip : "Show/hide public data",
					id : "geneGroupData-show-public",
					enableToggle : true,
					icon : "/Gemma/images/icons/world_add.png",
					handler : function() {
						refreshGeneGroupData();
					}
				}, {
					icon : "/Gemma/images/icons/group_delete.png",
					tooltip : "Delete a group",

					handler : function() {

						var sel = Ext.getCmp('gene-group-panel')
								.getSelectionModel().getSelected();
						var groupId = sel.get("entityId");
						var groupName = sel.get("entityName");

						var processResult = function(btn) {

							if (btn == 'yes') {

								SecurityController.deleteGeneGroup(groupId, {
											callback : function() {
												refreshGeneGroupData();
											},
											errorHandler : function(e) {
												Ext.Msg.alert('Sorry', e);
											}
										});
							};
						}

						Ext.Msg.show({
							title : 'Are you sure?',
							msg : 'The group "'
									+ groupName
									+ " with id "
									+ groupId
									+ '" will be permanently deleted. This cannot be undone.',
							buttons : Ext.Msg.YESNO,
							fn : processResult,
							animEl : 'elId',
							icon : Ext.MessageBox.QUESTION
						});

					}
				}]
			},// toolbar
			items : [this.geneGroupPanel, this.geneChooserPanel]
		});

		Gemma.GeneGroupImporter.superclass.initComponent.call(this);

		refreshGeneGroupData();

	}
});