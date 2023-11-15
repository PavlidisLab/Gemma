Ext.namespace( 'Gemma' );

/**
 * Forms the bottom panel of the experimental design editor.
 *
 * @author Luke, Paul
 */
Gemma.FactorValueRecord = Ext.data.Record.create( [ {
   name : "charId"
}, {
   name : "id"
}, {
   name : "category",
   type : "string"
}, {
   name : "categoryUri",
   type : "string"
}, {
   name : "value",
   type : "string"
}, {
   name : "valueUri",
   type : "string"
}, {
   name : "measurement",
   type : "bool"
}, {
   name : "object",
   type : "string"
}, {
   name : "objectUri",
   type : "string"
}, {
   name : "predicate",
   type : "string"
}, {
   name : "predicateUri",
   type : "string"
}, {
   name : "secondObject",
   type : "string"
}, {
   name : "secondObjectUri",
   type : "string"
}, {
   name : "secondPredicate",
   type : "string"
}, {
   name : "secondPredicateUri",
   type : "string"
}, {
   name : "factorValue",
   type : "string"
}, {
   name:"needsAttention",
   type: "boolean"
} ] );

Gemma.FactorValueGrid = Ext.extend( Gemma.GemmaGridPanel, {

   loadMask : true,

   viewConfig : {
      forceFit : false
   },

   taxonId : null,

   disabledClass : '.x-factor-grid-disabled',

   record : Gemma.FactorValueRecord,

   /**
    * @memberOf Gemma.FactorValueGrid
    */
   categoryStyler : function( value, metadata, record ) {
      return Gemma.GemmaGridPanel.formatTermWithStyle( value, record.data.categoryUri );
   },

   valueStyler : function( value, metadata, record ) {
      if ( value ) {
         return Gemma.GemmaGridPanel.formatTermWithStyle( value, record.data.valueUri );
      } else {
         return '<span style="color:grey">'
            + ((this.editable) ? 'Double-click to define the value' : '(no value)') + '</span>';
      }
   },

   predicateStyler : function( value, metadata, record ) {
      if ( value ) {
         return Gemma.GemmaGridPanel.formatTermWithStyle( value, record.data.predicateUri );
      } else {
         return '<span style="color:grey">'
            + ((this.editable) ? 'Double-click to define the predicate' : '(no value)') + '</span>';
      }
   },

   objectStyler : function( value, metadata, record ) {
      if ( value ) {
         return Gemma.GemmaGridPanel.formatTermWithStyle( value, record.data.objectUri );
      } else {
         return '<span style="color:#c47474">'
            + ((this.editable) ? 'Double-click to define the object term' : '(no value)') + '</span>';
      }
   },

   secondPredicateStyler : function( value, metadata, record ) {
      if ( value ) {
         return Gemma.GemmaGridPanel.formatTermWithStyle( value, record.data.secondPredicateUri );
      } else {
         return '<span style="color:grey">'
            + ((this.editable) ? 'Double-click to define the predicate' : '(no value)') + '</span>';
      }
   },

   secondObjectStyler : function( value, metadata, record ) {
      console.log( value, metadata, record.data );
      if ( value ) {
         return Gemma.GemmaGridPanel.formatTermWithStyle( value, record.data.secondObjectUri );
      } else {
         return '<span style="color:#c47474">'
            + ((this.editable) ? 'Double-click to define the object term' : '(no value)') + '</span>';
      }
   },

   createNew : function() {
      var ef = this.experimentalFactor;
      var oldmsg = this.loadMask.msg;
      this.loadMask.msg = "Creating new factor value";
      this.loadMask.show();

      var callback = function() {
         this.loadMask.hide();
         this.loadMask.msg = oldmsg;
         this.factorValueCreated( ef );
         this.getTopToolbar().characteristicToolbar.setExperimentalFactor( ef.id );
      }.createDelegate( this );

      var errorHandler = function( er ) {
         this.loadMask.hide();
         this.loadMask.msg = oldmsg;
         Ext.Msg.alert( "Error", er );
      }.createDelegate( this );

      ExperimentalDesignController.createFactorValue( this.experimentalFactor, {
         callback : callback,
         errorHandler : errorHandler
      } );
   },

   initComponent : function() {

      this.columns = [ {
         header : "FactorValue",
         dataIndex : "id"
      }, {
         header : "Category",
         dataIndex : "category",
         renderer : this.categoryStyler,
         width : 120
      }, {
         header : "Value",
         dataIndex : "value",
         renderer : this.valueStyler
      }, {
         header : "Predicate",
         dataIndex : "predicate",
         renderer : this.predicateStyler,
         width : 100
      }, {
         header : "Object",
         dataIndex : "object",
         renderer : this.objectStyler,
         width : 200
      }, {
         header : "Second Predicate",
         dataIndex : "secondPredicate",
         renderer : this.secondPredicateStyler,
         width : 100
      }, {
         header : "Second Object",
         dataIndex : "secondObject",
         renderer : this.secondObjectStyler,
         width : 200
      }, {
         header : "Summary",
         dataIndex : "factorValue",
         hidden : true
      } ];

      if ( this.editable ) {
         this.columns.push( {
            header : "Factor Value ID",
            dataIndex : "id"
         } );
      }

      this.experimentalDesign = {
         id : this.edId,
         classDelegatingFor : "ExperimentalDesign"
      };
      this.experimentalFactor = {
         id : this.efId,
         classDelegatingFor : "ExperimentalFactor"
      };

      this.categoryCombo = new Gemma.CategoryCombo( {
         lazyRender : true,
         termKey : "factorvalue" // not sure this does anything
      } );
      var categoryEditor = new Ext.grid.GridEditor( this.categoryCombo );
      // when user selects a category from the combo.
      this.categoryCombo.on( "select", function( combo, record, index ) {
         categoryEditor.completeEdit();
      } );

      this.valueCombo = new Gemma.CharacteristicCombo( {
         lazyRender : true,
         taxonId : this.taxonId
      } );
      var valueEditor = new Ext.grid.GridEditor( this.valueCombo );
      // when user selects an item from the search results.
      this.valueCombo.on( "select", function( combo, record, index ) {
         valueEditor.completeEdit();
      } );


      this.predicateCombo = new Gemma.RelationCombo( {
         lazyRender : true
      } );
      var predicateEditor = new Ext.grid.GridEditor( this.predicateCombo );
      // when user selects a predicate from the combo.
      this.predicateCombo.on( "select", function( combo, record, index ) {
         predicateEditor.completeEdit();
      } );

      this.objectCombo = new Gemma.CharacteristicCombo( {
         lazyRender : true,
         taxonId : this.taxonId
      } );
      var objectEditor = new Ext.grid.GridEditor( this.objectCombo );
      this.objectCombo.on( "select", function( combo, record, index ) {
         objectEditor.completeEdit();
      } );


      this.predicateCombo2 = new Gemma.RelationCombo( {
         lazyRender : true
      } );
      var predicateEditor2 = new Ext.grid.GridEditor( this.predicateCombo2 );
      // when user selects a predicate from the combo.
      this.predicateCombo2.on( "select", function( combo, record, index ) {
         predicateEditor2.completeEdit();
      } );

      this.objectCombo2 = new Gemma.CharacteristicCombo( {
         lazyRender : true,
         taxonId : this.taxonId
      } );
      var objectEditor2 = new Ext.grid.GridEditor( this.objectCombo2 );
      this.objectCombo2.on( "select", function( combo, record, index ) {
         objectEditor2.completeEdit();
      } );

      this.store = new Ext.data.GroupingStore( {
         proxy : new Ext.data.DWRProxy( ExperimentalDesignController.getFactorValuesWithCharacteristics ),
         reader : new Ext.data.ListRangeReader( {
            id : "charId"
         }, this.record ),
         groupField : "id"
      } );

      /*
       * The checkboxes defined here require that this.form be set: a <form> element wrapping the div that this
       * goes in. Clumsy but it works.
       */
      var groupTextTpl = "";
      if ( this.editable ) {
         groupTextTpl += '<input id="{[ values.rs[0].data.id ]}" type="checkbox" name="selectedFactorValues" value="{[ values.rs[0].data.id ]}"/>';
         groupTextTpl += ' ';
      }
      // label for the FV
      groupTextTpl = groupTextTpl + '<span>{[ values.rs[0].data.factorValue ]}</span>';
      if ( this.editable ) {
         // indicate if the FV needs attention
         groupTextTpl += '<tpl if="values.rs[0].data.needsAttention"> <span class="gold"><i class="fa fa-exclamation-circle fa-lg fa-fw"></i> Needs attention</span></tpl>';
      }

      this.view = new Ext.grid.GroupingView( {
         enableGroupingMenu : false,
         enableNoGroups : false,
         groupTextTpl : groupTextTpl,
         hideGroupedColumn : true,
         showGroupName : true,
         startCollapsed : true
      } );

      this.tbar = new Gemma.FactorValueToolbar( {
         editable : this.editable,
         experimentalDesign : this.experimentalDesign
      } );

      this.tbar.on( "save", function( e ) {
         this.valueCombo.clearCharacteristic(); // might not be necessary but can't hurt
      }, this );

      var FACTOR_VALUE_COLUMN = 0; // The id.
      var CATEGORY_COLUMN = 1;
      var VALUE_COLUMN = 2;
      var PREDICATE_COLUMN = 3;
      var OBJECT_COLUMN = 4;
      var PREDICATE_COLUMN2 = 5;
      var OBJECT_COLUMN2 = 6;
      this.autoExpandColumn = VALUE_COLUMN;

      // ///////////////////////////////////////
      Gemma.FactorValueGrid.superclass.initComponent.call( this );

      this.addEvents( 'factorvaluecreate', 'factorvaluechange', 'factorvaluedelete' );

      if ( this.editable ) {

         this.getColumnModel().setEditor( CATEGORY_COLUMN, categoryEditor );
         this.getColumnModel().setEditor( VALUE_COLUMN, valueEditor );
         this.getColumnModel().setEditor( PREDICATE_COLUMN, predicateEditor );
         this.getColumnModel().setEditor( OBJECT_COLUMN, objectEditor );
         this.getColumnModel().setEditor( PREDICATE_COLUMN2, predicateEditor2 );
         this.getColumnModel().setEditor( OBJECT_COLUMN2, objectEditor2 );

         this.on( "afteredit", function( e ) {
            var col = this.getColumnModel().getColumnId( e.column );
            if ( col === CATEGORY_COLUMN ) {
               var term = this.categoryCombo.getTerm.call( this.categoryCombo );
               e.record.set( "category", term.term );
               e.record.set( "categoryUri", term.uri );
            } else if ( col === VALUE_COLUMN ) {
               var c = this.valueCombo.getCharacteristic.call( this.valueCombo );
               e.record.set( "value", c.value );
               e.record.set( "valueUri", c.valueUri );
               this.valueCombo.clearCharacteristic.call( this.valueCombo ); // avoid carryover
            } else if ( col === PREDICATE_COLUMN ) {
               var c = this.predicateCombo.getTerm.call( this.predicateCombo );
               e.record.set( "predicate", c.label );
               e.record.set( "predicateUri", c.uri );
            } else if ( col === OBJECT_COLUMN ) {
               var c = this.objectCombo.getCharacteristic.call( this.objectCombo );
               e.record.set( "object", c.value );
               e.record.set( "objectUri", c.valueUri );
               this.objectCombo.clearCharacteristic.call( this.objectCombo ); // avoid carryover
            } else if ( col === PREDICATE_COLUMN2 ) {
               var c = this.predicateCombo2.getTerm.call( this.predicateCombo2 );
               e.record.set( "secondPredicate", c.label );
               e.record.set( "secondPredicateUri", c.uri );
            } else if ( col === OBJECT_COLUMN2 ) {
               var c = this.objectCombo2.getCharacteristic.call( this.objectCombo2 );
               e.record.set( "secondObject", c.value );
               e.record.set( "secondObjectUri", c.valueUri );
               this.objectCombo.clearCharacteristic.call( this.objectCombo2 ); // avoid carryover
            }
            this.getView().refresh();
         } );

         this.getSelectionModel().on( "selectionchange", function( model ) {
            var selected = model.getSelections();
            this.revertButton.disable();
            for ( var i = 0; i < selected.length; ++i ) {
               if ( selected[i].dirty ) {
                  this.revertButton.enable();
                  break;
               }
            }
            if ( selected.length > 0 ) {
               this.characteristicToolbar.deleteButton.enable();
            } else {
               this.characteristicToolbar.deleteButton.disable();
            }
         }, this.getTopToolbar() );

         this.on( 'groupclick', function( grid, groupField, groupValue, e ) {
            var el = Ext.get( grid.getView().getGroupId( groupValue ) );
            var cb = el.dom.getElementsByTagName( 'input' )[0];
            if ( cb.checked ) {
               this.deleteFactorValueButton.enable();
            } else {
               this.deleteFactorValueButton.disable();
            }
         }, this.getTopToolbar() );

         this.on( "afteredit", function( model ) {
            this.saveButton.enable();
            this.revertButton.enable();
         }, this.getTopToolbar() );

         /*
          * Create a new factorvalue
          */
         this.getTopToolbar().on(
            "create",
            function() {
               /*
                * Avoid accidents...
                */
               if ( this.store.getModifiedRecords().length > 0 ) {
                  Ext.Msg
                     .confirm(
                        'Unsaved changes!',
                        'You should save your changes before creating new values. Are you sure you want to erase them?',
                        function( but ) {
                           if ( but == 'yes' ) {
                              this.store.rejectChanges();

                              this.createNew();
                           }
                        }.createDelegate( this ) );
               } else {
                  this.createNew();
               }

            }.createDelegate( this ) );

         /*
          * delete a factor value
          */
         this.getTopToolbar().on(
            "delete",
            function() {
               var selectedIds = this.getSelectedFactorValues();

               if ( selectedIds && selectedIds.length > 0 ) {

                  Ext.Msg.confirm(
                     Gemma.HelpText.WidgetDefaults.ExperimentalFactorToolbar.deleteFactorWarningTitle,
                     Gemma.HelpText.WidgetDefaults.ExperimentalFactorToolbar.deleteFactorWarningText, function( but ) {
                        if ( but == 'yes' ) {
                           Ext.getCmp( 'factor-value-delete-button' ).disable();
                           var ef = this.experimentalFactor;
                           this.getEl().mask();
                           var callback = function() {
                              this.factorValuesDeleted( selectedIds );
                           }.createDelegate( this );
                           ExperimentalDesignController.deleteFactorValues( ef, selectedIds, callback, this );
                        }
                     }, this );
               } else {
                  Ext.Msg.alert( "Nothing selected", "You have not checked any factor values for deletion" );
               }
            }.createDelegate( this ), this );

         /*
          * Commit changes to factor values (added characteristics or updated characteristics)
          */
         this.getTopToolbar().on( "save", function() {
            var edited = this.getEditedRecords();
            var callback = function() {
               this.factorValuesChanged( edited );
            }.createDelegate( this );
            ExperimentalDesignController.updateFactorValueCharacteristics( edited, callback );
         }, this );

         this.getTopToolbar().on( "undo", this.revertSelected.createDelegate( this ), this );

         this.getTopToolbar().on( "refresh", function() {
            this.getStore().reload();
         }, this );

      }

      this.getTopToolbar().on( "toggleExpand", function() {
         this.getView().toggleAllGroups( true );
      }.createDelegate( this ), this );

      this.getTopToolbar().on( "toggleCollapse", function() {
         this.getView().toggleAllGroups( false );
      }.createDelegate( this ), this );

      if ( this.experimentalFactor.id ) {
         this.store.load( {
            params : [ this.experimentalFactor ]
         } );
      }

   }, // init component

   onRender : function( c, p ) {
      Gemma.FactorValueGrid.superclass.onRender.call( this, c, p );
      // have to do this here, because the toolbar isn't created until
      // rendering.
      var ct = this.getTopToolbar().characteristicToolbar;
      if ( ct ) {
         ct.on( "create", function( f, c ) {
            var callback = function() {
               // console.log("reload");
               ct.factorValueCombo.store.reload();
               this.factorValuesChanged.call( this, [] );
            }.createDelegate( this );
            ExperimentalDesignController.createFactorValueCharacteristic( f, c, callback );
         }.createDelegate( this ), this );

         ct.on( "delete", function() {
            // console.log("deleting factorvalue(s)");
            var selected = this.getSelectedRecords();
            this.store.reload();
            var callback = function() {
               this.factorValuesChanged.call( this, selected );
            }.createDelegate( this );
            ExperimentalDesignController.deleteFactorValueCharacteristics( selected, callback );
         }.createDelegate( this ), this );
      }
   },

   factorValueCreated : function( ef ) {
      this.refresh();
      var fvs = [];
      var ct = this.getTopToolbar().characteristicToolbar;
      ct.factorValueCombo.store.reload();
      this.fireEvent( 'factorvaluecreate', this, fvs );
   },

   factorValuesChanged : function( fvs ) {
      this.refresh();
      this.store.rejectChanges();
      var ct = this.getTopToolbar().characteristicToolbar;
      ct.factorValueCombo.store.reload();
      this.fireEvent( 'factorvaluechange', this, fvs );
   },

   factorValuesDeleted : function( fvIds ) {
      // don't reload store, caching issues can cause an error (bug 2553)
      // remove deleted records from store instead
      var ct = this.getTopToolbar().characteristicToolbar;
      var i;
      var matchIds = function( record, id ) {
         return (fvIds.indexOf( record.data.id + "" ) > -1);
      };
      for ( i = 0; i < fvIds.length; i++ ) {
         var fvId = fvIds[i];
         // combo and grid are not using the same store
         // store.findExact doesn't work
         // will be >1 record per factor value if a factor value has >1 category
         var indexToRemove;
         while ( ct.factorValueCombo.getStore().findBy( matchIds ) > -1 ) {
            indexToRemove = ct.factorValueCombo.getStore().findBy( matchIds );
            ct.factorValueCombo.getStore().removeAt( indexToRemove );
         }

         while ( this.store.findBy( matchIds ) > -1 ) {
            indexToRemove = this.store.findBy( matchIds );
            this.store.removeAt( indexToRemove );
         }

      }
      this.getEl().unmask();
      this.fireEvent( 'factorvaluedelete', this, fvIds );
   },

   changeExperimentalFactor : function( efId ) {
      this.experimentalFactor.id = efId;
      this.store.rejectChanges(); // reset.

      if ( efId === null ) {
         this.store.removeAll();
      } else {
         this.refresh( [ this.experimentalFactor ] ); // causes a load.
      }
      this.getTopToolbar().setExperimentalFactor( efId );

   },

   setExperimentalFactor : function( efId ) {

      if ( this.store.getModifiedRecords().length > 0 ) {
         Ext.Msg
            .confirm(
               'Unsaved changes!',
               'You should save your changes before switching to another factor. Are you sure you want to lose your changes?',
               function( but ) {
                  if ( but == 'yes' ) {
                     this.changeExperimentalFactor( efId );
                  }
               }.createDelegate( this ) );
      } else {
         this.changeExperimentalFactor( efId );
      }
   },

   /**
    * Processing of the checkboxes defined in the tmpl for the groups. Requires that this.form be set: a <form>
    * element wrapping the div that this goes in. Clumsy but it works.
    */
   getSelectedFactorValues : function() {
      if ( this.form ) {
         var f = document.forms[this.form];
         var checkboxes = f.selectedFactorValues;
         if ( !checkboxes.length ) {
            checkboxes = [ checkboxes ];
         }
         var values = [];
         for ( var i = 0; i < checkboxes.length; ++i ) {
            if ( checkboxes[i].checked ) {
               values.push( checkboxes[i].value );
            }
         }
         return values;
      }
   }

} );


/*
"Upper" toolbar for factorvalue pane.
 */
Gemma.FactorValueToolbar = Ext.extend( Ext.Toolbar, {

   /**
    * @memberOf Gemma.FactorValueToolbar
    */
   initComponent : function() {
      Gemma.FactorValueToolbar.superclass.initComponent.call( this );
      this.addEvents( "create", "save", "delete", "undo", "refresh", "toggleExpand", "toggleCollapse" );
   },

   onRender : function( c, p ) {

      Gemma.FactorValueToolbar.superclass.onRender.call( this, c, p );

      this.createFactorValueButton = new Ext.Toolbar.Button( {
         text : "Create",
         id : 'factor-value-create-button',
         tooltip : "Create a new factor value for the current factor",
         disabled : true,
         handler : function() {
            this.fireEvent( "create" );
            this.deleteFactorValueButton.enable();
         },
         scope : this
      } );

      this.deleteFactorValueButton = new Ext.Toolbar.Button( {
         text : "Delete",
         id : 'factor-value-delete-button',
         tooltip : "Delete checked factor values",
         disabled : true,
         handler : function() {
            this.fireEvent( "delete" );
         }.createDelegate( this )
      } );

      this.saveButton = new Ext.Toolbar.Button( {
         text : "Save",
         id : 'factor-value-save-button',
         tooltip : "Commit changes to factor values",
         disabled : true,
         handler : function() {
            this.saveButton.disable();
            this.fireEvent( "save" );
         }.createDelegate( this )
      } );

      this.revertButton = new Ext.Toolbar.Button( {
         text : "Undo",
         id : 'factor-value-undo-button',
         tooltip : "Undo changes to selected factor values",
         disabled : true,
         handler : function() {
            this.fireEvent( "undo" );
         },
         scope : this
      } );

      this.refreshButton = new Ext.Toolbar.Button( {
         icon : ctxBasePath + '/images/icons/arrow_refresh_small.png',
         tooltip : 'Refresh the factor values',
         handler : function() {
            this.fireEvent( "refresh" );
         },
         scope : this
      } );

      if ( this.editable ) {
         this.addButton( this.createFactorValueButton );
         this.addSeparator();
         this.addButton( this.deleteFactorValueButton );
         this.addSpacer();
         this.addButton( this.saveButton );
         this.addSpacer();
         this.addButton( this.revertButton );
         this.addSpacer();
         this.addButton( this.refreshButton );
      }

      this.collapseButton = new Ext.Toolbar.Button( {
         text : "Collapse all",
         tooltip : "Hide all factor value details",
         handler : function() {
            this.fireEvent( "toggleCollapse" );
         },
         scope : this
      } );

      this.addFill();
      this.addButton( this.collapseButton );

      this.expandButton = new Ext.Toolbar.Button( {
         text : "Expand all",
         tooltip : "Show all factor value details",
         handler : function() {
            this.fireEvent( "toggleExpand" );
         },
         scope : this
      } );

      this.addFill();
      this.addButton( this.expandButton );

      if ( this.editable ) {
         this.characteristicToolbar = new Gemma.FactorValueCharacteristicToolbar( {
            id : 'fv-char-toolbar',
            renderTo : this.ownerCt.tbar
         } );
      }

   },

   setExperimentalFactor : function( efId ) {
      this.efId = efId;
      if ( efId != null ) {
         this.createFactorValueButton.enable();
      }
      if ( this.characteristicToolbar ) {
         this.characteristicToolbar.setExperimentalFactor( efId );
      }

   }

} );

/**
 * The toolbar that allows the user to add characteristics to factor values, it has its own combo boxes for value and category.
 * Once the user inializes the characteristic they can edit it further in the grid.
 * @class Gemma.FactorValueCharacteristicToolbar
 * @extends Ext.Toolbar
 */
Gemma.FactorValueCharacteristicToolbar = Ext.extend( Ext.Toolbar, {

   /**
    * @memberOf Gemma.FactorValueCharacteristicToolbar
    */
   initComponent : function() {
      Gemma.FactorValueCharacteristicToolbar.superclass.initComponent.call( this );

      Ext.apply( this, {
         factorValue : {
            id : -1,
            classDelegatingFor : "FactorValue"
         },

         experimentalFactor : {
            id : -1,
            classDelegatingFor : "ExperimentalFactor"
         }
      } );

      this.addEvents( "create", "delete" );
   },

   onRender : function( c, l ) {
      Gemma.FactorValueCharacteristicToolbar.superclass.onRender.call( this, c, l );

      this.factorValueCombo = new Gemma.FactorValueCombo( {
         id : 'fv-char-toolbar-fvcombo',
         taxonId : this.taxonId,
         disabled : this.experimentalFactor.id >= 0 ? false : true,
         efId : this.experimentalFactor.id >= 0 ? this.experimentalFactor.id : null
      } );

      this.factorValueCombo.on( "select", function( combo, record, index ) {
         this.factorValue.id = record.get( "id" );
         this.categoryCombo.enable();
      }.createDelegate( this ) );

      this.categoryCombo = new Gemma.CategoryCombo( {
         disabled : true,
         emptyText : "Select a category",
         termKey : "factorvalue"
      } );

      this.categoryCombo.on( "select", function( combo, record, index ) {
         this.charCombo.setCategory( record.get( "term" ), record.get( "uri" ) );
         this.charCombo.enable();
         this.createButton.enable();
      }.createDelegate( this ) );

      this.charCombo = new Gemma.CharacteristicCombo( {
         disabled : true,
         taxonId : this.taxonId
      } );

      this.createButton = new Ext.Toolbar.Button( {
         text : "Add",
         tooltip : "Add the new characteristic to the selected factor value",
         disabled : true,
         handler : function() {
            var c = this.charCombo.getCharacteristic();

            if ( !this.factorValue || !c ) {
               Ext.Msg.alert( "You must select a factor value and set a characteristic." );
            } else {
               this.createButton.disable();
               // removed in response to bug 1016
               // categoryCombo.reset();
               this.charCombo.reset();
               this.fireEvent( "create", this.factorValue, c );
               this.charCombo.clearCharacteristic(); // avoid carryover to next edit
            }
         },
         scope : this
      } );

      this.charCombo.on( "select", function() {
         this.createButton.enable();
      }.createDelegate( this ) );

      this.charCombo.on( "change", function() {
         this.createButton.enable();
      }.createDelegate( this ) );

      this.deleteButton = new Ext.Toolbar.Button( {
         text : "Remove Characteristic",
         tooltip : "Remove/delete the selected characteristic(s) from the factor value",
         disabled : true,
         handler : function() {
            this.deleteButton.disable();
            this.fireEvent( "delete" );
         },
         scope : this
      } );

      this.addText( "Append a characteristic to:" );
      this.add( this.factorValueCombo );
      this.addSpacer();
      this.addText( " with category:" );
      this.add( this.categoryCombo );
      this.addSpacer();
      this.addText( " and value:" );
      this.add( this.charCombo );
      this.addSpacer();
      this.addButton( this.createButton );
      this.addSpacer();
      this.addButton( this.deleteButton );
   },

   setExperimentalFactor : function( efId ) {
      this.experimentalFactor.id = efId;
      this.factorValueCombo.setExperimentalFactor( efId, function() {
         this.factorValueCombo.enable();
         this.categoryCombo.enable();
         this.charCombo.enable();
      }.createDelegate( this ) );
   }
} );