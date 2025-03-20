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
 * Component holding a Cytoscapejs canvas.
 */
Gemma.CytoscapeJSDisplay = Ext.extend( Ext.BoxComponent, {

   /**
    * @private
    */
   ready : false,

   /**
    * @private
    */
   emphasized : true,

   /**
    * @memberOf Gemma.CytoscapeJSDisplay
    */
   initComponent : function() {
      this.ready = false;

      var display = this;

      Gemma.CytoscapeJSDisplay.superclass.initComponent.apply( this, arguments );
      this.addEvents( 'selection_available', 'selection_unavailable', 'layout_complete' );

      display.coexDisplaySettings.on( 'stringency_change', display.update.createDelegate( display ) );

      display.coexDisplaySettings.on( 'query_genes_only_change', display.update.createDelegate( display ) );

      display.coexDisplaySettings.on( 'gene_overlay', display.update.createDelegate( display ) );

      display.coexDisplaySettings.on( 'search_text_change', function( text ) {
         display.selectNodesMatchingText( text );
      } );
   },

   hideAll : function() {
      this.cy.elements().hide();
   },

   /**
    * 
    * @param {Array}
    *           graphData
    */
   initializeGraph : function( graphData ) {
      Gemma.CytoscapeJSCoexGraphInitializer( jQuery( '#cy' ), graphData, this.onGraphReady, this );
   },

   /**
    * ownerRef refers to 'this'. Attach lissteners
    * 
    * @param {Object}
    *           ownerRef
    */
   onGraphReady : function( ownerRef ) {
      ownerRef.cy.on( 'done', function( e ) {
         ownerRef.cy.panningEnabled( true );
         ownerRef.fireEvent( 'selection_available' );
         ownerRef.ready = true;
      } );

      ownerRef.cy.on( 'layoutstop', function( e ) {
         ownerRef.update();
         ownerRef.cytoscapePanel.loadMask.hide();
         ownerRef.zoomToFit();
      } );
   },

   /**
    * @private
    * @param isNodeDegreeEmphasis
    */
   applyDefaultGraphStyle : function() {
      // reset the overlay
      this.cy.nodes().toggleClass( 'overlay', false );

      // switch nodes that have emphasis to the opposite; switch nodes that are basic to the opposite? I don't get it.
      this.cy.elements().toggleClass( 'emphasis', this.emphasized );
      this.cy.elements().toggleClass( 'basic', !this.emphasized );
   },

   /**
    * Update the display based on the current settings.
    */
   update : function() {
      if ( !this.ready ) {
         return;
      }
      try {
         this.filter();
         this.applyDefaultGraphStyle();
         this.applyGeneListOverlay();
      } catch (err) {
         Gemma.Error.genericErrorHandler( err );
      }
   },

   /**
    * 
    * @param isNodeDegreeEmphasis
    */
   nodeDegreeEmphasize : function( isNodeDegreeEmphasis ) {
      if ( !this.ready ) {
         return;
      }
      this.emphasized = isNodeDegreeEmphasis;
      this.update();
   },

   /**
    * 
    */
   filter : function() {
      if ( !this.ready ) {
         return;
      }

      var stringency = this.coexDisplaySettings.getStringency();
      var queryGenesOnly = this.coexDisplaySettings.getQueryGenesOnly();

      var trimmed = {};
      if ( queryGenesOnly ) {
         trimmed.trimmedNodeIds = this.coexpressionSearchData.getQueryGeneIds();
      } else {
         trimmed.trimmedNodeIds = Gemma.CoexVOUtil.trimResultsForQueryGenes( this.coexpressionSearchData.getResults(),
            this.coexpressionSearchData.getQueryGeneIds(), stringency );
      }

      var nodeIdsToHide = this.getNodeIdsToHide( trimmed.trimmedNodeIds, this.coexpressionSearchData.allGeneIdsSet );

      var nodeHideFunction = function( i, element ) {
         return element.isNode() && nodeIdsToHide.indexOf( element.data( "geneid" ) ) !== -1;
      };

      var nodeShowFunction = function( i, element ) {
         return element.isNode() && trimmed.trimmedNodeIds.indexOf( element.data( "geneid" ) ) !== -1;
      };

      var nodesToHide = this.cy.filter( nodeHideFunction );
      var nodesToShow = this.cy.filter( nodeShowFunction );

      nodesToHide.hide();
      nodesToHide.unselectify();

      nodesToShow.selectify();
      nodesToShow.show();

      var edgeShowFunction = function( i, element ) {
         return element.isEdge() && element.data( "support" ) >= stringency;
      };

      var edgeHideFunction = function( i, element ) {
         return element.isEdge() && element.data( "support" ) < stringency;
      };

      var edgesToHide = this.cy.filter( edgeHideFunction );
      var edgesToShow = this.cy.filter( edgeShowFunction );

      edgesToHide.hide();
      edgesToShow.show();

   },

   /**
    * 
    * @returns {Array}
    */
   getSelectedGeneIds : function() {
      if ( !this.ready ) {
         return;
      }

      var nodes = this.cy.elements( "node:selected:visible" );

      var geneIds = [];
      for (var i = 0; i < nodes.length; i++) {
         geneIds.push( nodes[i].data( "geneid" ) );
      }
      return geneIds;
   },

   /**
    * 
    */
   refreshLayout : function() {
      if ( !this.ready ) {
         return;
      }
      this.hideAll();

      this.cy.layout( Gemma.CytoscapejsSettings.arborLayout );
   },

   /**
    * 
    */
   zoomToFit : function() {
      if ( !this.ready ) {
         return;
      }
      this.cy.fit( 50 ); // 50=margin
   },

   /**
    * @public
    * @param {boolean}
    *           visible
    */
   toggleNodeLabels : function( visible ) {
      if ( !this.ready ) {
         return;
      }
      console.log( "toggleNodeLabels" );
      var content = "";
      if ( visible ) {
         content = 'data(name)';
      }
      this.cy.style().selector( 'node' ).css( {
         'content' : content
      } ).update();

   },

   /**
    * @private
    * @param nodeIds
    */
   selectNodes : function( nodeIds ) {

      var nodeSelectFunction = function( i, element ) {
         return element.isNode() && nodeIds.indexOf( element.data( "geneid" ) ) !== -1;
      };

      var nodesToSelect = this.cy.filter( nodeSelectFunction );

      nodesToSelect.select();

   },

   /**
    * @private
    */
   deselectNodesAndEdges : function() {
      this.cy.nodes().unselect();
      this.cy.edges().unselect();
   },

   /**
    * @private
    * @public
    */
   applyGeneListOverlay : function() {
      if ( !this.ready ) {
         return;
      }

      var overlayIds = this.coexDisplaySettings.getOverlayGeneIds();

      if ( overlayIds.length == 0 ) {
         // this.cy.nodes().toggleClass( 'overlay', false );
         return;
      }

      var nodeOverlayFunction = function( i, element ) {
         return element.isNode() && overlayIds.indexOf( element.data( "geneid" ) ) !== -1;
      };

      var nodesToOverlay = this.cy.filter( nodeOverlayFunction );
      if ( nodesToOverlay.length > 0 ) {
         nodesToOverlay.toggleClass( 'overlay', true );
      }

   },

   /**
    * @public
    * @param nodeIds
    * @returns {{total: number, hidden: number}}
    */
   getNodesMatching : function( nodeIds ) {
      var matchingCounts = {
         total : 0,
         hidden : 0
      };

      var nodesMatchingFunction = function( i, element ) {

         return element.isNode() && nodeIds.indexOf( element.data( "geneid" ) ) !== -1;
      };

      var nodesMatched = this.cy.filter( nodesMatchingFunction );
      for (var i = 0; i < nodesMatched.length; i++) {

         if ( nodesMatched[i] !== null ) {
            matchingCounts.total += 1;
            if ( !nodesMatched[i].visible() ) {
               matchingCounts.hidden += 1;
            }
         }
      }
      return matchingCounts;
   },

   /**
    * @private
    * @param text
    */
   selectNodesMatchingText : function( text ) {
      if ( !this.ready ) {
         return;
      }

      this.deselectNodesAndEdges();
      if ( text.length < 2 ) {
         return;
      }
      var nodeIdsToSelect = this.coexpressionSearchData.getCytoscapeGeneSymbolsMatchingQuery( text );
      this.selectNodes( nodeIdsToSelect );
   },

   /**
    * @private
    */
   applySelection : function() {
      if ( !this.ready ) {
         return;
      }

      this.selectNodesMatchingText( this.coexDisplaySettings.getSearchTextValue() );
   },

   /**
    * @private
    * @param nodesToShow
    * @param allNodes
    * @returns {Array}
    */
   getNodeIdsToHide : function( nodesToShow, allNodes ) {

      var nodeIdsToHide = [];

      var length = allNodes.length;

      for (var i = 0; i < length; i++) {

         if ( nodesToShow.indexOf( allNodes[i] ) == -1 ) {
            nodeIdsToHide.push( allNodes[i] );
         }

      }

      return nodeIdsToHide;

   },

   /**
    * Produce screen view
    */
   exportPNG : function() {

      var htmlString = '<img src="' + this.cy.png() + '"/>';

      var win = new Ext.Window( {
         title : Gemma.HelpText.WidgetDefaults.CytoscapePanel.exportPNGWindowTitle,
         plain : false,
         bodyStyle : {
            "background-color" : "white"
         },
         html : htmlString,
         height : 700,
         width : 900,
         autoScroll : true
      } );
      win.show();
   },

   /**
    * Produce text version of graph
    */
   exportText : function() {
      var filteredData;
      var queryGenesOnly = this.coexDisplaySettings.getQueryGenesOnly();
      var stringency = this.coexDisplaySettings.getStringency();

      if ( queryGenesOnly && !this.coexpressionSearchData.searchCommandUsed.queryGenesOnly ) {
         filteredData = Gemma.CoexVOUtil.trimResults( this.coexpressionSearchData.getQueryGenesOnlyResults(),
            stringency );
      } else {
         filteredData = Gemma.CoexVOUtil.trimResults( this.coexpressionSearchData.getCytoscapeResults(), stringency );

      }

      var win = new Gemma.CoexpressionDownloadWindow( {
         title : "Coexpression Data",
         queryGenesOnlyResults : true
      } );
      win.convertText( filteredData );
   }

} );