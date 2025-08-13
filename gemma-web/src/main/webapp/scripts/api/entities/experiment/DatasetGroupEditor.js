/**
 * Main user interface for viewing, creating and editing DatasetGroups. Three panes.
 *
 * At the top, the available ExpressionExperimentSets are shown. At the bottom left, searching for experiments; at the
 * bottom right, the experiments that are in the set. If the user is authenticated, they can save new DatasetGroups to
 * the database.
 *
 * @author Paul
 *
 *
 *
 *
 * @class Gemma.DatasetGroupEditor
 * @extends Ext.Panel
 */

Gemma.DatasetGroupEditor = Ext.extend( Ext.Panel, {

   id : 'dataset-chooser',
   name : 'datasetchooser',
   layout : 'border',
   title : "Dataset Group Editor <a href='javascript:void()' style='float:right' title=''"
      + Gemma.HelpText.WidgetDefaults.DatasetGroupEditor.widgetTT + "' " + "onClick='window.open('"
      + Gemma.HelpText.WidgetDefaults.DatasetGroupEditor.helpURL + "', 'DataSetChooserHelp');'>"
      + "<img src='" + Gemma.CONTEXT_PATH + "/images/icons/question_blue.png'></a>",
   isLoggedIn : false,

   /**
    * @memberOf Gemma.DatasetGroupEditor
    */
   initComponent : function() {

      /*
       * Ext.apply(this, { buttons : [{ id : 'done-selecting-button', text : "Done", handler : this.onCommit, scope :
       * this }, { id : 'help-selecting-button', text : "Help", handler : this.onHelp, scope : this }] });
       */

      Gemma.DatasetGroupEditor.superclass.initComponent.call( this );

      var userLoggedIn = (Ext.get( 'hasUser' ) && Ext.get( 'hasUser' ).getValue() === 'true') ? true : false;
      this.isLoggedIn = Ext.get( 'loggedIn' ).getValue() || userLoggedIn;

      /**
       * Plain grid for displaying datasets in the current set. Editable.
       */
      this.datasetGroupMembersGrid = new Gemma.ExpressionExperimentGrid( {
         isLoggedIn : this.isLoggedIn,
         region : 'east',
         title : "Datasets in current set",
         showAnalysisInfo : false,
         loadMask : {
            msg : 'Loading datasets ...'
         },
         tbar : [ '->', {
            text : "Delete selected",
            icon : Gemma.CONTEXT_PATH + "/images/icons/cross.png",
            handler : this.removeSelectedFromdatasetGroupMembersGrid,
            scope : this
         } ],
         split : true,
         width : 400,
         height : 200,
         experimentNameAsLink : false
      // because will interfere with row selection
      } );

      /*
       * Space to show the details about the currently selected expression experiment
       */
      this.dataSetDetailsPanel = new Ext.Panel( {
         region : 'south',
         split : true,
         bodyStyle : 'padding:8px',
         height : 200
      } );

      /**
       * Datasets that can be added to the current set.
       */
      this.sourceDatasetsGrid = new Gemma.ExpressionExperimentGrid( {
         editable : false,
         isLoggedIn : this.isLoggedIn,
         title : "Dataset locator",
         region : 'center',
         split : true,
         width : 400,
         showAnalysisInfo : true,
         loadMask : {
            msg : 'Searching ...'
         },
         tbar : new Gemma.DataSetSearchAndGrabToolbar( {
            taxonSearch : true,
            targetGrid : this.datasetGroupMembersGrid

         } )

      } );

      /*
       * Top grid for showing the datasetGroups
       */
      this.datasetGroupGrid = new Gemma.DatasetGroupGridPanel( {
         region : 'west',
         layout : 'fit',
         split : true,
         collapsible : true,
         collapseMode : 'mini',
         width : 400,
         height : 200,
         title : "Available dataset groups",
         displayGrid : this.datasetGroupMembersGrid,
         tbar : new Gemma.DatasetGroupEditToolbar()
      } );
      if ( this.datasetGroupStore ) {
         Ext.apply( this.datasetGroupGrid, {
            store : this.datasetGroupStore
         } );
      }
      this.datasetGroupGrid.on( 'beforeload', function() {
         // /console.log('beforeload');
      } );
      this.datasetGroupGrid.on( 'load', function() {
         // console.log('doneload');
      } );

      this.add( this.datasetGroupGrid );
      this.add( this.datasetGroupMembersGrid );
      this.add( this.sourceDatasetsGrid );
      this.add( this.dataSetDetailsPanel );

      /*
       * filter so we only see data sets that are NOT in the GroupMembersGrid - Remove datasets that are in the other
       * grid.
       */
      this.sourceDatasetsGrid.getStore().on(
         'load',
         function( idsFound ) {
            this.sourceDatasetsGrid.getStore().filterBy( function( record, id ) {
               var rid = record.get( 'id' );
               return this.datasetGroupMembersGrid.getStore().find( 'id', rid ) < 0;
            }, this );

            this.sourceDatasetsGrid.setTitle( this.sourceDatasetsGrid.title + ", "
               + this.sourceDatasetsGrid.getStore().getCount() + " addable" );

         }.createDelegate( this ) );

      this.datasetGroupGrid.getTopToolbar().on( "delete-set", function( rec ) {
         this.clearDisplay();
         this.fireEvent( 'delete-set' );
      }.createDelegate( this ) );

      this.datasetGroupMembersGrid.getStore().on( 'remove', function( store, record, index ) {
         this.dirtySet( store );
      }, this );
      this.datasetGroupMembersGrid.getStore().on( 'add', function( store, records, index ) {
         this.dirtySet( store );
      }, this );

      this.datasetGroupGrid.getSelectionModel().on( 'rowselect', function( model, rowindex, record ) {
         this.display( record );
      }, this );

      this.sourceDatasetsGrid.getSelectionModel().on( 'rowselect', this.showEEDetails, this, {
         buffer : 100
      // keep from firing too many times at once
      } );
      this.datasetGroupMembersGrid.getSelectionModel().on( 'rowselect', this.showEEDetails, this, {
         buffer : 100
      // keep from firing too many times at once
      } );
      /*
       * this.on('show', function(panel) { var r = this.datasetGroupGrid.getStore().getSelected(); if (r) {
       * this.datasetGroupGrid.getSelectionModel().selectRecords([r], false);
       * this.datasetGroupGrid.getView().focusRow(this.datasetGroupGrid.getStore().indexOf(r)); } }, this, { delay :
       * 100, single : true });
       */
      this.addEvents( {
         "select" : true,
         "commit" : true,
         'delete-set' : true
      } );

   },

   showEEDetails : function( model, rowindex, record ) {

      if ( typeof this.detailsmask == 'undefined' || this.detailsmask === null ) {
         this.detailsmask = new Ext.LoadMask( this.dataSetDetailsPanel.body, {
            msg : "Loading details ..."
         } );
      }

      this.detailsmask.show();
      ExpressionExperimentController.getDescription( record.id, {
         callback : function( data ) {
            Ext.DomHelper.overwrite( this.dataSetDetailsPanel.body,
               '<h1><a href="' + Gemma.CONTEXT_PATH + '/expressionExperiment/showExpressionExperiment.html?id=' + record.id + '">'
                  + record.get( 'shortName' ) + "</a></h1><h2>" + record.get( 'name' ) + "</h2><p>" + data + "</p>" );
            this.detailsmask.hide();
         }.createDelegate( this )
      } );

   },

   dirtySet : function( store ) {
      store.clearFilter( false );
      // collect current ids from store
      var currentIds = [];
      store.each( function( r ) {
         currentIds.push( r.get( 'id' ) );
      }, this );

      var rec = this.datasetGroupGrid.getSelectionModel().getSelected();

      if ( rec ) {
         rec.set( 'expressionExperimentIds', currentIds );
         rec.set( 'numExperiments', currentIds.length );
      }
   },

   /**
    * Show the selected eeset members in the lower right-hand grid, if it exists
    */

   display : function( record ) {

      if ( !record ) {
         return;
      }
      if ( this.datasetGroupMembersGrid ) {
         this.datasetGroupMembersGrid.getStore().removeAll();
         this.datasetGroupMembersGrid.setTitle( record.get( "name" ) );

         if ( record.get( 'expressionExperimentIds' ).length === 0 && record.get( 'id' ) ) {
            /*
             * Fetch the ids.
             */
            ExpressionExperimentSetController.getExperimentIdsInSet( record.get( 'id' ), {
               callback : function( ids ) {
                  if ( !ids || ids.length === 0 ) {
                     return;
                  }
                  record.set( 'expressionExperimentIds', ids );
                  this.datasetGroupMembersGrid.getStore().load( {
                     params : [ ids ]
                  } );

               }.createDelegate( this )
            } );
         } else {

            this.datasetGroupMembersGrid.getStore().load( {
               params : [ record.get( "expressionExperimentIds" ) ]
            } );

         }
      }

      // Set up the taxon displayed.
      Gemma.EVENTBUS.fireEvent( 'taxonchanged', record.get( 'taxonId' ) ); // OK?

      if ( this.sourceDatasetsGrid ) {
         this.sourceDatasetsGrid.getTopToolbar().setTaxon( record.get( "taxonId" ) ); // get rid of this?
         this.sourceDatasetsGrid.getTopToolbar().taxonCombo.disable();
      }
   },

   /**
    * Clear the lower right grid
    */
   clearDisplay : function() {
      this.datasetGroupMembersGrid.getStore().removeAll();
      this.datasetGroupMembersGrid.setTitle( 'Set members' );
      this.sourceDatasetsGrid.getTopToolbar().taxonCombo.enable();
   },

   removeSelectedFromdatasetGroupMembersGrid : function() {
      this.datasetGroupMembersGrid.removeSelected();
   },

   /**
    * When a edit is completed and we're closing the window.
    */
   onCommit : function() {
      var rec = this.datasetGroupGrid.getStore().getSelected();

      /*
       * If any are dirty, and if any of the modified records are saveable by this user, then prompt for save.
       */
      var numModified = this.datasetGroupStore.getModifiedRecords().length;

      var canSave = this.isLoggedIn;
      for (var i = 0; i < numModified; i++) {
         var r = this.datasetGroupStore.getModifiedRecords()[i];
         if ( r.get( 'userCanWrite' ) ) {
            canSave = true;
            break;
         }
      }

      if ( numModified > 0 && canSave ) {
         Ext.Msg.show( {
            animEl : this.getEl(),
            title : 'Save Changes?',
            msg : 'You have unsaved changes. Would you like to save them?',
            buttons : {
               ok : 'Yes',
               cancel : 'No'
            },
            fn : function( btn, text ) {
               if ( btn === 'ok' ) {
                  this.datasetGroupStore.commitChanges();
               }
               if ( rec ) {
                  this.datasetGroupStore.setSelected( rec );
                  this.fireEvent( "select", rec );
                  this.fireEvent( "commit", rec );
               }
               this.hide();
            }.createDelegate( this ),
            scope : this,
            icon : Ext.MessageBox.QUESTION
         } );
      } else {
         this.hide();
         if ( rec ) {
            this.datasetGroupStore.setSelected( rec );
            this.fireEvent( "select", rec );
         }
         this.fireEvent( "commit", rec );
      }

   },

   onHelp : function() {
      window.open( Gemma.HelpText.WidgetDefaults.DatasetGroupEditor.helpURL, 'DataSetChooserHelp' );
   }

} );
