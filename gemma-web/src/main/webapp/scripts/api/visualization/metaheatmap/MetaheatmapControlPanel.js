Ext.namespace( 'Gemma.Metaheatmap', 'Gemma.Metaheatmap.Strings.ControlPanel.SortConditionCombo',
   'Gemma.Metaheatmap.Strings.ControlPanel.SortGeneCombo' );

Gemma.Metaheatmap.Strings.ControlPanel.SortConditionCombo.fieldTipHTML = '<br><b>Full Name</b>: official descriptive title<br><br>'
   + '<b>Short Name</b>: short name or ID (ex: GSE1234)<br><br>'
   + '<b>q Value</b>: confidence that the selected genes are differentially expressed<br><br>'
   + '<b>Diff. Exp. Specificity</b>: within each column, this is the proportion of probes that are differentially expressed '
   + 'versus the total number of expressed probes. This measure is represented by each column\'s pie chart. Experiments are ordered based '
   + 'on their columns\' average specificity.<br><br>';

Gemma.Metaheatmap.Strings.ControlPanel.SortConditionCombo.fieldTip = 'Name: official descriptive title.  q Value: confidence in the expression levels of the selected genes.  '
   + 'Diff. Exp. Specificity: the proportion of probes that are differentially expressed '
   + 'across each experimental factor '
   + 'versus the total number of expressed probes. This measure is represented by each column\'s pie chart. Experiments are ordered based '
   + 'on their column\'s average specificity.';

Gemma.Metaheatmap.Strings.ControlPanel.SortGeneCombo.fieldTipHTML = '<b>Symbol</b>: official gene symbol<br>'
   + '<b>q Values</b>: confidence that the gene is differentially expressed, averaged across the queried experiments<br><br>';

Gemma.Metaheatmap.Strings.ControlPanel.SortGeneCombo.fieldTip = 'Symbol: official gene symbol.  '
   + 'q Values: confidence that the gene is differentially expressed, averaged across the queried experiments';

Gemma.Metaheatmap.ControlPanel = Ext
   .extend(
      Ext.Panel,
      {
         layout : {
            type : 'vbox',
            align : 'stretch',
            pack : 'start',
            defaultMargins : {
               top : 5,
               right : 10,
               bottom : 5,
               left : 10
            }
         },

         factorTreeFilter : [],

         autoScroll : false,
         border : false,

         /**
          * update the title of the genes section of the control panel with the number of genes filtered
          * 
          * @param {int}
          *           numFiltered
          * @param {int}
          *           numTotal
          * @memberOf Gemma.Metaheatmap.ControlPanel
          */
         updateGenesTitle : function( numFiltered, numTotal ) {
            this.genesControlPanel.setTitle( "Genes: <span style=\"color:grey;font-weight:normal;\"><b>" + numFiltered
               + "</b> of " + numTotal + " filtered </span>" );
         },

         /**
          * update the title of the conditions section of the control panel with the number of conditions filtered
          * 
          * @param {int}
          *           numFiltered
          * @param {int}
          *           numTotal
          */
         updateConditionsTitle : function( numFiltered, numTotal ) {
            this.conditionsControlPanel.setTitle( "Conditions: <span style=\"color:grey;font-weight:normal;\"><b>"
               + numFiltered + "</b> of " + numTotal + " filtered </span>" );
         },
         initComponent : function() {

            Ext
               .apply(
                  this,
                  {

                     items : [
                              {
                                 xtype : 'fieldset',
                                 title : 'Genes',
                                 ref : 'genesControlPanel',
                                 flex : 0,
                                 height : 100,
                                 layout : {
                                    type : 'vbox',
                                    align : 'stretch',
                                    pack : 'start'
                                 },
                                 defaults : {
                                    flex : 0,
                                    width : 250,
                                    height : 20
                                 },
                                 items : [
                                          {
                                             ref : 'cmbGenePresets',
                                             xtype : 'combo',
                                             hideLabel : true,
                                             minHeight : 22,
                                             triggerAction : 'all',
                                             displayField : 'text',
                                             valueField : 'id',
                                             editable : false,
                                             mode : 'local',
                                             forceSelection : true,
                                             autoSelect : true,
                                             margins : '0 0 4 0',
                                             store : new Ext.data.ArrayStore(
                                                {
                                                   fields : [ 'text', 'id' ],
                                                   data : Gemma.Metaheatmap.ControlPresets
                                                      .getSortGroupPresetsNames( Gemma.Metaheatmap.ControlPresets.geneSortGroupPresets ),
                                                   idIndex : 0
                                                } ),
                                             listeners : {
                                                scope : this,
                                                'select' : function( field, record, selIndex ) {
                                                   var selectedIndex = record.get( 'id' );
                                                   this.genePreset = this.ownerCt.genePresets[selectedIndex];
                                                   this.applySortFilter();
                                                }
                                             }
                                          },
                                          {
                                             xtype : 'label',
                                             // html : 'Missing data filter <img ext:qtip="<b>Control the tolerance for
                                             // missing data.</b>
                                             // '+
                                             // 'Allows you to hide a gene based on how many conditions it\'s missing
                                             // data for"
                                             // src=Gemma.CONTEXT_PATH + "/images/icons/question_blue.png"/>:',
                                             html : 'Missing data filter:',
                                             height : 15
                                          },
                                          {
                                             xtype : 'slider',
                                             ref : 'sldGeneDataMissingFilter',
                                             height : 20,
                                             value : 70,
                                             increment : 10,
                                             minValue : 0,
                                             maxValue : 100,
                                             plugins : new Ext.slider.Tip(
                                                {
                                                   getText : function( thumb ) {
                                                      if ( thumb.value === 0 ) {
                                                         return "Hide gene if it's missing data for <b>any</b> conditions";
                                                      } else if ( thumb.value === 100 ) {
                                                         return "Don't hide genes for missing data";
                                                      }
                                                      return String
                                                         .format(
                                                            'Hide gene if it\'s missing data for more than <b>{0}%</b> of conditions',
                                                            thumb.value );
                                                   }
                                                } ),
                                             listeners : {
                                                changecomplete : function( slider, newValue, thumb ) {
                                                   this.applySortFilter();
                                                },
                                                scope : this
                                             }
                                          },
                                          {
                                             xtype : 'label',
                                             text : 'Sum qValue filter:',
                                             hidden : true
                                          },
                                          {
                                             xtype : 'slider',
                                             ref : 'sldGenePvalueFilter',
                                             hidden : true,
                                             disabled : true,
                                             value : 0,
                                             increment : 1,
                                             minValue : 0,
                                             maxValue : 100,
                                             plugins : new Ext.slider.Tip( {
                                                getText : function( thumb ) {
                                                   return String.format( 'Hide genes with corrected pValue sum <={0}',
                                                      thumb.value / 100 );
                                                }
                                             } ),
                                             listeners : {
                                                changecomplete : function( slider, newValue, thumb ) {
                                                   this.applySortFilter();
                                                },
                                                scope : this
                                             }
                                          } ]
                              }, // eo gene fieldset
                              {
                                 xtype : 'fieldset',
                                 title : 'Conditions',
                                 ref : 'conditionsControlPanel',
                                 flex : 1,
                                 layout : {
                                    type : 'vbox',
                                    align : 'stretch',
                                    pack : 'start'
                                 },
                                 defaults : {
                                    flex : 0
                                 },
                                 items : [
                                          {
                                             // fieldLabel : 'Sort conditions by',
                                             // fieldTipTitle : 'Sort Conditions By:',
                                             // fieldTipHTML :
                                             // Gemma.Metaheatmap.Strings.ControlPanel.SortConditionCombo.fieldTipHTML,
                                             // fieldTip :
                                             // Gemma.Metaheatmap.Strings.ControlPanel.SortConditionCombo.fieledTip ,
                                             xtype : 'combo',
                                             ref : 'cmbConditionPresets',
                                             minHeight : 22,
                                             margins : '0 0 4 0',
                                             hideLabel : true,
                                             triggerAction : 'all',
                                             displayField : 'text',
                                             valueField : 'index',
                                             editable : false,
                                             mode : 'local',
                                             forceSelection : true,
                                             autoSelect : true,
                                             store : new Ext.data.ArrayStore(
                                                {
                                                   fields : [ 'text', 'index' ],
                                                   data : Gemma.Metaheatmap.ControlPresets
                                                      .getSortGroupPresetsNames( Gemma.Metaheatmap.ControlPresets.conditionSortGroupPresets ),
                                                   idIndex : 0
                                                } ),
                                             listeners : {
                                                scope : this,
                                                'select' : function( field, record, selIndex ) {
                                                   var selectedIndex = record.get( 'index' );
                                                   this.conditionPreset = this.ownerCt.conditionPresets[selectedIndex];
                                                   this.applySortFilter();
                                                }
                                             }
                                          },
                                          {
                                             xtype : 'label',
                                             text : 'Missing data filter:',
                                             // html : 'Missing data filter <img ext:qtip="<b>Control the tolerance for
                                             // missing data.</b>
                                             // '+
                                             // 'Allows you to hide a condition based on how many genes it\'s missing
                                             // data for"
                                             // src=Gemma.CONTEXT_PATH + "/images/icons/question_blue.png"/>:',
                                             height : 15
                                          },
                                          {
                                             xtype : 'slider',
                                             ref : 'sldConditionDataMissingFilter',
                                             width : 150,
                                             height : 20,
                                             value : 70,
                                             increment : 10,
                                             minValue : 0,
                                             maxValue : 100,
                                             plugins : new Ext.slider.Tip(
                                                {
                                                   getText : function( thumb ) {
                                                      if ( thumb.value === 0 ) {
                                                         return "Hide condition if it's missing data for <b>any</b> genes";
                                                      } else if ( thumb.value === 100 ) {
                                                         return "Don't hide conditions for missing data";
                                                      }
                                                      return String
                                                         .format(
                                                            'Hide condition if it\'s missing data for more than <b>{0}%</b> of genes',
                                                            thumb.value );
                                                   }
                                                } ),
                                             listeners : {
                                                changecomplete : function( slider, newValue, thumb ) {
                                                   this.applySortFilter();
                                                },
                                                scope : this
                                             }
                                          },
                                          {
                                             xtype : 'label',

                                             html : 'Specificity filter <img ext:qtip="'
                                                + 'An experiment\'s specificity is measured as the total number of probes <b>differentially</b> expressed versus the total '
                                                + 'number of probes expressed. " src="' + Gemma.CONTEXT_PATH + '/images/icons/question_blue.png"/>:',
                                             // text : 'Specificity filter',
                                             height : 15
                                          },
                                          {
                                             xtype : 'slider',
                                             ref : 'sldSpecificityFilter',
                                             width : 150,
                                             height : 20,
                                             value : 70,
                                             increment : 10,
                                             minValue : 0,
                                             maxValue : 100,
                                             plugins : new Ext.slider.Tip(
                                                {
                                                   getText : function( thumb ) {
                                                      if ( thumb.value === 100 ) {
                                                         return "Show conditions with any specificity";
                                                      }
                                                      return String.format(
                                                         'Show conditions with better than {0}% specificity',
                                                         thumb.value );
                                                   }
                                                } ),
                                             listeners : {
                                                changecomplete : function( slider, newValue, thumb ) {
                                                   this.applySortFilter();
                                                },
                                                scope : this
                                             }
                                          },
                                          {
                                             xtype : 'label',
                                             hidden : true,
                                             text : 'Sum corrected P value filter:',
                                             height : 15
                                          },
                                          {
                                             xtype : 'slider',
                                             hidden : true,
                                             disabled : false,
                                             ref : 'sldConditionPvalueFilter',
                                             width : 150,
                                             height : 20,
                                             value : 0,
                                             increment : 10,
                                             minValue : 0,
                                             maxValue : 100,
                                             plugins : new Ext.slider.Tip(
                                                {
                                                   getText : function( thumb ) {
                                                      return String
                                                         .format(
                                                            'Show conditions Hide conditions with corrected pValue sum <={0}',
                                                            thumb.value / 100 );
                                                   }
                                                } ),
                                             listeners : {
                                                changecomplete : function( slider, newValue, thumb ) {
                                                   this.applySortFilter();
                                                },
                                                scope : this
                                             }
                                          }, {
                                             xtype : 'Metaheatmap.FactorTree',
                                             ref : 'factorTree',
                                             sortedTree : this.sortedTree,
                                             autoScroll : true,
                                             // bodyStyle : 'padding-bottom:5px',
                                             border : false,
                                             bodyStyle : 'background:#F1F6F6',
                                             flex : 1
                                          } ]
                              } // eo condition fieldset
                     ]

                  } );

            Gemma.Metaheatmap.ControlPanel.superclass.initComponent.apply( this, arguments );

            this.addEvents( 'gene_zoom_change', 'condition_zoom_change' );

            /** *********************** Selection grids *************************************** */

            // see history for code
            /** ************* end of selection grids ********************* */

         },

         makeFilterFunction : function( filterString ) {
            return function( o ) {
               return (o.contrastFactorValue == filterString);
            };
         },

         applySortFilter : function() {
            this.ownerCt.visualizationPanel.mask.show();
            this.doFiltering_.defer( 100, this );

         },

         doFiltering_ : function() {
            var genePercentMissingThreshold = this.genesControlPanel.sldGeneDataMissingFilter.getValue() / 100;
            var genePercentMissingFilter = [ {
               'filterFn' : function( o ) {
                  return o.percentProbesMissing > genePercentMissingThreshold;
               }
            } ];

            var conditionPercentMissingThreshold = this.conditionsControlPanel.sldConditionDataMissingFilter.getValue() / 100;
            var conditionPercentMissingFilter = [ {
               'filterFn' : function( o ) {
                  return o.percentProbesMissing > conditionPercentMissingThreshold;
               }
            } ];

            var specificityThreshold = this.conditionsControlPanel.sldSpecificityFilter.getValue() / 100;
            var specificityFilter = [ {
               'filterFn' : function( o ) {
                  return o.experimentSpecificity > specificityThreshold;
               }
            } ];

            // var geneThreshold = this.genesControlPanel.sldGenePvalueFilter.getValue() / 100;
            // var genePvalueFilter = [{'filterFn' : function (o) {return o.inverseSumPvalue < geneThreshold;} }];

            // var conditionThreshold = this.conditionsControlPanel.sldConditionPvalueFilter.getValue() / 100;
            // var conditionPvalueFilter = [{'filterFn' : function (o) {return o.inverseSumPvalue < conditionThreshold;}
            // }];

            var conditionSort = [];
            conditionSort = conditionSort.concat( this.conditionPreset.sort );

            var conditionFilter = [];
            conditionFilter = conditionFilter.concat( specificityFilter );
            // conditionFilter = conditionFilter.concat (conditionPvalueFilter);
            conditionFilter = conditionFilter.concat( conditionPercentMissingFilter );
            conditionFilter = conditionFilter.concat( this.factorTreeFilter );

            var geneSort = [];
            geneSort = geneSort.concat( this.genePreset.sort );

            var geneFilter = [];
            // geneFilter = geneFilter.concat (genePvalueFilter);
            geneFilter = geneFilter.concat( genePercentMissingFilter );

            this.fireEvent( 'applySortGroupFilter', geneSort, geneFilter, conditionSort, conditionFilter );
         },

         onRender : function() {
            Gemma.Metaheatmap.ControlPanel.superclass.onRender.apply( this, arguments );

            this.addEvents( 'applySortGroupFilter' );

            this.genesControlPanel.cmbGenePresets.setValue( 0 );
            this.genePreset = this.ownerCt.genePresets[0];
            this.conditionsControlPanel.cmbConditionPresets.setValue( 0 );
            this.conditionPreset = this.ownerCt.conditionPresets[0];

            this.conditionsControlPanel.factorTree.on( 'checkchange', function( node, checked ) {
               var i;
               if ( node.isLeaf() ) {

               } else {
                  // Propagate choice to children.
                  for (i = 0; i < node.childNodes.length; i++) {
                     var child = node.childNodes[i];
                     child.ui.toggleCheck( checked );
                     child.attributes.checked = checked;
                  }
               }

               // Go through factorTree and create filter functions for unchecked factor values.
               this.factorTreeFilter = [];
               var root = this.conditionsControlPanel.factorTree.root;
               for (i = 0; i < root.childNodes.length; i++) {
                  var categoryNode = root.childNodes[i];
                  for (var j = 0; j < categoryNode.childNodes.length; j++) {
                     var factorNode = categoryNode.childNodes[j];
                     if ( factorNode.attributes.checked === false ) {
                        this.factorTreeFilter.push( {
                           'filterFn' : this.makeFilterFunction( factorNode.contrastFactorValue )
                        } );
                     }
                  }
               }

               this.applySortFilter();
            }, this );

         }

      } );

Ext.reg( 'Metaheatmap.ControlPanel', Gemma.Metaheatmap.ControlPanel );
