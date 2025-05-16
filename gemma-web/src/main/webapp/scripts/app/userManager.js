/**
 * User management tool.
 *
 * @author keshav
 *
 */
Ext.namespace( 'Gemma' );
Ext.BLANK_IMAGE_URL = Gemma.CONTEXT_PATH + '/images/default/s.gif';

Gemma.USER_PAGE_SIZE = 10;

Ext.onReady( function() {
   Ext.QuickTips.init();
   Ext.state.Manager.setProvider( new Ext.state.CookieProvider() );

   // A simple test store using canned "data" and an array "reader".
   // var store = new Ext.data.Store({
   // data : [[1, "mjudge", "Judge", "foo@foo.com", true],
   // [2, "mjudge", "Judge", "foo@foo.com", true],
   // [3, "mjudge", "Judge", "foo@foo.com", true],
   // [4, "mjudge", "Judge", "foo@foo.com", false]],
   //
   // reader : new Ext.data.ArrayReader({
   // id : 'id'
   // }, ['id', 'userName', 'lastName', 'email', 'enabled'])
   //
   // });

   var record = Ext.data.Record.create( [ {
      name : "id",
      type : "int"
   }, {
      name : "userName",
      type : "string"
   }, {
      name : "email",
      type : "string"
   }, {
      name : "groups",
      type : "array"
   }, {
      name : "enabled",
      type : "boolean"
   } ] );

   var userStore = new Ext.data.Store( {
      proxy : new Ext.data.DWRProxy( UserListController.getUsers ),
      reader : new Ext.data.ListRangeReader( {
         id : 'id'
      }, record ),
      remoteSort : false
   } );
   userStore.load();

   /* create editors for the grid */
   var userNameEdit = new Ext.form.TextField( {
      allowBlank : false
   } );

   var emailEdit = new Ext.form.TextField( {
      allowBlank : false,
      vtype : 'email'
   } );

   var checkColumn = new Ext.ux.grid.CheckColumn( {
      header : "Enabled?",
      dataIndex : 'enabled',
      width : 55
   } );

   var groupsEdit = new Ext.form.ComboBox( {
      typeAhead : true,
      lazyRender : true,
      triggerAction : 'all',
      mode : 'local',
      selectOnFocus : true,
      displayField : 'groups',

      // the groups (combo box) editor needs a store
      store : new Ext.data.Store( {
         data : [ [ 1, "Administrators" ], [ 2, "Users" ] ],

         reader : new Ext.data.ArrayReader( {
            id : 'id'
         }, [ 'id', 'groups' ] )

      } ),
      valueField : 'groups'
   } );

   var userGrid = new Ext.grid.EditorGridPanel( {

      renderTo : "userList",
      title : "User Management",
      frame : true,
      height : 300,
      width : 900,
      stripeRows : true,
      clicksToEdit : 2,
      plugins : checkColumn,
      loadMask : true,
      autoScroll : true,
      store : userStore,

      tbar : [

      {
         text : 'Add',
         tooltip : 'Add a new user',
         disabled : true,
         icon : Gemma.CONTEXT_PATH + '/images/icons/add.png',
         cls : 'x-btn-text-icon',
         handler : function() {
            userGrid.getStore().insert( 0, new record( {
               userName : 'New User',
               email : '',
               groups: 'Select Groups',
               enabled : true
            } ) );
            userGrid.startEditing( 0, 0 );
         }
      },

      {
         text : 'Remove',
         disabled : true,
         tooltip : 'Remove selected user',
         icon : Gemma.CONTEXT_PATH + '/images/icons/delete.png',
         cls : 'x-btn-text-icon',
         handler : function() {
            var sm = userGrid.getSelectionModel();
            var sel = sm.getSelected();
            if ( sm.hasSelection() ) {
               Ext.Msg.show( {
                  title : 'Remove User',
                  buttons : Ext.MessageBox.YESNO,
                  msg : 'Remove ' + sel.data.userName + '?',
                  fn : function( btn ) {
                     if ( btn == 'yes' ) {
                        userGrid.getStore().remove( sel );
                     }
                  }
               } );
            }
         }
      },

      {
         text : 'Save',
         tooltip : 'Save the selected user',
         icon : Gemma.CONTEXT_PATH + "/images/icons/database_save.png",
         cls : 'x-btn-text-icon',
         handler : function() {
            var sm = userGrid.getSelectionModel();
            var sel = sm.getSelected();
            if ( sm.hasSelection() ) {
               Ext.Msg.show( {
                  title : 'Save User',
                  buttons : Ext.MessageBox.YESNO,
                  msg : 'Save ' + sel.data.userName + '?',
                  fn : function( btn ) {
                     if ( btn == 'yes' ) {
                        UserListController.saveUser( {
                           userName : sel.data.userName,
                           email : sel.data.email,
                           enabled : sel.data.enabled,
                           groups: [sel.data.groups]
                        }, {
                           callback : function() {
                              userGrid.getStore().reload();
                           }
                        } );
                     }
                  }
               } );
            }
         }
      }

      ],

      cm : new Ext.grid.ColumnModel( [ {
         header : "Username",
         dataIndex : 'userName',
         editor : userNameEdit,
         sortable : true
      }, {
         header : "Email",
         dataIndex : 'email',
         editor : emailEdit
      }, {
         header : "Groups",
         dataIndex : 'groups',
         editor : groupsEdit
      }, checkColumn ] ),

      viewConfig : {
         forceFit : true
      },

      sm : new Ext.grid.RowSelectionModel( {
         singleSelect : true
      } )

   } );
} );