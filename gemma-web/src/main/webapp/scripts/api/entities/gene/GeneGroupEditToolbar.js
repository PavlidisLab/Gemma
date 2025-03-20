/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 * 
 */

Ext.namespace( 'Gemma' );

/**
 * Toolbar for the GeneGroupPanel
 * 
 * @class Gemma.GeneGroupEditToolbar
 * @extends Ext.Toolbar
 */
Gemma.GeneGroupEditToolbar = Ext.extend( Ext.Toolbar, {

   getCurrentSetGeneIds : function() {
      return this.getCurrentSet().get( "geneIds" );
   },

   getCurrentSet : function() {
      var sm = this.ownerCt.getSelectionModel();
      return sm.getSelected();
   },

   getCurrentSetId : function() {
      return this.getCurrentSet().get( "id" );
   },

   /**
    * Create a new one.
    * 
    * @memberOf Gemma.GeneGroupEditToolbar
    */
   getNewDetails : function() {
      if ( !this.detailsWin ) {
         this.detailsWin = new Gemma.CreateSetDetailsWindow( {
            store : this.ownerCt.getStore()
         } );
      }

      this.detailsWin.purgeListeners();

      this.detailsWin.on( "commit", function( args ) {
         var constr = this.ownerCt.getStore().record;
         var newRec = new constr( {
            name : args.name,
            description : args.description,
            taxonId : args.taxon.data.id,
            taxonName : args.taxon.data.commonName,
            isPublic : args.isPublic,
            // id : -1, // maybe not important.
            userCanWrite : true,
            geneIds : [],
            size : 0
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
      Gemma.GeneGroupEditToolbar.superclass.afterRender.call( this );

      this.addButton( this.newBut );
      this.addButton( this.commitBut );
      this.addButton( this.cloneBut );
      this.addButton( this.resetBut );
      this.addButton( this.deleteBut );
      this.addButton( this.publicOrPrivateBut );
      this.addFill();
      // this.addButton(this.clearFilterBut);

   },

   /**
    * 
    */
   initComponent : function() {

      Gemma.GeneGroupEditToolbar.superclass.initComponent.call( this );
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

      this.publicOrPrivateBut = new Ext.Button( {
         tooltip : "Show/hide public data",
         enableToggle : true,
         icon : Gemma.CONTEXT_PATH + "/images/icons/world_add.png",
         handler : this.refreshData,
         pressed : true, // has to match default value in store.
         scope : this
      } );

      // this.clearFilterBut = new Ext.Button({
      // text : "Show all",
      // handler : this.clearFilter,
      // scope : this,
      // disabled : true,
      // tooltip : "Clear filters"
      // });

   },

   /**
    * 
    * @param {}
    *           ct
    * @param {}
    *           position
    */
   onRender : function( ct, position ) {
      Gemma.GeneGroupEditToolbar.superclass.onRender.apply( this, arguments );

      // owner isn't set until rendering...
      this.ownerCt.on( 'rowselect', function( selector, rowindex, record ) {

         if ( !record.phantom ) {
            this.cloneBut.enable();
         }

         if ( record.get( 'userCanWrite' ) ) {
            this.deleteBut.enable();

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

      // if (this.ownerCt.getStore().isFiltered()) {
      // this.clearFilterBut.enable();
      // } else {
      // this.clearFilterBut.disable();
      // }

      this.ownerCt.getStore().on( 'update', function( store, record, operation ) {
         // if (store.isFiltered()) {
         // this.clearFilterBut.enable();
         // } else {
         // this.clearFilterBut.disable();
         // }

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
    * Handler. Remove a group. If it is persistent, you need to have permission to do this.
    */
   remove : function() {
      var rec = this.getCurrentSet();
      if ( rec ) {
         Ext.Msg.confirm( Gemma.HelpText.CommonWarnings.Deletion.title, String.format(
            Gemma.HelpText.CommonWarnings.Deletion.text, "set" ), function( but ) {
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
               GeneSetController.remove( [ rec.data ], callback );
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
    * Handler
    */
   refreshData : function() {
      var showPrivateOnly = !this.publicOrPrivateBut.pressed;
      if ( this.ownerCt.getStore().getModifiedRecords().length > 0 ) {
         Ext.Msg.show( {
            title : 'Are you sure?',
            msg : 'You have unsaved changes which will be lost if you change modes.',
            buttons : Ext.Msg.YESNO,
            fn : function( btn, text ) {
               if ( btn === 'yes' ) {

                  this.ownerCt.getStore().load( {
                     params : [ showPrivateOnly, null ]
                  } );
               }
            },
            scope : this
         } );
      } else {
         this.ownerCt.getStore().load( {
            params : [ showPrivateOnly, null ]
         } );
      }
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
         if ( !rec.get( "geneIds" ) || rec.get( "geneIds" ).length === 0 ) {
            Ext.Msg.show( {
               title : 'Cannot save ' + rec.get( "name" ),
               msg : 'You cannot save an empty set. No changes have been saved.' + ' Add genes to set "'
                  + rec.get( "name" ) + '" or delete it.',
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
      var constr = this.ownerCt.getStore().record;
      var newRec = new constr( {
         name : "Copy of " + rec.get( "name" ), // indicate they should edit it.
         description : rec.get( "description" ),
         size : rec.get( "size" ),
         geneIds : rec.get( "geneIds" ),
         userCanWrite : true
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
    * Handler. Reset the currently selected Set
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