Ext.namespace( 'Gemma' );
/**
 * Grid of expression experiment groups (data set groups) with a toolbar for doing basic operations.
 * 
 *
 * @see DatasetGroupEditor
 */

/**
 * Toolbar for CUD on expressionExperimentSet. Attach to the DatasetGroupGridPanel if you want editing.
 */
Gemma.DatasetGroupEditToolbar = Ext.extend( Ext.Toolbar, {

   getCurrentSetEEIds : function() {
      return this.getCurrentSet().get( "expressionExperimentIds" );
   },

   getCurrentSet : function() {
      var sm = this.ownerCt.getSelectionModel();
      return sm.getSelected();
   },

   getCurrentSetId : function() {
      return this.getCurrentSet().get( "id" );
   },

   /**
    * @memberOf Gemma.DatasetGroupEditToolbar
    */
   getNewDetails : function() {
      if ( !this.detailsWin ) {
         this.detailsWin = new Gemma.CreateSetDetailsWindow( {
            isDisplayTaxonWithDatasets : true,
            store : this.ownerCt.getStore()
         } );
      }

      this.detailsWin.purgeListeners();

      this.detailsWin.on( "commit", function( args ) {
         var Constr = this.ownerCt.getStore().record;
         var newRec = new Constr( {
            name : args.name,
            description : args.description,
            // id : -1, // maybe not important.
            modifiable : true,
            userCanWrite : true,
            expressionExperimentIds : [],
            size : 0,
            isPublic : args.isPublic,
            taxonName : args.taxon.get( 'commonName' ),
            taxonId : args.taxon.get( 'id' )
         } );

         newRec.markDirty();

         /*
          * Select it.
          */
         this.ownerCt.getStore().add( newRec );
         this.ownerCt.getSelectionModel().selectRecords( [ newRec ] );
         this.ownerCt.getView().focusRow( this.ownerCt.getStore().indexOf( newRec ) );

         this.commitBut.enable();
         this.deleteBut.enable();
         this.cloneBut.disable();
         this.resetBut.disable();

      }, this );

      this.detailsWin.name = '';
      this.detailsWin.description = '';
      this.detailsWin.show();
   },

   /**
    * 
    */
   afterRender : function() {
      Gemma.DatasetGroupEditToolbar.superclass.afterRender.call( this );

      this.addButton( this.newBut );
      this.addButton( this.commitBut );
      this.addButton( this.cloneBut );
      this.addButton( this.resetBut );
      this.addButton( this.deleteBut );
      this.addFill();
      this.addButton( this.clearFilterBut );

   },

   /**
    * 
    */
   initComponent : function() {

      Gemma.DatasetGroupEditToolbar.superclass.initComponent.call( this );
      this.newBut = new Ext.Button( {
         handler : this.initNew,
         scope : this,
         icon : Gemma.CONTEXT_PATH + "/images/icons/add.png",
         disabled : false, // if they are logged in.
         tooltip : "Create a new set (click 'commit' when you are done)"
      } );

      this.commitBut = new Ext.Button( {
         handler : this.commit,
         disabled : true,
         scope : this,
         icon : Gemma.CONTEXT_PATH + "/images/icons/database_save.png",
         tooltip : "Commit all changes to the database"
      } );

      this.cloneBut = new Ext.Button( {
         handler : this.copy,
         scope : this,
         disabled : true,
         icon : Gemma.CONTEXT_PATH + "/images/icons/arrow_branch.png",
         tooltip : "Clone as a new set (click 'save' afterwards)"
      } );

      this.resetBut = new Ext.Button( {
         handler : this.reset,
         scope : this,
         disabled : true,
         icon : Gemma.CONTEXT_PATH + "/images/icons/arrow_undo.png",
         tooltip : "Reset selected set to stored version"
      } );

      this.deleteBut = new Ext.Button( {
         handler : this.remove,
         scope : this,
         disabled : true,
         icon : Gemma.CONTEXT_PATH + "/images/icons/database_delete.png",
         tooltip : "Delete selected set"
      } );

      this.clearFilterBut = new Ext.Button( {
         text : "Show all",
         handler : this.clearFilter,
         scope : this,
         disabled : true,
         tooltip : "Clear filters"
      } );

   },

   /**
    * 
    * @param {}
    *           ct
    * @param {}
    *           position
    */
   onRender : function( ct, position ) {
      Gemma.DatasetGroupEditToolbar.superclass.onRender.apply( this, arguments );
      // owner isn't set until rendering...
      this.ownerCt.on( 'rowselect', function( selector, rowindex, record ) {
         if ( !record.phantom ) {
            this.cloneBut.enable();
         }

         if ( record.get( 'userCanWrite' ) ) {

            if ( record.get( 'modifiable' ) ) {
               this.deleteBut.enable();
            } else {
               this.deleteBut.disable();
            }

            if ( record.isModified() ) {
               this.resetBut.enable();
               this.commitBut.enable();
            }

         } else {
            this.deleteBut.disable();
            this.commitBut.disable();
            this.resetBut.disable();
         }

      }, this );

      if ( this.ownerCt.getStore().isFiltered() ) {
         this.clearFilterBut.enable();
      } else {
         this.clearFilterBut.disable();
      }

      this.ownerCt.getStore().on( 'update', function( store, record, operation ) {
         if ( store.isFiltered() ) {
            this.clearFilterBut.enable();
         } else {
            this.clearFilterBut.disable();
         }

         if ( this.getCurrentSet && this.getCurrentSet() && this.getCurrentSet().dirty ) {
            this.cloneBut.enable();
            this.resetBut.enable();
            this.commitBut.enable();
         }
      }, this );

      this.ownerCt.on( 'afteredit', function( e ) {
         this.resetBut.enable();
         this.commitBut.enable();
      }, this );

      this.ownerCt.getStore().on( 'write', function( store, action, data, records, options ) {
         this.ownerCt.loadMask.hide();
         this.commitBut.disable();
      }, this );

      this.ownerCt.getStore().on( 'exception', function( proxy, type, action, options, response, arg ) {
         this.ownerCt.loadMask.hide();
      }, this );

   },

   /**
    * Handler
    */
   initNew : function() {
      this.getNewDetails();
   },

   /**
    * Handler. Remove a dataset group. If it is persistent, you need to have permission to do this.
    */
   remove : function() {
      var rec = this.getCurrentSet();
      if ( rec ) {
         Ext.Msg.confirm( Gemma.HelpText.CommonWarnings.Deletion.title, String.format(
            Gemma.HelpText.CommonWarnings.Deletion.text, 'set' ), function( but ) {

            if ( but === 'no' ) {
               return;
            }

            this.ownerCt.loadMask.show();

            if ( rec.phantom ) {
               // nonpersistent, go ahead.
               this.ownerCt.getStore().remove( rec );
               this.ownerCt.getStore().clearSelected();
               this.resetBut.disable();
               this.deleteBut.disable();
               this.commitBut.disable();
               this.ownerCt.loadMask.hide();
            } else {
               var callback = function( data ) {
                  if ( data ) {
                     this.ownerCt.getStore().remove( rec );
                     this.ownerCt.getStore().clearSelected();
                     this.resetBut.disable();
                     this.deleteBut.disable();
                     this.commitBut.disable();
                     this.ownerCt.loadMask.hide();
                  }
               }.createDelegate( this );
               ExpressionExperimentSetController.remove( [ rec.data ], callback );
            }
            this.fireEvent( "delete-set", rec );
         }, this );
      }
   },

   /**
    * Handler.
    */
   clearFilter : function() {
      this.ownerCt.getStore().clearFilter();
   },

   /**
    * Handler.
    */
   commit : function() {
      this.ownerCt.loadMask.show();
      var recordsToSave = this.ownerCt.getStore().getModifiedRecords();
      var i, rec;
      for (i = 0; recordsToSave.length > i; i++) {
         rec = recordsToSave[i];
         if ( !rec.get( "expressionExperimentIds" ) || rec.get( "expressionExperimentIds" ).length === 0 ) {
            Ext.Msg.show( {
               title : Gemma.HelpText.CommonErrors.EmptySet.title,
               msg : String.format( Gemma.HelpText.CommonErrors.EmptySet.text, rec.get( "name" ) ),
               buttons : Ext.Msg.OK,
               icon : Ext.MessageBox.WARNING
            } );
            this.ownerCt.loadMask.hide();
            return;
         }
      }
      this.ownerCt.getStore().save();
   },

   /**
    * Handler. Clone (copy) an existing EESet
    */
   copy : function() {
      var rec = this.getCurrentSet();
      var Constr = this.ownerCt.getStore().record;
      var newRec = new Constr( {
         name : "Copy of " + rec.get( "name" ), // indicate
         // they
         // should
         // edit it.
         description : rec.get( "description" ),
         modifiable : true,
         expressionExperimentIds : rec.get( "expressionExperimentIds" ),
         size : rec.get( "size" ),
         taxonId : rec.get( "taxonId" ),
         taxonName : rec.get( "taxonName" )
      } );

      this.ownerCt.getStore().add( newRec );
      this.ownerCt.getSelectionModel().selectRecords( [ newRec ] );
      this.ownerCt.getView().focusRow( this.ownerCt.getStore().indexOf( newRec ) );

      this.deleteBut.enable();
      this.commitBut.enable();
      this.resetBut.disable();
      this.cloneBut.disable(); // until we change it.
   },

   /**
    * Handler. Reset the currently selected EESet
    */
   reset : function() {
      if ( this.getCurrentSet() ) {
         this.getCurrentSet().reject();
         this.resetBut.disable();
         this.commitBut.disable();
         this.cloneBut.enable();
         this.ownerCt.getSelectionModel().fireEvent( "rowselect", this.ownerCt.getSelectionModel(),
            this.ownerCt.getStore().indexOf( this.getCurrentSet() ), this.getCurrentSet() );
      }
   }

} );

/**
 * Basic grid editor for displaying DatasetGroups (EESets).
 * 
 * @class Gemma.DatasetGroupGridPanel
 * @extends Ext.grid.GridPanel
 */
Gemma.DatasetGroupGridPanel = Ext
   .extend(
      Ext.grid.EditorGridPanel,
      {

         autoExpandColumn : 'description',

         selModel : new Ext.grid.RowSelectionModel( {
            singleSelect : true
         } ),

         name : 'datasetGroupGridPanel',
         width : 400,
         height : 250,

         stripeRows : true,
         viewConfig : {
            forceFit : true
         },
         colModel : new Ext.grid.ColumnModel(
            {

               columns : [
                          {
                             header : "Name",
                             dataIndex : "name",
                             tooltip : 'The unique name of this group',
                             sortable : true,
                             editable : true,
                             width : 0.17,
                             editor : new Ext.form.TextField( {
                                allowBlank : false
                             } ),
                             renderer : function( value, metadata, record, rowIndex, colIndex, store ) {
                                if ( record.data.id && record.data.id > 0 ) {
                                   return '<a target="_blank" href="' + Gemma.CONTEXT_PATH + '/expressionExperimentSet/showExpressionExperimentSet.html?id='
                                      + record.data.id + '">' + value + '</a>';
                                }
                                return value;
                             }
                          },
                          {
                             header : "Description",
                             dataIndex : "description",
                             sortable : true,
                             editable : true,
                             width : 0.3,
                             editor : new Ext.form.TextField( {
                                allowBlank : true
                             } )
                          },
                          {
                             header : "Size",
                             tooltip : 'How many datasets make up this group',
                             dataIndex : "size",
                             sortable : true,
                             editable : false,
                             // width : 60,
                             width : 0.09
                          // fixed : true
                          },
                          {
                             header : "Taxon",
                             dataIndex : "taxonName",
                             sortable : true,
                             editable : false,
                             // width : 100
                             width : 0.125
                          },
                          {
                             header : "Flags",
                             dataIndex : "modifiable",
                             sortable : true,
                             editable : false,
                             tooltip : 'Status including security',
                             // width : 60,
                             width : 0.12,
                             renderer : function( value, metaData, record, rowIndex, colIndex, store ) {
                                var v = "";
                                if ( !value ) {
                                   v = "<img src='" + Gemma.CONTEXT_PATH + "/images/icons/shield.png' height='16' width='16' "
                                      + "ext:qtip='" + Gemma.HelpText.WidgetDefaults.DatasetGroupGridPanel.protectedTT
                                      + "' />";
                                }
                                var canEdit = (record.get( 'id' ) && record.get( 'id' ) > 0) ? record
                                   .get( 'userCanWrite' ) : false;

                                var isCurrentOwner = (record.get( 'id' ) && record.get( 'id' ) > 0) ? record
                                   .get( 'userOwned' ) : false;

                                var sl = Gemma.SecurityManager.getSecurityLink(
                                   "ubic.gemma.model.analysis.expression.ExpressionExperimentSet", record
                                      .get( 'id' ), record.get( 'isPublic' ), record.get( 'isShared' ), canEdit, null,
                                   null, null, isCurrentOwner );

                                v = v + "&nbsp;" + sl;
                                return v;
                             },
                             // fixed : true,
                             scope : this
                          },
                          {
                             header : "Editable",
                             dataIndex : "userCanWrite",
                             sortable : true,
                             editable : false,
                             tooltip : 'Do you have permission to edit this group?',
                             // width : 60,
                             width : 0.12,
                             renderer : function( value, metaData, record, rowIndex, colIndex, store ) {
                                if ( value ) {
                                   return "<img src='" + Gemma.CONTEXT_PATH + "/images/icons/ok.png' height='16' width='16' ext:qtip='You can edit this group' />";
                                } else {
                                   return " ";
                                }
                             },
                             // fixed : true,
                             scope : this
                          } ]
            } ),

         /**
          * Override
          */
         initComponent : function() {

            Gemma.DatasetGroupGridPanel.superclass.initComponent.call( this );
            if ( !this.store ) {
               Ext.apply( this, {
                  store : new Gemma.DatasetGroupStore()
               } );
            }

            this.addEvents( {
               'dirty' : true
            } );

            this.record = this.getStore().record;

         },

         afterRender : function() {

            Gemma.DatasetGroupGridPanel.superclass.afterRender.call( this );

            this.loadMask = new Ext.LoadMask( this.body, {
               msg : 'Loading ...',
               store : this.store
            } );

            // these don't seem to work!
            // this.relayEvents(this.getSelectionModel(), 'rowselect');
            // this.relayEvents(this.getStore(), 'datachanged');

            /*
             * this.getStore().on('datachanged', function( store ){ this.fireEvent('datachanged', store); // causes
             * recursive loop });
             */
            this.getSelectionModel().on( "rowselect", function( selmol, index, rec ) {
               this.getStore().setSelected( rec );
               this.fireEvent( 'rowselect', selmol, index, rec );
            }, this );
         },

         /**
          * Called by outside when adding members.
          * 
          * @param {}
          *           store
          */
         updateMembers : function( store ) {
            var rec = this.getSelectionModel().getSelected();

            if ( !rec ) {
               // if no EEset is currently selected.
               Ext.Msg.alert( "Sorry", "You must select a set or create a new set before adding experiments.",
                  function() {
                     store.un( "remove", this.updateMembers );
                     store.un( "add", this.updateMembers );
                     store.removeAll();
                  } );
               return;
            }

            var ids = [];
            store.each( function( rec ) {
               ids.push( rec.get( "id" ) );
            } );
            rec.set( "expressionExperimentIds", ids );
            rec.set( "size", ids.length );

            this.fireEvent( "dirty", rec );
         }

      } );
