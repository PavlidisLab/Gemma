/**
 * It displays all the available phenotypes in the system.
 * 
 * @author frances
 * @version $Id$
 */
Ext.namespace('Gemma');

Gemma.PhenotypeTreeGridPanel = Ext.extend(Ext.ux.maximgb.tg.GridPanel, {
      storeAutoLoad : false,
      title : "Phenotypes Tree",
      autoScroll : true,
      stripeRows : true,
      loadMask : true,
      master_column_id : 'value', // for using maximgb's GridPanel
      autoExpandColumn : 'value', // for using maximgb's GridPanel
      initComponent : function() {
         var DISABLED_CLASS = 'x-item-disabled';
         var PHENOTYPE_COLUMN_INDEX = 1;

         var currentSearchedCellRowIndices = [];

         var setRowLookSelected = function(index, isLookSelected) {
            var view = this.getView();
            if (isLookSelected) {
               if (this.getSelectionModel().isSelected(index)) {
                  this.getSelectionModel().deselectRow(index);
               }

               view.addRowClass(index, DISABLED_CLASS);
               view.addRowClass(index, view.selectedRowClass);
            } else {
               view.removeRowClass(index, DISABLED_CLASS);
               view.removeRowClass(index, view.selectedRowClass);
            }
         }.createDelegate(this);

         var isAncestorSelected = function(record) {
            var ancestors = this.getStore().getNodeAncestors(record);
            var isSelected = false;
            for (var i = 0; !isSelected && i < ancestors.length; i++) {
               isSelected = this.getSelectionModel().isSelected(ancestors[i]);
            }

            return isSelected;
         }.createDelegate(this);

         var findSamePhenotypeIndices = function(urlId) {
            var allIndices = [];
            var index = this.getStore().findExact('urlId', urlId, 0);

            while (index >= 0) {
               allIndices.push(index);
               index = this.getStore().findExact('urlId', urlId, index + 1);
            }

            return allIndices;
         }.createDelegate(this);

         var setDescendantRowsLookSelected = function(parentRecord, isLookSelected) {
            var childrenRecords = this.getStore().getNodeChildren(parentRecord);
            var view = this.getView();

            for (var i = 0; i < childrenRecords.length; i++) {
               var currIndex = this.getStore().indexOf(childrenRecords[i]);

               var samePhenotypeIndices = findSamePhenotypeIndices(childrenRecords[i].data.urlId);

               var shouldChangeRowLookSelected = true;
               // When we deselect rows that look selected, if ancestor rows
               // have been selected, we should not change selection states.
               if (!isLookSelected) {
                  for (var j = 0; shouldChangeRowLookSelected && j < samePhenotypeIndices.length; j++) {
                     shouldChangeRowLookSelected = !isAncestorSelected(this.getStore().getAt(samePhenotypeIndices[j]));
                  }
               }
               if (shouldChangeRowLookSelected) {
                  for (var j = 0; j < samePhenotypeIndices.length; j++) {
                     setRowLookSelected(samePhenotypeIndices[j], isLookSelected);
                  }

                  setRowLookSelected(currIndex, isLookSelected);
               }

               if (this.getStore().hasChildNodes(childrenRecords[i])) {
                  setDescendantRowsLookSelected(childrenRecords[i], isLookSelected);
               }
            }
         }.createDelegate(this);

         // This variable is only used by the function following it. When selectRow and
         // deselectRow are called, events will be fired and the function will be called
         // in event listeners. So, this variable is used to indicate if we are selecting
         // rows that have the same phenotype.
         var isSelectingSamePhenotypes = false;
         var setSamePhenotypeRowsSelected = function(urlId, currentRowIndex, isSelected) {
            if (!isSelectingSamePhenotypes) {
               var samePhenotypeIndices = findSamePhenotypeIndices(urlId);

               for (var i = 0; i < samePhenotypeIndices.length; i++) {
                  if (i == 0) {
                     isSelectingSamePhenotypes = true;
                  }
                  if (samePhenotypeIndices[i] != currentRowIndex) {
                     if (isSelected) {
                        // true to keep existing selections
                        this.getSelectionModel().selectRow(samePhenotypeIndices[i], true);
                     } else {
                        this.getSelectionModel().deselectRow(samePhenotypeIndices[i]);
                     }
                  }
                  if (i == samePhenotypeIndices.length - 1) {
                     isSelectingSamePhenotypes = false;
                  }
               }
            }
         }.createDelegate(this);

         var checkboxSelectionModel = new Ext.grid.CheckboxSelectionModel({
               singleSelect : false,
               header : '', // remove the "select all" checkbox on the header
               listeners : {
                  beforerowselect : function(selectionModel, rowIndex, keep, record) {
                     return !Ext.get(this.getView().getRow(rowIndex)).hasClass(DISABLED_CLASS);
                  },
                  rowdeselect : function(selectionModel, rowIndex, record) {
                     setDescendantRowsLookSelected(record, false);
                     setSamePhenotypeRowsSelected(record.data.urlId, rowIndex, false);
                  },
                  rowselect : function(selectionModel, rowIndex, record) {
                     setDescendantRowsLookSelected(record, true);
                     setSamePhenotypeRowsSelected(record.data.urlId, rowIndex, true);
                  },
                  scope : this
               }
            });

         var phenotypeSearchComboBox = new Ext.form.ComboBox({
               allowBlank : true,
               editable : true,
               forceSelection : true,
               mode : 'local',
               store : new Ext.data.JsonStore({
                     fields : [{
                           name : 'value',
                           sortType : Ext.data.SortTypes.asUCString
                        }, // case-insensitively
                        'urlId'],
                     idProperty : 'urlId',
                     sortInfo : {
                        field : 'value',
                        direction : 'ASC'
                     }
                  }),
               valueField : 'urlId',
               displayField : 'value',
               width : 200,
               hideTrigger : true,
               typeAhead : false,
               emptyText : 'Search Phenotypes',
               triggerAction : 'all',
               selectOnFocus : true
            });

         phenotypeSearchComboBox.on({
               select : function(comboBox, record, index) {
                  applyPhenotypeSearch();
               },
               blur : function(comboBox) {
                  applyPhenotypeSearch();
               },
               scope : this
            });

         var applyPhenotypeSearch = function() {
            for (var i = 0; i < currentSearchedCellRowIndices.length; i++) {
               this.getView().onCellDeselect(currentSearchedCellRowIndices[i], PHENOTYPE_COLUMN_INDEX);
            }

            currentSearchedCellRowIndices = findSamePhenotypeIndices(phenotypeSearchComboBox.getValue());
            for (var i = 0; i < currentSearchedCellRowIndices.length; i++) {
               this.getView().onCellSelect(currentSearchedCellRowIndices[i], PHENOTYPE_COLUMN_INDEX);
               this.getView().ensureVisible(currentSearchedCellRowIndices[i], PHENOTYPE_COLUMN_INDEX, false); // false
                                                                                                               // for
                                                                                                               // hscroll
            }
         }.createDelegate(this);

         var commonConfig = new Gemma.PhenotypeGridPanelCommonConfig();

         Ext.apply(this, {
               viewConfig : commonConfig.getViewConfig(),
               store : new Ext.ux.maximgb.tg.AdjacencyListStore({
                     proxy : commonConfig.getStoreProxy(this.phenotypeStoreProxy),
                     baseParams : commonConfig.getBaseParams(),
                     reader : new Ext.data.JsonReader({
                           // Use _id instead of urlId so phenotypes can show up more than once in different parts of
                           // the tree.
                           idProperty : '_id',
                           fields : commonConfig.getStoreReaderFields()
                        }),
                     autoLoad : this.storeAutoLoad
                  }),
               columns : [checkboxSelectionModel, commonConfig.getPhenotypeValueColumn({
                        id : 'value'
                     }), // for using maximgb's GridPanel
                  commonConfig.getGeneCountColumn()],
               sm : checkboxSelectionModel,
               listeners : {
                  hide : commonConfig.getHideHandler,
                  cellclick : commonConfig.getCellClickHandler
               },
               tbar : [phenotypeSearchComboBox, commonConfig.getAddNewPhenotypeAssociationButton(this), {
                     handler : function() {
                        this.loadMask.show();

                        // Defer the call. Otherwise, the loading mask does not show.
                        Ext.defer(function() {
                              this.getStore().collapseAll();
                           }, 10, this);

                     },
                     scope : this,
                     icon : "/Gemma/images/icons/details_hidden.gif",
                     tooltip : "Collapse all"
                  }, {
                     handler : function() {
                        this.loadMask.show();

                        // Defer the call. Otherwise, the loading mask does not show.
                        Ext.defer(function() {
                              this.getStore().expandAll();
                           }, 10, this);
                     },
                     scope : this,
                     icon : "/Gemma/images/icons/details.gif",
                     tooltip : "Expand all"
                  }]
            });

         Gemma.PhenotypeTreeGridPanel.superclass.initComponent.call(this);

         this.getStore().on('load', function(store, records, options) {
               commonConfig.resetSelectionConfig();

               var phenotypeSearchComboBoxData = [];
               for (var i = 0; i < records.length; i++) {
                  phenotypeSearchComboBoxData.push({
                	    value : records[i].data.value+" ("+records[i].data.urlId+")",
                        urlId : records[i].data.urlId
                     });
               }
               phenotypeSearchComboBox.getStore().loadData(phenotypeSearchComboBoxData);

               if (phenotypeSearchComboBox.getValue() !== '') {
                  currentSearchedCellRowIndices = [];

                  applyPhenotypeSearch();

                  // If previously selected cells cannot be found, we assume that
                  // previously selected phenotypes are private. So, clear the search field.
                  if (currentSearchedCellRowIndices.length <= 0) {
                     phenotypeSearchComboBox.setValue('');
                  }
               }
            }, this, // scope
            {
               delay : 500
               // Delay the handler. Otherwise, the current record is selected but not viewable in FireFox as of
               // 2012-02-01 if it is not in the first page of the grid. There is no such issue in Chrome.
         });

         // Hide the loadMask which is shown when collapse all or expand all is executed.
         this.getStore().on('datachanged', function(store, rc) {
               if (this.loadMask instanceof Ext.LoadMask) {
                  this.loadMask.hide();
               }
            }, this);
      }
   });
