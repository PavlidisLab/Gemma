Ext.namespace( 'Gemma' );
Ext.BLANK_IMAGE_URL = Gemma.CONTEXT_PATH + '/images/default/s.gif';

/**
 * @author keshav
 * 
 */
Ext
   .onReady( function() {

      Ext.QuickTips.init();
      Ext.state.Manager.setProvider( new Ext.state.CookieProvider() );

      var currentGroup;

      var groupMembershipChks = new Ext.ux.grid.CheckColumn( {
         header : 'In group',
         dataIndex : 'inGroup',
         tooltip : 'If the user is in the group',
         width : 55
      } );

      var writeableChks = new Ext.ux.grid.CheckColumn( {
         header : 'Read Write',
         dataIndex : 'currentGroupCanWrite',
         tooltip : 'Current group can write?',
         groupable : false,
         width : 55
      } );

      var readableChks = new Ext.ux.grid.CheckColumn( {
         header : 'Read',
         dataIndex : 'currentGroupCanRead',
         tooltip : 'Current group can read?',
         groupable : false,
         width : 55
      } );
      var publicChks = new Ext.ux.grid.CheckColumn( {
         header : 'Public',
         dataIndex : 'publiclyReadable',
         tooltip : 'Data is publicly readable?',
         groupable : false,
         width : 55
      } );

      var selectAllChecks = function( selectAll ) {
         var recs = Ext.getCmp( "group-data-grid" ).getStore().getRange();
         Ext.each( recs, function( rec ) {
            if ( rec.get( 'allowModification' ) ) {
               rec.set( 'publiclyReadable', selectAll );
               rec.set( 'currentGroupCanRead', selectAll );
               rec.set( 'currentGroupCanWrite', selectAll );
            }
         }, this );

      };

      var refresh = function( groupName, showPrivateOnly ) {
         currentGroup = groupName;

         activateButtons();
         refreshGroupMembers( groupName );
         refreshData( groupName, showPrivateOnly );

      };

      /**
       * when a user has selected a group, activate the group member and data panel buttons
       */
      var activateButtons = function() {
         Ext.getCmp( "manager-group-data-panel" ).getTopToolbar().enable();
         Ext.getCmp( 'manager-group-members-panel' ).getTopToolbar().enable();
      };

      /*
       * Load the users in the group, put in the members store.
       */
      var refreshGroupMembers = function( groupName ) {
         Ext.getCmp( 'group-members-grid' ).getStore().load( {
            params : [ groupName ]
         } );
      };

      /*
       * Load the data available to the _current_ user w.r.t the selected group, put in the data column.
       */
      var refreshData = function( groupName, showPrivateOnly ) {

         if ( showPrivateOnly == undefined ) {
            showPrivateOnly = !Ext.getCmp( "manager-data-panel-show-public" ).showPublicData;
         }

         Ext.getCmp( 'group-data-grid' ).getStore().load( {
            params : [ groupName, showPrivateOnly ]
         } );
      };

      /*
       * The GUI
       */

      // if window is wider than 1000, give wider panel
      var availablePanelWidth = Ext.getBody().getViewSize().width * 0.9;
      var panelWidth = (availablePanelWidth > 1000) ? availablePanelWidth : 1000;

      var groupviewer = new Ext.Panel(
         {
            height : 600,
            id : 'manager-panel',
            width : panelWidth,
            renderTo : 'manageGroups-div',

            layout : 'border',
            defaults : {
               stateful : true,
               stateEvents : [ 'resize' ],
               split : true,
               bodyStyle : 'padding:1px'
            },
            items : [ /*
                         * GROUPS
                         */
               {
                  title : 'Your groups',
                  id : 'manager-groups-panel',
                  region : 'west',
                  margins : '5 0 0 0',
                  cmargins : '5 5 0 0',
                  collapsible : true,
                  width : panelWidth * 0.25,

                  tbar : {
                     items : [
                              {
                                 icon : Gemma.CONTEXT_PATH + "/images/icons/group_add.png",
                                 tooltip : "Add a group",
                                 handler : function() {

                                    Ext.Msg.prompt( 'New Group', 'Please enter the group name:', function( btn, text ) {
                                       if ( btn == 'ok' ) {

                                          SecurityController.createGroup( text, {
                                             callback : function( groupname ) {

                                                var c = Ext.getCmp( 'manager-groups-listview' ).getStore().recordType;

                                                var newrec = new c( {
                                                   groupName : groupname
                                                }, groupname );

                                                /*
                                                 * Refresh
                                                 */
                                                Ext.getCmp( "manager-groups-listview" ).getStore().load(
                                                   {
                                                      // select the new group
                                                      callback : function() {
                                                         Ext.getCmp( 'manager-groups-listview' ).getSelectionModel()
                                                            .selectRow(
                                                               Ext.getCmp( "manager-groups-listview" ).getStore()
                                                                  .findExact( 'groupName', text ) );
                                                      }
                                                   } );

                                             },
                                             errorHandler : function( e ) {
                                                Ext.Msg.alert( 'Sorry', e );
                                             }
                                          } );
                                       }

                                    } );
                                 }
                              },
                              {
                                 icon : Gemma.CONTEXT_PATH + "/images/icons/group_delete.png",
                                 tooltip : "Delete a group",
                                 // disabled : true,
                                 // hidden : true,
                                 handler : function() {

                                    var sel = Ext.getCmp( 'manager-groups-listview' ).getSelectionModel().getSelected();
                                    var groupName = sel.get( "groupName" );

                                    var processResult = function( btn ) {

                                       if ( btn == 'yes' ) {

                                          SecurityController.deleteGroup( groupName, {
                                             callback : function() {
                                                Ext.getCmp( 'manager-groups-listview' ).getStore().load( {
                                                   params : []
                                                } );
                                                /* reset the group-members grid */
                                                Ext.getCmp( 'group-members-grid' ).getStore().loadData( {} );
                                                /* reset the data grid */
                                                Ext.getCmp( 'group-data-grid' ).getStore().loadData( {} );
                                             },
                                             errorHandler : function( e ) {
                                                Ext.Msg.alert( 'Sorry', e );
                                             }
                                          } );
                                       }
                                    };

                                    Ext.Msg
                                       .show( {
                                          title : 'Are you sure?',
                                          msg : 'The group "'
                                             + groupName
                                             + '" will be permanently deleted. All associated permissions will be cleared. This cannot be undone.',
                                          buttons : Ext.Msg.YESNO,
                                          fn : processResult,
                                          animEl : 'elId',
                                          icon : Ext.MessageBox.QUESTION
                                       } );

                                 }
                              } ]
                  },
                  items : [ new Ext.grid.GridPanel( {
                     id : 'manager-groups-listview',
                     height : 535,
                     loadMask : true,
                     selModel : new Ext.grid.RowSelectionModel( {
                        singleSelect : true,
                        listeners : {
                           'selectionChange' : {
                              fn : function( selmod ) {

                                 var sel = selmod.getSelected();

                                 if ( !sel ) {
                                    return;
                                 }
                                 refresh( sel.get( "groupName" ) );
                              }
                           }
                        }
                     } ),
                     store : new Ext.data.Store( {
                        autoLoad : true,
                        sortInfo : {
                           field : 'groupName',
                           direction : 'ASC'
                        },
                        proxy : new Ext.data.DWRProxy( SecurityController.getAvailableGroups ),
                        reader : new Ext.data.ListRangeReader( {
                           id : "groupName"
                        }, Ext.data.Record.create( [ {
                           name : "groupName"

                        }, {
                           name : "canEdit",
                           type : "boolean"
                        }, {
                           name : "member",
                           type : "boolean"
                        } ] ) ),
                        listeners : {
                           "exception" : function( proxy, type, action, options, response, arg ) {
                              Ext.Msg.alert( 'Sorry', response );
                           }
                        }
                     } ),
                     columns : [ {
                        header : 'Group name',
                        dataIndex : 'groupName',
                        sortable : true
                     }, {
                        header : 'Edit',
                        width : 60,
                        dataIndex : 'canEdit',
                        tooltip : "Can you edit this group?",
                        sortable : true,
                        renderer : function( value, metaData, record, rowIndex, colIndex, store ) {
                           return value ? "Y" : "N";
                        }
                     }, {
                        header : 'Member',
                        width : 60,
                        dataIndex : 'member',
                        tooltip : "Are  you a member of this group?",
                        sortable : true,
                        renderer : function( value ) {
                           return value ? "Y" : "N";
                        }

                     } ],
                     viewConfig : {
                        forceFit : true
                     }

                  } )

                  ]
               }, /*
                   * GROUP MEMBERS
                   */
               {
                  title : 'Group members',
                  id : 'manager-group-members-panel',
                  region : 'center',
                  width : panelWidth * 0.25,
                  margins : '5 0 0 0',
                  cmargins : '5 5 0 0',
                  tbar : {
                     disabled : true,
                     items : [
                              {
                                 icon : Gemma.CONTEXT_PATH + "/images/icons/user_add.png",
                                 tooltip : "Invite new member",
                                 handler : function() {

                                    Ext.Msg.prompt( 'New group member', 'Please enter the user name or email address:',
                                       function( btn, userNameOrEmail ) {
                                          if ( btn == 'ok' ) {

                                             SecurityController.addUserToGroup( userNameOrEmail, currentGroup, {
                                                callback : function( d ) {
                                                   refreshGroupMembers( currentGroup );
                                                },
                                                errorHandler : function( e ) {
                                                   Ext.Msg.alert( 'Sorry', e );
                                                }
                                             } );
                                          }
                                       } );

                                 }
                              }, {
                                 tooltip : "Save changes",
                                 icon : Gemma.CONTEXT_PATH + "/images/icons/database_save.png",
                                 id : 'manager-group-members-panel-save-btn',
                                 handler : function( b, e ) {
                                    /*
                                     * remove group members who are unchecked NOTE this does not add users! They get
                                     * removed from the table once they are not in the group.
                                     */

                                    var recs = Ext.getCmp( "group-members-grid" ).getStore().getModifiedRecords();
                                    if ( recs && recs[0] ) {

                                       var userNames = [];
                                       for (var i = 0; i < recs.length; i++) {
                                          var r = recs[i];
                                          if ( !r.get( "inGroup" ) ) {
                                             userNames.push( r.get( "userName" ) );
                                          }
                                       }

                                       if ( userNames.length > 0 ) {
                                          Ext.getCmp( "group-members-grid" ).loadMask.show();
                                          SecurityController.removeUsersFromGroup( userNames, currentGroup, {
                                             callback : function( d ) {
                                                refreshGroupMembers( currentGroup );
                                             },
                                             errorHandler : function( e ) {
                                                Ext.Msg.alert( 'Sorry', e );
                                                Ext.getCmp( "group-members-grid" ).loadMask.hide();
                                             }
                                          } );
                                       } else {
                                          Ext.Msg.alert( 'No changes', "There were no changes to save." );
                                       }

                                    }

                                 }
                              }, {
                                 tooltip : "Refresh from the database",
                                 icon : Gemma.CONTEXT_PATH + "/images/icons/arrow_refresh_small.png",
                                 handler : function() {
                                    refreshGroupMembers( currentGroup );
                                 }
                              } ]
                  },
                  items : [ new Ext.grid.EditorGridPanel( {
                     id : "group-members-grid",
                     plugins : [ groupMembershipChks ],
                     height : 535,
                     loadMask : true,
                     store : new Ext.data.Store( {
                        autoLoad : false,

                        proxy : new Ext.data.DWRProxy( SecurityController.getGroupMembers ),
                        reader : new Ext.data.ListRangeReader( {
                           id : 'userName'
                        }, Ext.data.Record.create( [ {
                           name : "userName"
                        }, {
                           name : "email"
                        }, {
                           name : "inGroup",
                           type : "boolean"
                        }, {
                           name : "allowModification",
                           type : "boolean"
                        } ] ) ),
                        listeners : {
                           "exception" : function( proxy, type, action, options, response, arg ) {
                              Ext.Msg.alert( 'Sorry', response );
                           }
                        }
                     } ),
                     columns : [ {
                        header : 'User name',
                        dataIndex : 'userName',
                        sortable : true,
                        editable : false
                     }, {
                        header : 'Email',
                        dataIndex : 'email',
                        sortable : true,
                        editable : false
                     }, groupMembershipChks ],
                     viewConfig : {
                        forceFit : true
                     }
                  } )

                  ]

               }, /*
                   * DATA
                   */
               {
                  title : 'Data',
                  id : 'manager-group-data-panel',
                  region : 'east',
                  width : panelWidth * 0.5,
                  margins : '5 0 0 0',
                  cmargins : '5 5 0 0',
                  tbar : {
                     disabled : true,
                     items : [ {
                        tooltip : "Save changes",
                        icon : Gemma.CONTEXT_PATH + "/images/icons/database_save.png",
                        id : 'manager-data-panel-save-btn',
                        handler : function( b, e ) {
                           /*
                            * change R/W/P on selected data, set owner. Get just the edited records.
                            */
                           var recs = Ext.getCmp( "group-data-grid" ).getStore().getModifiedRecords();
                           if ( recs && recs[0] ) {
                              var p = [];
                              for (var i = 0; i < recs.length; i++) {
                                 /*
                                  * This is ugly. The 'owner' object gets turned into a plain string.have to reconstruct
                                  * the owner from strings
                                  */
                                 // This is the value if the owner has not been
                                 // changed in the combo box
                                 if ( recs[i].data.owner.authority ) {
                                    recs[i].data.owner = {
                                       authority : recs[i].data.owner.authority,
                                       principal : recs[i].data.owner.principal
                                    };
                                 }
                                 // this is the value if the owner has not been
                                 // changed. principal is always true as combo
                                 // only filled with principals
                                 else if ( recs[i].data.owner ) {
                                    recs[i].data.owner = {
                                       authority : recs[i].data.owner,
                                       principal : "true"
                                    };
                                 } else {
                                    Ext.Msg.alert( 'Owner can not be changed' );
                                 }
                                 p.push( recs[i].data );
                              }

                              SecurityController.updatePermissions( p, {
                                 callback : function( d ) {
                                    refreshData( currentGroup );
                                 },
                                 errorHandler : function( e ) {
                                    Ext.Msg.alert( 'Sorry', e );
                                 }
                              } );
                           }

                        }
                     }, {
                        tooltip : "Refresh from the database",
                        icon : Gemma.CONTEXT_PATH + "/images/icons/arrow_refresh_small.png",
                        handler : function() {
                           refreshData( currentGroup );
                        }
                     }, '-', {
                        xtype : 'checkbox',
                        id : "manager-data-panel-show-public",
                        tooltip : "Show/hide your public data sets.",
                        boxLabel : "Show your public data",
                        // hide by default if admin user
                        showPublicData : !Gemma.SecurityManager.isAdmin(),
                        checked : !Gemma.SecurityManager.isAdmin(),
                        handler : function( checkbox, event ) {
                           this.showPublicData = checkbox.getValue();
                           refreshData( currentGroup, !this.showPublicData );
                        }
                     } /*
                         * ,'-',{ tooltip : "Select All/None", id : "manager-data-panel-select-all", text: "Select All /
                         * None", selectAll: true, handler : function() {
                         * 
                         * selectAllChecks(this.selectAll);
                         * 
                         * //(this.selectAll)? this.setText("Select None") : this.setText("Select All"); this.selectAll =
                         * !this.selectAll; } }
                         */]
                  },
                  items : [ new Ext.grid.EditorGridPanel(
                     {
                        height : 535,
                        id : "group-data-grid",
                        loadMask : true,
                        stateful : false,
                        plugins : [ publicChks, readableChks, writeableChks ],
                        store : new Ext.data.GroupingStore( {
                           name : "data-store",
                           autoLoad : false,
                           groupField : "entityClazz",
                           sortInfo : {
                              field : 'entityName'
                           },
                           proxy : new Ext.data.DWRProxy( SecurityController.getUsersData ),
                           reader : new Ext.data.ListRangeReader( {}, Ext.data.Record.create( [ {
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
                           }, {
                              name : "allowModification",
                              type : "boolean",
                              convert : function( v, record ) {
                                 // note: just using "( record.currentUserOwns || record.currentUserCanWrite )" below
                                 // can return undefined for some reason
                                 return (record.currentUserOwns || record.currentUserCanWrite) ? true : false;
                              }
                           } ] ) ),
                           listeners : {
                              "exception" : function( proxy, type, action, options, response, arg ) {
                                 Ext.Msg.alert( 'Sorry', response );
                              }
                           }
                        } ),
                        columns : [
                                   {
                                      header : 'Type',
                                      dataIndex : 'entityClazz',
                                      groupable : true,
                                      editable : false,
                                      sortable : true,
                                      renderer : function( value, metaData, record, rowIndex, colIndex, store ) {
                                         return value.replace( /.*\./, '' ).replace( "Impl", '' ).replace( "Set",
                                            'Group' ).replace( /([A-Z])/g, ' $1' );
                                      }
                                   },
                                   {
                                      header : 'Identifier',
                                      dataIndex : 'entityShortName',
                                      editable : false,
                                      groupable : false,
                                      sortable : true,
                                      renderer : function( value, metaData, record, rowIndex, colIndex, store ) {

                                         if ( record.get( 'entityClazz' ) === "ubic.gemma.model.expression.experiment.ExpressionExperiment" ) {
                                            return '<a target="_blank" href="'
                                               + Gemma.LinkRoots.expressionExperimentPage + record.get( "entityId" )
                                               + '">' + value + '</a>';
                                         }
                                         if ( record.get( 'entityClazz' ) === "ubic.gemma.model.analysis.expression.ExpressionExperimentSet" ) {
                                            return '<a target="_blank" href="'
                                               + Gemma.LinkRoots.expressionExperimentSetPage + record.get( "entityId" )
                                               + '">' + value + '</a>';

                                         }
                                         if ( record.get( 'entityClazz' ) === "ubic.gemma.model.genome.gene.GeneSet" ) {
                                            return '<a target="_blank" href="' + Gemma.LinkRoots.geneSetPage
                                               + record.get( "entityId" ) + '">' + value + '</a>';
                                         }
                                         return value;
                                      }
                                   },
                                   {
                                      header : 'Name/Desc',
                                      dataIndex : 'entityName',
                                      editable : false,
                                      groupable : false,
                                      sortable : true
                                   },
                                   {
                                      header : 'Owner',
                                      tooltip : 'Who owns the data',
                                      dataIndex : 'owner',
                                      groupable : true,
                                      sortable : true,
                                      renderer : function( value, metaData, record, rowIndex, colIndex, store ) {
                                         return value.authority ? value.authority : value;
                                      },
                                      editor : new Ext.form.ComboBox(
                                         {
                                            typeAhead : true,
                                            displayField : "authority",
                                            triggerAction : 'all',
                                            lazyRender : true,
                                            store : new Ext.data.Store(
                                               {
                                                  proxy : new Ext.data.DWRProxy(
                                                     SecurityController.getAvailablePrincipalSids ),
                                                  reader : new Ext.data.ListRangeReader( {}, Ext.data.Record
                                                     .create( [ {
                                                        name : "authority"
                                                     }, {
                                                        name : "principal",
                                                        type : "boolean"
                                                     } ] ) ),
                                                  listeners : {
                                                     "exception" : function( proxy, type, action, options, response,
                                                        arg ) {
                                                        Ext.Msg.alert( 'Sorry', response );
                                                     }
                                                  }
                                               } )
                                         } )
                                   }, publicChks, readableChks, writeableChks ],
                        view : new Ext.grid.GroupingView( {
                           hideGroupedColumn : true,
                           forceFit : true,
                           groupTextTpl : '{text} ({[values.rs.length]} {[values.rs.length > 1 ? "Items" : "Item"]})'
                        } )
                     } )

                  ]

               } ]

         } );

   } );
