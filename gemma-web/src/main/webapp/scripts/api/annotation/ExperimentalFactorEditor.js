Ext.namespace( 'Gemma' );

/**
 * Display experimental factors, allow editing.
 *
 * @class Gemma.ExperimentalFactorGrid
 * @extends Gemma.GemmaGridPanel
 */
Gemma.ExperimentalFactorGrid = Ext.extend( Gemma.GemmaGridPanel, {

   loadMask : true,

   record : Ext.data.Record.create( [ {
      name : "id",
      type : "int"
   }, {
      name : "name",
      type : "string"
   }, {
      name : "description",
      type : "string"
   }, {
      name : "category",
      type : "string"
   }, {
      name : "categoryUri",
      type : "string"
   }, {
      name : 'type',
      type : 'string'
   } ] ),

   /**
    * @memberOf Gemma.ExperimentalFactorGrid
    */
   categoryStyler : function( value, metadata, record, row, col, ds ) {
      return Gemma.GemmaGridPanel.formatTermWithStyle( value, record.get( "categoryUri" ) );
   },

   initComponent : function() {

      this.experimentalDesign = {
         id : this.edId,
         classDelegatingFor : "ExperimentalDesign"
      };

      var cols = [ {
         header : "Name",
         dataIndex : "name",
         sortable : true
      }, {
         header : "Category",
         dataIndex : "category",
         renderer : this.categoryStyler,
         sortable : true

      }, {
         header : "Description",
         dataIndex : "description",
         sortable : true
      }, {
         header : "Type",
         dataIndex : 'type'
      } ];

      if ( Ext.get( "hasAdmin" ).getValue() === 'true' ) {
         cols.push( {
            header : "Factor ID",
            dataIndex : 'id'
         } )
      }

      Ext.apply( this, {
         columns : cols
      } );

      this.store = new Ext.data.Store( {
         proxy : new Ext.data.DWRProxy( ExperimentalDesignController.getExperimentalFactors ),
         reader : new Ext.data.ListRangeReader( {
            id : "id"
         }, this.record )
      } );

      if ( this.editable ) {
         this.tbar = new Gemma.ExperimentalFactorToolbar( {
            store : this.store,
            experimentalDesignId : this.edId
            // used for validation.
         } );
      }

      Gemma.ExperimentalFactorGrid.superclass.initComponent.call( this );

      this.addEvents( 'experimentalfactorchange', 'experimentalfactorselected' );

      this.getSelectionModel().on( "selectionchange", function( model ) {
         var selected = model.getSelections();
         if ( selected.length == 1 ) {
            this.fireEvent( "experimentalfactorselected", selected[0].data );
         }

      }.createDelegate( this ) );

      this.store.load( {
         params : [ this.experimentalDesign ]
      } );

   },

   onRender : function( c, l ) {

      Gemma.ExperimentalFactorGrid.superclass.onRender.call( this, c, l );

      var NAME_COLUMN = 0;
      var CATEGORY_COLUMN = 1;
      var DESCRIPTION_COLUMN = 2;
      var TYPE_COLUMN = 3;

      this.autoExpandColumn = DESCRIPTION_COLUMN;

      this.nameField = new Ext.form.TextField( {} );
      var nameEditor = new Ext.grid.GridEditor( this.nameField );

      this.categoryCombo = new Gemma.CategoryCombo( {
         lazyRender : true,
         termKey : "factor"
      } );

      this.descriptionField = new Ext.form.TextField( {
         allowBlank : false
      } );

      if ( this.editable ) {

         var categoryEditor = new Ext.grid.GridEditor( this.categoryCombo );
         this.categoryCombo.on( "select", function( combo, record, index ) {
            categoryEditor.completeEdit();
         } );

         var descriptionEditor = new Ext.grid.GridEditor( this.descriptionField );

         this.factorTypeCombo = new Ext.form.ComboBox( {
            triggerAction : 'all',
            lazyRender : true,
            width : 120,
            mode : 'local',
            store : new Ext.data.ArrayStore( {
               id : 0,
               fields : [ 'type', 'display' ],
               data : [ [ 'continuous', 'continuous' ], [ 'categorical', 'categorical' ] ]
            } ),
            valueField : 'type',
            displayField : 'display'
         } );

         var typeEditor = new Ext.grid.GridEditor( this.factorTypeCombo );

         this.factorTypeCombo.on( "select", function( combo, record, index ) {
            typeEditor.completeEdit();
         } );

         this.getColumnModel().setEditor( NAME_COLUMN, nameEditor );
         this.getColumnModel().setEditor( CATEGORY_COLUMN, categoryEditor );
         this.getColumnModel().setEditor( DESCRIPTION_COLUMN, descriptionEditor );

         this.getColumnModel().setEditor( TYPE_COLUMN, typeEditor );

         this.getTopToolbar().on( "create", function( newFactorValue ) {
            var oldmsg = this.loadMask.msg;
            this.loadMask.msg = String.format( Gemma.StatusText.creating, 'experimental factor' );
            this.loadMask.show();

            var callback = function() {
               this.factorCreated( newFactorValue );
               this.loadMask.hide();
               this.loadMask.msg = oldmsg;
            }.createDelegate( this );

            var errorHandler = function( er ) {
               this.loadMask.hide();
               this.loadMask.msg = oldmsg;
               this.refresh();
               Ext.Msg.alert( "Error", er );

            }.createDelegate( this );

            ExperimentalDesignController.createExperimentalFactor( this.experimentalDesign, newFactorValue, {
               callback : callback,
               errorHandler : errorHandler
            } );
         }.createDelegate( this ) );

         this.getTopToolbar().on( "delete", function() {
            var selected = this.getSelectedIds();
            var oldmsg = this.loadMask.msg;
            this.loadMask.msg = String.format( Gemma.StatusText.deletingSpecific, 'experimental factor' );
            this.loadMask.show();

            var callback = function() {
               this.idsDeleted( selected );
               this.loadMask.hide();
               this.loadMask.msg = oldmsg;
            }.createDelegate( this );

            var errorHandler = function( er ) {
               this.loadMask.hide();
               this.loadMask.msg = oldmsg;
               Ext.Msg.alert( "Error", er );
            }.createDelegate( this );

            ExperimentalDesignController.deleteExperimentalFactors( this.experimentalDesign, selected, {
               callback : callback,
               errorHandler : errorHandler
            } );
         }.createDelegate( this ) );

         this.getTopToolbar().on( "save", function() {
            var edited = this.getEditedRecords();
            var oldmsg = this.loadMask.msg;
            this.loadMask.msg = Gemma.StatusText.saving;
            this.loadMask.show();
            var callback = function() {
               this.recordsChanged( edited );
               this.loadMask.hide();
               this.loadMask.msg = oldmsg;
            }.createDelegate( this );

            var errorHandler = function( er ) {
               this.loadMask.hide();
               this.loadMask.msg = oldmsg;
               Ext.Msg.alert( "Error", er );
            }.createDelegate( this );

            ExperimentalDesignController.updateExperimentalFactors( edited, callback );
         }.createDelegate( this ) );

         this.getTopToolbar().on( "undo", this.revertSelected.createDelegate( this ) );

         this.on( "afteredit", function( e ) {
            var col = this.getColumnModel().getColumnId( e.column );
            if ( col == CATEGORY_COLUMN ) {
               var f = this.categoryCombo.getTerm.bind( this.categoryCombo );
               var term = f();
               e.record.set( "category", term.term );
               e.record.set( "categoryUri", term.uri );
            }
         }.createDelegate( this ) );

         this.on( "afteredit", function( model ) {
            this.saveButton.enable();
            this.revertButton.enable();
         }, this.getTopToolbar() );

         this.getSelectionModel().on( "selectionchange", function( model ) {
            if ( model ) {
               var selected = model.getSelections();
               if ( selected.length > 0 ) {
                  this.deleteButton.enable();
               } else {
                  this.deleteButton.disable();
               }
               this.revertButton.disable();
               for ( var i = 0; i < selected.length; ++i ) {
                  if ( selected[i].dirty ) {
                     this.revertButton.enable();
                     break;
                  }
               }
            }
         }, this.getTopToolbar() );
      } // if editable.
   },

   factorCreated : function( factor ) {
      this.refresh();
      this.fireEvent( 'experimentalfactorselected', factor );
   },

   recordsChanged : function( records ) {
      this.refresh();
      var efs = [];
      for ( var i = 0; i < records.length; ++i ) {
         efs.push( records[i].data );
      }
      var selModel = this.getSelectionModel();
      var selected = (selModel.hasSelection()) ? selModel.getSelected() : selModel.selectFirstRow().getSelected();
      this.fireEvent( 'experimentalfactorchange', this, efs, selected );
   },

   idsDeleted : function( ids ) {
      this.refresh();
      var efs = [];
      for ( var i = 0; i < ids.length; ++i ) {
         efs.push( this.store.getById( ids[i] ).data );
      }
      this.fireEvent( 'experimentalfactorchange', this, efs );
   }

} );

/**
 * Accept entry of data for a new factor, including category and whether it is a continuous variable.
 *
 * @class Gemma.ExperimentalFactorAddWindow
 * @extends Ext.Window
 */
Gemma.ExperimentalFactorAddWindow = Ext.extend( Ext.Window, {

   modal : true,
   closeAction : 'close',
   title : "Fill in new factor details",
   width : 420,

   /**
    * @memberOf Gemma.ExperimentalFactorAddWindow
    */
   initComponent : function() {

      Ext.apply( this, {
         items : [ {
            xtype : 'form',
            width : 400,
            bodyStyle : "padding:10px",
            monitorValid : true,
            id : 'factor-create-form',
            items : [
               new Gemma.CategoryCombo( {
                  id : 'factor-category-combo',
                  emptyText : "Select a category",
                  fieldLabel : "Category",
                  allowBlank : false,
                  width: 250,
                  termKey : "factor"
               } ),
               {
                  xtype : 'textfield',
                  width : 250,
                  id : 'factor-description-field',
                  allowBlank : false,
                  fieldLabel : "Description",
                  validator : function( value ) {
                     return this.store.findExact( 'description', value ) < 0 ? true
                        : Gemma.HelpText.WidgetDefaults.ExperimentalFactorAddWindow.descriptionUnique;
                  }.createDelegate( this ),
                  emptyText : Gemma.HelpText.WidgetDefaults.ExperimentalFactorAddWindow.emptyText
               }, {
                  xtype : 'checkbox',
                  id : 'factor-type-checkbox',
                  fieldLabel : 'Continuous',
                  //toolTip : Gemma.HelpText.WidgetDefaults.ExperimentalFactorAddWindow.continuousCheckboxTT
                  listeners : { 
                     render : function( combo ) {
                        new Ext.ToolTip( {
                           target : combo.getEl(),
                           html : Gemma.HelpText.WidgetDefaults.ExperimentalFactorAddWindow.continuousCheckboxTT
                        } )
                     }.createDelegate( this )
                  }
               },
               {
                  xtype : 'combo',
                  emptyText : "Base on an existing characteristic?",
                  id : 'factor-existing-characteristic-combo',
                  fieldLabel : "Prepopulate from",
                  valueField : 'category',
                  displayField : 'category',
                  allowBlank : true,
                  disabled : false,
                  readOnly : false,
                  width : 250,
                  typeAhead: true,
                  minChars : 2,
                  triggerAction : 'all',
                  tooltip : Gemma.HelpText.WidgetDefaults.ExperimentalFactorAddWindow.makeFromExistingCharacteristicTT,
                  store : new Ext.data.Store( {
                     autoLoad : true,
                     proxy : new Ext.data.DWRProxy(
                        {
                           apiActionToHandlerMap : {
                              read : {
                                 dwrFunction : ExperimentalDesignController.getBioMaterialCharacteristicCategories,
                                 getDwrArgsFunction : function() {
                                    return [ this.experimentalDesignId ];
                                 }.createDelegate( this )
                              }
                           },
                        }
                     ),
                     reader : new Ext.data.ListRangeReader( {
                        totalProperty : 'totalCount',
                        root : 'records',
                        fields : Ext.data.Record.create( [
                           {name : 'category', type : 'string'}
                        ] )
                     } )
                  } ),
                  listeners : {
                     select : function( combo, record, index ) {
                        combo.setValue( record.data.category ); // shouldn't need this?
                        this.fireEvent( "change", combo );
                     }.createDelegate( this ),
                     render : function( combo ) {
                        new Ext.ToolTip( {
                           target : combo.getEl(),
                           html : Gemma.HelpText.WidgetDefaults.ExperimentalFactorAddWindow.makeFromExistingCharacteristicTT
                        } )
                     }.createDelegate( this )
                  }
               }
            ]
         } ],
         buttons : [ {
            text : "Create",
            id : 'factor-create-button',
            tooltip : "Create the new experimental factor",
            disabled : true,
            handler : function() {
               this.fireEvent( "done", this.getExperimentalFactorValueObject() );
               this.close();
            },
            scope : this
         }, {
            text : "Cancel",
            handler : function() {
               this.close();
            },
            scope : this
         } ]
      } )
      ;

      Gemma.ExperimentalFactorAddWindow.superclass.initComponent.call( this );

      this.addEvents( "done" );

      Ext.getCmp( 'factor-create-form' ).on( 'clientvalidation', function( form, valid ) {
         if ( valid ) {
            Ext.getCmp( 'factor-create-button' ).enable();
         } else {
            Ext.getCmp( 'factor-create-button' ).disable();
         }
      } );
   },

   getExperimentalFactorValueObject : function() {
      var category = Ext.getCmp( 'factor-category-combo' ).getTerm();
      var description = Ext.getCmp( 'factor-description-field' ).getValue();
      return {
         name : category.term,
         description : description,
         category : category.term,
         categoryUri : category.uri,
         bioMaterialCharacteristicCategoryToUse : Ext.getCmp( 'factor-existing-characteristic-combo' ).getValue(),
         type : Ext.getCmp( 'factor-type-checkbox' ).getValue() ? "continuous" : "categorical"
      };
   }
} );

Gemma.ExperimentalFactorToolbar = Ext.extend( Ext.Toolbar, {

   onRender : function( c, l ) {
      Gemma.ExperimentalFactorToolbar.superclass.onRender.call( this, c, l );

      this.createButton = new Ext.Toolbar.Button( {
         text : "Add new",
         tooltip : "Add a new experimental factor to the design",
         disabled : false,
         handler : function() {
            var w = new Gemma.ExperimentalFactorAddWindow( {
               store : this.store,
               experimentalDesignId : this.experimentalDesignId
            } );
            w.on( 'done', function( object ) {
               this.fireEvent( 'create', object );
            }.createDelegate( this ) );
            w.show();
         },
         scope : this
      } );

      this.deleteButton = new Ext.Toolbar.Button( {
         text : "Delete",
         tooltip : "Delete the selected experimental factor(s)",
         disabled : true,
         handler : function() {
            Ext.Msg.confirm( Gemma.HelpText.WidgetDefaults.ExperimentalFactorToolbar.deleteFactorWarningTitle,
               Gemma.HelpText.WidgetDefaults.ExperimentalFactorToolbar.deleteFactorWarningText, function( but ) {
                  if ( but === 'yes' ) {
                     this.deleteButton.disable();
                     this.fireEvent( "delete" );
                  }
               }.createDelegate( this ) );

         },
         scope : this
      } );

      this.revertButton = new Ext.Toolbar.Button( {
         text : "Undo",
         tooltip : "Undo changes to the selected experimental factors",
         disabled : true,
         handler : function() {
            this.fireEvent( "undo" );
         },
         scope : this
      } );

      this.saveButton = new Ext.Toolbar.Button( {
         text : "Save",
         tooltip : "Commit changes",
         disabled : true,
         handler : function() {
            this.saveButton.disable();
            this.revertButton.disable();
            this.fireEvent( "save" );
         },
         scope : this
      } );

      this.addButton( this.createButton );
      this.addSeparator();
      this.addButton( this.deleteButton );
      this.addSeparator();
      this.addButton( this.saveButton );
      this.addSeparator();
      this.addButton( this.revertButton );
   },

   /**
    * @memberOf Gemma.ExperimentalFactorToolbar
    */
   initComponent : function() {
      Gemma.ExperimentalFactorToolbar.superclass.initComponent.call( this );
      this.addEvents( "create", "save", "undo", "delete" );
   }
} );