/**
 * 
 * @author AZ
 */
Ext.namespace( 'Gemma.Metaheatmap' );

/**
 * MetaHeatmap Application Consist of 3 main panels:
 * 
 * <pre>
 * - side labels area
 * - top labels area 
 * - main area 
 * It is controlled by window that allows sorting/filtering and choosing data.
 * 
 * genes - Metaheatmap.Genes is shared between control panel and visualization panel.
 *         It takes care of sorting/filtering of the genes.
 * geneSelection
 * experimentSelection
 * 
 * 
 *  ----------------------------------------
 *                Toolbar	
 *  ------  --------------------  ----------
 *       | TopLabelArea  &circ; |           |
 *          ---------------|----			|
 *  -----                  |    |   Sort    |
 * |Side |	boxHeatmap     |	|  Filter	|
 * |Label|				   |	|	Panel	|
 * |Area |				   |	|			|
 * |	 |				   |	|			|
 * |	 |				   |	|			|
 * |	 |				   |	|			|
 * |&lt;-----VisualizationPanel---&gt;|			|
 * |	 |						|			|
 * ------ ---------------------- -----------
 * </pre>
 * 
 * 
 * If 'savedState' object is passed, the application is initialized using values passed in it. The state captures:
 * sort/filter + search queries/sets used to retrieve genes and experiments. Due to URL length limitation this will not
 * always work for bookmarkable links. We can store 'savedState' in database in the future to work around this
 * limitation.
 */

var makeSortFunction = function( property, direction ) {
   if ( direction === 'DESC' ) {
      return function( a, b ) {
         if ( typeof a[property] === "number" ) {
            return (b[property] - a[property]);
         } else {
            return ((a[property] > b[property]) ? -1 : ((a[property] < b[property]) ? 1 : 0));
         }
      };
   } else {
      return function( a, b ) {
         if ( typeof a[property] === "number" ) {
            return (a[property] - b[property]);
         } else {
            return ((a[property] < b[property]) ? -1 : ((a[property] > b[property]) ? 1 : 0));
         }
      };
   }
};

Gemma.Metaheatmap.ControlPresets = {

   geneSortGroupPresets : [ {
      name : 'sort alphabetically',
      sort : [ {
         sortFn : makeSortFunction( 'groupName' ),
         groupBy : 'groupName'
      }, {
         sortFn : makeSortFunction( 'name' ),
         groupBy : null
      } ],
      filter : []
   }, {
      name : 'sort by meta p-value',
      sort : [ {
         sortFn : makeSortFunction( 'groupName' ),
         groupBy : 'groupName'
      }, {
         sortFn : makeSortFunction( 'metaPvalue', 'ASC' ),
         groupBy : null
      } ],
      filter : []
   } ],

   conditionSortGroupPresets : [ {
      name : 'sort by experiment',
      sort : [ {
         sortFn : makeSortFunction( 'experimentGroupName' ),
         groupBy : 'experimentGroupName'
      }, {
         sortFn : makeSortFunction( 'datasetShortName' ),
         groupBy : 'datasetShortName'
      }, {
         sortFn : makeSortFunction( 'contrastFactorValue' ),
         groupBy : null
      } ],
      filter : []
   }, {
      name : 'sort by specificity (group by factor)',
      sort : [ {
         sortFn : makeSortFunction( 'factorCategory' ),
         groupBy : 'factorCategory'
      }, {
         sortFn : makeSortFunction( 'miniPieValue' ),
         groupBy : null
      } ],
      filter : []
   }, {
      name : 'sort by enrichment (group by factor)',
      sort : [ {
         sortFn : makeSortFunction( 'factorCategory' ),
         groupBy : 'factorCategory'
      }, {
         sortFn : makeSortFunction( 'ora', 'ASC' ),
         groupBy : null
      } ],
      filter : []
   } ],

   factorTreeSortGroupPreset : [ {
      sortFn : makeSortFunction( 'factorCategory' ),
      groupBy : 'factorCategory'
   }, {
      sortFn : makeSortFunction( 'contrastFactorValue' ),
      groupBy : 'contrastFactorValue'
   }, {
      sortFn : makeSortFunction( 'contrastFactorValue' ),
      groupBy : null
   } ],

   getSortGroupPresetsNames : function( sortGroupPresets ) {
      var names = [];
      for (var i = 0; i < sortGroupPresets.length; i++) {
         names.push( [ sortGroupPresets[i]['name'], i ] );
      }
      return names;
   }

};

Gemma.Metaheatmap.defaultConditionZoom = 10;
Gemma.Metaheatmap.defaultGeneZoom = 10;

Gemma.Metaheatmap.Application = Ext
   .extend(
      Ext.Panel,
      {

         tutorialReady : false,

         /**
          * @memberOf Gemma.Metaheatmap.Application
          */
         initComponent : function() {

            // Build required data structures.
            this.conditions = this.visualizationData.conditions;
            this.genes = this.visualizationData.genes;
            this.cells = {};
            this.cells.cellData = this.visualizationData.cellData;

            // Add convenience cell retrieval function to cellData object.
            this.cells.getCell = function( gene, condition ) {
               if ( !gene || !condition ) {
                  return null;
               }
               var geneToCellMap = this.cellData[condition.id];
               if ( typeof geneToCellMap != 'undefined' ) {
                  var cellValueObj = geneToCellMap[gene.id];
                  if ( typeof cellValueObj != 'undefined' ) {
                     return cellValueObj;
                  }
               }
               return null;
            };

            this.genePresets = Gemma.Metaheatmap.ControlPresets.geneSortGroupPresets;
            this.conditionPresets = Gemma.Metaheatmap.ControlPresets.conditionSortGroupPresets;

            this.geneTree = new Gemma.Metaheatmap.SortedFilteredTree( this.genes, this.genePresets[0].sort, [] );
            this.conditionTree = new Gemma.Metaheatmap.SortedFilteredTree( this.conditions,
               this.conditionPresets[0].sort, [] );
            this.factorTree = new Gemma.Metaheatmap.SortedFilteredTree( this.conditions,
               Gemma.Metaheatmap.ControlPresets.factorTreeSortGroupPreset, [] );

            Ext
               .apply(
                  this,
                  {
                     layout : 'border',
                     width : Ext.getBody().getViewSize().width - 40, // eventually it'll be a viewport
                     height : Ext.getBody().getViewSize().height - 20,
                     tbar : [
                             {
                                xtype : 'label',
                                ref : 'titleLabel',
                                text : ""
                             },
                             '->',
                             {
                                ref : 'colorLegendButton',
                                xtype : 'button',
                                text : '<b>Color Legend</b>',
                                enableToggle : true,
                                tooltip : 'Show/hide the color legend',
                                toggleHandler : function( btn, pressed ) {
                                   if ( pressed ) {
                                      this.visualizationPanel.variableWidthCol.colorLegend.show();
                                      this.visualizationPanel.variableWidthCol.colorLegend.isShown = true;

                                   } else {
                                      this.visualizationPanel.variableWidthCol.colorLegend.hide();
                                      this.visualizationPanel.variableWidthCol.colorLegend.isShown = false;
                                   }
                                },
                                scope : this
                             },
                             '-',
                             {
                                xtype : 'button',
                                text : '<b>Bookmarkable Link</b>',
                                tooltip : 'Get a link to re-run this search',
                                disabled : true,
                                handler : function() {
                                   this.showBookmarkableLinkWindow();
                                },
                                scope : this
                             },
                             '-',
                             {
                                xtype : 'button',
                                ref : 'saveSelectedButton',
                                text : '<b>Save Selected</b>',
                                icon : Gemma.CONTEXT_PATH + '/images/icons/disk.png',
                                cls : 'x-btn-text-icon',
                                tooltip : 'Select genes or experiments by holding down the "Ctrl" key and clicking on row or column labels.',
                                scope : this,
                                menu : new Ext.menu.Menu(
                                   {
                                      scope : this,
                                      items : [
                                               {
                                                  text : 'Genes',
                                                  scope : this,
                                                  handler : function() {
                                                     if ( this.visualizationPanel.getSelectedGeneIds().length == 0 ) {
                                                        Ext.Msg
                                                           .alert(
                                                              Gemma.HelpText.WidgetDefaults.MetaheatmapApplication.noGenesSelectedTitle,
                                                              Gemma.HelpText.WidgetDefaults.MetaheatmapApplication.noGenesSelectedText );
                                                        return;
                                                     }
                                                     var geneSetGrid = new Gemma.GeneMembersSaveGrid( {
                                                        genes : this.visualizationPanel.getSelectedGeneIds(),
                                                        allowSaveToSession : false,
                                                        frame : false
                                                     } );
                                                     this.getEl().mask();
                                                     var popup = new Ext.Window( {
                                                        closable : false,
                                                        layout : 'fit',
                                                        width : 450,
                                                        height : 500,
                                                        items : geneSetGrid
                                                     } );

                                                     geneSetGrid.on( 'doneModification', function() {
                                                        this.getEl().unmask();
                                                        popup.hide();
                                                     }, this );
                                                     popup.show();
                                                  }
                                               },
                                               {
                                                  text : 'Experiments',
                                                  scope : this,
                                                  handler : function() {
                                                     if ( this.visualizationPanel.getSelectedDatasetIds().length == 0 ) {
                                                        Ext.Msg
                                                           .alert(
                                                              Gemma.HelpText.WidgetDefaults.MetaheatmapApplication.noDatasetsSelectedTitle,
                                                              Gemma.HelpText.WidgetDefaults.MetaheatmapApplication.noDatasetsSelectedText );
                                                        return;
                                                     }
                                                     var eeSetGrid = new Gemma.ExpressionExperimentMembersGrid( {
                                                        eeids : this.visualizationPanel.getSelectedDatasetIds(),
                                                        frame : false,
                                                        allowSaveToSession : false
                                                     } );
                                                     this.getEl().mask();
                                                     var popup = new Ext.Window( {
                                                        closable : false,
                                                        layout : 'fit',
                                                        width : 450,
                                                        height : 500,
                                                        items : eeSetGrid
                                                     } );

                                                     eeSetGrid.on( 'doneModification', function() {
                                                        this.getEl().unmask();
                                                        popup.hide();
                                                     }, this );
                                                     popup.show();
                                                  }
                                               } ]
                                   } )
                             },
                             '-',
                             {
                                xtype : 'button',
                                text : '<b>Download</b>',
                                ref : 'downloadButton',
                                icon : Gemma.CONTEXT_PATH + '/images/download.gif',
                                menu : new Ext.menu.Menu( {
                                   items : [ {
                                      text : 'As text',
                                      icon : Gemma.CONTEXT_PATH + '/images/icons/page_white_text.png',
                                      tooltip : 'Download a formatted text version of your search results',
                                      handler : function() {
                                         var textWindow = new Gemma.Metaheatmap.DownloadWindow( {
                                            geneTree : this.geneTree,
                                            conditionTree : this.conditionTree,
                                            cells : this.cells,
                                            isPvalue : this.visualizationPanel.variableWidthCol.boxHeatmap.isShowPvalue
                                         } );
                                         textWindow.convertToText();
                                         textWindow.show();
                                      },
                                      scope : this
                                   }, {
                                      text : 'As image',
                                      icon : Gemma.CONTEXT_PATH + '/images/icons/picture.png',
                                      tooltip : 'Download heatmap image',
                                      handler : function() {
                                         this.visualizationPanel.downloadImage();
                                      },
                                      scope : this
                                   } ]
                                } )
                             },
                             '-',
                             {
                                xtype : 'button',
                                icon : Gemma.CONTEXT_PATH + '/images/icons/question_blue.png',
                                cls : 'x-btn-icon',
                                tooltip : 'Click here for documentation on how to use this visualizer.',
                                handler : function() {
                                   window
                                      .open( 'https://pavlidislab.github.io/Gemma/search.html#differential-expression-results' );
                                },
                                scope : this
                             } ], // end tbar
                     items : [ {
                        ref : 'visualizationPanel',
                        xtype : 'Metaheatmap.VisualizationPanel',
                        conditionTree : this.conditionTree,
                        geneTree : this.geneTree,
                        cells : this.cells,
                        geneControls : this.geneControls,
                        conditionControls : this.conditionControls,
                        region : 'center',
                        autoScroll : true
                     }, {
                        ref : 'controlPanel',
                        xtype : 'Metaheatmap.ControlPanel',
                        conditionTree : this.conditionTree,
                        geneTree : this.geneTree,
                        geneControls : this.geneControls,
                        conditionControls : this.conditionControls,
                        sortedTree : this.factorTree,
                        collapsible : true,
                        floatable : false,
                        animFloat : false,
                        title : 'Sort & Filter',
                        border : true,
                        region : 'east',
                        split : true,
                        width : 300
                     } ]
                  } );

            Gemma.Metaheatmap.Application.superclass.initComponent.apply( this, arguments );
         },

         onRender : function() {
            Gemma.Metaheatmap.Application.superclass.onRender.apply( this, arguments );

            this.controlPanel.on( 'applySortGroupFilter', function( geneSort, geneFilter, conditionSort,
               conditionFilter ) {

               this.geneTree = new Gemma.Metaheatmap.SortedFilteredTree( this.genes, geneSort, geneFilter );
               this.conditionTree = new Gemma.Metaheatmap.SortedFilteredTree( this.conditions, conditionSort,
                  conditionFilter );

               this.controlPanel.updateGenesTitle( this.geneTree.numFiltered, this.genes.length );
               this.controlPanel.updateConditionsTitle( this.conditionTree.numFiltered, this.conditions.length );

               this.visualizationPanel.setConditionTree( this.conditionTree );
               this.visualizationPanel.setGeneTree( this.geneTree );

               this.visualizationPanel.redraw( true );

               this.visualizationPanel.mask.hide();
            }, this );

            // this.on('afterLayout', this.showHelpConditionally);
            this.showHelpConditionally();

         },

         refreshVisualization : function() {
            this.visualizationPanel.updateVisibleScores();
            this.controlPanel.doFiltering_();
         },

         // TODO: finish this, this is currently out of scope.
         getApplicationState : function() {
            var state = {};
            // Get gene group ids.
            // If there are any session-bound groups, get query that made them.
            state.geneGroupIds = [];
            state.geneIds = [];
            var i, ref, k = 0;
            for (i = 0; i < this.metaheatmapData.geneGroupReferences.length; i++) {
               ref = this.metaheatmapData.geneGroupReferences[i];
               if ( typeof ref.type !== 'undefined' ) {
                  if ( ref.type === 'databaseBackedGene' ) {
                     state.geneIds.push( ref.id );
                  } else if ( ref.type.toLowerCase().indexOf( 'session' ) === -1
                     && ref.type.toLowerCase().indexOf( 'group' ) !== -1 ) {
                     state.geneGroupIds.push( ref.id );
                  } else {
                     this.usingSessionGroup = true;
                  }
               }
            }
            if ( this.experimentSessionGroupQueries ) {
               state.experimentSessionGroupQueries = this.experimentSessionGroupQueries;
            }
            if ( this.geneSessionGroupQueries ) {
               state.geneSessionGroupQueries = this.geneSessionGroupQueries;
            }

            // Get experiment group ids.
            // If there are any session-bound groups, get queries that made them.
            state.eeGroupIds = [];
            state.eeIds = [];
            for (i = 0; i < this.metaheatmapData.datasetGroupReferences.length; i++) {
               ref = this.metaheatmapData.datasetGroupReferences[i];
               if ( typeof ref.type !== 'undefined' ) {
                  if ( ref.type === 'databaseBackedExperiment' ) {
                     state.eeIds.push( ref.id );
                  } else if ( ref.type.toLowerCase().indexOf( 'session' ) === -1
                     && ref.type.toLowerCase().indexOf( 'group' ) !== -1 ) {
                     state.eeGroupIds.push( ref.id );
                  } else {
                     this.usingSessionGroup = true;
                  }
               }
            }

            // Gene sort state.
            state.geneSort = this.toolPanel_._sortPanel._geneSort.getValue();

            // Experiment sort state.
            state.eeSort = this.toolPanel_._sortPanel._experimentSort.getValue();
            if ( state.eeSort === '--' ) {
               state.eeSort = null;
            }

            // filters
            var toFilter = [];
            var children = this.tree.getRootNode().childNodes;
            for (i = 0; i < children.length; i++) {
               if ( !children[i].attributes.checked ) {
                  toFilter.push( children[i].id );
               }
            }
            state.factorFilters = toFilter;
            state.taxonId = this.metaheatmapData.taxonId;
            return state;
         },

         /**
          * 
          * this.getSApplicationState
          *           should have format: state.geneIds = array of gene ids that occur singly (not in a group): [7,8,9]
          *           state.geneGroupIds = array of db-backed gene group ids: [10,11,12] ^same for experiments^
          *           state.geneSort state.eeSort state.filters = list of filters applied, values listed should be
          *           filtered OUT (note this is the opposite heuristic as in viz) (done to minimize url length)
          *           state.taxonId
          * @return url string or null if error or nothing to link to
          */
         getBookmarkableLink : function() {
            var state = this.getApplicationState();
            return Gemma.Metaheatmap.Utils.getBookmarkableLink( state );
         },

         showBookmarkableLinkWindow : function() {
            var url = this.getBookmarkableLink();

            var warning = (this.selectionsModified) ? "Please note: you have unsaved modifications in one or more of your"
               + " experiment and/or gene groups. <b>These changes will not be saved in this link.</b>"
               + " In order to keep your modifications, please log in and save your unsaved groups.<br><br>"
               : "";

            if ( url === null && warning === "" ) {
               url = "Error creating your link.";
            }
            var win = new Ext.Window( {
               closeAction : 'close',
               title : "Bookmark or sharable link",
               html : '<b>Use this link to re-run your search:</b><br> <a target="_blank" href="' + url + '">' + url
                  + '</a>',
               width : 650,
               padding : 10
            } );
            win.show();
         },

         showHelpConditionally : function() {

            if ( this.showTutorial && !this.tutorialControlPanel ) {

               this.tutorialControlPanel = new Gemma.Tutorial.ControlPanel( {
                  instructions : Gemma.HelpText.WidgetDefaults.MetaheatmapApplication.Tutorial.instructions,
                  renderTo : 'tutorial-control-div',
                  // need id to clear tutorial between searches
                  id : 'tutorial-cntlPanel-diff-ex'
               // stateId: 'diffExVisualiserTutorial'
               } );
               // hidden is stateful, the panel will be created hidden if the tutorial has already been shown
               // 
               if ( !this.tutorialControlPanel.hidden ) {
                  var tipDefinitions = [];
                  tipDefinitions.push( {
                     element : this.visualizationPanel.variableWidthCol.boxHeatmap,
                     title : Gemma.HelpText.WidgetDefaults.MetaheatmapApplication.Tutorial.searchResultsTitle,
                     text : Gemma.HelpText.WidgetDefaults.MetaheatmapApplication.Tutorial.searchResultsText,
                     tipConfig : {
                        anchor : 'bottom',
                        anchorOffset : 130
                     // offsets the little arrow part only
                     },
                     position : {
                        moveDown : 50
                     }
                  } );
                  tipDefinitions
                     .push( {
                        element : this.visualizationPanel.fixedWidthCol.pnlControlAndLabels.pnlMiniControl.showFoldChangeToggle,
                        title : Gemma.HelpText.WidgetDefaults.MetaheatmapApplication.Tutorial.foldChangeTitle,
                        text : Gemma.HelpText.WidgetDefaults.MetaheatmapApplication.Tutorial.foldChangeText,
                        tipConfig : {
                           anchor : 'left'
                        }
                     } );
                  tipDefinitions.push( {
                     element : this.getTopToolbar().colorLegendButton,
                     title : Gemma.HelpText.WidgetDefaults.MetaheatmapApplication.Tutorial.colourLegendTitle,
                     text : Gemma.HelpText.WidgetDefaults.MetaheatmapApplication.Tutorial.colourLegendText
                  } );
                  tipDefinitions.push( {
                     element : this.controlPanel,
                     title : Gemma.HelpText.WidgetDefaults.MetaheatmapApplication.Tutorial.sortAndFilterTitle,
                     text : Gemma.HelpText.WidgetDefaults.MetaheatmapApplication.Tutorial.sortAndFilterText,
                     tipConfig : {
                        anchor : 'right'
                     },
                     position : {
                        moveDown : 150
                     }
                  } );
                  tipDefinitions.push( {
                     element : this.getTopToolbar().downloadButton,
                     title : Gemma.HelpText.WidgetDefaults.MetaheatmapApplication.Tutorial.downloadTitle,
                     text : Gemma.HelpText.WidgetDefaults.MetaheatmapApplication.Tutorial.downloadText
                  } );

                  this.tutorialControlPanel.addTips( tipDefinitions );

               }
               this.tutorialControlPanel.on( 'tutorialHidden', function() {
                  this.tutorialControlPanel.hide();
               }, this );
            }
            this.on( 'afterrender', function() {
               if ( this.showTutorial && !this.tutorialControlPanel ) {
                  this.showHelpConditionally();
               }
               if ( this.showTutorial && this.tutorialControlPanel ) {
                  this.tutorialControlPanel.playTips( 0 );
               }
            } );
         }

      } );