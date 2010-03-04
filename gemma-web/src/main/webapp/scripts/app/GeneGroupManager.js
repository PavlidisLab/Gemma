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

// FIXME: Not sure how to get these as part of the widget directly.

writeableChks = new Ext.ux.grid.CheckColumn({
			header : 'Read Write',
			dataIndex : 'currentGroupCanWrite',
			tooltip : 'Current group can write?',
			groupable : false,
			width : 55
		});

readableChks = new Ext.ux.grid.CheckColumn({
			header : 'Read',
			dataIndex : 'currentGroupCanRead',
			tooltip : 'Current group can read?',
			groupable : false,
			width : 55
		});
publicChks = new Ext.ux.grid.CheckColumn({
			header : 'Public',
			dataIndex : 'publiclyReadable',
			tooltip : 'Data is publicly readable?',
			groupable : false,
			width : 55
		});

// Displays the current users Gene Groups
Gemma.GeneGroupPanel = Ext.extend(Ext.grid.EditorGridPanel, {

	plugins : [publicChks, readableChks, writeableChks],
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

								// Need call back for load mask?
								if (!geneValueObjs || geneValueObjs.length == 0)
									return; // No genes returned throw an error?
								var geneIds = [];
								for (var i = 0; i < geneValueObjs.length; i++) {
									geneIds.push(geneValueObjs[i].id);
								}
								Ext.getCmp('gene-chooser-panel')
										.loadGenes(geneIds);
							});
				}
			}
		}
	}),
	store : new Ext.data.Store({
		name : "geneGroupData-store",
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
							name : "publiclyReadable",
							type : "boolean"
						}, {
							name : "currentGroup",
							type : "string"
						}, {
							name : "currentGroupCanRead",
							type : "boolean"
						}, {
							name : "currentGroupCanWrite",
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
		groupable : true,
		editable : false,
		sortable : true,
		renderer : function(value, metaData, record, rowIndex, colIndex, store) {
			return value.replace(/.*\./, '');
		}
	}, {
		header : 'ShortName',
		dataIndex : 'entityShortName',
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
	}, publicChks, readableChks, writeableChks

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
					height : 300,
					region : 'south',
					id : 'gene-chooser-panel'
				});

		this.geneGroupPanel = new Gemma.GeneGroupPanel({
					height : 300,
					id : 'gene-group-panel',
					region : 'north'
				});

		Ext.apply(this.geneChooserPanel.getTopToolbar().taxonCombo, {
					stateId : "",
					stateful : false,
					stateEvents : []
				});

		Ext.apply(this, {
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

														/*
														 * Refresh
														 */
														Ext
																.getCmp("gene-group-panel")
																.getStore()
																.load({
																			params : []
																		});

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
						/*
						 * change R/W/P on selected data, set owner. Get just
						 * the edited records.
						 */
						var recs = Ext.getCmp("gene-group-panel").getStore()
								.getModifiedRecords();
						if (recs && recs[0]) {
							var p = [];
							for (var i = 0; i < recs.length; i++) {
								/*
								 * This is ugly. The 'owner' object gets turned
								 * into a plain string.have to reconstruct the
								 * owner from strings
								 */
								// This is the value if the owner has not been
								// changed in the combo box
								if (recs[i].data.owner.authority) {
									recs[i].data.owner = {
										authority : recs[i].data.owner.authority,
										principal : recs[i].data.owner.principal
									};
								}
								// this is the value if the owner has not been
								// changed. principal is always true as combo
								// only filled with principals
								else if (recs[i].data.owner) {
									recs[i].data.owner = {
										authority : recs[i].data.owner,
										principal : "true"
									};
								} else {
									Ext.Msg.alert('Owner can not be changed');
								}
								p.push(recs[i].data);
							}

							SecurityController.updatePermissions(p, {
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

						var processResult = function(btn) {
							/*
							 * TODO -- no full server side support yet! Deleting
							 * group is not so easy if there is data attached.
							 */

							if (btn == 'yes') {

								SecurityController.deleteGeneGroup(groupId, {
											callback : function() {
												Ext.getCmp('gene-group-panel')
														.getStore().load({
																	params : []
																});
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

	}
});