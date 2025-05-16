/**
 * Panel for selecting factors of experiments
 * 
 * @author frances
 *
 */
Ext.namespace( 'Gemma' );

Gemma.MetaAnalysisSelectFactorPanel = Ext
   .extend(
      Gemma.WizardTabPanelItemPanel,
      {
         nextButtonText : 'Run meta-analysis',
         listeners : {
            render : function( thisPanel ) {
               if ( !thisPanel.loadMask ) {
                  var element = thisPanel.getEl();

                  // Set height so that load mask is in the middle.
                  element.setHeight( thisPanel.height / 2 + 200 );

                  thisPanel.loadMask = new Ext.LoadMask( element, {
                     msg : "Loading ..."
                  } );
               }
               // This load mask will be shown for viewing analysis.
               thisPanel.loadMask.show();
            }
         },
         initComponent : function() {
            var experimentSelectedCount = 0;

            var nextButton = this.createNextButton();
            nextButton.disable();

            // Assume that if this.metaAnalysis is not null, result sets are shown
            // for viewing only. So, editing is not allowed.
            var generateExperimentComponents = function( experimentDetails ) {
               var radioGroup = new Ext.form.RadioGroup( {
                  items : []
               } );

               var experimentTitle = '<b>'
                  + '<a ext:qtip="Click for details on experiment (opens in new window)" target="_blank"  href="' + Gemma.CONTEXT_PATH + '/expressionExperiment/showExpressionExperiment.html?id='
                  + experimentDetails.id + '">' + experimentDetails.shortName + '</a> ' + experimentDetails.name
                  + '</b>';

               var experimentTitleComponent;
               if ( this.metaAnalysis ) {
                  experimentTitleComponent = new Ext.form.DisplayField( {
                     style : 'margin: 10px 0 0 20px;', // DisplayField instead of Label is used. Otherwise, top
                     // margin
                     // is not honored.
                     html : experimentTitle
                  } );
               } else {
                  experimentTitleComponent = new Ext.form.Checkbox( {
                     style : 'margin: 10px 0 0 10px;',
                     boxLabel : experimentTitle,
                     listeners : {
                        check : function( checkbox, checked ) {
                           if ( checked ) {
                              experimentSelectedCount++;

                              if ( radioGroup.getValue() == null ) {
                                 for ( var i = 0; i < radioGroup.items.length; i++) {
                                    if ( !radioGroup.items[i].disabled ) {
                                       radioGroup.items[i].setValue( true );
                                       break;
                                    }
                                 }
                              }
                           } else {
                              experimentSelectedCount--;

                              radioGroup.reset();
                           }

                           nextButton.setDisabled( experimentSelectedCount < 2 );
                        }
                     }
                  } );
               }

               var experimentResultSetsPanel = new Ext.Panel( {
                  bodyStyle : 'background-color: transparent; padding: 0 0 20px 40px;',
                  border : false
               } );

               var totalSuitableResultSetCount = 0;

               if ( experimentDetails.differentialExpressionAnalyses.length == 0 ) {
                  experimentResultSetsPanel.add( new Ext.form.Label( {
                     style : 'font-style: italic; ',
                     disabled : true,
                     html : 'No differential expression analysis available' + '<br />'
                  } ) );
               } else {
                  var analysesSummaryTree = new Gemma.DifferentialExpressionAnalysesSummaryTree( {
                     experimentDetails : experimentDetails,
                     editable : false,
                     style : 'padding-bottom: 20px;'
                  } );

                  var generateResultSetComponent = function( text, marginLeft, notSuitableForAnalysisMessage,
                     inputValue, shouldResultSetSelected ) {
                     var resultSetComponent;
                     var resultSetRadio = null;
                     /*
                      * If this panel is editable, it will be set and stored as a component inside resultSetComponent.
                      */
                     if ( this.metaAnalysis ) {
                        resultSetComponent = {
                           xtype : 'displayfield',
                           value : (shouldResultSetSelected ? '<img src="' + Gemma.CONTEXT_PATH + '/images/icons/ok16.png" width="16" height="16" />'
                              : '<span style="margin-left: 16px;" />')
                              + '&nbsp;'
                              + text
                              + (notSuitableForAnalysisMessage ? ' <i>' + notSuitableForAnalysisMessage + '</i>' : ''),
                           /*
                            * Note that this displayfield should not be disabled because cursors for icons will not be
                            * changed to pointers. So, I have to make all text and icons look disabled manually.
                            */

                           style : 'margin-left: ' + marginLeft + 'px;'
                              + (notSuitableForAnalysisMessage ? ' color: gray; opacity: 0.6;' : '')
                        };
                     } else {
                        /*
                         * When icons placed after radio buttons are clicked, these radio buttons should not be
                         * selected. So, radio buttons should contain text only. Assume all text ends right before the
                         * first html tag "span". indexOfFirstSpan is used to store the start index of the first html
                         * tag "span". Radio button will be created using this text only.
                         */
                        var indexOfFirstSpan = text.indexOf( '<span' );

                        resultSetRadio = new Ext.form.Radio( {
                           checked : shouldResultSetSelected,
                           boxLabel : text.substring( 0, indexOfFirstSpan ), // text only and without any icons
                           name : (this.metaAnalysis ?
                           // Meta-analysis id should be used because another window may have the same set
                           // of radio buttons for the same experiment.
                           this.metaAnalysis.id + '-' + experimentDetails.id : experimentDetails.id),
                           style : 'margin-left: ' + marginLeft + 'px;',
                           disabled : notSuitableForAnalysisMessage != null,
                           inputValue : inputValue,
                           listeners : {
                              check : function( radio, checked ) {
                                 if ( checked ) {
                                    if ( experimentTitleComponent.isXType( Ext.form.Checkbox ) ) {
                                       experimentTitleComponent.setValue( true );
                                    }
                                 }
                              }
                           }
                        } );

                        resultSetComponent = [
                                              resultSetRadio,
                                              {
                                                 /*
                                                    * Put all icons and not for analysis messages (if any) in a new
                                                    * panel.
                                                    */
                                                 xtype : 'displayfield',
                                                 value : text.substring( indexOfFirstSpan )
                                                    + (notSuitableForAnalysisMessage ? ' <i>'
                                                       + notSuitableForAnalysisMessage + '</i>' : ''),
                                                 // Note that icons should not be put inside radio buttons even if
                                                   // these radio buttons
                                                 // are disabled because cursors for icons will not be changed to
                                                   // pointers. So, I have
                                                 // to make all text and icons look disabled manually.
                                                 style : 'margin-top: -5px;'
                                                    + (notSuitableForAnalysisMessage ? ' color: gray; opacity: 0.6;'
                                                       : '')
                                              } ];
                     }

                     return {
                        border : false,
                        bodyStyle : 'background-color: transparent;',
                        layout : 'hbox',
                        getRadio : function() {
                           return resultSetRadio;
                        },
                        items : resultSetComponent
                     };
                  }.createDelegate( this );

                  var checkSuitableForAnalysis = function( attributes ) {
                     var notSuitableForAnalysisMessage = null;
                     if ( attributes.numberOfFactors > 1 ) {
                        notSuitableForAnalysisMessage = '(Not suitable - Analysis used more than 1 factor)';
                     } else if ( attributes.numberOfFactorValues == null || attributes.numberOfFactorValues > 2 ) {
                        notSuitableForAnalysisMessage = '(Not suitable - Analysis based on more than 2 groups)';
                     }

                     return notSuitableForAnalysisMessage;
                  };

                  // Sort the tree's child nodes.
                  analysesSummaryTree.root.childNodes.sort( function( group1, group2 ) {
                     var strippedText1 = Ext.util.Format.stripTags( group1.text );
                     var strippedText2 = Ext.util.Format.stripTags( group2.text );

                     return (strippedText1 < strippedText2 ? -1 : strippedText1 > strippedText2 ? 1 : 0);
                  } );

                  var checkResultSetAvailability = function( analysisId, resultSetId ) {
                     var shouldResultSetCreated = true;
                     var shouldResultSetSelected = false;
                     if ( this.metaAnalysis ) {
                        for ( var i = 0; i < this.metaAnalysis.includedResultSetsInfo.length; i++) {
                           var currIncludedResultSetsInfo = this.metaAnalysis.includedResultSetsInfo[i];

                           if ( currIncludedResultSetsInfo.experimentId == experimentDetails.id ) {
                              shouldResultSetCreated = (currIncludedResultSetsInfo.analysisId === analysisId);
                              shouldResultSetSelected = (currIncludedResultSetsInfo.resultSetId === resultSetId);

                              break;
                           }
                        }
                     }
                     return {
                        shouldResultSetCreated : shouldResultSetCreated,
                        shouldResultSetSelected : shouldResultSetSelected
                     };
                  }.createDelegate( this );

                  Ext.each( analysesSummaryTree.root.childNodes, function( resultSetParent, unusedI ) {
                     if ( resultSetParent.childNodes.length > 0 ) {
                        var label = new Ext.form.Label( {
                           html : resultSetParent.text + '<br />'
                        } );
                        experimentResultSetsPanel.add( label );

                        var currSuitableResultSetCount = 0;
                        var currCreatedResultSetCount = 0;

                        Ext.each( resultSetParent.childNodes, function( resultSet, unusedJ ) {
                           radioAvailability = checkResultSetAvailability( resultSet.attributes.analysisId,
                              resultSet.attributes.resultSetId );
                           if ( radioAvailability.shouldResultSetCreated ) {
                              currCreatedResultSetCount++;

                              var notSuitableForAnalysisMessage = checkSuitableForAnalysis( resultSet.attributes );

                              if ( notSuitableForAnalysisMessage == null ) {
                                 currSuitableResultSetCount++;
                                 totalSuitableResultSetCount++;
                              }
                              var resultSetComponent = generateResultSetComponent( resultSet.text, 15,
                                 notSuitableForAnalysisMessage, resultSet.attributes.resultSetId,
                                 radioAvailability.shouldResultSetSelected );

                              var resultSetRadio = resultSetComponent.getRadio();
                              if ( resultSetRadio != null ) {
                                 radioGroup.items.push( resultSetRadio );
                              }
                              experimentResultSetsPanel.add( resultSetComponent );
                           }
                        }, this ); // scope

                        label.setDisabled( currSuitableResultSetCount === 0 );

                        // If no result sets are created, we should remove the experiment label added earlier.
                        if ( currCreatedResultSetCount === 0 ) {
                           experimentResultSetsPanel.remove( label );
                        }
                     } else {
                        radioAvailability = checkResultSetAvailability( resultSetParent.attributes.analysisId,
                           resultSetParent.attributes.resultSetId );
                        if ( radioAvailability.shouldResultSetCreated ) {
                           var notSuitableForAnalysisMessage = checkSuitableForAnalysis( resultSetParent.attributes );

                           if ( notSuitableForAnalysisMessage == null ) {
                              totalSuitableResultSetCount++;
                           }
                           var resultSetComponent = generateResultSetComponent( resultSetParent.text, 0,
                              notSuitableForAnalysisMessage, resultSetParent.attributes.resultSetId,
                              radioAvailability.shouldResultSetSelected );

                           var resultSetRadio = resultSetComponent.getRadio();
                           if ( resultSetRadio != null ) {
                              radioGroup.items.push( resultSetRadio );
                           }
                           experimentResultSetsPanel.add( resultSetComponent );
                        }
                     }
                  }, this ); // scope

                  experimentResultSetsPanel.on( 'afterlayout', function() {
                     analysesSummaryTree.drawPieCharts();
                  }, analysesSummaryTree, {
                     single : true,
                     delay : 100
                  } );
               }

               if ( totalSuitableResultSetCount === 0 ) {
                  experimentTitleComponent.setDisabled( true );
               }

               return {
                  hasEnabledRadioButtons : (totalSuitableResultSetCount > 0),
                  experimentTitleComponent : experimentTitleComponent,
                  experimentResultSetsPanel : experimentResultSetsPanel
               };
            }.createDelegate( this );

            var showExperiments = function( experimentIds ) {
               this.maskWindow();

               analyzableExperimentsPanel.removeAll();
               nonAnalyzableExperimentsPanel.removeAll();

               nextButton.setDisabled( true );

               ExpressionExperimentController.loadDetailedExpressionExperiments( experimentIds,
                  function( experiments ) {
                     // Sort experiment by short name.
                     experiments.sort( function( experiment1, experiment2 ) {
                        return experiment1.shortName.localeCompare( experiment2.shortName );
                     } );

                     var nonAnalyzableExperimentComponents = [];

                     var addExperimentComponentsToPanel = function( experimentComponents, containerPanel,
                        componentIndex ) {
                        var panel = new Ext.Panel( {
                           border : false,
                           bodyStyle : (componentIndex % 2 === 0 ? 'background-color: #FAFAFA;'
                              : 'background-color: #FFFFFF;')
                        } );
                        panel.add( experimentComponents.experimentTitleComponent );
                        panel.add( experimentComponents.experimentResultSetsPanel );
                        containerPanel.add( panel );
                     };

                     var i;
                     var analyzableExperimentsPanelIndex = 0;

                     for (i = 0; i < experiments.length; i++) {
                        var experimentComponents = generateExperimentComponents( experiments[i] );

                        if ( experimentComponents.hasEnabledRadioButtons ) {
                           addExperimentComponentsToPanel( experimentComponents, analyzableExperimentsPanel,
                              analyzableExperimentsPanelIndex );
                           analyzableExperimentsPanelIndex++;
                        } else {
                           nonAnalyzableExperimentComponents.push( experimentComponents );
                        }
                     }

                     for ( var j = 0; j < nonAnalyzableExperimentComponents.length; j++) {
                        addExperimentComponentsToPanel( nonAnalyzableExperimentComponents[j],
                           nonAnalyzableExperimentsPanel, j );
                     }

                     this.doLayout();

                     // Hide the mask created for adding new analysis or viewing analysis.
                     this.unmaskWindow();
                     this.loadMask.hide();
                  }.createDelegate( this ) );
            }.createDelegate( this );

            var analyzableExperimentsPanel = new Ext.Panel( {
               header : (!this.metaAnalysis),
               title : 'Analyzable experiments',
               region : 'center',
               autoScroll : true,
               border : false
            } );
            var nonAnalyzableExperimentsPanel = new Ext.Panel( {
               title : 'Non-analyzable experiments',
               region : 'south',
               autoScroll : true,
               border : false,

               split : true,
               height : 200
            } );

            var setDisabledChildComponentsVisible = function( container, visible ) {
               if ( container.items && container.items.length > 0 ) {
                  Ext.each( container.items.items, function( item, index ) {
                     if ( item ) {
                        if ( item.items && item.items.length > 0 ) {
                           setDisabledChildComponentsVisible( item, visible );
                        } else if ( item.disabled && item instanceof Ext.form.Radio ) {
                           // Besides radio button, its parent should be set visible/invisible
                           // because its parent also contains icons and text related to it.
                           item.ownerCt.setVisible( visible );
                        }
                     }
                  } );
               }
            };

            var findSelectedResultSetIds = function( resultSetIds, container ) {
               if ( container.items && container.items.length > 0 ) {
                  Ext.each( container.items.items, function( item, index ) {
                     if ( item ) {
                        if ( item.items && item.items.length > 0 ) {
                           findSelectedResultSetIds( resultSetIds, item );
                        } else if ( item instanceof Ext.form.Radio && item.getValue() ) {
                           resultSetIds.push( item.inputValue );
                        }
                     }
                  } );
               }
            };

            var setPanelReadOnly = function( panel, isReadOnly ) {
               var radioButtons = panel.findByType( 'radio' );
               Ext.each( radioButtons, function( radio, index ) {
                  if ( isReadOnly ) {
                     radio.el.parent().mask();
                  } else {
                     radio.el.parent().unmask();
                  }
               } );

               var checkboxButtons = panel.findByType( 'checkbox' );
               Ext.each( checkboxButtons, function( checkbox, index ) {
                  if ( isReadOnly ) {
                     checkbox.el.parent().mask();
                  } else {
                     checkbox.el.parent().unmask();
                  }
               } );
            };

            var buttonPanel = new Ext.Panel( {
               region : 'south',
               border : false,
               height : 40,
               padding : '10px 0 0 10px',
               items : [ nextButton ]
            } );

            var thisPanelItems;
            if ( this.metaAnalysis ) {
               var expressionExperimentIds = [];

               Ext.each( this.metaAnalysis.includedResultSetsInfo, function( includedResultSetInfo, index ) {
                  expressionExperimentIds.push( includedResultSetInfo.experimentId );
               } );

               showExperiments( expressionExperimentIds );

               thisPanelItems = [ analyzableExperimentsPanel ];
            } else {
               thisPanelItems = [ {
                  region : 'center',
                  layout : 'border',
                  items : [ analyzableExperimentsPanel, nonAnalyzableExperimentsPanel ]
               }, buttonPanel ];
            }

            Ext.apply( this, {
               height : 600,
               layout : 'border',
               title : (this.metaAnalysis ? 'Selected' : 'Select') + ' factors',
               getSelectedResultSetIds : function() {
                  var selectedResultSetIds = [];

                  findSelectedResultSetIds( selectedResultSetIds, this );

                  return selectedResultSetIds;
               },
               items : thisPanelItems,
               setSelectedExperimentIds : function( expressionExperimentIds ) {
                  showExperiments( expressionExperimentIds );
               },
               setPanelReadOnly : function( msg, msgCls ) {
                  if ( analyzableExperimentsPanel.header ) {
                     analyzableExperimentsPanel.header.mask( msg, msgCls );
                  }

                  setPanelReadOnly( analyzableExperimentsPanel, true );

                  if ( !this.metaAnalysis ) {
                     buttonPanel.body.mask();
                  }
               },
               unsetPanelReadOnly : function() {
                  analyzableExperimentsPanel.header.unmask();
                  buttonPanel.body.unmask();

                  setPanelReadOnly( analyzableExperimentsPanel, false );
                  setPanelReadOnly( nonAnalyzableExperimentsPanel, false );
               }
            } );

            if ( !this.metaAnalysis ) {
               Ext.apply( this, {
                  tbar : [ {
                     xtype : 'checkbox',
                     boxLabel : 'Hide non-analyzable experiments and factors',
                     listeners : {
                        check : function( checkbox, checked ) {
                           nonAnalyzableExperimentsPanel.setVisible( !checked );
                           setDisabledChildComponentsVisible( this, !checked );
                           this.doLayout();
                        },
                        scope : this
                     }
                  } ]
               } );
            }

            Gemma.MetaAnalysisSelectFactorPanel.superclass.initComponent.call( this );
         }
      } );
