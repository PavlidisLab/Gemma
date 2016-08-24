/**
 * Common config for components in phenotype tab panel
 * 
 * @author frances
 * @version $Id$
 */
Ext.namespace('Gemma');

Gemma.PhenotypeGridPanelCommonConfig = Ext.extend(Object, {
      constructor : function() {
         var clickedSelections = [];

         var phenotypeAssociationFormWindow;

         var phenotypeStoreProxy = null;

         var generateGeneCountHTML = function(width, geneCountText) {
            return '<span style="float: right; text-align: right; width: ' + width + 'px;">' + geneCountText + '</span>';
         };

         Ext.apply(this, {
               resetSelectionConfig : function() {
                  // It is set to null instead of [] to indicate that there may
                  // be selections, but we don't know what they are. Maybe, there
                  // is a better way to implement this situation.
                  clickedSelections = null;
               },
               getViewConfig : function() {
                  // Don't need any emptyText.
                  return {
                     forceFit : true
                  };
               },
               getStoreProxy : function(defaultProxy) {
                  var proxyToBeReturned;

                  if (defaultProxy == null) {
                     if (phenotypeStoreProxy == null) {
                        phenotypeStoreProxy = new Ext.data.DWRProxy({
                              apiActionToHandlerMap : {
                                 read : {
                                    dwrFunction : PhenotypeController.loadAllPhenotypesByTree,
                                    getDwrArgsFunction : function(request) {
                                       return [request.params['taxonId'], request.params['showOnlyEditable'],request.params['databaseIds']];
                                    }.createDelegate(this)
                                 }
                              }
                           });
                     }
                     proxyToBeReturned = phenotypeStoreProxy;
                  } else {
                     proxyToBeReturned = defaultProxy;
                  }

                  return proxyToBeReturned;
               },
               getBaseParams : function() {
                  return {
                     taxonId : '-1',
                     showOnlyEditable : false,
                     databaseIds : null
                  };
               },
               getStoreReaderFields : function() {
                  return ['urlId', {
                        name : 'value',
                        sortType : Ext.data.SortTypes.asUCString
                     }, 'valueUri', 'publicGeneCount', 'privateGeneCount', '_id', // for phenotype tree only
                     '_parent', // for phenotype tree only
                     '_is_leaf', // for phenotype tree only
                     'children', // for phenotype tree only, ids of the children.
                     'dbPhenotype', // for phenotype list only
                     {
                        name : 'isChecked',
                        sortDir : 'DESC',
                        defaultValue : false
                     } // for phenotype list only
                  ];
               },
               getHideHandler : function(gridPanel) {
                  // false NOT to bypass the conditional checks and events described in deselectRow
                  gridPanel.getSelectionModel().clearSelections(false);
                  clickedSelections = [];
                  gridPanel.fireEvent('phenotypeSelectionChange', clickedSelections);
               },
               // cellclick instead of selection model's selectionchange event handler is implemented
               // for letting listeners know that phenotype selections have been changed
               // because selectionchange events are fired even when rows are deselected in code.
               getCellClickHandler : function(gridPanel, rowIndex, columnIndex, event) {
                  var newSelections = gridPanel.getSelectionModel().getSelections();

                  var hasSameSelections = (clickedSelections != null && clickedSelections.length === newSelections.length);

                  if (hasSameSelections) {
                     for (var i = 0; hasSameSelections && i < clickedSelections.length; i++) {
                        hasSameSelections = (clickedSelections[i].get('urlId') === newSelections[i].get('urlId'));
                     }
                  }

                  if (!hasSameSelections) {
                     var selectedPhenotypes = [];

                     clickedSelections = newSelections;
                     for (var i = 0; i < clickedSelections.length; i++) {
                        selectedPhenotypes.push(Ext.apply({}, clickedSelections[i].data));
                     }
                     gridPanel.fireEvent('phenotypeSelectionChange', selectedPhenotypes);
                  }
               },
               getPhenotypeValueColumn : function(defaults) {
                  return Ext.apply({
                        header : "Phenotype",
                        dataIndex : 'value',
                        width : 215,
                        renderer : function(value, metaData, record, rowIndex, colIndex, store) {
                           metaData.attr = 'ext:qtip="' + value + '<br />' + record.data.valueUri + '"';
                           return value;
                        }
                     }, defaults);
               },
               getGeneCountColumn : function(defaults) {
                  return Ext.apply({
                        header : "Gene Count",
                        dataIndex : 'publicGeneCount',
                        align : "right",
                        width : 115,
                        renderer : function(value, metaData, record, rowIndex, colIndex, store) {
                           // Use min-width so that the cell will not be wrapped into 2 lines.
                           // min-width is equal to the sum of the widths of private and public count.
                           metaData.attr = 'style="padding-right: 15px; min-width: 66px;"';

                           return generateGeneCountHTML(26, (record.data.privateGeneCount > 0 ? '(' + record.data.privateGeneCount + ')' : '&nbsp;')) + ' '
                              + generateGeneCountHTML(40, record.data.publicGeneCount);
                        }
                     }, defaults);
               },
               getAddNewPhenotypeAssociationButton : function(gridPanel, defaultButtonHandler) {
                  return {
                     handler : Gemma.isRunningOutsideOfGemma() ? function() {
                        Ext.Msg.alert(Gemma.HelpText.WidgetDefaults.PhenotypePanel.modifyPhenotypeAssociationOutsideOfGemmaTitle,
                           Gemma.HelpText.WidgetDefaults.PhenotypePanel.modifyPhenotypeAssociationOutsideOfGemmaText);
                     } : function() {
                        if (!phenotypeAssociationFormWindow || (phenotypeAssociationFormWindow && phenotypeAssociationFormWindow.isDestroyed)) {
                           phenotypeAssociationFormWindow = new Gemma.PhenotypeAssociationForm.Window();
                           gridPanel.relayEvents(phenotypeAssociationFormWindow, ['phenotypeAssociationChanged']);
                        }

                        phenotypeAssociationFormWindow.showWindow(Gemma.PhenotypeAssociationForm.ACTION_CREATE, {
                              gene : gridPanel.currentGene,
                              phenotypes : gridPanel.currentPhenotypes
                           });
                     },
                     icon : "/Gemma/images/icons/add.png",
                     tooltip : "Add new phenotype association"
                  };
               }
            });
      }
   });
