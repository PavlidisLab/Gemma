/*
 * The Gemma project
 * 
 * Copyright (c) 2009 University of British Columbia
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

Gemma.VIZ_WINDOW_WIDTH = 750;
Gemma.VIZ_WINDOW_HEIGHT = 500;
Gemma.ZOOM_PLOT_SIZE = 500;
Gemma.LEGEND_PANEL_HEIGHT = 100;
Gemma.DIFFEXVIS_QVALUE_THRESHOLD = 0.05;

// Multiply the line thickness by this factor when it is
// selected in the legend
Gemma.SELECTED = 2;
Gemma.LINE_THICKNESS = 1;
Gemma.ZOOM_LINE_THICKNESS = 2;
Gemma.THUMBNAIL_PLOT_SIZE = 120;

Gemma.HOT_FADE_COLOR = "#FFDDDD";
// Gemma.HOT_FADE_SELECTED_COLOR = "#FFBBBB";
Gemma.COLD_FADE_COLOR = "#DDDDDD";
// Gemma.COLD_FADE_SELECTED_COLOR = "#BBBBBB";

Gemma.MAX_LABEL_LENGTH_CHAR = 12;
Gemma.MAX_THUMBNAILLABEL_LENGTH_CHAR = 30;
Gemma.MAX_EE_NAME_LENGTH = 40;

/**
 * Vertical strip of plots, with a legend. Supports little heatmaps or little linecharts.
 */
Gemma.DataVectorThumbnailsView = Ext.extend( Ext.DataView, {
   autoHeight : true,
   emptyText : 'No data',
   loadingText : 'Loading data ...',
   name : "vectorDV",

   singleSelect : true,
   showPValues : true,

   itemSelector : 'div.vizWrap',

   /**
    * The data get from the server is not compatible with flotr out-of-the box. A little transformation is needed.
    *
    * @param data for one record.
    * @memberOf Gemma.DataVectorThumbnailsView
    */
   prepareData : function( data ) {
      return Gemma.prepareProfiles( data, this.showPValues );
   },

   /**
    * Gets the selected node's record; or, if no node is selected, returns the first record; or null if there are no
    * nodes.
    *
    * @return {record}
    */
   getSelectedOrFirst : function() {
      if ( this.getSelectionCount() > 0 ) {
         return this.getSelectedRecords()[0];
      } else {
         var node = this.getNode( 0 );
         if ( node ) {
            return this.getRecord( node );
         }
      }
      return null;
   },

   /*
    * Used to switch between heatmap and line plots.
    */
   setTemplate : function( tpl ) {
      var k = this.getSelectedNodes();
      this.tpl = tpl;

      /*
       * Check that we're rendered already.
       */
      if ( this.el ) {
         this.refresh();
         this.select( k );
      }
   }

} );

/**
 * Takes a collection of VisualizationValueObjects, which in turn each contain a collection of GeneExpressionProfiles.
 *
 */
Gemma.prepareProfiles = function( data, showPValues ) {
   var preparedData = [];
   var geneExpressionProfile = data.profiles;

   for ( var i = 0; i < geneExpressionProfile.length; i++ ) {
      var profile = geneExpressionProfile[i].profile;

      var probeId = geneExpressionProfile[i].probe.id;
      var probe = geneExpressionProfile[i].probe.name;
      var genes = geneExpressionProfile[i].genes;
      var color = geneExpressionProfile[i].color;
      var factor = geneExpressionProfile[i].factor;
      var pvalue = geneExpressionProfile[i].PValue; // yes, it's PValue, not
      // pValue.
      var rank = geneExpressionProfile[i].rank;

      var fade = factor < 2;

      if ( fade ) {
         color = color === 'red' ? Gemma.HOT_FADE_COLOR : Gemma.COLD_FADE_COLOR;
      }

      /*
       * Format the gene symbols and names into strings that will be displayed in the legend.
       */

      var orderedGeneLinksArr = [];
      var orderedGeneNamesArr = [];
      var qtip = 'Go to gene page (Element: ' + probe + ')';
      if ( genes !== undefined && genes.length > 0 ) {
         var k, gene, link, geneName;

         for ( k = 0; k < genes.length; k++ ) {
            gene = genes[k];
            geneName = genes[k].officialName;
            link = '<a href="' + Gemma.LinkRoots.genePage + gene.id + '" target="_blank" ext:qtip="' + qtip + '">'
               + Ext.util.Format.ellipsis( gene.officialSymbol, Gemma.MAX_THUMBNAILLABEL_LENGTH_CHAR ) + '</a> ';

            // put the query gene first.
            if ( this.queryGene && geneName === this.queryGene ) {
               orderedGeneLinksArr.unshift( link );
               orderedGeneNamesArr.unshift( geneName );
            } else {
               orderedGeneLinksArr.push( link );
               orderedGeneNamesArr.push( geneName );
            }
         }
      } else {
         orderedGeneLinksArr.push( "<a href='" + ctxBasePath + "/compositeSequence/show.html?id=" + probeId
            + "' target='_blank' ext:qtip= '" + qtip + "'>Unmapped</a>" );
         orderedGeneNamesArr.push( "" );
      }

      /*
       * Turn a flat vector into an array of points (that's what flotr needs)
       */
      var points = [];
      for ( var j = 0; j < profile.length; j++ ) {
         var point = [ j, profile[j] ];
         points.push( point );
      }

      // Label for the thumbnail legend.
      var pvalueLabel = "";
      if ( pvalue !== undefined && showPValues ) {
         pvalueLabel = sprintf( "%.2e ", pvalue );
      }

      // use a fixed font size that matches the heatmap row height
      var labelStyle = 'font-size: 12px';
      if ( factor && factor < 2 ) {
         labelStyle += ";font-style:italic";
         // qtip = qtip + " [Not significant]";
      }

      /*
       * Note: flotr requires that the data be called 'data'.
       */
      var plotConfig = {
         profile : profile,
         data : points, // this is what gets plotted. Flotr wants this name.
         color : color,
         genes : genes,
         rawLabel : pvalueLabel + orderedGeneLinksArr.join( "; " ) + " " + orderedGeneNamesArr.join( "; " ),
         label : pvalueLabel + "<span style='" + labelStyle + "'>" + orderedGeneLinksArr.join( "; " ) + " "
            + orderedGeneNamesArr.join( "; " ) + "</span>",
         lines : {
            lineWidth : Gemma.LINE_THICKNESS
         },

         labelID : probeId,
         factor : factor,
         probe : {
            id : probeId,
            name : probe
         },
         PValue : pvalue, // yes, it's
         // PValue, not
         // pValue.
         rank : rank,
         smoothed : false

      };

      preparedData.push( plotConfig );
   }

   /*
    * The prepared data is augmented with the 'data' field and formatted labels.
    */
   data.profiles = preparedData;

   data.profiles.sort( Gemma.sortByImportance );

   return data;
};

/**
 * Used for thumbnails.
 */
Gemma.ProfileTemplate = Ext.extend( Ext.XTemplate, {

   cmpID : '123', // should be overwritten upon instantiation!
   graphConfig : {
      lines : {
         lineWidth : 1
      },
      bars : {
         fill : false
      },
      xaxis : {
         noTicks : 0
      },
      yaxis : {
         noTicks : 0
      },
      grid : {
         color : "white"
      },
      shadowSize : 0,
      legend : {
         show : false
      },
      forceFit : true
   },

   overwrite : function( el, values, ret ) {

      Gemma.ProfileTemplate.superclass.overwrite.call( this, el, values, ret );

      for ( var i = 0; i < values.length; i++ ) {
         var randomnumber = Math.floor( Math.random() * 101 );
         var record = values[i];
         var shortName = record.eevo.id; // can be a subset, which has no shortName.

         Ext.DomHelper.append( shortName + '_vizwrap_' + this.cmpID, {
            tag : 'div',
            id : shortName + "_vis" + randomnumber,
            style : 'width:' + Gemma.THUMBNAIL_PLOT_SIZE + 'px;height:' + Gemma.THUMBNAIL_PLOT_SIZE + 'px;'
         } );

         /*
          * Note: passing in 'newDiv' works in FF but not in IE. (flotr, anyway)
          */
         Gemma.LinePlot.draw( Ext.get( shortName + "_vis" + randomnumber ), record.profiles, this.graphConfig, null,
            record.factorValuesToNames ); // no
         // sample
         // names
      }
   }
} );

/**
 * Used for thumbnails.
 */
Gemma.HeatmapTemplate = Ext.extend( Ext.XTemplate, {

   cmpID : '123', // should be overwritten upon instantiation!
   graphConfig : {
      label : false, // shows labels at end of row, no for thumbnails
      forceFit : true,
      maxBoxHeight : 3,
      allowTargetSizeAdjust : true
      // make the rows smaller in the thumbnails
   },

   overwrite : function( el, values, ret ) {
      Gemma.HeatmapTemplate.superclass.overwrite.call( this, el, values, ret );

      for ( var i = 0; i < values.length; i++ ) {
         var randomnumber = Math.floor( Math.random() * 101 );
         var record = values[i];
         var shortName = record.eevo.id;
         Ext.DomHelper.append( shortName + '_vizwrap_' + this.cmpID, {
            tag : 'div',
            id : shortName + "_vis_" + randomnumber,
            style : 'width:' + Gemma.THUMBNAIL_PLOT_SIZE + 'px;height:' + Gemma.THUMBNAIL_PLOT_SIZE + 'px;'
         } );
         /*
          * Note: 'newDiv' works in FF but not in IE.
          */
         Heatmap.draw( Ext.get( shortName + "_vis_" + randomnumber ), record.profiles, this.graphConfig, null,
            record.factorValuesToNames, null ); // no
         // sample
         // names
      }
   }
} );

/**
 * Pick the appropriate template
 *
 */
Gemma.getProfileThumbnailTemplate = function( heatmap, havePvalues, smooth, cmpID ) {
   if ( cmpID === undefined || cmpID === null ) {
      cmpID = '';
   }
   var pvalueString = "";
   if ( havePvalues ) {
      // yes, minPvalue, from EEVO pValue. The best pvalue of any of the
      // profiles.
      pvalueString = '{[(values.minPvalue < 1) ? sprintf("<br/><span style=\'font-size:smaller\'>p=%.2e</span>", values.minPvalue) : ""]}';
   }

   if ( heatmap ) {
      var tmpl = new Gemma.HeatmapTemplate(
         '<tpl for="."><tpl for="eevo">',
         '<div class="vizWrap" ext.qtip="{values.name}; Click to zoom" id ="{id}_vizwrap_'
         + cmpID
         + '" style="cursor:pointer;float:left;padding: 10px"> <strong>{shortName}</strong>: <small> {[Ext.util.Format.ellipsis( values.name, Gemma.MAX_EE_NAME_LENGTH)]} </small> &nbsp;&nbsp;<i>'
         + pvalueString + '</i></div>', '</tpl></tpl>' );
      tmpl.cmpID = cmpID;
      return tmpl;
   } else {
      var tmpl = new Gemma.ProfileTemplate(
         '<tpl for="."><tpl for="eevo">',
         '<div class="vizWrap" ext.qtip="{values.name}; Click to zoom" id ="{id}_vizwrap_'
         + cmpID
         + '" style="cursor:pointer;float:left;padding: 10px"> <strong> {shortName}</strong>: <small> {[Ext.util.Format.ellipsis( values.name, Gemma.MAX_EE_NAME_LENGTH)]} </small> &nbsp;&nbsp; <i>'
         + pvalueString + '</i></div>', '</tpl></tpl>', {
            smooth : smooth
         } );
      tmpl.cmpID = cmpID;
      return tmpl;
   }

};

/**
 * Zoom panel.
 */
Gemma.VisualizationZoomPanel = Ext
   .extend(
      Ext.Panel,
      {

         region : 'center',
         split : true,
         // width : Gemma.ZOOM_PLOT_SIZE + 100,
         // height : Gemma.ZOOM_PLOT_SIZE + 100,
         width : Gemma.VIZ_WINDOW_WIDTH,
         height : Gemma.VIZ_WINDOW_HEIGHT - 100,
         stateful : false,
         autoScroll : false,
         closeAction : 'destroy',
         bodyStyle : "background:white",
         layout : 'fit',
         title : "", // dont show a title.
         name : "vizZoom",

         /*
          * The following are only used if we don't have a parent container, or on initialization.
          */
         heatmapMode : true,
         forceFitPlots : false,
         smoothLineGraphs : false,
         showLegend : false,
         showSampleNames : false,

         initComponent : function() {

            Gemma.VisualizationZoomPanel.superclass.initComponent.call( this );

            this.on( 'resize', function( component, width, height ) {
               component.update();
            }.createDelegate( this ) );
         },

         html : {
            tag : 'div',
            id : 'inner-zoom-html-' + Ext.id(),
            style : 'overflow:auto;width:' + Gemma.ZOOM_PLOT_SIZE + 'px;height:' + Gemma.ZOOM_PLOT_SIZE
               + 'px; margin:5px 2px 2px 5px;'
         },

         /**
          * Zoom panel update
          */
         update : function( eevo, profiles, sampleNames, conditionLabels, conditionLabelKey ) {
            if ( (profiles === undefined || profiles === null) && this.dv !== null ) {
               var record = this.dv.getSelectedOrFirst();

               if ( record === null || record === undefined ) {
                  // can happen at startup.
                  return;
               }

               profiles = record.get( "profiles" );
               if ( profiles === undefined ) {
                  // hopefully this doesn't happen.
                  return;
               }

               sampleNames = record.get( "sampleNames" );
               conditionLabels = record.get( "factorValuesToNames" );
               conditionLabelKey = record.get( "factorNames" );
            }

            if ( profiles === undefined ) {
               throw "No profiles!!";
            }

            if ( eevo ) {
               var eeInfoTitle = "";

               eeInfoTitle = "<a ext.qtip='Click for details on experiment (opens in new window)' target='_blank'  href='"
                  + ctxBasePath + "/expressionExperiment/showExpressionExperiment.html?shortName="
                  + eevo.shortName
                  + " '>"
                  + eevo.shortName
                  + "</a> ("
                  + Ext.util.Format.ellipsis( eevo.name, 65 ) + ")";

               if ( this.vizWindow && this.vizWindow.originalTitle ) {
                  this.vizWindow.setTitle( this.vizWindow.originalTitle + "&nbsp;in:&nbsp;" + eeInfoTitle );
               } else {
                  this.setTitle( eeInfoTitle );
               }
            }

            var forceFit = this.visPanel ? this.vizPanel.forceFitPlots : this.forceFitPlots;

            var smooth = this.vizPanel ? this.vizPanel.smoothLineGraphs : this.smoothLineGraphs;

            var showSampleNames = this.vizPanel ? this.vizPanel.showSampleNames : this.showSampleNames;

            var graphConfig = {
               xaxis : {
                  noTicks : 0
               },
               yaxis : {
                  noTicks : 0
               },
               grid : {
                  labelMargin : 0,
                  marginColor : "white"
               },
               shadowSize : 0,
               forceFit : forceFit,
               smoothLineGraphs : smooth,
               showSampleNames : showSampleNames,
               legend : {
                  show : this.showLegend, // &&
                  // !this.heatmapMode,
                  // container : this.legendDiv ? this.legendDiv : this.body.id,
                  labelFormatter : function( s ) {
                     // assume we only have one link defined...
                     var k = s.split( "</a>", 2 );

                     return k[0] + "</a>" + Ext.util.Format.ellipsis( k[1], Gemma.MAX_THUMBNAILLABEL_LENGTH_CHAR );
                  },
                  position : "sw" // best to be west, if we're expanded...applies
                  // to linecharts.
               },
               conditionLegend : false,
               label : true

            };

            Ext.DomHelper.overwrite( this.body.id, '' );

            var doHeatmap = (this.vizPanel && this.vizPanel.heatmapMode != 'undefined') ? this.vizPanel.heatmapMode
               : this.heatmapMode;

            if ( doHeatmap ) {
               graphConfig.legend.container = this.legendDiv ? this.legendDiv : this.body.id;
               profiles.sort( Gemma.sortByImportance );
               Heatmap.draw( Ext.get( this.body.id ), profiles, graphConfig, sampleNames, conditionLabels,
                  conditionLabelKey );
            } else {
               profiles.sort( Gemma.sortByImportance );

               // clear the heatmap legend, if it's there
               if ( this.legendDiv && this.legendDiv != this.body.id ) {
                  Ext.DomHelper.overwrite( this.legendDiv, '' );
               }

               Gemma.LinePlot.draw( Ext.get( this.body.id ), profiles, graphConfig, sampleNames, conditionLabels,
                  conditionLabelKey );

               // make the line chart legend clickable. Selector is based on
               // flotr's output.
               var legend = Ext.DomQuery.select( "div.flotr-legend", this.el.dom );

               if ( legend && legend[0] ) {

                  var onLegendClick = function( event, component ) {

                     var probeId = event.getTarget().id;

                     var record;
                     if ( this.dv.getSelectionCount() > 0 ) {
                        record = this.dv.getSelectedRecords()[0];
                     } else {
                        record = this.dv.getStore().getRange( 0, 0 )[0];
                     }

                     var eevo = record.get( "eevo" );
                     var profiles = record.get( "profiles" );
                     var sampleNames = record.get( "sampleNames" );
                     var conditionLabels = record.get( "factorValuesToNames" );
                     var conditionLabelKey = record.get( "factorNames" );

                     for ( var i = 0; i < profiles.length; i++ ) {

                        if ( profiles[i].labelID == probeId ) {
                           if ( profiles[i].selected ) {
                              profiles[i].lines.lineWidth = (profiles[i].lines.lineWidth / Gemma.SELECTED);
                              // profiles[i].lines.color =
                              // profile[i].lines.color; // put it back...
                              Ext.DomHelper.applyStyles( event.getTarget(), "border:black 2px" );
                              profiles[i].selected = false;
                           } else {
                              profiles[i].selected = true;
                              profiles[i].lines.lineWidth = profiles[i].lines.lineWidth * Gemma.SELECTED;
                              // profiles[i].lines.color =
                              // profile[i].lines.color; // make it selected

                              Ext.DomHelper.applyStyles( event.getTarget(), "border:green 2px" );

                           }
                           break;
                        }
                     }
                     this.update( eevo, profiles, sampleNames, conditionLabels, conditionLabelKey );

                  };

                  var el = new Ext.Element( legend[0] );
                  el.on( 'click', onLegendClick.createDelegate( this ) );
               }

            }

         }

      } ); // zoom panel.

Gemma.VisualizationWithThumbsWindow = Ext.extend( Ext.Window, {
   extraPanelParams : {}, // used for class extensions,
   // allows more configs to be
   // passed on to panel
   layout : 'fit',
   constructor : function( config ) {

      // don't want panel and window to have title
      if ( config.title ) {
         this.title = config.title;
         delete config.title;
      }
      var panelConfigParam = {
         havePvalues : true
      };

      // add extra config params to panel
      if ( this.extraPanelParams ) {
         Ext.apply( panelConfigParam, this.extraPanelParams );
      }
      // add extra config params to panel
      if ( config ) {
         Ext.apply( panelConfigParam, config );

      }
      if ( config.extraPanelParams ) {
         Ext.apply( panelConfigParam, config.extraPanelParams );
      }

      this.panelConfigParam = panelConfigParam;
      Gemma.VisualizationWithThumbsWindow.superclass.constructor.apply( this, arguments );
   },

   /**
    *
    */
   initComponent : function() {

      if ( this.title ) {
         this.originalTitle = this.title;
      }

      Gemma.VisualizationWithThumbsWindow.superclass.initComponent.apply( this, arguments );
      this.panelConfigParam.vizWindow = this;
      this.vizPanel = new Gemma.VisualizationWithThumbsPanel( this.panelConfigParam );
      this.relayEvents( this.vizPanel, [ 'loadFailed', 'loadSucceeded' ] );
      this.add( this.vizPanel );
   },

   show : function( config ) {
      Gemma.VisualizationWithThumbsWindow.superclass.show.call( this );

      if ( this.prevX !== null ) {
         this.setPosition( this.prevX + 20, this.prevY + 20 );
      }

      var params = config.params || [];

      this.vizPanel.loadFromParam( config );

   }

} );

/**
 * Two-part panel with thumbnails on the left, zoom view on the right.
 */
Gemma.VisualizationWithThumbsPanel = Ext.extend( Ext.Panel, {
   closeAction : 'destroy',
   bodyStyle : "background:white",
   layout : 'border',
   border : false,
   // title : "Visualization",
   maximizable : false,
   height : Gemma.VIZ_WINDOW_HEIGHT,
   width : Gemma.VIZ_WINDOW_WIDTH,
   name : "VizWin",

   thumbnails : true,

   // supply an initial default. Important!
   // default params will be updated in initComponent based on
   // this.heatmapMode & this.havePvalues
   tpl : Gemma.getProfileThumbnailTemplate( true, true ),

   showThumbNails : true,

   heatmapSortMethod : Gemma.sortByImportance,

   stateful : true,
   stateId : "visualization-window",
   stateEvents : [ 'destroy' ],
   heatmapMode : true,
   forceFitPlots : false,
   smoothLineGraphs : false,
   havePvalues : false,
   showLegend : false,
   showSampleNames : false,

   getState : function() {
      return Ext.apply( Ext.Window.superclass.getState.call( this ) || {}, {
         heatmapMode : this.heatmapMode,
         forceFitPlots : this.forceFitPlots,
         smoothLineGraphs : this.smoothLineGraphs,
         showLegend : false,
         showSampleNames : this.showSampleNames
      } );
   },

   /**
    * @memberOf Gemma.VisualizationWithThumbsPanel
    */
   loadcallback : function( records, options, success ) {

      if ( this.loadMask ) {
         this.loadMask.hide();
      }

      if ( !this.hidden ) { // in case window was closed before
         // it finished loading
         if ( !success || records.length === 0 ) {
            // this really should be a listener in the container?
            this.fireEvent( 'loadFailed' );
            Ext.Msg.alert( "No data", "Data could not be displayed", function() {
               // if (this.vizPanel) {
               // this.vizPanel.destroy();
               // }
               // if (this.vizWindow) {
               // this.vizWindow.destroy();
               // }
               // this.destroy();
            }, this );

            return;
         }

         var queryGeneList = options.params[1];
         var returnedGeneCount = this.getReturnedGeneCount( records );

         this.zoom( records[0], this.id );
         this.fireEvent( 'loadSucceeded', returnedGeneCount, queryGeneList.length );
      }

   },
   getReturnedGeneCount : function( records ) {
      var returnedGeneIds = {};
      var returnedGeneCount = 0;
      for ( var i = 0; i < records.length; i++ ) {
         for ( var j = 0; j < records[i].get( 'profiles' ).length; j++ ) {
            for ( var k = 0; k < records[i].get( 'profiles' )[j].genes.length; k++ ) {
               if ( returnedGeneIds[records[i].get( 'profiles' )[j].genes[k].id] === undefined ) {
                  returnedGeneIds[records[i].get( 'profiles' )[j].genes[k].id] = true;
                  returnedGeneCount++;
               }
            }
         }
      }
      return returnedGeneCount;
   },

   setHeatmapMode : function( b ) {
      this.heatmapMode = b;
      if ( this.zoomPanel ) {
         this.zoomPanel.heatmapMode = b;
      }
   },


   zoom : function( record ) {
      if ( !record ) {
         return;
      }
      var eevo = record.get( "eevo" );
      var profiles = record.get( "profiles" );
      var sampleNames = record.get( "sampleNames" );
      var conditionLabels = record.get( "factorValuesToNames" );
      var conditionLabelKey = record.get( "factorNames" );
      this.factorValueLegendPanel.update( conditionLabelKey );
      this.zoomPanel.update( eevo, profiles, sampleNames, conditionLabels, conditionLabelKey );
   },

   toggleForceFit : function( btn ) {
      if ( this.forceFitPlots ) {
         this.forceFitPlots = false;
         this.zoomPanel.forceFitPlots = false;
         // btn.setText("Fit Width");
      } else {
         this.forceFitPlots = true;
         this.zoomPanel.forceFitPlots = true;
         // btn.setText("Expand");
      }

      // force a refresh of the zoom.
      var record = this.dv.getSelectedOrFirst();

      this.zoom( record );
   },

   toggleSampleNames : function( btn ) {

      if ( this.showSampleNames ) {
         this.showSampleNames = false;
         this.zoomPanel.showSampleNames = false;
         btn.setText( "Show sample names" );
      } else {
         this.showSampleNames = true;
         this.zoomPanel.showSampleNames = true;
         btn.setText( "Hide sample names" );
      }

      // force a refresh of the thumbnails.
      var template = Gemma.getProfileThumbnailTemplate( this.heatmapMode, this.havePvalues, /* this.smoothLineGraphs */
         false, Ext.id() );
      this.dv.setTemplate( template );

      // force a refresh of the zoom.
      var record = this.dv.getSelectedOrFirst();

      this.zoom( record );
   },

   toggleLegend : function( btn ) {
      // if (this.heatmapMode) {
      // return;
      // }

      if ( this.showLegend ) {
         this.showLegend = false;
         this.zoomPanel.showLegend = false;
         btn.setText( "Show legend" );
      } else {
         this.showLegend = true;
         this.zoomPanel.showLegend = true;
         btn.setText( "Hide legend" );
      }

      // force a refresh of the thumbnails.
      var template = Gemma.getProfileThumbnailTemplate( this.heatmapMode, this.havePvalues, /* this.smoothLineGraphs */
         false, Ext.id() );
      this.dv.setTemplate( template );

      // force a refresh of the zoom.
      var record = this.dv.getSelectedOrFirst();

      this.zoom( record );
   },

   downloadData : function( btn ) {
      if ( this.downloadLink ) {
         window.open( this.downloadLink );
      }
   },

   updateTemplate : function() {
      var template = Gemma.getProfileThumbnailTemplate( this.heatmapMode, this.havePvalues, /* this.smoothLineGraphs */
         false, this.id );
      this.dv.setTemplate( template ); // causes update of thumbnails.
   },

   switchView : function( btn ) {
      // var smoothBtn = Ext.getCmp(this.smoothBtnId);
      var toggleLegendBtn = this.getBottomToolbar().toggleLegendBtn;
      if ( this.heatmapMode ) {
         this.setHeatmapMode( false );
         btn.setText( "Switch to heatmap" );

         // if (smoothBtn) {
         // smoothBtn.setVisible(true);
         // }

         if ( toggleLegendBtn ) {
            toggleLegendBtn.setVisible( true );
         }

      } else {
         this.setHeatmapMode( true );

         // if (smoothBtn) {
         // smoothBtn.setVisible(false);
         // }

         if ( toggleLegendBtn ) {
            toggleLegendBtn.setVisible( false );
         }

         var zoomLegendDiv = Ext.get( this.zoomLegendId );
         if ( zoomLegendDiv && zoomLegendDiv != null ) {
            zoomLegendDiv.innerHTML = '';
         }
         btn.setText( "Switch to line plots" );
      }

      this.updateTemplate();

      // force a refresh of the zoom.
      var record = this.dv.getSelectedOrFirst();
      this.zoom( record );

   },

   show : function( config ) {
      Gemma.VisualizationWithThumbsPanel.superclass.show.call( this );

      if ( this.prevX !== null ) {
         this.setPosition( this.prevX + 20, this.prevY + 20 );
      }

      var params = config.params || [];

      this.dv.store.load( {
         params : params,
         callback : this.loadcallback.createDelegate( this ),
         scope : this
      } );

   },

   initComponent : function() {

      // update default config with other configs
      // I would remove tpl from default configs altogether, but it seems required

      this.tpl = Gemma.getProfileThumbnailTemplate( this.heatmapMode, this.havePvalues, false, this.id );

      this.zoomLegendId = 'zoomLegend-' + Ext.id();

      this.store = this.store || new Gemma.VisualizationStore( {
         readMethod : this.readMethod
      } );

      this.dv = new Gemma.DataVectorThumbnailsView( {
         tpl : this.tpl,
         store : this.store,
         heatmapSortMethod : this.heatmapSortMethod,
         showPValues : this.havePvalues
      } );

      this.zoomPanel = new Gemma.VisualizationZoomPanel( {
         store : this.store,
         legendDiv : this.zoomLegendId,
         dv : this.dv, // perhaps give store?
         heatmapSortMethod : this.heatmapSortMethod,
         vizPanel : this,
         vizWindow : this.vizWindow
      } );

      this.thumbnailPanel = new Ext.Panel( {
         region : 'west',
         split : true,
         width : Gemma.THUMBNAIL_PLOT_SIZE + 50, // little extra..
         collapsible : true,
         title : "Thumbnails",
         stateful : false,
         margins : '3 0 3 3',
         items : this.dv,
         autoScroll : true,
         zoomPanel : this.zoomPanel,
         legendDiv : this.zoomLegendId,
         hidden : !this.thumbnails,

         html : {
            id : this.zoomLegendId,
            tag : 'div',
            style : 'width:' + (Gemma.THUMBNAIL_PLOT_SIZE + 50) + 'px;height:' + Gemma.THUMBNAIL_PLOT_SIZE
               + 'px; float:left;'
         }
      } );

      this.factorValueLegendPanel = new Ext.Panel( {
         // html: 'factorValueLegend',
         region : 'north',
         collapseMode : 'mini',
         split : true,
         autoScroll : true,
         height : Gemma.LEGEND_PANEL_HEIGHT,

         /**
          * factor value legend panel update
          */
         update : function( conditionLabelKey ) {
            if ( (conditionLabelKey === undefined || conditionLabelKey === null) && this.dv !== undefined ) {
               var record = this.dv.getSelectedOrFirst();

               if ( record === null || record === undefined ) {
                  // can happen at startup.
                  return;
               }

               conditionLabelKey = record.get( "factorNames" );
            }

            this.removeAll();
            var factorCount = 0;
            for ( factorCategory in conditionLabelKey ) {
               factorCount++;
               break;
            }
            if ( factorCount === 0 ) {
               Ext.DomHelper.overwrite( this.body.id, '' );
               this.add( {
                  html : 'No experimental design available.'
               } );
               this.collapse();
            } else {
               if ( this.collapsed ) {
                  this.expand();
               }
               Ext.DomHelper.overwrite( this.body.id, '' );
               FactorValueLegend.draw( Ext.get( this.body.id ), conditionLabelKey );
            }
         }
      } );

      var items = [ {
         xtype : 'panel',
         region : 'center',
         layout : 'border',
         items : [ this.zoomPanel, this.factorValueLegendPanel ]
      }, this.thumbnailPanel ];

      // var items = [this.thumbnailPanel, this.zoomPanel];

      var browserWarning = "";

      // check if canvas is supported (not supported in IE < 9; need to use excanvas in IE8)
      if ( !document.createElement( "canvas" ).getContext && Ext.isIE ) {
         browserWarning = "<span ext:qtip='"
            + Gemma.HelpText.WidgetDefaults.VisualizationWithThumbsPanel.browserWarning
            + "' target='_blank'>Chrome Frame</a></span>";
      }

      Ext.apply( this, {
         items : items,
         bbar : new Ext.Toolbar( {
            items : [ browserWarning, '->', {
               xtype : 'button',
               ref : 'downloadDataBtn',
               // border : true,
               icon : ctxBasePath + '/images/download.gif',
               // iconCls : 'fa fa-download fa-lg', // uses extension.
               // glyph : 'xf0192FontAwesome', // ext 4
               cls : 'x-btn-text-icon',
               hidden : this.downloadLink === undefined,
               disabled : true, // enabled after
               // load.
               tooltip : "Download displayed data in a tab-delimited format",
               handler : this.downloadData.createDelegate( this )
            }, '-', {
               xtype : 'button',
               text : this.showSampleNames ? "Hide sample names" : "Show sample names",
               ref : 'toggleSampleNamesBtn',
               handler : this.toggleSampleNames.createDelegate( this ),
               tooltip : "Toggle display of the sample names",
               disabled : true
            }, '-', {
               xtype : 'button',
               text : this.showLegend ? "Hide legend" : "Show legend",
               ref : 'toggleLegendBtn',
               handler : this.toggleLegend.createDelegate( this ),
               tooltip : "Toggle display of the plot legend",
               disabled : true,
               hidden : this.heatmapMode
            }, '-', {
               xtype : 'button',
               // text : this.forceFitPlots ? "Fit width" : "Expand",
               text : "Zoom +/-",
               ref : 'forceFitBtn',
               handler : this.toggleForceFit.createDelegate( this ),
               tooltip : "Toggle forcing of the plot to fit in the width of the window",
               disabled : true,
               hidden : false
            }, '-', {
               xtype : 'button',
               text : this.heatmapMode ? "Switch to line plot" : "Switch to heatmap",
               ref : 'toggleViewBtn',
               disabled : true,
               handler : this.switchView.createDelegate( this )
            } ]
         } )
      } );

      Gemma.VisualizationWithThumbsPanel.superclass.initComponent.call( this );

      this.dv.getStore().on(
         'load',
         function( s, records, options ) {
            // check in case window was closed before finished loading, will show user error otherwise
            if ( typeof this.getBottomToolbar().toggleViewBtn !== 'undefined' ) {
               this.getBottomToolbar().toggleViewBtn.enable();
               // this.getBottomToolbar().smoothBtn.enable();
               this.getBottomToolbar().forceFitBtn.enable();
               this.getBottomToolbar().toggleLegendBtn.enable();
               this.getBottomToolbar().toggleSampleNamesBtn.enable();
               this.getBottomToolbar().downloadDataBtn.enable();

               // So initial state is sure to be okay, after restore from
               // cookie
               this.getBottomToolbar().toggleViewBtn.setText( this.heatmapMode ? "Switch to line plot"
                  : "Switch to heatmap" );
               // this.getBottomToolbar().forceFitBtn.setText(this.forceFitPlots ? "Fit width" : "Expand");
               this.getBottomToolbar().toggleLegendBtn.setText( this.showLegend ? "Hide legend" : "Show legend" );
               this.getBottomToolbar().toggleSampleNamesBtn.setText( this.showSampleNames ? "Hide sample names"
                  : "Show sample names" );
               // Ext.getCmp(this.smoothBtnId).setText(this.smoothLineGraphs
               // ? "Unsmooth" : "Smooth");
               if ( this.heatmapMode ) {
                  this.getBottomToolbar().toggleLegendBtn.hide();
                  // Ext.getCmp(this.smoothBtnId).hide();
                  // Ext.getCmp(this.toggleLegendBtnId).hide();
               } else {
                  this.getBottomToolbar().toggleLegendBtn.show();
                  // Ext.getCmp(this.smoothBtnId).show();
                  // Ext.getCmp(this.toggleLegendBtnId).show();
               }
            }

         }, this );

      this.dv.getStore().on( 'loadexception', function( e ) {
         Ext.Msg.alert( "No data", "Sorry, no data were available:" + e, function() {
            this.close();
         }.createDelegate( this ), this );
      }.createDelegate( this ), this );

      this.dv.on( 'selectionchange', function( dv, selections ) {
         if ( selections.length > 0 ) {
            var record = dv.getRecord( selections[0] );
            if ( !record || record === undefined ) {
               return;
            }
            this.zoom( record );
         }
      }.createDelegate( this ), this );

      this.on( 'staterestore', function( w, state ) {
         this.zoomPanel.heatmapMode = this.heatmapMode;
         this.zoomPanel.forceFitPlots = this.forceFitPlots;
         // this.zoomPanel.smoothLineGraphs = this.smoothLineGraphs;
         this.zoomPanel.showLegend = this.showLegend;
         this.zoomPanel.showSampleNames = this.showSampleNames;
         this.updateTemplate();
      }, this );

      /*
       * Tell thumbnails where to put the legend. Currently it's in the body of the graph.
       */
      // this.on('show', function(cmp) {
      // cmp.zoomLegendId = cmp.getBottomToolbar().id;
      // cmp.zoomPanel.legendDiv = cmp.zoomLegendId;
      // cmp.thumbnailPanel.legendDiv = cmp.zoomLegendId;
      // }.createDelegate(this), this);
      // this.loadFromParam();
   },
   loadFromParam : function( config ) {

      this.loadMask = new Ext.LoadMask( this.getEl(), {
         msg : Gemma.StatusText.Loading.generic,
         msgCls : 'absolute-position-loading-mask ext-el-mask-msg x-mask-loading'
      } );
      this.loadMask.show();
      var params = config.params || this.params || [];

      this.dv.store.load( {
         params : params,
         callback : this.loadcallback.createDelegate( this ),
         scope : this
      } );
   }

} );

/**
 * Specialization to show differentially expressed genes.
 */
Gemma.VisualizationDifferentialWindow = Ext.extend( Gemma.VisualizationWithThumbsWindow, {
   extraPanelParams : {
      havePvalues : true,
      tpl : Gemma.getProfileThumbnailTemplate( false, true ),
      readMethod : DEDVController.getDEDVForDiffExVisualization
   }
} );

/**
 * Specialization for coexpression display
 *
 * @class Gemma.CoexpressionVisualizationWindow
 * @extends Gemma.VisualizationWithThumbsWindow
 */
Gemma.CoexpressionVisualizationWindow = Ext.extend( Gemma.VisualizationWithThumbsWindow, {
   extraPanelParams : {
      tpl : Gemma.getProfileThumbnailTemplate( false, false ),
      readMethod : DEDVController.getDEDVForCoexpressionVisualization
   }
} );

// ////////////////////////////////////////////////////////////////////////////////////////

/**
 * Represents a VisualizationValueObject.
 *
 * @extends Ext.data.Store
 */
Gemma.VisualizationStore = function( config ) {

   this.record = Ext.data.Record.create( [ {
      name : "id",
      type : "int"
   }, {
      // EE value object
      name : "eevo"
   }, {
      // GeneExpressionProfile value object.
      name : "profiles"
   }, {
      // not used yet.
      name : "factorProfiles"
   }, {
      name : "sampleNames"
   }, {
      name : "factorNames"
   }, {
      name : "factorValues"
   }, {
      name : "factorValuesToNames"
   } ] );

   if ( config && config.readMethod ) {
      this.readMethod = config.readMethod;
   } else {
      this.readMethod = DEDVController.getDEDVForVisualization; // takes
      // eeids,geneids.
   }

   this.proxy = new Ext.data.DWRProxy( this.readMethod );

   this.reader = new Ext.data.ListRangeReader( {
      id : "id"
   }, this.record );

   Gemma.VisualizationStore.superclass.constructor.call( this, config );

   this.relayEvents( this.proxy, [ 'loadexception' ] );

};

/**
 *
 * @class Gemma.VisualizationStore
 * @extends Ext.data.Store
 */

Ext.extend( Gemma.VisualizationStore, Ext.data.Store, {} );

// //////////////////////////////////////////////////////////////////////////////////

// Utility functions for visualization

/**
 * Sort in this order: 1. Query probes that show coexpression or sig. diff ex., ordered by pvalue 2. Target probes that
 * show coexp or sig diff ex. 3. Query probes that do not show coexpression or sig. diff ex. (faded) 4. Target probes
 * that do not show coexp or sig diff ex.
 *
 * @param {}
 *           a
 * @param {}
 *           b
 * @return {Number}
 */
Gemma.sortByImportance = function( a, b ) {

   // first level sort: by pvalue, if present, or by rank, if present.
   if ( a.PValue !== null && a.PValue !== undefined && b.PValue !== null && b.PValue !== undefined ) {
      return Math.log( a.PValue ) - Math.log( b.PValue ); // log to avoid roundoff trouble.
   } else if ( a.rank !== null && a.rank !== undefined && b.rank !== null && b.rank !== undefined ) {
      return a.rank - b.rank;
   }

   // Second level sort: by factor > 1 means 'involved in sig. coexpression' or
   // "query gene".
   if ( a.factor > b.factor ) {
      return -1;
   }
   if ( a.factor < b.factor ) {
      return 1;
   }

   if ( (!a.genes || a.genes.length < 1) && (!b.genes || b.genes.length < 1) ) {
      return a.labelID > b.labelID ? -1 : 1;
   }

   if ( !a.genes || a.genes.length < 1 ) {
      return -1;
   }
   if ( !b.genes || b.genes.length < 1 ) {
      return 1;
   }

   if ( a.genes[0].name > b.genes[0].name ) {
      return 1;
   } else if ( a.genes[0].name < b.genes[0].name ) {
      return -1;
   } else {
      return a.labelID > b.labelID ? -1 : 1;
   }

};

var FactorValueLegend = (function() {

   // column labels
   var PER_CONDITION_LABEL_HEIGHT = 10;

   // condition key
   var FACTOR_VALUE_LABEL_MAX_CHAR = 125;
   var FACTOR_VALUE_LABEL_BOX_WIDTH = 10;

   // extra space
   var TRIM = 5;
   var SMALL_TRIM = 3;

   function FactorValueLegend( target, conditionLabelKey ) {

      drawLegend( target, conditionLabelKey );

      function drawLegend( target, conditionLabelKey ) {

         var labelHeight = 10;

         var increment = 1;
         var boxWidth = 10;
         var boxHeight = 10;
         var fontSize = 10;
         var lineHeight = 10;
         var legendWidth = 300; // to start
         var legendHeight = 100; // to start

         var id = 'factorValueLegend-' + Ext.id();

         Ext.DomHelper.append( target, {
            id : id,
            tag : 'div'
            // ,
            // width: legendWidth,
            // height: legendHeight
         } );
         var legendDiv = Ext.get( id );

         var ctx = constructCanvas( Ext.get( legendDiv ), legendWidth, legendHeight );

         // draw legend for experimental design / condition bar chart

         var factorValueCount = 0;
         var factorCount = 0;
         var allColumnsWidth = 0;
         var maxColumnWidth = 0;
         // var ctxDummy = this.el.dom.getContext("2d");
         // CanvasTextFunctions.enable(ctxDummy);
         // ctxDummy.font = fontSize + "px sans-serif";
         for ( var factorCategory in conditionLabelKey ) {
            factorCount++;
            // compute the room needed for the labels.
            if ( ctx.measureText( factorCategory ).width > maxColumnWidth ) {
               maxColumnWidth = ctx.measureText( factorCategory ).width;
            }
            for ( var factorValue in conditionLabelKey[factorCategory] ) {
               factorValueCount++;
               // compute the room needed for the labels.
               var dim = ctx.measureText( factorValue );
               var width = Math.round( dim.width );
               if ( width > maxColumnWidth ) {
                  maxColumnWidth = width;
               }
            }
            allColumnsWidth += maxColumnWidth;
            maxColumnWidth = 0;
         }
         // calculate actual width
         legendWidth = 3 * TRIM + allColumnsWidth + (FACTOR_VALUE_LABEL_BOX_WIDTH + SMALL_TRIM + SMALL_TRIM)
            * factorCount;
         legendHeight = 3 * TRIM + lineHeight * (factorValueCount + 1);

         Ext.DomHelper.overwrite( Ext.get( legendDiv ), '' );
         ctx = constructCanvas( Ext.get( legendDiv ), legendWidth, legendHeight );

         ctx.fillStyle = "#000000";
         ctx.font = fontSize + "px sans-serif";
         ctx.textAlign = "left";
         ctx.translate( 10, 20 );
         x = 0;
         y = 0;
         for ( var factorCategory in conditionLabelKey ) {
            facCat = Ext.util.Format.ellipsis( factorCategory, FACTOR_VALUE_LABEL_MAX_CHAR );
            var maxLabelWidthInCategory = 0;
            dim = ctx.measureText( facCat );
            width = Math.round( dim.width );
            if ( width > maxLabelWidthInCategory ) {
               maxLabelWidthInCategory = width;
            }
            ctx.fillText( facCat, x, y );
            y += PER_CONDITION_LABEL_HEIGHT + 2;
            for ( var factorValue in conditionLabelKey[factorCategory] ) {
               var facVal = Ext.util.Format.ellipsis( factorValue, FACTOR_VALUE_LABEL_MAX_CHAR );
               colour = conditionLabelKey[factorCategory][factorValue];
               ctx.fillStyle = colour;
               ctx.fillRect( x, y - fontSize, FACTOR_VALUE_LABEL_BOX_WIDTH, PER_CONDITION_LABEL_HEIGHT );
               x += FACTOR_VALUE_LABEL_BOX_WIDTH + SMALL_TRIM;
               ctx.fillStyle = "#000000";
               ctx.fillText( facVal, x, y );
               x = 0;
               y += PER_CONDITION_LABEL_HEIGHT;
               dim = ctx.measureText( facVal );
               width = Math.round( dim.width );
               if ( width > maxLabelWidthInCategory ) {
                  maxLabelWidthInCategory = width;
               }
            }
            ctx.translate( FACTOR_VALUE_LABEL_BOX_WIDTH + SMALL_TRIM + maxLabelWidthInCategory + SMALL_TRIM, 0 );
            x = 0;
            y = 0;
         }

      }

      /**
       * Function: (private) constructCanvas
       *
       * Initializes a canvas. When the browser is IE, we make use of excanvas.
       *
       * Parameters: none
       *
       * Returns: ctx
       */
      function constructCanvas( div, canvasWidth, canvasHeight ) {

         /**
          * For positioning labels and overlay.
          */
         div.setStyle( {
            'position' : 'relative'
         } );

         if ( canvasWidth <= 0 || canvasHeight <= 0 ) {
            throw 'Invalid dimensions for plot, width = ' + canvasWidth + ', height = ' + canvasHeight;
         }

         var canvas = Ext.DomHelper.append( div, {
            tag : 'canvas',
            width : canvasWidth,
            height : canvasHeight
         } );

         // check if canvas is supported (not supported in IE < 9; need to use excanvas in IE8)
         if ( !document.createElement( "canvas" ).getContext && Prototype.Browser.IE ) {
            canvas = Ext.get( window.G_vmlCanvasManager.initElement( canvas ) );
         }

         return canvas.getContext( '2d' );
      }

   }

   return {
      clean : function( element ) {
         if ( element && element != null && element.inner ) {
            element.innerHTML = '';
         }

      },

      draw : function( target, conditionLabelKey ) {
         var legend = new FactorValueLegend( target, conditionLabelKey );
         return legend;
      }
   };
}());

/*
 * Handy test data.
 */
Gemma.testVisData = function() {
   var s0 = {};
   var s1 = {};
   var s2 = [];
   var s4 = {};
   var s6 = [];
   var s9 = {};
   var s7 = {};
   var s8 = [];
   var s5 = {};
   var s10 = [];
   var s13 = {};
   var s11 = {};
   var s12 = [];
   var s3 = [];
   s0.eevo = s1;
   s0.profiles = s2;
   s0.sampleNames = s3;
   s1.clazz = "ExpressionExperimentValueObject";
   s1.id = 1567;
   s1.investigators = null;
   s1.isPublic = true;
   s1.isShared = false;
   s1.linkAnalysisEventType = null;
   s1.minPvalue = 1.12992044357193E-29;
   s1.missingValueAnalysisEventType = null;
   s1.name = "High fat diet leads to increased storage of mono-unsaturated fatty acids and tissue specific risk factors for disease";
   s1.numAnnotations = null;
   s1.numPopulatedFactors = null;
   s1.owner = null;
   s1.processedDataVectorComputationEventType = null;
   s1.processedExpressionVectorCount = null;
   s1['public'] = true;
   s1.shortName = "GSE15822";

   // data vectors
   s2[0] = s4;
   s2[1] = s5;

   // first data vector
   s4.PValue = 1.03114379442728E-24;
   s4.allMissing = false;
   s4.color = "red";
   s4.factor = 2;
   s4.genes = s6;
   s4.probe = s7;
   s4.profile = s8;
   s4.standardized = true;
   s6[0] = s9;
   s9.description = "Imported from NCBI gene; Nomenclature status: INTERIM";
   s9.id = 590525;
   s9.name = "Wdr1";
   s9.ncbiId = "22388";
   s9.officialName = "WD repeat domain 1";
   s9.officialSymbol = "Wdr1";
   s9.score = null;
   s9.taxonId = 2;
   s9.taxonName = "Mus musculus";
   s7.arrayDesign = null;
   s7.description = " Wdr1";
   s7.id = 6346520;
   s7.name = "ILMN_2460168";
   s8[0] = -1.5727;
   s8[1] = -1.0453;
   s8[2] = -1.4501;
   s8[3] = -1.2332;
   s8[4] = -0.7847;
   s8[5] = -1.7017;
   s8[6] = -1.4386;
   s8[7] = -1.0650;
   s8[8] = -1.3970;
   s8[9] = -0.8819;

   // another data vector
   s5.PValue = 1.12992044357193E-29;
   s5.allMissing = false;
   s5.color = "red";
   s5.factor = 2;
   s5.genes = s10;
   s5.probe = s11;
   s5.profile = s12;
   s5.standardized = true;
   s10[0] = s13;
   s13.description = "Imported from NCBI gene; Nomenclature status: INTERIM";
   s13.id = 590525;
   s13.name = "Wdr1";
   s13.ncbiId = "22388";
   s13.officialName = "WD repeat domain 1";
   s13.officialSymbol = "Wdr1";
   s13.score = null;
   s13.taxonId = 2;
   s13.taxonName = "Mus musculus";
   s11.arrayDesign = null;
   s11.description = " Wdr1";
   s11.id = 6345825;
   s11.name = "ILMN_2497268";

   s12[0] = -1.588;
   s12[1] = NaN;
   s12[2] = -0.656;
   s12[3] = NaN;
   s12[4] = -1.370;
   s12[5] = -1.417;
   s12[6] = -1.035;
   s12[7] = NaN;
   s12[8] = NaN;
   s12[9] = -1.044;

   // s12[0] = NaN;
   // s12[1] = NaN;
   // s12[2] = NaN;
   // s12[3] = NaN;
   // s12[4] = NaN;
   // s12[5] = NaN;
   // s12[6] = NaN;
   // s12[7] = NaN;
   // s12[8] = NaN;
   // s12[9] = NaN;

   // orignal , unmessedup
   // s12[0] = -1.588;
   // s12[1] = -0.894;
   // s12[2] = -0.656;
   // s12[3] = -1.402;
   // s12[4] = -1.370;
   // s12[5] = -1.417;
   // s12[6] = -1.035;
   // s12[7] = -1.133;
   // s12[8] = -0.734;
   // s12[9] = -1.044;

   // sample names
   s3[0] = "Muscle.HFD.6";
   s3[1] = "Muscle.HFD.5";
   s3[2] = "Muscle.HFD.4";
   s3[3] = "Muscle.HFD.3";
   s3[4] = "Muscle.HFD.2";
   s3[5] = "Muscle.HFD.1";
   s3[6] = "Muscle.SBD.6";
   s3[7] = "Muscle.SBD.5";
   s3[8] = "Muscle.SBD.4";
   s3[9] = "Muscle.SBD.3";

   return [ s0 ];
};
