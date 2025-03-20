/*
 * The Gemma project
 * 
 * Copyright (c) 2013 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 * 
 */
Ext.namespace( 'Gemma' );

/**
 * Panel to hold the CytoscapeJSDisplay, with the toolbar at the bottom. This is used as a tab in the results display
 */
Gemma.CytoscapeJSPanel = Ext.extend( Ext.Panel, {
   title : 'Cytoscape',
   layout : 'fit',
   autoScroll : false,

   // This is for a bug in ExtJS tabPanel that causes an unactivated Panel in a TabPanel to be rendered
   // when the Panel is removed from the tabPanel
   stopRender : false,

   coexpressionSearchData : null,
   coexDisplaySettings : null,

   /**
    * @private
    * 
    * @param searchResults
    * @returns
    */
   getJSONGraph : function( searchResults ) {
      return Gemma.CoexpressionJSONUtils.constructJSONGraphData( searchResults.getQueryGeneIds(), searchResults
         .getCytoscapeResults() );
   },

   /**
    * @memberOf Gemma.CytoscapeJSPanel
    */
   initComponent : function() {
      this.display = new Gemma.CytoscapeJSDisplay( {
         id : 'cy',
         autoScroll : false,
         cytoscapePanel : this,
         listeners : {
            afterrender : {
               fn : function() {
                  // stopRender is needed because of a bug in extjs where a tabpanel renders its components
                  // upon removal
                  if ( !this.stopRender ) {
                     this.loadMask = new Ext.LoadMask( this.getEl(), {
                        msg : Gemma.StatusText.Searching.analysisResults,
                        msgCls : 'absolute-position-loading-mask ext-el-mask-msg x-mask-loading'
                     } );
                  }
               },
               scope : this
            }
         },
         coexpressionSearchData : this.coexpressionSearchData,
         coexDisplaySettings : this.coexDisplaySettings
      } );

      this.controlBar = new Gemma.CytoscapeControlBar( {
         coexpressionSearchData : this.coexpressionSearchData,
         coexDisplaySettings : this.coexDisplaySettings,
         display : this.display,
         cytoscapePanel : this
      } );

      this.graphSizeMenu = new Ext.menu.Menu( {
         items : [ {
            itemId : 'graphSizeLarge',
            text : 'Large',
            handler : function() {
               this.graphSizeMenuHandler( "large" );
            },
            scope : this
         }, {
            itemId : 'graphSizeMedium',
            text : 'Medium',
            handler : function() {
               this.graphSizeMenuHandler( "medium" );
            },
            scope : this
         }, {
            itemId : 'graphSizeSmall',
            text : 'Small',
            handler : function() {
               this.graphSizeMenuHandler( "small" );
            },
            scope : this
         } ]
      } );

      var bbar = new Ext.Toolbar( {
         hidden : false,
         items : [
                  {
                     xtype : 'tbtext',
                     text : '',
                     itemId : 'bbarStatus'
                  },
                  {
                     xtype : 'button',
                     id : 'graphSizeMenu',
                     text : '<b>Graph Size</b>',
                     menu : this.graphSizeMenu

                  },
                  {
                     xtype : 'label',
                     id : 'tooltipMenuNotEnabled',
                     html : '&nbsp&nbsp<img ext:qtip="' + Gemma.HelpText.WidgetDefaults.CytoscapePanel.graphSizeMenuTT2
                        + '" src="' + Gemma.CONTEXT_PATH + '/images/icons/question_blue.png"/>&nbsp',
                     height : 15
                  },
                  {
                     xtype : 'label',
                     id : 'tooltipMenuEnabled',
                     html : '&nbsp&nbsp<img ext:qtip="' + Gemma.HelpText.WidgetDefaults.CytoscapePanel.graphSizeMenuTT
                        + '" src="' + Gemma.CONTEXT_PATH + '/images/icons/question_blue.png"/>&nbsp',
                     height : 15
                  } ]

      } );

      Ext.apply( this, {
         tbar : this.controlBar,
         bbar : bbar,
         margins : {
            top : 0,
            right : 0,
            bottom : 0,
            left : 0
         },
         items : [ this.display ]
      } );

      Gemma.CytoscapeJSPanel.superclass.initComponent.apply( this, arguments );

      // Extjs event, fired by TabPanel when this tab is activated/displayed.
      this.on( 'activate', this.onPanelActivation, this );

      this.coexpressionSearchData.on( 'search-started', function() {
         if ( this.loadMask ) {
            this.loadMask.show();
         }
      }, this );

      /*
       * Event handler: when the search results are ready. SearchResults is
       */
      this.coexpressionSearchData.on( 'complete-search-results-ready',
         function( searchResults, cytoscapeSearchCommand ) {

            // FIXME this method needs to be revised/simplifed
            // debugger;
            if ( this.coexpressionSearchData.getQueryGeneIds().length < 2 ) {
               this.coexDisplaySettings.setQueryGenesOnly( false );
               this.controlBar.disableQueryGenesOnlyCheckBox( true );
            } else {
               this.controlBar.disableQueryGenesOnlyCheckBox( false );
            }

            this.coexGraphData = new Gemma.CoexGraphData( this.coexpressionSearchData, cytoscapeSearchCommand );

            // if no trimming on the front end
            var cytoscapeSearchResults = this.coexpressionSearchData.cytoscapeSearchResults;
            if ( !this.coexGraphData.mediumGraphDataEnabled ) {
               this.initializeGraph();
               // if the graph was trimmed on the back end
               if ( cytoscapeSearchResults.trimStringency > cytoscapeSearchResults.queryStringency ) {
                  this.showUserMessageBar( cytoscapeSearchResults.trimStringency, false );
               } else {
                  this.getBottomToolbar().hide();
               }
               return;
            }

            var graphData = this.coexGraphData.getGraphData();

            this.coexpressionSearchData.setCytoscapeResults( graphData.geneResults );

            if ( graphData.trimStringency ) {
               this.coexpressionSearchData.setCytoscapeResults( graphData.geneResults );
               this.initializeGraph();
               // this.coexpressionSearchData.cytoscapeResults.knownGeneResults = resultsObject.geneResults;
               this.showUserMessageBar( graphData.trimStringency, true );
            } else if ( cytoscapeSearchResults.trimStringency > cytoscapeSearchResults.queryStringency ) {
               this.initializeGraph();
               this.showUserMessageBar( cytoscapeSearchResults.getTrimStringency(), false );
            } else {
               this.initializeGraph();
               this.getBottomToolbar().hide();
            }

         }, this );

      this.on( 'search-error', function( error ) {
         Ext.Msg.alert( "Error", error );
         this.loadMask.hide();
         this.fireEvent( 'beforesearch' );
      }, this );

      this.display.on( 'layout_complete', function() {
         this.loadMask.hide();
      }, this );

      this.addEvents( 'queryUpdateFromCoexpressionViz', 'coexWarningAlreadyDisplayed' );

      this.relayEvents( this.coexpressionSearchData, [ 'search-results-ready', 'complete-search-results-ready',
                                                      'search-error' ] );

      if ( this.searchPanel ) {
         this.searchPanel.relayEvents( this, [ 'queryUpdateFromCoexpressionViz', 'beforesearch' ] );
      }
   },

   /**
    * 
    */
   searchForCytoscapeData : function() {
      this.loadMask.show();
      var stringency = this.coexpressionSearchData.getQueryStringency();
      this.coexpressionSearchData.searchForCytoscapeDataWithStringency( stringency );
   },

   /**
    * @private Since we are a panel inside tabpanel [grid, visualization]. This is run when user selects visualization
    *          tab.
    * 
    */
   onPanelActivation : function() {
      if ( this.coexpressionSearchData.searchCommandUsed.queryGenesOnly
         && !this.coexpressionSearchData.cytoscapeResultsUpToDate ) {
         this.searchForCytoscapeData();
         return;
      }

      if ( this.coexpressionSearchData.searchCommandUsed.queryGenesOnly
         && this.coexpressionSearchData.cytoscapeResultsUpToDate ) {
         return;
      }

      if ( !this.coexpressionSearchData.cytoscapeResultsUpToDate ) {
         this.searchForCytoscapeData();
         return;
      }

      // check to see if coexGrid display stringency is below cytoscape results stringency, if so, give the user
      // the FIXME bug 4361 the coexGrid doesn't allow lowering the stringency.
      // option of reloading graph ("a new query will be required...")
      // at new stringency or returning display to current cytoscape stringency
      var displayStringency = this.coexDisplaySettings.getStringency();
      var resultsStringency = this.coexpressionSearchData.getQueryStringency();

      // FIXME see bug 4361 - this only fires when the tab is activated, so lowering the stringency while on the tab
      // does nothing.
      if ( this.display.ready && displayStringency < resultsStringency ) {
         Ext.Msg.show( {
            title : 'New Search Required',
            msg : String.format(
               Gemma.HelpText.WidgetDefaults.CytoscapePanel.newSearchOrReturnToCurrentStringencyOption,
               resultsStringency, displayStringency ),
            buttons : {
               ok : 'New search',
               cancel : 'Cancel'
            },
            fn : function( button ) {
               var resultsStringency = this.coexpressionSearchData.getResultsStringency();
               if ( button === 'ok' ) {
                  resultsStringency = Gemma.CytoscapePanelUtil.restrictResultsStringency( displayStringency );
                  this.coexpressionSearchData.searchForCytoscapeDataWithStringency( resultsStringency );
                  this.coexDisplaySettings.setStringency( displayStringency );
               } else { // 'cancel'
                  this.coexDisplaySettings.setStringency( resultsStringency );
               }
            }.createDelegate( this )
         } );

      }
   },

   refreshLayout : function() {
      this.loadMask.show();
      this.display.refreshLayout();
   },

   /**
    * This must update the search form and previews.
    * 
    * @public
    * @memberOf Gemma.CytoscapeJSPanel
    */
   searchWithSelectedNodes : function() {
      this.clearError();

      var selectedGeneIds = this.display.getSelectedGeneIds();

      if ( selectedGeneIds.length === 0 ) {
         Ext.Msg.alert( Gemma.HelpText.WidgetDefaults.CytoscapePanel.searchStatusTitle,
            Gemma.HelpText.WidgetDefaults.CytoscapePanel.searchStatusTooFew );
         return;
      }

      if ( selectedGeneIds.length > Gemma.MAX_GENES_PER_CO_EX_VIZ_QUERY ) {

         Ext.Msg.show( {
            title : Gemma.HelpText.WidgetDefaults.CytoscapePanel.searchStatusTitle,
            msg : String.format( Gemma.HelpText.WidgetDefaults.CytoscapePanel.searchStatusTooMany,
               Gemma.MAX_GENES_PER_CO_EX_VIZ_QUERY ),
            buttons : {
               ok : 'Ok',
               cancel : 'Cancel'
            },
            fn : function( btn ) {
               if ( btn === 'ok' ) {
                  this.updateSearchFormGenes( selectedGeneIds );
                  this.loadMask.show();
                  this.display.hideAll();
                  this.coexpressionSearchData.searchWithGeneIds( selectedGeneIds, true );
               }
            },
            scope : this
         } );

      } else {
         this.updateSearchFormGenes( selectedGeneIds );
         this.display.hideAll();
         this.loadMask.show();
         this.coexpressionSearchData.searchWithGeneIds( selectedGeneIds, false );
      }
   },

   /**
    * This must update the search form and previews.
    * 
    * @public
    * @memberOf Gemma.CytoscapeJSPanel
    */
   extendSelectedNodes : function() {
      this.clearError();

      var selectedGeneIds = this.display.getSelectedGeneIds();
      var queryGeneIds = this.coexpressionSearchData.getQueryGeneIds();

      function containsAll( needles, haystack ) {
         for (var i = 0, len = needles.length; i < len; i++) {
            if ( haystack.indexOf( needles[i] ) === -1 ) {
               return false;
            }
         }
         return true;
      }

      if ( selectedGeneIds.length === 0 ) {
         Ext.Msg.alert( Gemma.HelpText.WidgetDefaults.CytoscapePanel.searchStatusTitle,
            Gemma.HelpText.WidgetDefaults.CytoscapePanel.searchStatusTooFew );
         return;
      }

      if ( containsAll( selectedGeneIds, queryGeneIds ) ) {
         Ext.Msg.alert( Gemma.HelpText.WidgetDefaults.CytoscapePanel.searchStatusTitle,
            Gemma.HelpText.WidgetDefaults.CytoscapePanel.searchStatusNoExtraSelectedForExtend );
         return;
      }

      // add the selected genes to the query.
      for (var i = 0; i < selectedGeneIds.length; i++) {
         queryGeneIds.push( selectedGeneIds[i] );
      }

      if ( (queryGeneIds.length + selectedGeneIds.length) <= Gemma.MAX_GENES_PER_CO_EX_VIZ_QUERY ) {

         this.updateSearchFormGenes( queryGeneIds );
         this.loadMask.show();
         this.display.hideAll();
         this.coexpressionSearchData.searchWithGeneIds( queryGeneIds, false );
      } else {
         Ext.Msg.confirm( Gemma.HelpText.WidgetDefaults.CytoscapePanel.searchStatusTitle, String
            .format( Gemma.HelpText.WidgetDefaults.CytoscapePanel.searchStatusTooManyReduce,
               Gemma.MAX_GENES_PER_CO_EX_VIZ_QUERY ), function( btn ) {
            if ( btn === 'yes' ) {
               // Do a query genes only search
               this.updateSearchFormGenes( queryGeneIds );
               this.loadMask.show();
               this.display.hideAll();
               this.coexpressionSearchData.searchWithGeneIds( queryGeneIds, true );
            }
         }, this );
      }
   },

   initializeGraph : function() {
      this.display.initializeGraph( this.getJSONGraph( this.coexpressionSearchData ) );
   },

   clearError : function() {
      if ( Ext.get( "analysis-results-search-form-messages" ) ) {
         Ext.DomHelper.overwrite( "analysis-results-search-form-messages", "" );
      }
   },

   /**
    * @memberOf Gemma.CytoscapeJSPanel
    * @param geneIds
    */
   updateSearchFormGenes : function( geneIds ) {
      // This collects all the query Genevalueobjects and fires an event to let the search form listening know
      // that the query has been changed.
      // We already have the geneValueObjects from the search results so this saves an extra call to the backend
      // because the search form usually queries the backend for this information
      // var genesToPreview = [];
      // var genesToPreviewIds = [];
      //
      // // the current results
      // var results = this.coexpressionSearchData.getResults();
      //
      // // I'm not quite sure this is necessary or wise.
      // for (var i = 0; i < results.length; i++) {
      // if ( genesToPreviewIds.indexOf( results[i].foundGene.id ) === -1
      // && geneIds.indexOf( results[i].foundGene.id ) !== -1 ) {
      // genesToPreview.push( results[i].foundGene );
      // genesToPreviewIds.push( results[i].foundGene.id );
      // }
      // if ( genesToPreviewIds.indexOf( results[i].queryGene.id ) === -1
      // && geneIds.indexOf( results[i].queryGene.id ) !== -1 ) {
      // genesToPreview.push( results[i].queryGene );
      // genesToPreviewIds.push( results[i].queryGene.id );
      // }
      // }
      //
      // // We have to search through query genes in case none showed up.
      // var queryGenes = this.coexpressionSearchData.getQueryGenes();
      // var qglength = queryGenes.length;
      // for (i = 0; i < qglength; i++) {
      // if ( genesToPreviewIds.indexOf( queryGenes[i].id ) === -1 && geneIds.indexOf( queryGenes[i].id ) !== -1 ) {
      // genesToPreview.push( queryGenes[i] );
      // genesToPreviewIds.push( queryGenes[i].id );
      // }
      // }
      // Add new genes to search from.
      this.fireEvent( 'queryUpdateFromCoexpressionViz', geneIds );
   },

   showUserMessageBar : function( trimmedValue, showGraphSizeMenu ) {

      var bbarText = this.getBottomToolbar().getComponent( 'bbarStatus' );

      var graphSizeButton = this.getBottomToolbar().getComponent( "graphSizeMenu" );

      if ( showGraphSizeMenu ) {

         bbarText.setText( "Edges not involving query genes have been trimmed at stringency: " );

         if ( trimmedValue == 0 ) {
            graphSizeButton.setText( "No Trimming " );
         } else {
            graphSizeButton.setText( trimmedValue + " " );
         }
         this.getBottomToolbar().getComponent( 'tooltipMenuEnabled' ).setVisible( true );
         this.getBottomToolbar().getComponent( 'tooltipMenuNotEnabled' ).setVisible( false );

         if ( this.coexGraphData.largeGraphDataEnabled ) {

            // case where the original Results haven't been trimmed
            if ( this.coexGraphData.originalResults.trimStringency == 0 ) {

               this.graphSizeMenu.getComponent( "graphSizeLarge" ).setText(
                  "No Trimming (" + this.coexGraphData.originalResults.geneResults.length + " edges)" );
            } else {
               this.graphSizeMenu.getComponent( "graphSizeLarge" ).setText(
                  this.coexGraphData.originalResults.trimStringency + " ("
                     + this.coexGraphData.originalResults.geneResults.length + " edges)" );
            }

            this.graphSizeMenu.getComponent( "graphSizeLarge" ).show();

            if ( trimmedValue == this.coexGraphData.originalResults.trimStringency ) {
               this.graphSizeMenu.getComponent( "graphSizeLarge" ).addClass( "buttonBold" );
            } else {
               this.graphSizeMenu.getComponent( "graphSizeLarge" ).removeClass( "buttonBold" );
            }
         } else {
            this.graphSizeMenu.getComponent( "graphSizeLarge" ).hide();
         }

         if ( this.coexGraphData.mediumGraphDataEnabled ) {
            this.graphSizeMenu.getComponent( "graphSizeMedium" ).setText(
               this.coexGraphData.graphDataMedium.trimStringency + " ("
                  + this.coexGraphData.graphDataMedium.geneResults.length + " edges)" );
            this.graphSizeMenu.getComponent( "graphSizeMedium" ).show();

            if ( trimmedValue == this.coexGraphData.graphDataMedium.trimStringency ) {
               this.graphSizeMenu.getComponent( "graphSizeMedium" ).addClass( "buttonBold" );
            } else {
               this.graphSizeMenu.getComponent( "graphSizeMedium" ).removeClass( "buttonBold" );
            }

         } else {
            this.graphSizeMenu.getComponent( "graphSizeMedium" ).hide();
         }

         if ( this.coexGraphData.smallGraphDataEnabled ) {
            this.graphSizeMenu.getComponent( "graphSizeSmall" ).setText(
               this.coexGraphData.graphDataSmall.trimStringency + " ("
                  + this.coexGraphData.graphDataSmall.geneResults.length + " edges)" );
            this.graphSizeMenu.getComponent( "graphSizeSmall" ).show();

            if ( trimmedValue == this.coexGraphData.graphDataSmall.trimStringency ) {
               this.graphSizeMenu.getComponent( "graphSizeSmall" ).addClass( "buttonBold" );
            } else {
               this.graphSizeMenu.getComponent( "graphSizeSmall" ).removeClass( "buttonBold" );
            }
         } else {
            this.graphSizeMenu.getComponent( "graphSizeSmall" ).hide();
         }

         if ( !graphSizeButton.isVisible() ) {
            graphSizeButton.show();
         }

      } else {

         this.getBottomToolbar().getComponent( 'tooltipMenuEnabled' ).setVisible( false );
         this.getBottomToolbar().getComponent( 'tooltipMenuNotEnabled' ).setVisible( true );

         if ( graphSizeButton.isVisible() ) {
            graphSizeButton.hide();
         }
         bbarText.setText( "Edges not involving query genes have been trimmed at stringency: " + trimmedValue );
      }

      this.getBottomToolbar().show();

   },

   graphSizeMenuHandler : function( graphSize ) {
      var graphData = this.coexGraphData.getGraphData( graphSize );
      this.coexpressionSearchData.cytoscapeSearchResults.knownGeneResults = graphData.geneResults;
      this.showUserMessageBar( graphData.trimStringency, true );
      this.loadMask.show();
      this.display.hideAll();
      this.initializeGraph();
   }
} );