Ext.namespace( 'Gemma' );

Gemma.CharDumpViewer = function( config ) {
   this.originalConfig = config;
   this.expressionExperiment = {
      id : config.eeId,
      classDelegatingFor : "ExpressionExperimentController"
   };

   Gemma.BioMaterialEditor.superclass.constructor.call( this, config );
};

/**
 * Grid with list of biomaterials for editing experimental design parameters.
 */
Ext.extend( Gemma.CharDumpViewer, Ext.Panel, {

   firstInitDone : false,

   /*
    * We make two ajax calls; the first gets the biomaterials, the second gets the experimentalfactors. These are run in
    * succession so both values can be given to the BioMaterialGrid constructor. We could make a method that gets them
    * both at once...
    */
   firstCallback : function( data ) {
	   //console.log(data);
      // second ajax call.
//      ExpressionExperimentController.getExperimentalFactors( this.expressionExperiment, function( factorData ) {
//         config = {
//            factors : factorData,
//            bioMaterials : data
//         };
        // Ext.apply( config, this.originalConfig );
	   
         this.grid = new Gemma.CharDumpGrid( {'data': data });
         
         this.grid.init = this.init.createDelegate( this );

         this.add( this.grid );

         this.loadMask.hide();

         this.doLayout( false, true );

         this.firstInitDone = true;

//      }.createDelegate( this ) );
   },

   /**
    * Gets called on startup but also when a refresh is needed.
    * 
    * @memberOf Gemma.BioMaterialEditor
    */
   init : function() {
      var loadMaskTarget = this.el !== null ? this.el : Ext.getBody();

      this.loadMask = new Ext.LoadMask( loadMaskTarget, {
         msg : Gemma.StatusText.waiting
      } );

      this.loadMask.show();
      ExpressionExperimentController
         .getCharDump( this.expressionExperiment, this.firstCallback.createDelegate( this ) );
      //console.log('here!');
      
   }

} );

Gemma.CharDumpGrid = Ext
   .extend(
      Gemma.GemmaGridPanel,
      {

         loadMask : true,
         autoExpandColumn : 'bm-column',
         fvMap : this.data,
         rowsExpanded : false,
         height: 500,
         
         /**
          * See ExpressionExperimentController.getExperimentalFactors and ExperimentalFactorValueObject AND
          * FactorValueValueObject to see layout of the object that is passed.
          * 
          * @param data,
          *           fetched with getExperimentalFactors.
          * @memberOf Gemma.BioMaterialGridF
          */
         
         createColumns : function( data ) {

            var columns = [ {
               id : "bm-column",
               header : "Experiment factors",
               dataIndex : "factor",
               sortable : true,
               width : 120,
               tooltip : 'Experimental Factor Name',
               
            }, {
               id : "ba-column",
               header : "count",
               width : 150,
               dataIndex : "count",
               sortable : true,
               tooltip : 'count of experimental factor'
            } ];
            
//            rend = this.createValueRenderer();
            
            
            
            return columns;
         },
         createValueRenderer : function() {

             return function( value, metadata, record, row, col, ds ) {

                if ( !value ) {
                   return "-";
                }

                var k = this.data[value];
                return k ? k : value;

             }.createDelegate( this );
          },
            //this.factorValueEditors = [];

            /*
             * sort by id to give consistency.
             */
//            data.sort( function( a, b ) {
//               return a.id - b.id;
//            } );
           
//            for (var i = 0; i < data.length; i++) {
//               var factor = data[i];
//               var factorId = "factor" + factor.id;
//               
//               var rend = this.createValueRenderer();
//               
//               columns.push( {
//                   id : eeId,
//                   header : eeId,
//                   dataIndex : factorId,
//                   renderer : rend,
//                   editor : ue,
//                   width : 120,
//                   //tooltip : label,
//                   sortable : true,
//                   //continuous : continuous
//                } );

               //var editor;
//               var continuous = factor.type == "continuous";
//               if ( continuous ) {
//
////                  editor = new Ext.form.NumberField( {
////                     id : factorId + '-valueeditor',
////                     lazyInit : false,
////                     lazyRender : true,
////                     record : this.fvRecord,
////                     continuous : continuous, // might be useful.
////                     data : factor.values
////                  } );
//               } else {

                  /*
                   * Create one factorValueCombo per factor. It contains all the factor values.
                   */
//                  editor = new Gemma.FactorValueCombo( {
//                     id : factorId + '-valueeditor',
//                     lazyInit : false,
//                     lazyRender : true,
//                     record : this.fvRecord,
//                     continuous : continuous,
//                     data : factor.values
//                  } );

                  // console.log("Categorical");
               //}

//               this.factorValueEditors[factorId] = editor;

               // factorValueValueObjects
//               if ( factor.values ) {
//                  for (var j = 0; j < factor.values.length; j++) {
//                     fv = factor.values[j];
//                     var fvs = fv.factorValue; // descriptive string formed on server side.
//                     this.fvMap["fv" + fv.id] = fvs;
//                  }
//               }

               /*
                * Generate a function to render the factor values as displayed in the cells. At this point factorValue
                * contains all the possible values for this factor.
                */
              // var rend = null;

               // if (!continuous) {
             
               // }/

               /*
                * Define the column for this particular factor.
                */
//               var ue = null;
//               if ( this.editable ) {
//                  ue = editor;
//               }

               // text used for header of the column.
//               var label = factor.description ? factor.description : factor.name
//                  + (factor.name === factor.description || factor.description === "" ? "" : " (" + factor.description
//                     + ")");

               

            


         /**
          * See ExpressionExperimentController.getBioMaterials BioMaterialValueObject to see layout of the object that is
          * passed. *
          * 
          * @param biomaterial
          *           A template so we know how the records will be laid out.
          */
         
         createRecord : function() {
        	
        	 
        	 var fields = [ {
               name : "eeId",
               type : "int"
            }, {
               name : "factor",
               type : "string"
            }, {
               name : "count",
               type : "int"
            },];
        	
            var record = Ext.data.Record.create( fields );
          
            return record;
         },

         initComponent : function() {
        	
            this.record = this.createRecord();
           
            var data = this.transformData(this.data);
            //console.log(this.record, data);

            Ext.apply( this, {
               store : new Ext.data.Store( {
                  proxy : new Ext.data.MemoryProxy( data ),
                  reader : new Ext.data.ArrayReader( {}, this.record )
               } )
            } );
			
            // must be done separately.
            
            Ext.apply( this, {
               columns : this.createColumns( this.data )
            } );
            
            /*
             * Always show the toolbar, for regular user functions like toggleExpand
             */
//            this.tbar = new Gemma.BioMaterialToolbar( {
//               edId : this.edId,
//               editable : this.editable
//            } );

            Gemma.CharDumpGrid.superclass.initComponent.call( this );
            
            this.getStore().load( {
                params : {},
                callback : function() {
                   this.getStore().sort( "count" );
                   //this.getStore().fireEvent( "datachanged" );
                },
                scope : this
             } );
            
         },
         /**
          * Turn the incoming biomaterial valueobjects into an array structure that can be loaded into an ArrayReader.
          */
            transformData: function (incoming) {
            var data = [];
            for (var i = 0; i < incoming.length; ++i) {
               var bmvo = incoming[i];

               data[i] = [ bmvo.eeId, bmvo.factor, bmvo.count];

//               var data = bmvo.data;
//
//               /*
//                * Use this to keep the order the same as the record.
//                */
//               for (var j = 0; j < this.data.length; j++) {
//                  var factor = this.data[j];
//                  var factorId = "factor" + factor.id;
//                  var k = bmvo.factorIdToFactorValueId[factorId];
//                  if ( k ) {
//                     data[i].push( k );
//                  } else {
//                     data[i].push( "" ); // no value assigned.
//                  }
//               }

            }
            return data;
         }
} );

            /*
             * Event handlers for toolbar buttons.
             * 
             */
//            this.getTopToolbar().on( "toggleExpand", function() {
//               if ( this.rowsExpanded ) {
//                  this.rowExpander.collapseAll();
//                  this.getTopToolbar().expandButton.setText( "Expand all" );
//                  this.rowsExpanded = false;
//               } else {
//                  this.rowExpander.expandAll();
//                  this.getTopToolbar().expandButton.setText( "Collapse all" );
//                  this.rowsExpanded = true;
//               }
//
//            }, this );
//
//            this.getTopToolbar().on(
//               "refresh",
//               function() {
//                  if ( this.store.getModifiedRecords().length > 0 ) {
//                     Ext.Msg.confirm( Gemma.HelpText.CommonWarnings.LoseChanges.title,
//                        Gemma.HelpText.CommonWarnings.LoseChanges.text,
//
//                        function( but ) {
//                           if ( but == 'yes' ) {
//                              this.init();
//                           }
//                        }.createDelegate( this ) );
//                  } else {
//                     this.init();
//                  }
//
//               }, this );
//
//            this.getTopToolbar().on( "filter", function( text ) {
//               this.searchForText( text );
//            }, this );
//
//            if ( this.editable ) {
//
//               /**
//                * Editing of a specific record fires this.
//                */
//               this.on( "afteredit", function( e ) {
//                  var factorId = this.getColumnModel().getColumnId( e.column );
//                  var editor = this.factorValueEditors[factorId];
//
//                  if ( editor.continuous ) {
//                     // e.record.set(factorId, editor.value); // use the value, not the id
//                  } else {
//                     var fvvo = editor.getFactorValue();
//                     e.record.set( factorId, fvvo.id );
//                  }
//
//                  // if (e.originalValue != e.value) {
//                  this.getTopToolbar().saveButton.enable();
//                  this.getView().refresh();
//                  // }
//
//               }, this );
//
//               /**
//                * Bulk update biomaterial -> factorvalue associations (must click save to persist)
//                */
//               this.getTopToolbar().on( "apply", function( factor, factorValue ) {
//                  var selected = this.getSelectionModel().getSelections();
//                  for (var i = 0; i < selected.length; ++i) {
//                     selected[i].set( factor, factorValue );
//                  }
//                  this.getView().refresh();
//               }, this );
//
//               /**
//                * Save edited records to the db.
//                */
//               this.getTopToolbar().on( "save", function() {
//                  // console.log("Saving ...");
//                  this.loadMask.show();
//                  var edited = this.getEditedRecords();
//                  var bmvos = [];
//                  for (var i = 0; i < edited.length; ++i) {
//                     var row = edited[i];
//                     var bmvo = {
//                        id : row.id,
//                        factorIdToFactorValueId : {}
//                     };
//
//                     for ( var j in row) {
//                        if ( typeof j == 'string' && j.indexOf( "factor" ) >= 0 ) {
//                           // console.log(j + "...." + row[j]);
//                           bmvo.factorIdToFactorValueId[j] = row[j];
//                        }
//                     }
//                     bmvos.push( bmvo );
//                  }
//
//                  /*
//                   * When we return from the server, reload the factor values.
//                   */
//                  var callback = this.init; // check
//
//                  ExpressionExperimentController.updateBioMaterials( bmvos, callback );
//               }.createDelegate( this ), this );
//
//               this.getSelectionModel().on( "selectionchange", function( model ) {
//                  var selected = model.getSelections();
//                  this.getTopToolbar().revertButton.disable();
//                  for (var i = 0; i < selected.length; ++i) {
//                     if ( selected[i].dirty ) {
//                        this.getTopToolbar().revertButton.enable();
//                        break;
//                     }
//                  }
//               }.createDelegate( this ), this );
//
//               this.getSelectionModel().on( "selectionchange", function( model ) {
//                  this.enableApplyOnSelect( model );
//               }.createDelegate( this.getTopToolbar() ), this.getTopToolbar() );
//
//               this.getTopToolbar().on( "undo", this.revertSelected, this );
//            }
//
//            this.getStore().load( {
//               params : {},
//               callback : function() {
//                  this.getStore().sort( "factor" );
//                  this.getStore().fireEvent( "datachanged" );
//               },
//               scope : this
//            } );
//         },

        
            //,}
//
//         /**
//          * Represents a FactorValueValueObject; used in the Store for the ComboBoxes.
//          */
//         fvRecord : Ext.data.Record.create( [ {
//            name : "id",
//            type : "string",
//            convert : function( v ) {
//               return "fv" + v;
//            }
//         },{
//             name : "count",
//             type : "int"
//          }, {
//            name : "factor", // human-readable string
//            type : "string"
//         } ] ),
//
//         reloadFactorValues : function() {
//            for ( var i in this.factorValueEditors) {
//               var factorId = this.factorValueEditors[i];
//               if ( typeof factorId == 'string' && factorId.substring( 0, 6 ) == "factor" ) {
//                  var editor = this.factorValueEditors[factorId];
//                  var column = this.getColumnModel().getColumnById( factorId );
//
//                  // this should not fire if it's a continuous variable; this is for combos.
//                  if ( editor.setExperimentalFactor ) {
//                     editor.setExperimentalFactor( editor.experimentalFactor.id, function( r, options, success ) {
//                        this.fvMap = {};
//                        for (var i = 0; i < r.length; ++i) {
//                           var rec = r[i];
//                           this.fvMap["fv" + rec.get( "id" )] = rec.get( "factorValue" );
//                        }
//                        var renderer = this.createValueRenderer();
//                        column.renderer = renderer;
//                        this.getView().refresh();
//                     }.createDelegate( this ) );
//                  }
//               }
//            }
//            this.getTopToolbar().factorValueCombo.store.reload();
//         },
//
//         createValueRenderer : function() {
//
//            return function( value, metadata, record, row, col, ds ) {
//
//               if ( !value ) {
//                  return "-";
//               }
//
//               var k = this.fvMap[value];
//               return k ? k : value;
//
//            }.createDelegate( this );
//         },
//
//         rowExpander : new Ext.grid.RowExpander(
//            {
//               tpl : new Ext.Template(
//                  "<dl style='background-color:#EEE;padding:2px;margin-left:1em;margin-bottom:2px;'><dt>BioMaterial {factor}</dt><dd>{bmDesc}<br>{bmChars}</dd>",
//                  "<dt>BioAssay {count}</dt><dd>{baDesc}</dd></dl>" )
//            } ),
//
//         searchForText : function( text ) {
//            if ( text.length < 1 ) {
//               this.getStore().clearFilter();
//               return;
//            }
//            this.getStore().filterBy( this.filter( text ), this, 0 );
//         },
//
//         filter : function( text ) {
//            var valueRegEx = new RegExp( Ext.escapeRe( text ), 'i' );
//            var fvColumnRegEx = new RegExp( /^fv\d+$/ );
//            var columnArr = this.getColumnModel().config;
//
//            return function( r, id ) {
//               var fields = r.fields;
//               var found = false;
//               var value;
//               fields.each( function( item, index, length ) {
//                  if ( !found ) {
//                     value = r.get( item.name );
//                     if ( fvColumnRegEx.test( value ) ) {
//                        value = (this.fvMap[value]) ? this.fvMap[value] : value;
//                     }
//                     if ( item.name !== "id" && item.name !== "bmDesc" && item.name !== "bmChars"
//                        && item.name !== "baDesc" && valueRegEx.test( value ) ) {
//                        found = true;
//                     }
//                  }
//               }, this );
//               return found;
//            };
//         }
//
//      } );

/**
 * 
 */
//Gemma.BioMaterialToolbar = Ext.extend( Ext.Toolbar, {
//
//   /**
//    * @memberOf Gemma.BioMaterialToolbar
//    */
//   initComponent : function() {
//
//      this.items = [];
//      if ( this.editable ) {
//
//         this.saveButton = new Ext.Toolbar.Button( {
//            text : "Save",
//            tooltip : "Save changed biomaterials",
//            disabled : true,
//            handler : function() {
//               this.fireEvent( "save" );
//               this.saveButton.disable();
//            },
//            scope : this
//         } );
//
//         this.revertButton = new Ext.Toolbar.Button( {
//            text : "Undo",
//            tooltip : "Undo changes to selected biomaterials",
//            disabled : true,
//            handler : function() {
//               this.fireEvent( "undo" );
//            },
//            scope : this
//         } );
//
//         this.factorCombo = new Gemma.ExperimentalFactorCombo( {
//            width : 200,
//            emptyText : "select a factor",
//            edId : this.edId
//         } );
//
//         this.factorCombo.on( "select", function( combo, record, index ) {
//
//            /*
//             * FIXME, don't enable this if the factor is continuous.
//             */
//            this.factorValueCombo.setExperimentalFactor( record.id );
//            this.factorValueCombo.enable();
//         }, this );
//
//         this.factorValueCombo = new Gemma.FactorValueCombo( {
//            emptyText : "Select a factor value",
//            disabled : true,
//            width : 200
//         } );
//
//         this.factorValueCombo.on( "select", function( combo, record, index ) {
//            this.applyButton.enable();
//         }, this );
//
//         this.applyButton = new Ext.Toolbar.Button( {
//            text : "Apply",
//            tooltip : "Apply this value to selected biomaterials",
//            disabled : true,
//            width : 100,
//            handler : function() {
//               // console.log("Apply");
//               var factor = "factor" + this.factorCombo.getValue();
//               var factorValue = "fv" + this.factorValueCombo.getValue();
//               this.fireEvent( "apply", factor, factorValue );
//               this.saveButton.enable();
//            },
//            scope : this
//         } );
//
//         this.items = [ this.saveButton, ' ', this.revertButton, '-', "Bulk changes:", ' ', this.factorCombo, ' ',
//                       this.factorValueCombo, this.applyButton ];
//      }
//
//      var textFilter = new Ext.form.TextField( {
//         ref : 'searchInGrid',
//         tabIndex : 1,
//         enableKeyEvents : true,
//         emptyText : 'Filter samples',
//         listeners : {
//            "keyup" : {
//               fn : function( textField ) {
//                  this.fireEvent( 'filter', textField.getValue() );
//               },
//               scope : this,
//               options : {
//                  delay : 100
//               }
//            }
//         }
//      } );
//
//      var refreshButton = new Ext.Toolbar.Button( {
//         text : "Refresh",
//         tooltip : "Reload the data",
//         handler : function() {
//            this.fireEvent( "refresh" );
//         }.createDelegate( this )
//
//      } );
//
//      var expandButton = new Ext.Toolbar.Button( {
//         ref : 'expandButton',
//         text : "Expand all",
//         tooltip : "Show/hide all biomaterial details",
//         handler : function() {
//            this.fireEvent( "toggleExpand" );
//         }.createDelegate( this )
//      } );
//
//      this.items.push( '->' );
//      this.items.push( textFilter );
//      this.items.push( refreshButton );
//      this.items.push( expandButton );
//
//      Gemma.BioMaterialToolbar.superclass.initComponent.call( this );
//
//      this.addEvents( "revertSelected", "toggleExpand", "apply", "save", "refresh", "undo" );
//   },
//
//   /**
//    * @memberOf Gemma.BioMaterialToolbar
//    */
//   enableApplyOnSelect : function( model ) {
//      var selected = model.getSelections();
//      if ( selected.length > 0 && this.factorValueCombo.getValue() ) {
//         this.applyButton.enable();
//      } else {
//         this.applyButton.disable();
//      }
 
