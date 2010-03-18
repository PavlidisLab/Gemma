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
	 * Show a panel to 1) make the data set private or public 2) share the data
	 * with groups the user is in an which 3) shows the current permissions.
	 * There can be any number of groups. On returning, update the div.
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

		var readers = securityInfo.groupsThatCanRead;
		var writers = securityInfo.groupsThatCanWrite;
		var availableGroups = securityInfo.availableGroups;

		readerChecks = [];
		writerChecks = [];
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
			title : "Security for: "
					+ Ext.util.Format.ellipsis(securityInfo.entityName, 70,
							true),
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
												checked : isPublic
											}, {
												xtype : 'checkboxgroup',
												width : 300,
												itemCls : 'x-check-group-alt',
												fieldLabel : "Readers",
												id : 'reader-checks',
												columns : 1
											}, {
												xtype : 'checkboxgroup',
												width : 300,
												itemCls : 'x-check-group-alt',
												fieldLabel : 'Writers',
												id : 'writer-checks',
												columns : 1
											}]
								}]
					}],
			buttons : [{
				text : "Save changes",
				handler : function(b, e) {

					var loadMask = new Ext.LoadMask(sp.getEl(), {
								msg : "Saving changes..."
							});
					loadMask.show();

					securityInfo.publiclyReadable = Ext
							.getCmp('public-checkbox').getValue();

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
						if (Ext.getCmp(groupName + "-read-chk").getValue()
								|| Ext.getCmp(groupName + "-write-chk")
										.getValue()) {
							updatedGroupsThatCanRead.push(groupName);
							shared = true;
						}
					}

					securityInfo.groupsThatCanWrite = updatedGroupsThatCanWrite;
					securityInfo.groupsThatCanRead = updatedGroupsThatCanRead;

					SecurityController.updatePermission(securityInfo, {
								callback : function() {
									sp.destroy();

									Gemma.SecurityManager.updateSecurityLink(
											elid,
											securityInfo.publiclyReadable,
											shared);
								},
								errorHandler : function() {
									sp.destroy();
									alert("There was an error saving the settings.");

									Gemma.SecurityManager.updateSecurityLink(
											elid,
											securityInfo.publiclyReadable,
											sared);

								}
							});

				}
			}, {
				text : 'Cancel',
				handler : function(b, e) {
					sp.destroy();
				}
			}]
		});

		sp.show();

		// remove the load mask from the table.
		Gemma.SecurityManager.updateSecurityLink(elid, isPublic, isShared);

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
					alert("There was an error getting your group information: "
							+ data);
				}

			});

};

Gemma.SecurityManager.updateSecurityLink = function(elid, isPublic, isShared) {

	var icon = isPublic
			? '<img src="/Gemma/images/icons/lock_open2.png" alt="public"/>'
			: '<img src="/Gemma/images/icons/lock.png" alt="public"/>';

	var sharedIcon = isShared
			? '<img src="/Gemma/images/icons/group.png" alt="shared"/>'
			: '';

	Ext.DomHelper.overwrite(elid, icon + '&nbsp;' + sharedIcon);
}

/**
 * Display an icon representing the security status. The icon is a link to the
 * security manager for that entity.
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
 * @return {} html for the link
 */
Gemma.SecurityManager.getSecurityLink = function(clazz, id, isPublic, isShared) {

	var icon = isPublic
			? '<img src="/Gemma/images/icons/lock_open2.png" alt="public"/>'
			: '<img src="/Gemma/images/icons/lock.png" alt="public"/>';

	var sharedIcon = isShared
			? '<img src="/Gemma/images/icons/group.png" alt="shared"/>'
			: '';

	var elid = Ext.id();
	var result = '<span style="cursor:pointer" onClick="return Gemma.SecurityManager.managePermissions(\''
			+ elid
			+ '\', \''
			+ clazz
			+ '\',\''
			+ id
			+ '\');" id="'
			+ elid
			+ '" >' + icon + '&nbsp;' + sharedIcon + '</span>';
	return result;
};
