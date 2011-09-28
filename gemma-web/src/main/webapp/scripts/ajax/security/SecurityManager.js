/**
 * Methods to view and edit security on objects
 * 
 * @version $Id$
 */
Ext.namespace('Gemma');

Gemma.SecurityManager = {};

Gemma.SecurityManager.adminGroupName = "Administrators";

/**
 * Show the manager for the given entity.
 * 
 * @param {}
 *            clazz full qualified class name of Gemma entity impl, e.g.
 *            ubic.gemma.model.expression.experiment.ExpressionExperimentImpl.
 * @param {}
 *            id of the entity
 * @param {}
 *            elid HTML element that will be used to show the results.
 */
Gemma.SecurityManager.managePermissions = function(elid, clazz, id) {
	/*
	 * Show a panel to 1) make the data set private or public 2) share the data with groups the user is in an which 3)
	 * shows the current permissions. There can be any number of groups. On returning, update the div.
	 */

	Ext.DomHelper.overwrite(elid, {
				tag : 'img',
				src : "/Gemma/images/loading.gif"
			});

	/*
	 * Need to get: public status from server; group sharing; available groups.
	 */
	var showSecurityForm = function(securityInfo) {

		var isPublic = securityInfo.publiclyReadable;
		var isShared = securityInfo.shared;
		var canEdit = securityInfo.currentUserCanwrite;

		var readers = securityInfo.groupsThatCanRead;
		var writers = securityInfo.groupsThatCanWrite;
		var availableGroups = securityInfo.availableGroups;
		var clazz = securityInfo.entityClazz;

		var canManage = securityInfo.currentUserCanwrite;

		// FIXME:
		// Server returns null if the currently logged user belongs to no
		// custom groups which is bad (for following reasons) and technikally
		// incorrect.
		// All users belong to the user group anyway but can't edit it.
		// Adding an array=null to a Component bombs. Adding a null check
		// doesn't
		// help because the panel has a 'field set' already declared, which at
		// runtime has to have at least one checkbox in it.
		// A way out of this is to apply the combo boxes and the field set at
		// the same time if they are necessary.
		// My kungfu no enough strong ;p

		readerChecks = [new Ext.form.Checkbox({
					checked : true,
					boxLabel : 'Users',
					id : 'user' + "-read-chk",
					disabled : true
				})];
		writerChecks = [new Ext.form.Checkbox({
					checked : true,
					boxLabel : 'Users',
					id : 'user' + "-write-chk",
					disabled : true
				})];

		for (var i = 0, len = availableGroups.length; i < len; i++) {
			var groupName = availableGroups[i];
			readerChecks.push(new Ext.form.Checkbox({
						checked : readers.indexOf(groupName) >= 0,
						boxLabel : groupName,
						id : groupName + "-read-chk",
						disabled : groupName === Gemma.SecurityManager.adminGroupName
					}));
			writerChecks.push(new Ext.form.Checkbox({
						checked : writers.indexOf(groupName) >= 0,
						boxLabel : groupName,
						id : groupName + "-write-chk",
						disabled : groupName === Gemma.SecurityManager.adminGroupName
					}));
		}

		/*
		 * show panel...
		 */
		var sp = new Ext.Window({
					title : "Security for: " + Ext.util.Format.ellipsis(securityInfo.entityName, 70, true),
					height : 300,
					width : 500,
					minimizable : false,
					maximizable : false,
					modal : true,
					bodyStyle : 'padding:5px 5px 0',
					stateful : false,

					initComponent : function() {
						Ext.Window.superclass.initComponent.call(this);

						/*
						 * Add checkboxes as needed...
						 */

						if (readerChecks.size() != 0) {
							Ext.apply(Ext.getCmp('reader-checks'), {
										items : readerChecks
									});
						}
						if (writerChecks.size() != 0) {
							Ext.apply(Ext.getCmp('writer-checks'), {
										items : writerChecks
									});
						}
						this.doLayout();

					},

					items : [{
								xtype : 'form',
								layout : 'column',
								autoHeight : true,
								autoWidth : true,
								defaults : {
									layout : 'form',
									border : false,
									bodyStyle : 'padding:4px'
								},
								items : [{
											xtype : 'fieldset',
											autoHeight : true,
											autoWidth : true,
											items : [{
														xtype : 'checkbox',
														boxLabel : "Public",
														id : 'public-checkbox',
														checked : isPublic,
														disabled : !canManage
													}, {
														xtype : 'checkboxgroup',
														width : 300,
														itemCls : 'x-check-group-alt',
														fieldLabel : "Readers",
														id : 'reader-checks',
														disabled : !canManage,
														columns : 1
													}, {
														xtype : 'checkboxgroup',
														width : 300,
														itemCls : 'x-check-group-alt',
														fieldLabel : 'Writers',
														id : 'writer-checks',
														disabled : !canManage,
														columns : 1
													}]
										}]
							}],
					buttons : [{
						text : "Save changes",
						disabled : !canManage,
						handler : function(b, e) {

							var loadMask = new Ext.LoadMask(sp.getEl(), {
										msg : "Saving changes..."
									});
							loadMask.show();

							securityInfo.publiclyReadable = Ext.getCmp('public-checkbox').getValue();

							var updatedGroupsThatCanRead = [];
							var updatedGroupsThatCanWrite = [];

							var shared = false;
							for (var i = 0, len = availableGroups.length; i < len; i++) {
								var groupName = availableGroups[i];
								if (groupName === Gemma.SecurityManager.adminGroupName) {
									continue;
								}
								if (Ext.getCmp(groupName + "-write-chk").getValue()) {
									updatedGroupsThatCanWrite.push(groupName);
									shared = true;
								}
								if (Ext.getCmp(groupName + "-read-chk").getValue() ||
										Ext.getCmp(groupName + "-write-chk").getValue()) {
									updatedGroupsThatCanRead.push(groupName);
									shared = true;
								}
							}

							securityInfo.groupsThatCanWrite = updatedGroupsThatCanWrite;
							securityInfo.groupsThatCanRead = updatedGroupsThatCanRead;

							SecurityController.updatePermission(securityInfo, {
										callback : function(updatedInfo) {
											sp.destroy();

											Gemma.SecurityManager.updateSecurityLink(elid, updatedInfo.entityClass,
													updatedInfo.entityId, updatedInfo.publiclyReadable, shared,
													updatedInfo.currentUserCanwrite);
										},
										errorHandler : function() {
											sp.destroy();
											alert("There was an error saving the settings.");

											Gemma.SecurityManager.updateSecurityLink(elid, updatedInfo.entityClass,
													updatedInfo.entityId, updatedInfo.publiclyReadable,
													updatedInfo.shared, updatedInfo.currentUserCanwrite);

										}
									});

						}
					}, {
						text : 'Cancel',
						handler : function(b, e) {
							sp.destroy();
							// remove the load mask from the icon.
							Gemma.SecurityManager.updateSecurityLink(elid, clazz, id, isPublic, isShared, canEdit);
						}
					}]
				});

		sp.show();


	};

	/*
	 * Initialization.
	 */
	SecurityController.getSecurityInfo({
				classDelegatingFor : clazz,
				id : id
			}, {
				callback : function(securityInfo) {
					showSecurityForm(securityInfo);
				},
				errorHandler : function(data) {
					alert("There was an error getting your group information: " + data);
				}

			});

};

Gemma.SecurityManager.updateSecurityLink = function(elid, clazz, id, isPublic, isShared, canEdit) {

	var newLink = Gemma.SecurityManager.getSecurityLink(clazz, id, isPublic, isShared, canEdit, elid, true);
	Ext.DomHelper.overwrite(elid, newLink);
};

/**
 * Display an icon representing the security status. The icon is a link to the security manager for that entity. If
 * IsPublic and isShared not provided Locked icon is returned
 * 
 * @param {}
 *            clazz full qualified class name of Gemma entity impl, e.g.
 *            ubic.gemma.model.expression.experiment.ExpressionExperimentImpl.
 * @param {}
 *            id of the entity
 * @param {}
 *            isPublic
 * @param {}
 *            isShared
 * @param {}
 *            canEdit if the current user should be able to edit permissions.
 * @return {} html for the link
 */
Gemma.SecurityManager.getSecurityLink = function(clazz, id, isPublic, isShared, canEdit, elid, forUpdate) {

	var icon = '';

	if (canEdit) {
		icon = isPublic
				? '<img src="/Gemma/images/icons/world_edit.png" ext:qtip="Public; click to edit permissions" ext:qtip="Public" alt="public"/>'
				: '<img src="/Gemma/images/icons/lock_edit.png" ext:qtip="Private; click to edit permissions" ext:qtip="Private"  alt="private"/>';

	} else {
		icon = isPublic
				? '<img src="/Gemma/images/icons/world.png" ext:qtip="Public" ext:qtip="Public"  alt="public"/>'
				: '<img src="/Gemma/images/icons/lock.png" ext:qtip="Private" ext:qtip="Private"  alt="private"/>';
	}

	var sharedIcon = isShared ? '<img src="/Gemma/images/icons/group.png" ext:qtip="Shared"  alt="shared"/>' : '';

	if(!elid){
		var elid = Ext.id();
	}

	var dialog = canEdit ? 'style="cursor:pointer" onClick="return Gemma.SecurityManager.managePermissions(\'' + elid +
			'\', \'' + clazz + '\',\'' + id + '\');"' : '';
	if (forUpdate) {
		return icon + '&nbsp;' + sharedIcon;
	} else {
		return '<span  ' + dialog + ' id="' + elid + '" >' + icon + '&nbsp;' + sharedIcon + '</span>';
	}
};

/**
 * Display an icon representing the security status. The icon is a link to the security manager for that entity. Makes a
 * call to the server side to get the security info for the given user.
 * 
 * @param {}
 *            clazz full qualified class name of Gemma entity impl, e.g.
 *            ubic.gemma.model.expression.experiment.ExpressionExperimentImpl.
 * @param {}
 *            id of the entity
 * @return {} html for the link
 */
Gemma.SecurityManager.getSecurityUrl = function(clazz, id) {

	// FIXME: make method wait for callback method to return ends prematurly
	// return no link at all.

	SecurityController.getSecurityInfo({
				classDelegatingFor : clazz,
				id : id
			}, {
				callback : function(securityInfo) {

					var isPublic = securityInfo.publiclyReadable;
					var isShared = securityInfo.shared;
					var canEdit = securityInfo.currentUserCanwrite;
					return Gemma.SecurityManager.getSecurityLink(clazz, id, isPublic, isShared, canEdit);

				},
				errorHandler : function(data) {
					alert("There was an error getting your group information: " + data);
				}

			});

};
