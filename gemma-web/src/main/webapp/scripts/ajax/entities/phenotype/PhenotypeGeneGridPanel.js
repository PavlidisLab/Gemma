/**
 * It displays genes linked to the current phenotypes.
 * 
 * @author frances
 * @version $Id$
 */
Ext.namespace('Gemma');

Gemma.PhenotypeGeneGridPanel = Ext.extend(Ext.grid.GridPanel, {
      title : 'Genes',
      autoScroll : true,
      stripeRows : true,
      loadMask : true,
      // Note that a general emptyText should NOT be defined here because this widget can
      // be used in other places and a general emptyText may not make sense for all cases.
      // If emptyText is needed, it should be defined outside of this class.
      viewConfig : {
         forceFit : true
      },
      currentPhenotypes : null,
      initComponent : function() {
         var DEFAULT_TITLE = this.title; // A constant title that will be used when we don't have current phenotypes.

         var titleText = this.title; // Contains the title's text without any HTML code whereas title may contain HTML
                                       // code.

         var phenotypeAssociationFormWindow;

         var currentStoreData = [];

         var downloadButton = new Ext.Button({
               text : '<b>Download</b>',
               disabled : true,
               icon : '/Gemma/images/download.gif',
               handler : function() {
                  var columnConfig = [{
                        header : 'NCBI ID',
                        dataIndex : 'ncbiId' // first column
                     }].concat(this.getColumnModel().config); // rest of columns

                  var downloadData = [];
                  var downloadDataRow = [];

                  for (var i = 0; i < columnConfig.length; i++) {
                     downloadDataRow.push(columnConfig[i].header);
                  }
                  downloadData.push(downloadDataRow);

                  this.getStore().each(function(record) {
                        downloadDataRow = [];
                        for (var i = 0; i < columnConfig.length; i++) {
                           downloadDataRow.push(record.get(columnConfig[i].dataIndex));
                        }
                        downloadData.push(downloadDataRow);
                     });

                  var downloadDataHeader = titleText;
                  if (geneSearchField.getValue() !== '') {
                     downloadDataHeader += ' AND matching pattern "' + geneSearchField.getValue() + '"';
                  }
                  var textWindow = new Gemma.DownloadWindow({
                        windowTitleSuffix : 'Genes associated with selected Phenotype(s)',
                        downloadDataHeader : downloadDataHeader,
                        downloadData : downloadData,
                        modal : true
                     });
                  textWindow.convertToText();
                  textWindow.show();
               },
               scope : this
            });

         var geneSearchField = new Gemma.PhenotypePanelSearchField({
               emptyText : 'Search Genes',
               disabled : true,
               listeners : {
                  filterApplied : function(recordFilter) {
                     var filterFields = ['officialSymbol', 'officialName'];
                     this.getStore().filterBy(function(record) {
                           if (this.getSelectionModel().isSelected(record) || recordFilter(record, filterFields)) {
                              return true;
                           }
                           return false;
                        }, this);
                  },
                  filterRemoved : function() {
                     this.getStore().clearFilter(false);
                  },
                  scope : this
               }
            });

         var createPhenotypeAssociationButton = new Ext.Button({
               disabled : true,
               handler : Gemma.isRunningOutsideOfGemma() ? function() {
                  Ext.Msg.alert(Gemma.HelpText.WidgetDefaults.PhenotypePanel.modifyPhenotypeAssociationOutsideOfGemmaTitle,
                     Gemma.HelpText.WidgetDefaults.PhenotypePanel.modifyPhenotypeAssociationOutsideOfGemmaText);
               } : function() {
                  if (!phenotypeAssociationFormWindow || (phenotypeAssociationFormWindow && phenotypeAssociationFormWindow.isDestroyed)) {
                     phenotypeAssociationFormWindow = new Gemma.PhenotypeAssociationForm.Window();
                     this.relayEvents(phenotypeAssociationFormWindow, ['phenotypeAssociationChanged']);
                  }

                  phenotypeAssociationFormWindow.showWindow(Gemma.PhenotypeAssociationForm.ACTION_CREATE, {
                        gene : null,
                        phenotypes : this.currentPhenotypes
                     });
               },
               scope : this,
               icon : "/Gemma/images/icons/add.png",
               tooltip : "Add new phenotype association"
            });

         var onStoreRecordChange = function() {
            downloadButton.setDisabled(this.getStore().getCount() <= 0);
         };

         Ext.apply(this, {
               store : new Ext.data.Store({
                     proxy : this.geneStoreProxy == null ? new Ext.data.DWRProxy({
                           apiActionToHandlerMap : {
                              read : {
                                 dwrFunction : PhenotypeController.findCandidateGenes,
                                 getDwrArgsFunction : function(request) {
                                    return [request.params['taxonId'], request.params['showOnlyEditable'], request.params["phenotypeValueUris"]];

                                 }
                              }
                           }
                        }) : this.geneStoreProxy,
                     reader : new Ext.data.JsonReader({
                           idProperty : 'id', // same as default
                           fields : ['id', 'ncbiId', 'taxonId', {
                                 name : 'officialSymbol',
                                 sortType : Ext.data.SortTypes.asUCString
                              }, // case-insensitively
                              {
                                 name : 'officialName',
                                 sortType : Ext.data.SortTypes.asUCString
                              }, // case-insensitively
                              'taxonCommonName']
                        }),
                     sortInfo : {
                        field : 'officialSymbol',
                        direction : 'ASC'
                     },
                     listeners : {
                        clear : onStoreRecordChange,
                        datachanged : onStoreRecordChange,
                        // The main purpose of this load listener is to fix the problem that loading genes for current
                        // phenotypes
                        // may take a very long time and current phenotypes may have been changed by users during this
                        // loading period.
                        // By the time genes are loaded, these genes will replace existing genes for current phenotypes.
                        // The solution is to check if load options' params have been changed. If they have been
                        // changed, we should
                        // load back previous gene data.
                        load : function(store, records, options) {
                           // options.params can be null if we use loadData() to fix the problem.
                           if (options.params != null) {
                              var haveSameParams = (options.params.phenotypeValueUris.length === this.currentPhenotypes.length);

                              if (haveSameParams) {
                                 for (var i = 0; haveSameParams && i < options.params.phenotypeValueUris.length; i++) {
                                    haveSameParams = (options.params.phenotypeValueUris[i] === this.currentPhenotypes[i].valueUri);
                                 }
                              }

                              if (haveSameParams) {
                                 currentStoreData = [];
                                 for (var i = 0; i < records.length; i++) {
                                    currentStoreData.push(Ext.apply({}, records[i].data));
                                 }
                              } else {
                                 store.loadData(currentStoreData);
                              }
                           }
                        },
                        scope : this
                     }
                  }),
               columns : [{
                  header : "Symbol",
                  dataIndex : 'officialSymbol',
                  width : 65,
                  renderer : function(value, metadata, record, rowIndex, colIndex, store) {
                     var geneLink = this.getGeneLink ? this.getGeneLink(record.data.id) : '/Gemma/gene/showGene.html?id=' + record.data.id;

                     return String.format("{0} <a target='_blank' href='" + geneLink
                           + "' ext:qtip='Go to {0} Details (in new window)'><img src='/Gemma/images/icons/magnifier.png' height='10' width='10'/></a> ",
                        record.data.officialSymbol);

                  },
                  sortable : true,
                  scope : this
               }, {
                  header : "Name",
                  dataIndex : 'officialName',
                  width : 215,
                  renderToolTip : true,
                  sortable : true
               }, {
                  header : "Taxon",
                  dataIndex : 'taxonCommonName',
                  width : 100,
                  sortable : true
               }],
               selModel : new Ext.grid.RowSelectionModel({
                     singleSelect : true,
                     listeners : {
                        selectionchange : function(selModel) {
                           var selectedGene = null;

                           if (selModel.hasSelection()) {
                              var geneGridSelection = this.getSelectionModel().getSelected();

                              selectedGene = {
                                 id : geneGridSelection.get('id'),
                                 ncbiId : geneGridSelection.get('ncbiId'),
                                 officialSymbol : geneGridSelection.get('officialSymbol'),
                                 officialName : geneGridSelection.get('officialName'),
                                 taxonCommonName : geneGridSelection.get('taxonCommonName'),
                                 taxonId : geneGridSelection.get('taxonId')
                              };
                           }
                           this.fireEvent('geneSelectionChange', this.currentPhenotypes, selectedGene);
                        },
                        scope : this
                     }
                  }),
               tbar : [geneSearchField, createPhenotypeAssociationButton, downloadButton],
               setCurrentPhenotypes : function(currentFilters, currentPhenotypes) {
                  this.currentPhenotypes = currentPhenotypes;

                  var hasCurrentPhenotypes = (currentPhenotypes != null && currentPhenotypes.length > 0);

                  createPhenotypeAssociationButton.setDisabled(!hasCurrentPhenotypes);
                  geneSearchField.setDisabled(!hasCurrentPhenotypes);
                  geneSearchField.setValue('');

                  if (hasCurrentPhenotypes) {
                     var currentPhenotypeValueUris = [];

                     var selectedPhenotypePrefix = 'Genes associated with';
                     var selectedPhenotypeHeader = selectedPhenotypePrefix + ' "';
                     var selectedPhenotypeTooltip = '&nbsp;&nbsp;&nbsp;';

                     for (var i = 0; i < currentPhenotypes.length; i++) {
                        var currPhenotypeValue = currentPhenotypes[i].value;

                        currentPhenotypeValueUris.push(currentPhenotypes[i].valueUri);

                        selectedPhenotypeHeader += currPhenotypeValue;
                        selectedPhenotypeTooltip += currPhenotypeValue;

                        if (i < currentPhenotypes.length - 1) {
                           selectedPhenotypeHeader += '" AND "';
                           selectedPhenotypeTooltip += '<br />&nbsp;&nbsp;&nbsp;';
                        } else {
                           selectedPhenotypeHeader += '"';
                        }
                     }

                     this.getStore().reload({
                           params : {
                              taxonId : currentFilters.taxonId,
                              showOnlyEditable : currentFilters.showOnlyEditable,
                              phenotypeValueUris : currentPhenotypeValueUris
                           }
                        });
                     this.getSelectionModel().clearSelections(false);

                     this.setTitle("<div style='height: 15px; overflow: hidden;' " + // Make the header one line only.
                        "ext:qtitle='" + selectedPhenotypePrefix + "' " + "ext:qtip='" + selectedPhenotypeTooltip + "'>" + selectedPhenotypeHeader + "</div>");
                     titleText = selectedPhenotypeHeader;
                  } else {
                     currentStoreData = [];

                     this.setTitle(DEFAULT_TITLE);
                     this.getStore().removeAll(false);
                  }
               }
            });

         Gemma.PhenotypeGeneGridPanel.superclass.initComponent.call(this);
      }
   });
