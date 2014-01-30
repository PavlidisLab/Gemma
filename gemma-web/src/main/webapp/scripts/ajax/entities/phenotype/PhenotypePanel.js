/**
 * It displays all the available phenotyope associations in the system.
 * 
 * @author frances
 * @version $Id$
 */
Ext.namespace('Gemma');

Gemma.PhenotypePanel = Ext.extend(Ext.Panel, {
      // Configs that should be set if used outside of Gemma (BEGIN)
      phenotypeStoreProxy : null,
      geneStoreProxy : null,
      evidenceStoreProxy : null,
      getGeneLink : null,
      // Configs that should be set if used outside of Gemma (END)
      height : 600,
      width : 760,
      layout : 'border',
      initComponent : function() {
         if (!((this.phenotypeStoreProxy && this.geneStoreProxy && this.evidenceStoreProxy && this.getGeneLink) || (!this.phenotypeStoreProxy && !this.geneStoreProxy
            && !this.evidenceStoreProxy && !this.getGeneLink))) {
            Ext.Msg.alert(Gemma.HelpText.WidgetDefaults.PhenotypePanel.setupErrorTitle, Gemma.HelpText.WidgetDefaults.PhenotypePanel.setupErrorText);
         } else {
            var currentPhenotypes = null;
            var currentGene = null;

            var currentFilters = {
               taxonId : '-1',
               showOnlyEditable : false
            };

            // Don't display toolbar if it is running outside of Gemma.
            var phenotypePanelToolbar = Gemma.isRunningOutsideOfGemma() ? null : new Gemma.PhenotypePanelToolbar({
                  listeners : {
                     filterApplied : function(filters) {
                        currentFilters = filters;
                        reloadWholePanel();
                     }
                  }
               });

            var phenotypeTabPanel = new Gemma.PhenotypeTabPanel({
                  region : "west",
                  phenotypeStoreProxy : this.phenotypeStoreProxy,
                  listeners : {
                     phenotypeSelectionChange : function(selectedPhenotypes) {
                        var uniquePhenotypes = [];

                        for (var i = 0; i < selectedPhenotypes.length; i++) {
                           var isUniquePhenotype = true;
                           for (var j = 0; isUniquePhenotype && j < uniquePhenotypes.length; j++) {
                              isUniquePhenotype = (uniquePhenotypes[j].urlId !== selectedPhenotypes[i].urlId);
                           }
                           if (isUniquePhenotype) {
                              uniquePhenotypes.push(Ext.apply({}, selectedPhenotypes[i]));
                           }
                        }

                        geneGrid.setCurrentPhenotypes(currentFilters, uniquePhenotypes);
                        currentPhenotypes = uniquePhenotypes;
                     }
                  }
               });

            this.relayEvents(phenotypeTabPanel, ['phenotypeAssociationChanged']);

            var geneGrid = new Gemma.PhenotypeGeneGridPanel({
                  region : "north",
                  height : 300,
                  split : true,
                  geneStoreProxy : this.geneStoreProxy,
                  getGeneLink : this.getGeneLink,
                  listeners : {
                     geneSelectionChange : function(selectedPhenotypes, selectedGene) {
                        evidenceGrid.setCurrentData(currentFilters, selectedPhenotypes, selectedGene);
                        currentGene = selectedGene;

                        // Reset emptyText.
                        evidenceGrid.getView().emptyText = Gemma.HelpText.WidgetDefaults.PhenotypePanel.noGeneSelectedForEvidenceGridEmptyText;
                        evidenceGrid.getView().applyEmptyText();
                     }
                  }
               });
            geneGrid.getView().deferEmptyText = false;
            geneGrid.getView().emptyText = Gemma.HelpText.WidgetDefaults.PhenotypePanel.noPhenotypeSelectedForGeneGridEmptyText;
            geneGrid.getStore().on('load', function(store, records, options) {
                  if (records.length === 0) {
                     geneGrid.getView().emptyText = Gemma.HelpText.WidgetDefaults.PhenotypePanel.noRecordEmptyText;
                  } else {
                     geneGrid.getView().emptyText = Gemma.HelpText.WidgetDefaults.PhenotypePanel.noPhenotypeSelectedForGeneGridEmptyText;
                  }
                  geneGrid.getView().applyEmptyText();
               });
            this.relayEvents(geneGrid, ['phenotypeAssociationChanged']);

            var evidenceGrid = new Gemma.PhenotypeEvidenceGridPanel({
            	  id : 'evidenceGrid',
                  region : 'center',
                  evidenceStoreProxy : this.evidenceStoreProxy,
                  getGeneLink : this.getGeneLink
               });
            evidenceGrid.getView().deferEmptyText = false;
            evidenceGrid.getView().emptyText = Gemma.HelpText.WidgetDefaults.PhenotypePanel.noGeneSelectedForEvidenceGridEmptyText;
            evidenceGrid.getStore().on('load', function(store, records, options) {
                  if (records.length === 0) {
                     evidenceGrid.getView().emptyText = Gemma.HelpText.WidgetDefaults.PhenotypePanel.noRecordEmptyText;
                  } else {
                     evidenceGrid.getView().emptyText = Gemma.HelpText.WidgetDefaults.PhenotypePanel.noGeneSelectedForEvidenceGridEmptyText;
                  }
                  evidenceGrid.getView().applyEmptyText();
               });
            this.relayEvents(evidenceGrid, ['phenotypeAssociationChanged']);

            // This method needs to be called whenever phenotype selections change in code.
            // Because grid panels in phenotypeTabPanel allow more than one selections,
            // selectionchange events will not be handled in them due to many events
            // being fired e.g. when users select 1 phenotype only and more than one
            // phenotypes have been previously selected. Thus, selection event should
            // be fired manually.
            var fireEventOnPhenotypeSelectionChange = function(phenotypeGrid) {
               var selectedPhenotypes = [];
               var selectionModel = phenotypeGrid.getSelectionModel();
               if (selectionModel.hasSelection()) {
                  var selections = selectionModel.getSelections();
                  for (var i = 0; i < selections.length; i++) {
                     selectedPhenotypes.push(Ext.apply({}, selections[i].data));
                  }
               }

               phenotypeGrid.fireEvent('phenotypeSelectionChange', selectedPhenotypes);
            };

            var selectRecordsOnLoad = function(gridPanel, fieldName, recordIds, callback) {
               gridPanel.getStore().on('load', function(store, records, options) {
                     if (recordIds.length > 0) {
                        var selModel = gridPanel.getSelectionModel();
                        selModel.clearSelections();

                        var firstRowIndex;

                        for (var i = 0; i < recordIds.length; i++) {
                           var currRowIndex = store.findExact(fieldName, recordIds[i]);

                           // Note that we may not be able to find the record after load.
                           if (currRowIndex >= 0) {
                              if (!firstRowIndex) {
                                 firstRowIndex = currRowIndex;
                              }

                              selModel.selectRow(currRowIndex, true); // true to keep existing selections
                              gridPanel.getView().focusRow(currRowIndex);
                           }
                        }
                        if (firstRowIndex >= 0) {
                           gridPanel.getView().focusRow(firstRowIndex); // Make sure the first selected record is
                           // viewable.
                        }

                        if (callback) {
                           callback.call(this);
                        }
                     }
                  }, this, // scope
                  {
                     single : true,
                     delay : 500
                     // Delay the handler. Otherwise, the current record is selected but not viewable in FireFox as of
                  // 2012-02-01 if it is not in the first page of the grid. There is no such issue in Chrome.
               });
            };

            var reloadWholePanel = function() {
               if (currentPhenotypes != null && currentPhenotypes.length > 0) {
                  var currentPhenotypeUrlIds = [];
                  for (var i = 0; i < currentPhenotypes.length; i++) {
                     currentPhenotypeUrlIds.push(currentPhenotypes[i].urlId);
                  }
                  var phenotypeActiveTabGrid = phenotypeTabPanel.getActiveTab();

                  selectRecordsOnLoad(phenotypeActiveTabGrid, 'urlId', currentPhenotypeUrlIds, function() {
                        fireEventOnPhenotypeSelectionChange(phenotypeActiveTabGrid);
                     });

                  if (currentGene != null) {
                     // geneGrid's store will be loaded after phenotypeGrid's original rows are selected later on.
                     selectRecordsOnLoad(geneGrid, 'id', [currentGene.id]);
                  }
               }

               phenotypeTabPanel.reloadActiveTab(currentFilters);
            };

            if (!Gemma.isRunningOutsideOfGemma()) {
               Gemma.Application.currentUser.on("logIn", reloadWholePanel);
               Gemma.Application.currentUser.on("logOut", function() {
                     currentFilters.showOnlyEditable = false;
                     phenotypePanelToolbar.setShowOnlyEditableCheckbox(currentFilters.showOnlyEditable);
                     reloadWholePanel();
                  });
            }

            Ext.apply(this, {
                  tbar : phenotypePanelToolbar,
                  items : [phenotypeTabPanel, {
                        xtype : 'panel',
                        height : 200,
                        layout : 'border',
                        viewConfig : {
                           forceFit : true
                        },
                        items : [geneGrid, evidenceGrid],
                        region : 'center',
                        split : true
                     }],
                  listeners : {
                     phenotypeAssociationChanged : function(phenotypes, gene) {
                        if (phenotypes != null && gene != null) {
                           currentPhenotypes = phenotypes;
                           currentGene = gene;
                        }
                        reloadWholePanel();
                     },
                     scope : this
                  }
               });

            if (Ext.get("phenotypeUrlId") != null && Ext.get("phenotypeUrlId").getValue() != "") {
               var phenotypeActiveTabGrid = phenotypeTabPanel.getActiveTab();

               selectRecordsOnLoad(phenotypeActiveTabGrid, 'urlId', [Ext.get("phenotypeUrlId").getValue()], function() {
                     fireEventOnPhenotypeSelectionChange(phenotypeActiveTabGrid);
                  });

               if (Ext.get("geneId") != null && Ext.get("geneId").getValue() != "") {
                  selectRecordsOnLoad(geneGrid, 'id', [parseInt(Ext.get("geneId").getValue())]);
               }
            }
         }

         Gemma.PhenotypePanel.superclass.initComponent.call(this);
      }
   });

Gemma.PhenotypePanelSearchField = Ext.extend(Ext.form.TwinTriggerField, {
      enableKeyEvents : true,
      validationEvent : false,
      validateOnBlur : false,
      trigger1Class : 'x-form-clear-trigger',
      trigger2Class : 'x-form-search-trigger',
      hideTrigger1 : true,
      width : 220,
      hasSearch : false,
      listeners : {
         keyup : function(field, e) {
            var typedStringLength = this.getRawValue().length;

            // Start searching after users have typed at least two characters.
            // If they remove all characters, call onTrigger1Click() to
            // remove the clear button.
            if (typedStringLength >= 2) {
               this.onTrigger2Click();
            } else if (typedStringLength < 1) {
               this.onTrigger1Click();
            }
         }
      },
      onTrigger1Click : function() {
         if (this.hasSearch) {
            this.el.dom.value = '';
            this.triggers[0].hide();
            this.hasSearch = false;
            this.fireEvent('filterRemoved');
         }
         Gemma.PhenotypePanelSearchField.superclass.onTrigger1Click.call(this);
      },
      onTrigger2Click : function() {
         var typedString = this.getRawValue().toLowerCase();
         if (typedString.length < 1) {
            this.onTrigger1Click();
            return;
         }

         this.hasSearch = true;
         this.triggers[0].show();

         var recordFilter = function(record, filterFields) {
            for (var i = 0; i < filterFields.length; i++) {
               if (record.get(filterFields[i]).toLowerCase().indexOf(typedString) >= 0) {
                  return true;
               }
            }
            return false;
         };
         this.fireEvent('filterApplied', recordFilter);

         Gemma.PhenotypePanelSearchField.superclass.onTrigger2Click.call(this);
      },
      applyCurrentFilter : function() {
         this.onTrigger2Click();
      }
   });
