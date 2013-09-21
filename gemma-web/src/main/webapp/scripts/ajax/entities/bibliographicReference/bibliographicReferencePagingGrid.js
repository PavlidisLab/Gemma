Ext.namespace('Gemma.BibliographicReference');

Gemma.BibliographicReference.PagingStore = Ext.extend(Ext.data.Store, {
      initComponent : function() {
         Gemma.BibliographicReference.PagingStore.superclass.initComponent.call(this);
      },
      remoteSort : true,
      proxy : new Ext.data.DWRProxy({
            apiActionToHandlerMap : {
               read : {
                  dwrFunction : BibliographicReferenceController.browse,
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
            totalProperty : 'totalRecords', // default is 'total'; optional unless
            // paging.
            idProperty : "id", // same as default
            fields : Gemma.BibliographicReference.Record
         }),

      writer : new Ext.data.JsonWriter({
            writeAllFields : true
         })

   });
Gemma.BibliographicReference.PagingGrid = Ext.extend(Ext.grid.GridPanel, {
      loadMask : true,
      autoScroll : true,
      layout : 'fit',
      viewConfig : {
         forceFit : true
      },

      colModel : new Ext.grid.ColumnModel({
            defaultSortable : true,
            columns : [{
                  header : "Authors",
                  dataIndex : 'authorList',
                  width : 215
               }, {
                  header : "Title",
                  dataIndex : 'title',
                  id : 'title',
                  width : 350
               }, {
                  header : "Publication",
                  dataIndex : 'publication',
                  width : 135
               }, {
                  header : "Date",
                  dataIndex : 'publicationDate',
                  width : 70,
                  renderer : Ext.util.Format.dateRenderer("Y")
               }, {
                  header : "Pages",
                  dataIndex : 'pages',
                  width : 80,
                  sortable : false
               }, {
                  header : "Experiments",
                  dataIndex : 'experiments',
                  width : 80,
                  renderer : function(value) {
                     var result = "";
                     for (var i = 0; i < value.length; i++) {
                        result = result + '&nbsp<a target="_blank" ext:qtip="View details of ' + value[i].shortName + ' (' + value[i].name
                           + ')" href="/Gemma/expressionExperiment/showExpressionExperiment.html?id=' + value[i].id + '">' + value[i].shortName + '</a>';
                     }
                     return result;
                  }

               }, {
                  header : "PubMed",
                  dataIndex : 'citation',
                  width : 70,
                  renderer : function(value) {
                     if (value && value.pubmedURL) {
                        return '<a target="_blank" href="' + value.pubmedURL
                           + '"><img ext:qtip="View at NCBI PubMed"  src="/Gemma/images/pubmed.gif" width="47" height="15" /></a>';
                     }
                     return '';
                  },
                  sortable : false
               }]
         }),
      initComponent : function() {

         var pagingStore = new Gemma.BibliographicReference.PagingStore({
               autoLoad : {
                  params : {
                     start : 0,
                     limit : 20
                  }
               }
            });

         var mybbar = new Ext.PagingToolbar({
               store : pagingStore, // grid and PagingToolbar using same
               // store
               displayInfo : true,
               pageSize : 20,
               plugins : [new Ext.ux.PageSizePlugin()]
            });

         var mytbar = new Ext.Toolbar({
               items : [new Ext.CycleButton({
                        ref : 'btnFilter',
                        showText : true,
                        prependText : 'Filter by ',
                        items : [{
                              text : 'Authors',
                              id : 'authorList',
                              iconCls : 'view-text',
                              checked : true
                           }, {
                              text : 'Title',
                              id : 'title',
                              iconCls : 'view-text'
                           }, {
                              text : 'PudMed ID',
                              id : 'pubAccession',
                              iconCls : 'view-text'
                           }, {
                              text : 'Mesh Terms',
                              id : 'meshTerms',
                              iconCls : 'view-text'

                           }]
                     }), new Ext.form.TextField({
                        ref : 'searchInGridField',
                        enableKeyEvents : true,
                        emptyText : 'Filter',
                        listeners : {
                           'keyup' : function() {
                              var txtValue = this.getTopToolbar().searchInGridField.getValue();
                              this.getStore().clearFilter();

                              if (txtValue.length > 1) {
                                 this.getStore().filter(this.getTopToolbar().btnFilter.getActiveItem().id, txtValue, true, false);
                              }
                           },
                           scope : this
                        }
                     })]
            });

         Ext.apply(this, {

               store : pagingStore,

               sm : new Ext.grid.RowSelectionModel({
                     singleSelect : true,
                     listeners : {

                        // when a row is selected trigger an action, ex: populate a details panel about the selected row
                        rowselect : function(sm, index, record) {
                           this.fireEvent('bibRefSelected', record);
                        },
                        scope : this
                     }
                  }),
               tbar : mytbar,
               bbar : mybbar
            });

         Gemma.BibliographicReference.PagingGrid.superclass.initComponent.call(this);
         // when the grid loads select the first row by default
         this.on('render', function() {
               this.getSelectionModel().selectFirstRow();
            }, this);
      }// eo initC
   });
