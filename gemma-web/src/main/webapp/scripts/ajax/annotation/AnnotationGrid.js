Ext.namespace('Gemma');

/**
 * The 'Characteristic browser' grid, also used for the basic Annotation view.
 * 
 * Gemma.AnnotationGrid constructor... div is the name of the div in which to render the grid. config is a hash with the
 * following options:
 * 
 * readMethod : the DWR method that returns the list of AnnotationValueObjects ( e.g.:
 * ExpressionExperimentController.getAnnotation )
 * 
 * readParams : an array of parameters that will be passed to the readMethod ( e.e.: [ { id:x,
 * classDelegatingFor:"ExpressionExperimentImpl" } ] ) or a pointer to a function that will return the array of
 * parameters
 * 
 * editable : if true, the annotations in the grid will be editable
 * 
 * showParent : if true, a link to the parent object will appear in the grid
 * 
 * noInitialLoad : if true, the grid will not be loaded immediately upon creation
 * 
 * 
 * writeMethod : function pointer to server side ajax call to add an annotation eg
 * 
 * removeMethod :function pointer to server side ajax call to remove an annotation
 * 
 * entId : the entity that the annotations belong to eg) eeId or bmId
 * 
 * TODO add writeParams and removeParams methods if more parameters are needed for removing and writing annotations
 * other than entId
 * 
 * @version $Id$
 */

/**
 * Shows tags in a simple row of links.
 * 
 * @class Gemma.AnnotationDataView
 * @extends Ext.DataView
 */
Gemma.AnnotationDataView = Ext.extend(Ext.DataView, {

   readMethod : ExpressionExperimentController.getAnnotation,

   record : Ext.data.Record.create([{
         name : "id",
         type : "int"
      }, {
         name : "classUri"
      }, {
         name : "className"
      }, {
         name : "termUri"
      }, {
         name : "termName"
      }, {
         name : "evidenceCode"
      }, {
         name : "objectClass"
      }]),

   getReadParams : function() {
      return (typeof this.readParams == "function") ? this.readParams() : this.readParams;
   },

   /*
    * Assumes search scope should be experiments only ..
    */
   tpl : new Ext.XTemplate(
      '<tpl for=".">',
      '<span class="ann-wrap" ext:qtip="{className}" ><span  class="x-editable">'
         + '<a ext:qtip="{className} : {termUri}" href="/Gemma/searcher.html?query={termName}&amp;termUri={termUri}&amp;scope=E" style="text-decoration:underline;">{termName}</a></span></span>&nbsp;&nbsp;',
      '</tpl>'),

   itemSelector : 'ann-wrap',
   emptyText : 'No tags',

   initComponent : function() {

      Ext.apply(this, {
            store : new Ext.data.Store({
                  proxy : new Ext.data.DWRProxy(this.readMethod),
                  reader : new Ext.data.ListRangeReader({
                        id : "id"
                     }, this.record)
               })
         });

      Gemma.AnnotationDataView.superclass.initComponent.call(this);

      this.store.load({
            params : this.getReadParams()
         });
   }

});

/*
 * Experimental, not used yet.
 */
Gemma.CharacteristicPagingStore = Ext.extend(Ext.data.Store, {
      constructor : function(config) {
         Gemma.CharacteristicPagingStore.superclass.constructor.call(this, config);
      },
      remoteSort : true,
      proxy : new Ext.data.DWRProxy({
            apiActionToHandlerMap : {
               read : {
                  dwrFunction : CharacteristicBrowserController.browse,
                  getDwrArgsFunction : function(request) {
                     var params = request.params;
                     return [params];
                  }
               }
            }
         }),

      reader : new Ext.data.JsonReader({
            root : 'records', // required.
            successProperty : 'success', // same as default.
            messageProperty : 'message', // optional
            totalProperty : 'totalRecords', // default is 'total'; optional unless paging.
            idProperty : "id", // same as default
            fields : [{
                  name : "id",
                  type : "int"
               }, {
                  name : "objectClass"
               }, {
                  name : "classUri"
               }, {
                  name : "className"
               }, {
                  name : "termUri"
               }, {
                  name : "termName"
               }, {
                  name : "parentLink"
               }, {
                  name : "parentDescription"
               }, {
                  name : "parentOfParentLink"
               }, {
                  name : "parentOfParentDescription"
               }, {
                  name : "evidenceCode"
               }]
         }),

      writer : new Ext.data.JsonWriter({
            writeAllFields : true
         })

   });

/**
 * 
 * @class Gemma.AnnotationGrid
 * @extends Gemma.GemmaGridPanel
 */
Gemma.AnnotationGrid = Ext.extend(Gemma.GemmaGridPanel, {

      // autoHeight : true,
      width : 500,
      height : 200,
      stateful : false,
      taxonId : null,
      name : 'AnnotationGrid',
      entityAnnotsAreValidated : false,

      viewConfig : {
         enableRowBody : true,
         emptyText : 'No annotations',
         showDetails : false,
         getRowClass : function(record, index, p, store) {
            if (this.showDetails) {
               p.body = "<p class='characteristic-body' >" + String.format("From {0}", record.data.parentOfParentLink) + "</p>";
            }
            return '';
         }
      },

      loadMask : {
         msg : Gemma.StatusText.processing,
         store : this.store
      },

      forceValidation : true, // always fire edit event even if text doesn't change - the url might have. But this
      // isn't good enough, ext has a limitation.

      useDefaultToolbar : true,

      record : Ext.data.Record.create([{
            name : "id",
            type : "int"
         }, {
            name : "objectClass"
         }, {
            name : "classUri"
         }, {
            name : "className"
         }, {
            name : "termUri"
         }, {
            name : "termName"
         }, {
            name : "parentLink"
         }, {
            name : "parentDescription"
         }, {
            name : "parentOfParentLink"
         }, {
            name : "parentOfParentDescription"
         }, {
            name : "evidenceCode"
         }]),

      parentStyler : function(value, metadata, record, row, col, ds) {
         return this.formatParentWithStyle(record.id, record.expanded, record.data.parentLink, record.data.parentDescription, record.data.parentOfParentLink,
            record.data.parentOfParentDescription);
         // return parentLink;
      },

      formatParentWithStyle : function(id, expanded, parentLink, parentDescription, parentOfParentLink, parentOfParentDescription) {
         var value;
         value = (parentLink ? (parentLink + "&nbsp;&nbsp;") : "") + (parentDescription ? parentDescription : "");

         if (parentOfParentLink) {
            value = value + "&nbsp;&laquo;&nbsp;" + parentOfParentLink;
         }

         // }
         return expanded ? value.concat(String.format("<div style='white-space: normal;'>{0}</div>", parentDescription)) : value;
      },

      termStyler : function(value, metadata, record, row, col, ds) {
         return Gemma.GemmaGridPanel.formatTermWithStyle(value, record.data.termUri);
      },

      termUriStyler : function(value, metadata, record, row, col, ds) {
         if (record.get('termUri')) {
            return String.format("<a target='_blank' href='{0}'>{0}</a>", record.get('termUri'));
         }
         return '';
      },

      /**
       * 
       */
      initComponent : function() {

         Ext.apply(this, {
               columns : [{
                     header : "Category",
                     dataIndex : "className",
                     sortable : true
                  }, {
                     header : "Term",
                     dataIndex : "termName",
                     renderer : this.termStyler.createDelegate(this),
                     sortable : true
                  }, {
                     header : "Term URI",
                     dataIndex : "termUri",
                     hidden : true,
                     renderer : this.termUriStyler.createDelegate(this),
                     sortable : true
                  }, {
                     header : "Annotation belongs to:",
                     dataIndex : "parentLink",
                     renderer : this.parentStyler.createDelegate(this),
                     tooltip : Gemma.HelpText.WidgetDefaults.AnnotationGrid.parentLinkDescription,
                     hidden : this.showParent ? false : true,
                     sortable : true
                  }, {
                     header : "Evidence",
                     dataIndex : "evidenceCode",
                     sortable : true
                  }]

            });

         if (this.store == null) {
            Ext.apply(this, {
                  store : new Ext.data.Store({
                        proxy : new Ext.data.DWRProxy(this.readMethod),
                        reader : new Ext.data.ListRangeReader({
                              id : "id"
                           }, this.record)
                     })
               });
         }

         if (this.editable && this.useDefaultToolbar) {
            Ext.apply(this, {
                  tbar : new Gemma.AnnotationToolBar({
                        annotationGrid : this,
                        showValidateButton : true,
                        isValidated : this.entityAnnotsAreValidated,
                        createHandler : function(characteristic, callback) {
                           this.writeMethod(characteristic, this.entId, callback);
                        }.createDelegate(this),
                        deleteHandler : function(ids, callback) {
                           this.removeMethod(ids, this.entId, callback);
                        }.createDelegate(this),
                        taxonId : this.taxonId
                     })
               });
         }
         //
         Gemma.AnnotationGrid.superclass.initComponent.call(this);

         this.getStore().setDefaultSort('className');

         this.loadMask.store = this.getStore();

         this.autoExpandColumn = this.showParent ? 2 : 1;

         this.getColumnModel().defaultSortable = true;

         if (this.editable) {

            /*
             * Display all the edit functions.
             */

            var CATEGORY_COLUMN = 0;
            var VALUE_COLUMN = 1;
            var PARENT_COLUMN = 2;
            var EVIDENCE_COLUMN = 3;

            // Category setup
            this.categoryCombo = new Gemma.CategoryCombo({
                  lazyRender : true
               });
            var categoryEditor = new Ext.grid.GridEditor(this.categoryCombo);
            this.categoryCombo.on("select", function(combo, record, index) {
                  categoryEditor.completeEdit();
               });
            this.getColumnModel().setEditor(CATEGORY_COLUMN, categoryEditor);

            // Value setup
            this.valueCombo = new Gemma.CharacteristicCombo({
                  taxonId : this.taxonId
               });
            var valueEditor = new Ext.grid.GridEditor(this.valueCombo);
            this.valueCombo.on("select", function(combo, record, index) {
                  valueEditor.completeEdit();
               });
            this.getColumnModel().setEditor(VALUE_COLUMN, valueEditor);

            // Evidence setup
            this.evidenceCombo = new Gemma.EvidenceCodeCombo({
                  lazyRender : true
               });
            var evidenceEditor = new Ext.grid.GridEditor(this.evidenceCombo);
            this.evidenceCombo.on("select", function(combo, record, index) {
                  evidenceEditor.completeEdit();
               });
            this.getColumnModel().setEditor(EVIDENCE_COLUMN, evidenceEditor);

            this.on("beforeedit", function(e) {
                  var row = e.record.data;
                  var col = this.getColumnModel().getColumnId(e.column);
                  if (col == VALUE_COLUMN) {
                     this.valueCombo.setCategory.call(this.valueCombo, row.className, row.classUri);
                  }
               });

            if (this.getTopToolbar().deleteButton) {
               this.getSelectionModel().on("selectionchange", function(model) {
                     var selected = model.getSelections();
                     if (selected.length > 0) {
                        this.getTopToolbar().deleteButton.enable();
                     } else {
                        this.getTopToolbar().deleteButton.disable();
                     }
                  }, this);
            }

            this.on("afteredit", function(e) {
                  var col = this.getColumnModel().getColumnId(e.column);
                  if (col == CATEGORY_COLUMN) {
                     var term = this.categoryCombo.getTerm.call(this.categoryCombo);
                     e.record.set("className", term.term);
                     e.record.set("classUri", term.uri);
                  } else if (col == VALUE_COLUMN) {
                     var c = this.valueCombo.getCharacteristic.call(this.valueCombo);
                     e.record.set("termName", c.value);
                     e.record.set("termUri", c.valueUri);
                  } else if (col == EVIDENCE_COLUMN) {
                     var c = this.evidenceCombo.getCode.call(this.evidenceCombo);
                     e.record.set("evidenceCode", c.code);
                  }
                  this.getView().refresh();
               });
         }

         this.on("celldblclick", function(grid, rowIndex, cellIndex) {
               var record = grid.getStore().getAt(rowIndex);
               var column = grid.getColumnModel().getColumnId(cellIndex);
               if (column == PARENT_COLUMN) {
                  record.expanded = record.expanded ? 0 : 1;
                  grid.getView().refresh(true);
               }
            }, this);

         if (!this.noInitialLoad) {
            this.getStore().load({
                  params : this.getReadParams()
               });
         }
      },

      getReadParams : function() {
         return (typeof this.readParams == "function") ? this.readParams() : this.readParams;
      },

      /*
       * TODO deal with records, let writer handle conversion.
       */
      getSelectedCharacteristics : function() {
         var selected = this.getSelectionModel().getSelections();
         var chars = [];
         for (var i = 0; i < selected.length; ++i) {
            var row = selected[i].data;
            chars.push(row);
         }
         return chars;
      },

      /*
       * TODO deal with records, let writer handle conversion.
       */
      getEditedCharacteristics : function() {
         var chars = [];
         this.getStore().each(function(record) {
               if (record.dirty) {
                  var row = record.data;
                  chars.push(row);
               }
            }.createDelegate(this), this);
         return chars;
      },

      setEEId : function(id) {
         this.eeId = id;

      }

   });