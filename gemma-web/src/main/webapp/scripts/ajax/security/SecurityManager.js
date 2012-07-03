/**
 * Methods to view and edit security on objects
 * 
 * @version $Id$
 */
Ext.namespace('Gemma');

Gemma.SecurityManager = {};

Gemma.SecurityManager.adminGroupName = "Administrators";
Gemma.SecurityManager.usersGroupName = "Users";

/**
 * Show the manager for the given entity.
 * 
 * The only user who can edit security permissions with this widget is the owner of the entity
 * 
 * @param {}
 *            clazz full qualified class name of Gemma entity impl, e.g.
 *            ubic.gemma.model.expression.experiment.ExpressionExperimentImpl.
 * @param {}
 *            id of the entity
 * @param {}
 *            elid HTML element that will be used to show the results.
 */
Gemma.SecurityManager.managePermissions = function(elid, clazz, id, securityFormTitle) {
	/*
	 * Show a panel to 1) make the data set private or public 2) share the data with groups the user is in an which 3)
	 * shows the current permissions. There can be any number of groups. On returning, update the div.
	 */

	Ext.DomHelper.overwrite(elid, {
				tag : 'img',
				src : "/Gemma/images/loading.gif"
			});


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
	
	/*
	 * Need to get: public status from server; group sharing; available groups.
	 */
	var showSecurityForm = function(securityInfo) {

		var widgetWidth = 500;
		var isPublic = securityInfo.publiclyReadable;
		var isShared = securityInfo.shared;
		// only owner can edit permissions
		var canEdit = securityInfo.currentUserCanwrite;
		var isOwner = securityInfo.currentUserOwns;
		
		var ownerName = securityInfo.owner.authority;

		var readers = securityInfo.groupsThatCanRead;
		var writers = securityInfo.groupsThatCanWrite;
		var availableGroups = securityInfo.availableGroups;
		var clazz = securityInfo.entityClazz;

		var readerChecks = [];
		var writerChecks = [];

		if(availableGroups.length === 0){
			readerChecks = [{
				xtype: 'panel',
				border:false,
				html: Gemma.HelpText.WidgetDefaults.SecurityManager.noGroupsToShareWith
			}];
			writerChecks = [{
				xtype: 'panel',
				border:false,
				html: Gemma.HelpText.WidgetDefaults.SecurityManager.noGroupsToShareWith
			}];
		}

		for (var i = 0, len = availableGroups.length; i < len; i++) {
			var groupName = availableGroups[i];
			var boxLabel = groupName + (
				(groupName === Gemma.SecurityManager.usersGroupName)? 
					' (This group includes all registered Gemma users)':
				(groupName === Gemma.SecurityManager.adminGroupName)? 
				' (Admin users can always see all entities)':'');
				
			readerChecks.push(new Ext.form.Checkbox({
						checked : readers.indexOf(groupName) >= 0,
						boxLabel : boxLabel,
						id : groupName + "-read-chk",
						disabled : groupName === Gemma.SecurityManager.adminGroupName || !canEdit
					}));
			writerChecks.push(new Ext.form.Checkbox({
						checked : writers.indexOf(groupName) >= 0,
						boxLabel : boxLabel,
						id : groupName + "-write-chk",
						disabled : groupName === Gemma.SecurityManager.adminGroupName || !canEdit
					}));
		}

		var publicReadingFieldSet = new Ext.ux.RadioFieldset({
            radioToggle: true,
			radioName: 'readingRadio',
			radioId: 'public-radio',
			disableRadio: !canEdit,
            title: 'Public',
            defaultType: 'checkbox',
            collapsed: !isPublic,
			checked: isPublic,
            layout: 'anchor',
            defaults: {
                anchor: '100%'
            },
            items : [{
				xtype: 'panel',
				border:false,
				width: widgetWidth-50,
				bodyStyle:'background-color:#F7F9D0;padding:5px;border:1px solid #FF7575',
				html: Gemma.HelpText.WidgetDefaults.SecurityManager.publicWarning
			}]
        });		
		var privateReadingFieldSet = new Ext.ux.RadioFieldset({
            radioToggle:true,
			radioName: 'readingRadio',
			radioId: 'private-radio',
			disableRadio: !canEdit,
            title: 'Private',
            defaultType: 'checkbox',
            collapsed: isPublic,
			checked: !isPublic,
            layout: 'anchor',
            defaults: {
                anchor: '100%'
            },
            items : readerChecks
        });

		publicReadingFieldSet.on('expand', function(){
			privateReadingFieldSet.collapse();
		},this);
		privateReadingFieldSet.on('expand', function(){
			publicReadingFieldSet.collapse();
		},this);
				
		var readerFieldSet = {
			width: widgetWidth,
            xtype:'fieldset',
            title: 'Reading Permissions',
            collapsed: false,
            layout: 'anchor',
            defaults: {
                anchor: '100%'
            },
            items : [publicReadingFieldSet,privateReadingFieldSet]
        };	
	
		var privateWritingFieldSet = new Ext.ux.RadioFieldset({
            radioToggle:true,
			radioName: 'writingRadio',
			disableRadio: !canEdit,
            title: 'Private',
            defaultType: 'checkbox',
            collapsed: false,
			checked: true,
            layout: 'anchor',
            defaults: {
                anchor: '100%'
            },
            items : writerChecks
        });
		
		var writerFieldSet = {
			width: widgetWidth,
            xtype:'fieldset',
            title: 'Writing Permissions',
			disableRadio: !canEdit,
            collapsed: false,
			style:'margin-top:40px',
            layout: 'anchor',
            defaults: {
                anchor: '100%'
            },
            items : [privateWritingFieldSet,
			{
				tag: 'div',
				html: 'Note that any group given write access will also have read access.',
				border:false
			}]
        };	


		var saveChanges = function(b, e) {

			if (securityPanel && securityPanel.getEl()) {
				var loadMask = new Ext.LoadMask(securityPanel.getEl(), {
					msg : "Saving changes..."
				});
				loadMask.show();
			}

			securityInfo.publiclyReadable = Ext.get('public-radio').dom.checked;

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
				// if you can write, then you need to be able to read
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
							securityPanel.destroy();

							Gemma.SecurityManager.updateSecurityLink(elid, updatedInfo.entityClazz,
									updatedInfo.entityId, updatedInfo.publiclyReadable, updatedInfo.shared,
									updatedInfo.currentUserOwns);
						},
						errorHandler : function() {
							securityPanel.destroy();
							alert("There was an error saving the settings.");

							Gemma.SecurityManager.updateSecurityLink(elid, updatedInfo.entityClazz,
									updatedInfo.entityId, updatedInfo.publiclyReadable,
									updatedInfo.shared, updatedInfo.currentUserOwns);

						}
					});
		};
		
		
		/*
		 * show panel...
		 */
		var securityPanel = new Ext.Window({
					title : "Security for: " +
								(securityFormTitle == null ?
									Ext.util.Format.ellipsis(securityInfo.entityName, 70, true) :
									securityFormTitle),
					minimizable : false,
					maximizable : false,
					width:widgetWidth + 30, // needed for chrome
					modal : true,
					bodyStyle : 'padding:5px 5px 0;background-color:white',
					stateful : false,
					autoScroll:true,
					shadow: false, // doesn't resize with window
					initComponent : function() {
						Ext.Window.superclass.initComponent.call(this);
					},
					// this set up method will not work if >1 of these windows per page
					// for now that can't happen so this is ok
					items : [{
						xtype: 'panel',
						html: "<b>Owner</b>: " + ownerName,
						border:false,
						padding: '10'
					}, readerFieldSet,writerFieldSet],
					buttons : [{
						text : "Save changes",
						disabled : !canEdit,
						hidden: !canEdit,
						handler : saveChanges.createDelegate(this),
						scope: this
					}, {
						text : (canEdit)?'Cancel':'OK',
						handler : function(b, e) {
							securityPanel.destroy();
							// remove the load mask from the icon.
							Gemma.SecurityManager.updateSecurityLink(elid, clazz, id, isPublic, isShared, canEdit,isOwner);
						}
					}]
				});

		securityPanel.show();

		
	};
	
	
};

Gemma.SecurityManager.updateSecurityLink = function(elid, clazz, id, isPublic, isShared, canEdit,isOwner) {

	var newLink = Gemma.SecurityManager.getSecurityLink(clazz, id, isPublic, isShared, canEdit, elid, true,null, isOwner);
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
Gemma.SecurityManager.getSecurityLink = function(clazz, id, isPublic, isShared, canEdit, elid, forUpdate, securityFormTitle, currentUserIsOwner) {

	var icon = '';

	if (canEdit) {
		
		if (currentUserIsOwner){
			icon = isPublic
			? '<img src="/Gemma/images/icons/world_edit_mine.png" ext:qtip="Public; click to edit permissions" ext:qtip="Public" alt="public"/>'
			: '<img src="/Gemma/images/icons/lock_edit_mine.png" ext:qtip="Private; click to edit permissions" ext:qtip="Private"  alt="private"/>';
			
		}else{
		
		icon = isPublic
				? '<img src="/Gemma/images/icons/world_edit.png" ext:qtip="Public; click to edit permissions" ext:qtip="Public" alt="public"/>'
				: '<img src="/Gemma/images/icons/lock_edit.png" ext:qtip="Private; click to edit permissions" ext:qtip="Private"  alt="private"/>';
		}

	} else {
		icon = isPublic
				? '<img src="/Gemma/images/icons/world.png" ext:qtip="Public; click to view details" ext:qtip="Public"  alt="public"/>'
				: '<img src="/Gemma/images/icons/lock.png" ext:qtip="Private click to view details" ext:qtip="Private"  alt="private"/>';
	}

	var sharedIcon = isShared ? '<img src="/Gemma/images/icons/group.png" ext:qtip="Shared"  alt="shared"/>' : '';

	if(!elid){
		var elid = Ext.id();
	}

	var dialog = 'style="cursor:pointer" onClick="return Gemma.SecurityManager.managePermissions(\'' + elid +
			'\', \'' + clazz + '\',\'' + id + '\'' + (securityFormTitle == null ? '' : ', \'' + securityFormTitle + '\'') + ');"';
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
					var isOwner = securityInfo.currentUserOwns;
					return Gemma.SecurityManager.getSecurityLink(clazz, id, isPublic, isShared, canEdit,null, null, null, isOwner);

				},
				errorHandler : function(data) {
					alert("There was an error getting your group information: " + data);
				}

			});

};
