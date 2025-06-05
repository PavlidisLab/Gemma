/**
 * Methods to view and edit security on objects
 * 
 * 
 */
Ext.namespace( 'Gemma' );

Gemma.SecurityManager = {};

Gemma.SecurityManager.adminGroupName = "Administrators";
Gemma.SecurityManager.usersGroupName = "Users";

Gemma.SecurityManager.isAdmin = function() {
   return Ext.get( 'hasAdmin' ) ? Ext.get( 'hasAdmin' ).getValue() : false;
};

Gemma.SecurityManager.isLoggedIn = function() {
   return Ext.get( 'hasUser' ) ? Ext.get( 'hasUser' ).getValue() : false;
};

/**
 * Show the manager for the given entity.
 * 
 * The only user who can edit security permissions with this widget is the owner of the entity
 * 
 * @param {string}
 *           elid
 * @param {string}
 *           clazz full qualified class name of Gemma entity impl, e.g.
 *           ubic.gemma.model.expression.experiment.ExpressionExperiment.
 * @param {number}
 *           id of the entity
 * @param {string}
 *           elid HTML element that will be used to show the results.
 */
Gemma.SecurityManager.managePermissions = function( elid, clazz, id, securityFormTitle ) {
   /*
    * Show a panel to 1) make the data set private or public 2) share the data with groups the user is in an which 3)
    * shows the current permissions. There can be any number of groups. On returning, update the div.
    */

   Ext.DomHelper.overwrite( elid, {
      tag : 'img',
      src : Gemma.CONTEXT_PATH + "/images/loading.gif"
   } );

   /*
    * Initialization.
    */
   SecurityController.getSecurityInfo( {
      classDelegatingFor : clazz,
      id : id
   }, {
      callback : function( securityInfo ) {
         showSecurityForm( securityInfo );
      },
      errorHandler : function( data ) {
         alert( "There was an error getting your group information: " + data );
      }

   } );

   /*
    * Need to get: public status from server; group sharing; available groups.
    */
   var showSecurityForm = function( securityInfo ) {

      var widgetWidth = 500;
      var isPublic = securityInfo.publiclyReadable;
      var isShared = securityInfo.shared;
      // only owner can edit permissions
      var canEdit = securityInfo.currentUserCanwrite;
      var isOwner = securityInfo.currentUserOwns;

      var ownerName = securityInfo.owner.authority;
      var ownersGroups = securityInfo.ownersGroups;
      var ownerIsAdmin = false;

      if ( ownerName == 'GROUP_ADMIN' ) {
         ownerIsAdmin = true;
      } else {
         for (var k = 0, len = ownersGroups.length; k < len; k++) {
            var groupName = ownersGroups[k];

            if ( groupName === Gemma.SecurityManager.adminGroupName ) {
               ownerIsAdmin = true;
               break;
            }

         }
      }

      var readers = securityInfo.groupsThatCanRead;
      var writers = securityInfo.groupsThatCanWrite;
      var availableGroups = securityInfo.availableGroups;
      var clazz = securityInfo.entityClazz;

      var readerChecks = [];
      var writerChecks = [];

      if ( availableGroups.length === 0 ) {
         readerChecks = [ {
            xtype : 'panel',
            border : false,
            html : Gemma.HelpText.WidgetDefaults.SecurityManager.noGroupsToShareWith
         } ];
         writerChecks = [ {
            xtype : 'panel',
            border : false,
            html : Gemma.HelpText.WidgetDefaults.SecurityManager.noGroupsToShareWith
         } ];
      }

      for (var i = 0, len = availableGroups.length; i < len; i++) {
         var groupName = availableGroups[i];
         var boxLabel = groupName
            + ((groupName === Gemma.SecurityManager.usersGroupName) ? ' (This group includes all registered Gemma users)'
               : (groupName === Gemma.SecurityManager.adminGroupName) ? ' (Admin users can manage all entities)' : '');

         readerChecks.push( new Ext.form.Checkbox( {
            checked : readers.indexOf( groupName ) >= 0,
            boxLabel : boxLabel,
            id : groupName + "-read-chk",
            disabled : groupName === Gemma.SecurityManager.adminGroupName || !canEdit
         } ) );
         writerChecks.push( new Ext.form.Checkbox( {
            checked : writers.indexOf( groupName ) >= 0,
            boxLabel : boxLabel,
            id : groupName + "-write-chk",
            disabled : groupName === Gemma.SecurityManager.adminGroupName || !canEdit
         } ) );
      }

      var publicReadingFieldSet = new Ext.ux.RadioFieldset( {
         radioToggle : true,
         radioName : 'readingRadio',
         radioId : 'public-radio',
         disableRadio : !canEdit,
         title : 'Public',
         defaultType : 'checkbox',
         collapsed : !isPublic,
         checked : isPublic,
         defaults : {
            anchor : '100%'
         },
         items : [ {
            xtype : 'panel',
            border : false,
            width : widgetWidth - 50,
            bodyStyle : 'background-color:#F7F9D0;padding:5px;border:1px solid #FF7575',
            html : Gemma.HelpText.WidgetDefaults.SecurityManager.publicWarning
         } ]
      } );

      /*
       * Indicates groups that have read-access.
       */
      var privateReadingFieldSet = new Ext.ux.RadioFieldset( {
         radioToggle : true,
         radioName : 'readingRadio',
         radioId : 'private-radio',
         disableRadio : !canEdit,
         title : 'Private',
         defaultType : 'checkbox',
         collapsed : isPublic,
         height : 200,
         autoScroll : true,
         checked : !isPublic,
         layout : 'anchor',
         defaults : {
            anchor : '100%'
         },
         items : readerChecks
      } );

      publicReadingFieldSet.on( 'expand', function() {
         privateReadingFieldSet.collapse();
      }, this );

      privateReadingFieldSet.on( 'expand', function() {
         publicReadingFieldSet.collapse();
      }, this );

      var readerFieldSet = {
         width : widgetWidth,
         xtype : 'fieldset',
         title : 'Reading Permissions',
         collapsed : false,
         layout : 'anchor',
         defaults : {
            anchor : '100%'
         },
         items : [ publicReadingFieldSet, privateReadingFieldSet ]
      };

      /*
       * Indicates groups that have write-access.
       */
      var privateWritingFieldSet = new Ext.ux.RadioFieldset( {
         radioToggle : true,
         radioName : 'writingRadio',
         disableRadio : !canEdit,
         title : 'Private',
         defaultType : 'checkbox',
         collapsed : false,
         height : 200,
         autoScroll : true,
         checked : true,
         layout : 'anchor',
         defaults : {
            anchor : '100%'
         },
         items : writerChecks
      } );

      var writerFieldSet = {
         width : widgetWidth,
         xtype : 'fieldset',
         title : 'Writing Permissions',
         disableRadio : !canEdit,
         collapsed : false,
         style : 'margin-top:40px',
         layout : 'anchor',
         defaults : {
            anchor : '100%'
         },
         items : [ privateWritingFieldSet, {
            tag : 'div',
            html : 'Note that any group given write access will also have read access.',
            border : false
         } ]
      };

      var saveChanges = function( b, e ) {

         if ( securityPanel && securityPanel.getEl() ) {
            var loadMask = new Ext.LoadMask( securityPanel.getEl(), {
               msg : "Saving changes..."
            } );
            loadMask.show();
         }

         securityInfo.publiclyReadable = Ext.get( 'public-radio' ).dom.checked;

         var updatedGroupsThatCanRead = [];
         var updatedGroupsThatCanWrite = [];

         var shared = false;
         for (var i = 0, len = availableGroups.length; i < len; i++) {
            var groupName = availableGroups[i];
            if ( groupName === Gemma.SecurityManager.adminGroupName ) {
               continue;
            }
            if ( Ext.getCmp( groupName + "-write-chk" ).getValue() ) {
               updatedGroupsThatCanWrite.push( groupName );
               shared = true;
            }
            // if you can write, then you need to be able to read
            if ( Ext.getCmp( groupName + "-read-chk" ).getValue() || Ext.getCmp( groupName + "-write-chk" ).getValue() ) {
               updatedGroupsThatCanRead.push( groupName );
               shared = true;
            }
         }

         securityInfo.groupsThatCanWrite = updatedGroupsThatCanWrite;
         securityInfo.groupsThatCanRead = updatedGroupsThatCanRead;

         SecurityController.updatePermission( securityInfo, {
            callback : function( updatedInfo ) {
               securityPanel.destroy();

               Gemma.SecurityManager.updateSecurityLink( elid, updatedInfo.entityClazz, updatedInfo.entityId,
                  updatedInfo.publiclyReadable, updatedInfo.shared, updatedInfo.currentUserCanwrite,
                  updatedInfo.currentUserOwns );
            },
            errorHandler : function() {
               securityPanel.destroy();
               alert( "There was an error saving the settings." );

               Gemma.SecurityManager.updateSecurityLink( elid, updatedInfo.entityClazz, updatedInfo.entityId,
                  updatedInfo.publiclyReadable, updatedInfo.shared, updatedInfo.currentUserOwns );

            }
         } );
      };

      /*
       * show panel...
       */

      var ownerHtml = "<b>Owner</b>: " + ownerName;

      var isAdmin = Gemma.SecurityManager.isAdmin();

      if ( isAdmin ) {

         if ( ownerIsAdmin ) {
            ownerHtml = ownerHtml + ' (Administrator)';
         } else {
            ownerHtml = ownerHtml + ' (Registered non-administrator)';
         }
      }

      var securityPanel = new Ext.Window( {
         title : "Security for: "
            + (securityFormTitle ? securityFormTitle
               : Ext.util.Format.ellipsis( securityInfo.entityName, 70, true )),
         minimizable : false,
         maximizable : false,
         width : widgetWidth + 30, // needed for chrome
         modal : true,
         bodyStyle : 'padding:5px 5px 0;background-color:white',
         stateful : false,
         boxMaxHeight : 500,
         height : 500,
         autoScroll : true,
         initComponent : function() {
            Ext.Window.superclass.initComponent.call( this );
         },
         // this set up method will not work if >1 of these windows per page
         // for now that can't happen so this is ok
         items : [ {
            xtype : 'panel',
            html : ownerHtml,
            border : false,
            padding : '10'
         }, readerFieldSet, writerFieldSet ],
         buttons : [ {
            text : "Save changes",
            disabled : !canEdit,
            hidden : !canEdit,
            handler : saveChanges.createDelegate( this ),
            scope : this
         }, {
            text : (canEdit) ? 'Cancel' : 'OK',
            handler : function( b, e ) {
               securityPanel.destroy();
               // remove the load mask from the icon.
               Gemma.SecurityManager.updateSecurityLink( elid, clazz, id, isPublic, isShared, canEdit, isOwner );
            }
         } ]
      } );

      securityPanel.show();

   };

};

Gemma.SecurityManager.updateSecurityLink = function( elid, clazz, id, isPublic, isShared, canEdit, isOwner ) {

   var newLink = Gemma.SecurityManager.getSecurityLink( clazz, id, isPublic, isShared, canEdit, elid, true, null,
      isOwner );
   Ext.DomHelper.overwrite( elid, newLink );
};

/**
 * Display an icon representing the security status. The icon is a link to the security manager for that entity. If
 * IsPublic and isShared not provided Locked icon is returned
 *
 * @param {string} clazz full qualified class name of Gemma entity impl, e.g.
 *           ubic.gemma.model.expression.experiment.ExpressionExperiment.
 * @param {string} id of the entity
 * @param {boolean} isPublic
 * @param {boolean} isShared
 * @param {boolean} canEdit if the current user should be able to edit permissions.
 * @param elid
 * @param forUpdate
 * @param securityFormTitle
 * @param currentUserIsOwner
 */
Gemma.SecurityManager.getSecurityLink = function( clazz, id, isPublic, isShared, canEdit, elid, forUpdate,
   securityFormTitle, currentUserIsOwner ) {

   var icon = '';

   if ( canEdit ) {

      if ( currentUserIsOwner ) {
          icon = isPublic ? '<i class="green fa fa-globe fa-lg fa-fw" ext:qtip="Public; click to view details"></i>'
              : '<i class="green fa fa-lock fa-lg fa-fw" ext:qtip="Private; click to view details"></i>';

      } else {
          icon = isPublic ? '<i class="gray-blue fa fa-globe fa-lg fa-fw" ext:qtip="Public; click to view details"></i>'
              : '<i class="gray-blue fa fa-lock fa-lg fa-fw" ext:qtip="Private; click to view details"></i>';
      }

   } else {
      icon = isPublic ? '<i class="dark-gray fa fa-globe fa-lg fa-fw" ext:qtip="Public"></i>'
         : '<i class="dark-gray fa fa-lock fa-lg fa-fw" ext:qtip="Private"></i>';
   }

   var sharedIcon = isShared ? '<i class="gray-blue fa fa-users fa-lg fa-fw" ext:qtip="Shared" ></i>' : '';

   if ( !elid ) {
      var elid = Ext.id();
   }

   var dialog = 'style="cursor:pointer" class="security-link" onClick="return Gemma.SecurityManager.managePermissions(\'' + elid + '\', \''
      + clazz + '\',\'' + id + '\'' + (securityFormTitle == null ? '' : ', \'' + securityFormTitle + '\'') + ');"';
   if ( forUpdate ) {
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
 *           clazz full qualified class name of Gemma entity impl, e.g.
 *           ubic.gemma.model.expression.experiment.ExpressionExperiment.
 * @param {}
 *           id of the entity
 * @return {} html for the link
 */
Gemma.SecurityManager.getSecurityUrl = function( clazz, id ) {

   SecurityController.getSecurityInfo( {
      classDelegatingFor : clazz,
      id : id
   }, {
      callback : function( securityInfo ) {

         var isPublic = securityInfo.publiclyReadable;
         var isShared = securityInfo.shared;
         var canEdit = securityInfo.currentUserCanwrite;
         var isOwner = securityInfo.currentUserOwns;
         return Gemma.SecurityManager.getSecurityLink( clazz, id, isPublic, isShared, canEdit, null, null, null,
            isOwner );

      },
      errorHandler : function( data ) {
         alert( "There was an error getting your group information: " + data );
      }

   } );

};
