Ext.namespace( 'Gemma.Metaheatmap' );
/**
 * Summary
 * 
 * 
 * Public methods: + draw() + redraw() + + +
 * 
 * Configuration: + + + +
 * 
 * Events: + +
 */
Gemma.Metaheatmap.VisualizationPanel = Ext
   .extend(
      Ext.Panel,
      {
         // need to use hbox so the heatmap box resizes when the control panel is collapsed
         layout : 'hbox',
         layoutConfig : {
            align : 'stretch',
            pack : 'start'
         },
         frame : false,
         border : false,
         defaults : {
            border : false
         },

         /**
          * @memberOf Gemma.Metaheatmap.VisualizationPanel
          */
         initComponent : function() {

            Ext
               .apply(
                  this,
                  {
                     isLegendShown : false,
                     geneTree : this.geneTree,
                     conditionTree : this.conditionTree,
                     cells : this.cells,
                     items : [
                              {
                                 xtype : 'Metaheatmap.HoverWindow',
                                 ref : 'hoverWindow'
                              },
                              {
                                 xtype : 'panel',
                                 name : 'fixedWidthCol',
                                 ref : 'fixedWidthCol',
                                 flex : 0,
                                 layout : 'vbox',
                                 layoutConfig : {
                                    align : 'stretch',
                                    pack : 'start'
                                 },
                                 defaults : {
                                    border : false
                                 },
                                 items : [
                                          {
                                             xtype : 'panel',
                                             ref : 'pnlControlAndLabels',
                                             layout : 'border',
                                             autoScroll : true,
                                             border : false,
                                             items : [
                                                      {
                                                         ref : 'pnlMiniControl',
                                                         xtype : 'panel',
                                                         region : 'center',
                                                         defaults : {
                                                            style : 'margin-left:10px;margin-top:5px;'
                                                         },
                                                         border : false,
                                                         width : 85,
                                                         height : 100,
                                                         items : [
                                                                  {
                                                                     xtype : 'label',
                                                                     text : 'Column zoom'
                                                                  },
                                                                  {
                                                                     xtype : 'slider',
                                                                     ref : 'sldHorizontalZoom',
                                                                     width : 80,
                                                                     height : 20,
                                                                     value : Gemma.Metaheatmap.defaultConditionZoom,
                                                                     increment : 1,
                                                                     minValue : 2,
                                                                     maxValue : 15,
                                                                     listeners : {
                                                                        changecomplete : function( slider, newValue,
                                                                           thumb ) {
                                                                           this.redraw();
                                                                        },
                                                                        scope : this
                                                                     }
                                                                  },
                                                                  {
                                                                     xtype : 'label',
                                                                     text : 'Row zoom'
                                                                  },
                                                                  {
                                                                     xtype : 'slider',
                                                                     ref : 'sldVerticalZoom',
                                                                     width : 80,
                                                                     height : 20,
                                                                     value : Gemma.Metaheatmap.defaultGeneZoom,
                                                                     increment : 1,
                                                                     minValue : 2,
                                                                     maxValue : 15,
                                                                     listeners : {
                                                                        changecomplete : function( slider, newValue,
                                                                           thumb ) {
                                                                           this.redraw();
                                                                        },
                                                                        scope : this
                                                                     }
                                                                  },
                                                                  {
                                                                     xtype : 'button',
                                                                     text : 'Dock popup',
                                                                     disabled : false,
                                                                     hidden : false,
                                                                     enableToggle : true,
                                                                     width : 95,
                                                                     toggleHandler : function( btn, toggle ) {
                                                                        if ( toggle ) {
                                                                           this.hoverWindow.isDocked = true;
                                                                           this.hoverWindow
                                                                              .setTitle( 'Docked popup: click & drag to move' );
                                                                           btn.setText( "Undock popup" );
                                                                        } else {
                                                                           this.hoverWindow.isDocked = false;
                                                                           this.hoverWindow.setTitle( '' );
                                                                           btn.setText( "Dock popup" );
                                                                        }
                                                                     },
                                                                     scope : this
                                                                  },
                                                                  {
                                                                     xtype : 'button',
                                                                     ref : 'showFoldChangeToggle',
                                                                     text : 'Hide q-value',
                                                                     width : 95,
                                                                     y : 100,
                                                                     handler : function( btn, e ) {
                                                                        this.variableWidthCol.boxHeatmap.isShowPvalue = !this.variableWidthCol.boxHeatmap.isShowPvalue;

                                                                        if ( this.variableWidthCol.boxHeatmap.isShowPvalue ) {
                                                                           btn.setText( "Hide q-value" );
                                                                        } else {
                                                                           btn.setText( "Show q-value" );
                                                                        }
                                                                        this.variableWidthCol.boxHeatmap.draw();
                                                                     },
                                                                     scope : this
                                                                  }, {
                                                                     xtype : 'button',
                                                                     text : 'Flip axes',
                                                                     disabled : true,
                                                                     hidden : true,
                                                                     width : 80,
                                                                     handler : function( btn, e ) {
                                                                        this.flipLabels();
                                                                        this.redraw();
                                                                     },
                                                                     scope : this
                                                                  } ]
                                                      },

                                                      {
                                                         autoEl : 'canvas',
                                                         ref : 'metaValuesSideLabels',
                                                         xtype : 'box',
                                                         region : 'east',
                                                         border : false,
                                                         width : 15
                                                      }, {
                                                         autoEl : 'canvas',
                                                         xtype : 'box',
                                                         region : 'south',
                                                         ref : 'metaValuesTopLabels',
                                                         border : false,
                                                         height : 15
                                                      } ]
                                          }, {
                                             xtype : 'Metaheatmap.LabelBox',
                                             ref : 'boxSideLabels',
                                             border : false,
                                             orientation : 'vertical',
                                             tree : this.geneTree,
                                             drawItemLabel_ : this.drawGeneLabel,
                                             onClick : this.onClickGeneLabel
                                          } ]
                              },
                              {
                                 xtype : 'panel',
                                 name : 'variableWidthCol',
                                 ref : 'variableWidthCol',
                                 flex : 1,
                                 layout : 'vbox',
                                 layoutConfig : {
                                    align : 'stretch',
                                    pack : 'start'
                                 },
                                 defaults : {
                                    border : false
                                 },
                                 items : [
                                          {
                                             xtype : 'Metaheatmap.LabelBox',
                                             ref : 'boxTopLabels',
                                             border : false,
                                             orientation : 'horizontal',
                                             tree : this.conditionTree,
                                             drawItemLabel_ : this.drawConditionLabel,
                                             onClick : this.onClickConditionLabel
                                          },
                                          {
                                             xtype : 'Metaheatmap.HeatmapBox',
                                             ref : 'boxHeatmap',
                                             border : false,
                                             geneTree : this.geneTree,
                                             conditionTree : this.conditionTree,
                                             cells : this.cells
                                          },
                                          {
                                             xtype : 'Metaheatmap.ColorLegend',
                                             ref : 'colorLegend',
                                             x : 200,
                                             y : 0,
                                             title : 'Fold change & q-value',
                                             cellSize : 14,
                                             isShown : false,

                                             foldChangeLabels : [ "Down, >2 fold", "Down, <2 fold", "Up, <2 fold",
                                                                 "Up, >2 fold" ],
                                             foldChangeValues : [ -3, -1, 1, 3 ],

                                             discreteColorRangeObject : Gemma.Metaheatmap.Config.contrastsColourRange,

                                             pValueLabels : [ "No Data", "Not Significant", "0.05~0.01", "0.01~0.005",
                                                             "< 0.005" ],
                                             pValueValues : [ 0, 0, 4, 7, 10 ],

                                             fontSize : 12
                                          } ]
                              } ]
                  } );

            Gemma.Metaheatmap.VisualizationPanel.superclass.initComponent.apply( this, arguments );

         },

         setGeneTree : function( tree ) {
            this.geneTree = tree;
            this.variableWidthCol.boxHeatmap.geneTree = tree;
            if ( this.isGeneOnTop ) {
               this.variableWidthCol.boxTopLabels.tree = tree;
            } else {
               this.fixedWidthCol.boxSideLabels.tree = tree;
            }
         },

         setConditionTree : function( tree ) {
            this.conditionTree = tree;
            this.variableWidthCol.boxHeatmap.conditionTree = tree;
            if ( this.isGeneOnTop ) {
               this.fixedWidthCol.boxSideLabels.tree = tree;
            } else {
               this.variableWidthCol.boxTopLabels.tree = tree;
            }
         },

         flipLabels : function() {
            if ( !this.variableWidthCol.boxHeatmap.isGeneOnTop ) {
               this.variableWidthCol.boxHeatmap.isGeneOnTop = true;

               this.fixedWidthCol.boxSideLabels.tree = this.conditionTree;
               this.fixedWidthCol.boxSideLabels.drawItemLabel_ = this.drawConditionLabel;
               this.fixedWidthCol.boxSideLabels.onClick = this.onClickConditionLabel;

               this.variableWidthCol.boxTopLabels.tree = this.geneTree;
               this.variableWidthCol.boxTopLabels.drawItemLabel_ = this.drawGeneLabel;
               this.variableWidthCol.boxTopLabels.onClick = this.onClickGeneLabel;

            } else {
               this.variableWidthCol.boxHeatmap.isGeneOnTop = false;

               this.fixedWidthCol.boxSideLabels.tree = this.geneTree;
               this.fixedWidthCol.boxSideLabels.drawItemLabel_ = this.drawGeneLabel;
               this.fixedWidthCol.boxSideLabels.onClick = this.onClickGeneLabel;

               this.variableWidthCol.boxTopLabels.tree = this.conditionTree;
               this.variableWidthCol.boxTopLabels.drawItemLabel_ = this.drawConditionLabel;
               this.variableWidthCol.boxTopLabels.onClick = this.onClickConditionLabel;

            }
            // update size of top left control panel so that gene labels line up with data rows
            this.updatePnlMiniControlSize();
         },

         /**
          * update size of top left control panel so that side labels line up with data rows
          * 
          * @private
          */
         updatePnlMiniControlSize : function() {
            // Update size of top left control panel so that gene labels line up with data rows.
            var topPadding = 0;
            // Need "Math.max(140, ...)" so that panel doesn't disappear when all columns are filtered out
            this.fixedWidthCol.pnlControlAndLabels.setHeight( Math.max( 140, this.variableWidthCol.boxTopLabels
               .getHeight() )
               + topPadding );

            // Redraw meta p-value and enrichment labels.
            var metaPvalueLabel = this.fixedWidthCol.pnlControlAndLabels.metaValuesSideLabels;
            var enrichmentLabel = this.fixedWidthCol.pnlControlAndLabels.metaValuesTopLabels;

            metaPvalueLabel.ctx = Gemma.Metaheatmap.Utils.getCanvasContext( metaPvalueLabel.el.dom );
            CanvasTextFunctions.enable( metaPvalueLabel.ctx );

            enrichmentLabel.ctx = Gemma.Metaheatmap.Utils.getCanvasContext( enrichmentLabel.el.dom );
            CanvasTextFunctions.enable( enrichmentLabel.ctx );

            metaPvalueLabel.ctx.canvas.width = metaPvalueLabel.getWidth();
            metaPvalueLabel.ctx.canvas.height = metaPvalueLabel.getHeight();
            metaPvalueLabel.ctx.clearRect( 0, 0, metaPvalueLabel.ctx.canvas.width, metaPvalueLabel.ctx.canvas.height );

            enrichmentLabel.ctx.canvas.width = enrichmentLabel.getWidth();
            enrichmentLabel.ctx.canvas.height = enrichmentLabel.getHeight();
            enrichmentLabel.ctx.clearRect( 0, 0, enrichmentLabel.ctx.canvas.width, enrichmentLabel.ctx.canvas.height );

            var x = enrichmentLabel.getWidth() - 93;
            var y = 10;
            enrichmentLabel.ctx.strokeStyle = 'black';
            enrichmentLabel.ctx.drawText( '', 12, x, y, "enrichment" );

            x = 10;
            y = metaPvalueLabel.getHeight() - 3;
            metaPvalueLabel.ctx.strokeStyle = 'black';
            metaPvalueLabel.ctx.drawRotatedText( x, y, 270, 12, 'black', "meta p-value" );

         },

         /**
          * @public
          */
         redraw : function( wasHeatmapChanged ) {
            // Hide hover window to prevent it being integrated into the layout.
            this.hoverWindow.hide();
            this.variableWidthCol.colorLegend.hide();

            if ( wasHeatmapChanged ) {
               this.updateVisibleScores();
            }

            if ( this.variableWidthCol.boxHeatmap.isGeneOnTop ) {
               this.conditionTree.applyZoom( this.fixedWidthCol.pnlControlAndLabels.pnlMiniControl.sldVerticalZoom
                  .getValue() );
               this.geneTree.applyZoom( this.fixedWidthCol.pnlControlAndLabels.pnlMiniControl.sldHorizontalZoom
                  .getValue() );
            } else {
               this.conditionTree.applyZoom( this.fixedWidthCol.pnlControlAndLabels.pnlMiniControl.sldHorizontalZoom
                  .getValue() );
               this.geneTree.applyZoom( this.fixedWidthCol.pnlControlAndLabels.pnlMiniControl.sldVerticalZoom
                  .getValue() );
            }

            this.fixedWidthCol.boxSideLabels.resizeAndPosition();
            this.variableWidthCol.boxTopLabels.resizeAndPosition();
            this.variableWidthCol.boxHeatmap.resizeAndPosition();

            // Update size of top left control panel so that gene labels line up with data rows
            this.updatePnlMiniControlSize();

            this.doLayout();

            this.draw();

            if ( this.hoverWindow.isDocked ) {
               this.hoverWindow.show();
            }
            if ( this.variableWidthCol.colorLegend.isShown ) {
               this.variableWidthCol.colorLegend.show();
            }
         },

         /**
          * @public
          */
         draw : function() {
            this.variableWidthCol.boxHeatmap.draw();
            this.fixedWidthCol.boxSideLabels.draw();
            this.variableWidthCol.boxTopLabels.draw();
         },

         /**
          * @private
          * @param label
          * @param e
          */
         onClickGeneLabel : function( label, e ) {
            // If user held down ctrl while clicking, select column or gene instead
            // of popping up window.
            if ( e.ctrlKey === true ) {
               if ( label.item.isSelected ) {
                  label.item.isSelected = false;
               } else {
                  label.item.isSelected = true;
               }
               this.draw();
            } else {
               Gemma.MetaVisualizationPopups.makeGeneInfoWindow( label.item.name, label.item.id );
            }
         },

         /**
          * @private
          * @param label
          * @param e
          */
         onClickConditionLabel : function( label, e ) {
            // If user held down ctrl while clicking, select column or gene instead
            // of popping up window.
            if ( e.ctrlKey === true ) {
               if ( label.item.isSelected ) {
                  label.item.isSelected = false;
               } else {
                  label.item.isSelected = true;
               }
               this.draw();
            } else {
               Gemma.MetaVisualizationPopups.makeDatasetInfoWindow( label.item.datasetName,
                  label.item.datasetShortName, label.item.datasetId );
            }
         },

         /**
          * 
          */
         downloadImage : function() {
            var ctxMain = this.variableWidthCol.boxHeatmap.ctx;
            var ctxSide = this.fixedWidthCol.boxSideLabels.ctx;
            var ctxTop = this.variableWidthCol.boxTopLabels.ctx;

            // create temporary canvas element
            var canvas = document.createElement( 'canvas' );
            canvas.width = ctxMain.canvas.width + 1 + ctxSide.canvas.width;
            canvas.height = ctxMain.canvas.height + 1 + ctxTop.canvas.height;
            var ctx = canvas.getContext( '2d' );
            ctx.fillStyle = "white";
            ctx.fillRect( 0, 0, canvas.width, canvas.height );

            var topImage = ctxTop.getImageData( 0, 0, ctxTop.canvas.width, ctxTop.canvas.height );
            var sideImage = ctxSide.getImageData( 0, 0, ctxSide.canvas.width, ctxSide.canvas.height );
            var mainImage = ctxMain.getImageData( 0, 0, ctxMain.canvas.width, ctxMain.canvas.height );

            ctx.drawImage( ctxTop.canvas, ctxSide.canvas.width + 1, 0 );
            ctx.drawImage( ctxSide.canvas, 0, ctxTop.canvas.height + 1 );
            ctx.drawImage( ctxMain.canvas, ctxSide.canvas.width + 1, ctxTop.canvas.height + 1 );

            var image_url = canvas.toDataURL( "image/png" );
            var win = new Ext.Window( {
               title : 'Right-click the image and save the image as file.',
               html : '<img src="' + image_url + '" />',
               height : 700,
               widht : 900
            } );
            win.show();
         },

         /**
          * 
          * @param gene
          * @param size
          * @param orientation
          * @param backgroundColor
          */
         drawGeneLabel : function( gene, size, orientation, backgroundColor ) {
            gene.miniPieValue = null;

            var maxSize = {
               'width' : this.getWidth(),
               'height' : this.getHeight()
            };

            gene.display.label = {};
            gene.display.label.drawFn = Gemma.Metaheatmap.ConditionLabel.constructDrawLabelFunction( this.ctx, gene,
               gene.name, orientation, size, maxSize, backgroundColor );
            gene.display.label.drawFn( false );
         },

         /**
          * 
          * @param condition
          * @param size
          * @param orientation
          * @param backgroundColor
          */
         drawConditionLabel : function( condition, size, orientation, backgroundColor ) {

            condition.miniPieValue = (condition.numberOfProbesOnArray === 0) ? -1 : 360.0
               * condition.numberDiffExpressedProbes / condition.numberOfProbesOnArray;
            condition.miniBarValue = (condition.numberOfProbesOnArray === 0) ? 0
               : 9.0 * (condition.numberDiffExpressedProbes / condition.numberOfProbesOnArray);

            var maxSize = {
               'width' : this.getWidth(),
               'height' : this.getHeight()
            };

            condition.display.label = {};
            condition.display.label.drawFn = Gemma.Metaheatmap.ConditionLabel.constructDrawLabelFunction( this.ctx,
               condition, condition.contrastFactorValue, orientation, size, maxSize, backgroundColor );
            condition.display.label.drawFn( false );
         },

         /**
          * Some gene/condition scores depend on what is currently visible: % of missing values, inverse of p values sum
          * Other scores are not affected by it: specificity
          */
         updateVisibleScores : function() {
            var i, j;
            // Calculate visible scores for conditons
            for (i = 0; i < this.conditionTree.items.length; i++) {
               var condition = this.conditionTree.items[i];
               condition.experimentSpecificity = condition.numberDiffExpressedProbes / condition.numberOfProbesOnArray;
               var numProbesMissing = 0;

               var numProbesInSet = 0;
               var numProbesOverThresholdInSet = 0;

               var numGenesInSet = 0;
               var numGenesOverThresholdInSet = 0;

               for (j = 0; j < this.geneTree.items.length; j++) {
                  var gene = this.geneTree.items[j];
                  var cell = this.cells.getCell( gene, condition );
                  if ( cell !== null ) {
                     if ( cell.isProbeMissing ) {
                        numProbesMissing++;
                     } else {
                        if ( cell.correctedPValue !== null ) {
                           numGenesInSet++;
                           numProbesInSet += cell.numberOfProbes;
                           if ( cell.correctedPValue < Gemma.Constants.DifferentialExpressionQvalueThreshold ) {
                              numGenesOverThresholdInSet++;
                              numProbesOverThresholdInSet += cell.numberOfProbesDiffExpressed;
                           }
                        }
                     }
                  }
               }

               if ( Gemma.Metaheatmap.Config.USE_GENE_COUNTS_FOR_ENRICHMENT ) {

                  if ( condition.numberOfGenesTested < numGenesInSet ) {
                     // this can happen if the data in the system is missing or corrupt.
                     condition.ora = null;
                  } else {
                     condition.ora = GemmaStatUtils.computeOraPvalue( condition.numberOfGenesTested, numGenesInSet,
                        numGenesOverThresholdInSet, condition.numberOfGenesDiffExpressed );
                  }

                  condition.numInSet = numGenesInSet;
                  condition.numDiffExpressed = numGenesOverThresholdInSet;
               } else {
                  condition.ora = GemmaStatUtils.computeOraPvalue( condition.numberOfProbesOnArray, numProbesInSet,
                     numProbesOverThresholdInSet, condition.numberDiffExpressedProbes );
                  condition.numInSet = numProbesInSet;
                  condition.numDiffExpressed = numProbesOverThresholdInSet;
               }

               condition.oraDisplayValue = this.calculateBarChartValueBasedOnPvalue( condition.ora );
               condition.percentProbesMissing = numProbesMissing / this.geneTree.items.length;
            }

            /*
             * Calculate visible scores for genes
             */
            for (j = 0; j < this.geneTree.items.length; j++) {
               gene = this.geneTree.items[j];
               var pValues = [];
               var alreadySeenFactor = [];
               var numProbesMissing = 0;
               var unusedProbes = 0;
               for (i = 0; i < this.conditionTree.items.length; i++) {
                  condition = this.conditionTree.items[i];
                  cell = this.cells.getCell( gene, condition );
                  if ( cell !== null ) {
                     if ( cell.isProbeMissing ) {
                        numProbesMissing++;
                     } else if ( cell.isProbeOmitted ) {
                        unusedProbes++;
                     } else {
                        // We get p-value per condition(factor value vs baseline) We have to keep only one p-value per
                        // factor.
                        if ( typeof alreadySeenFactor[condition.factorId] === "undefined" ) {
                           pValues.push( cell.pValue ); // uncorrected.
                           alreadySeenFactor[condition.factorId] = true;
                        }
                     }
                  }
               }

               /*
                * This is used as a sorting option, so we have to remove that if this is not computed 'correctly'. An
                * alternative would be to simply count the fraction of times it is significant.
                */
               gene.metaPvalue = GemmaStatUtils.computeMetaPvalue( pValues );
               /*
                * Use this method if we are not storing all the signiificant results.
                */
               // gene.metaPvalue = GemmaStatUtils.computeFractionFailure(pValues.length, unusedProbes +
               // pValues.length);
               gene.metaPvalueCount = pValues.length;
               gene.percentProbesMissing = numProbesMissing / this.conditionTree.items.length;
               gene.metaPvalueBarChart = this.calculateBarChartValueBasedOnPvalue( gene.metaPvalue );
            }
         },

         /**
          * @private
          * @param correctedPValue
          * @returns {Number}
          */
         calculateBarChartValueBasedOnPvalue : function( correctedPValue ) {
            var visualizationValue = 0;
            if ( correctedPValue < 0.5 && correctedPValue >= 0.25 )
               visualizationValue = 1;
            else if ( correctedPValue < 0.25 && correctedPValue >= 0.1 )
               visualizationValue = 2;
            else if ( correctedPValue < 0.1 && correctedPValue >= 0.05 )
               visualizationValue = 3;
            else if ( correctedPValue < 0.05 && correctedPValue >= 0.01 )
               visualizationValue = 4;
            else if ( correctedPValue < 0.01 && correctedPValue >= 0.001 )
               visualizationValue = 5;
            else if ( correctedPValue < 0.001 && correctedPValue >= 0.0001 )
               visualizationValue = 6;
            else if ( correctedPValue < 0.0001 && correctedPValue >= 0.00001 )
               visualizationValue = 7;
            else if ( correctedPValue < 0.00001 )
               visualizationValue = 8;
            return visualizationValue;
         },

         /**
          * @private
          * @param type
          * @param item
          * @returns {Object}
          */
         constructHoverWindowContent : function( type, item ) {
            var msg;
            if ( type === 'gene' ) {
               msg = {
                  type : 'gene',
                  geneSymbol : item.name,
                  geneId : item.id,
                  geneFullName : item.fullName,
                  geneMetaPvalue : (item.metaPvalue == 2.0) ? "NA" : item.metaPvalue,
                  metaPvalueCount : item.metaPvalueCount
               };
            } else if ( type === 'condition' ) {

               if ( Gemma.Metaheatmap.Config.USE_GENE_COUNTS_FOR_ENRICHMENT ) {
                  item.totalDiffExpressed = item.numberOfGenesDiffExpressed;
                  item.totalOnArray = item.numberOfGenesTested;
               } else {
                  item.totalDiffExpressed = item.numberDiffExpressedProbes;
                  item.totalOnArray = item.numberOfProbesOnArray;
               }

               msg = {
                  type : 'condition',
                  factorCategory : item.factorCategory,
                  factorName : item.factorName,
                  factorDescription : item.factorDescription,
                  datasetName : item.datasetName,
                  datasetShortName : item.datasetShortName,

                  contrastFactorValue : item.contrastFactorValue,
                  baselineFactorValue : item.baselineFactorValue,

                  numDiffExpressed : item.numDiffExpressed,
                  numInSet : item.numInSet,
                  ora : item.ora,
                  specificityPercent : Gemma.Metaheatmap.Utils.formatPercent( item.totalDiffExpressed,
                     item.totalOnArray, 2 ),
                  totalDiffExpressed : item.totalDiffExpressed,
                  totalOnArray : item.totalOnArray
               };
            } else {
               msg = {
                  type : 'cell',
                  factorCategory : item.condition.factorCategory,
                  factorName : item.condition.factorName,
                  factorDescription : item.condition.factorDescription,
                  datasetName : item.condition.datasetName,
                  datasetShortName : item.condition.datasetShortName,

                  contrastFactorValue : item.condition.contrastFactorValue,
                  baselineFactorValue : item.condition.baselineFactorValue,

                  geneSymbol : item.gene.name,
                  geneId : item.gene.id,
                  geneFullName : item.gene.fullName,

                  numberOfProbes : item.numberOfProbes,
                  numberOfProbesDiffExpressed : item.numberOfProbesDiffExpressed,

                  pvalue : (item.isProbeMissing) ? 'No data' : item.pValue,
                  correctedPValue : (item.isProbeMissing) ? 'No data' : item.correctedPValue,
                  foldChange : (item.isProbeMissing || item.foldChange == 0) ? 'No data' : 0.01 * Math
                     .ceil( item.foldChange * 100 )
               };
            }
            return msg;
         },

         getSelectedGeneIds : function() {
            var geneIds = [];
            for (var i = 0; i < this.geneTree.items.length; i++) {
               var gene = this.geneTree.items[i];
               if ( gene.isSelected ) {
                  geneIds.push( gene.id );
               }
            }
            return geneIds;
         },

         getSelectedDatasetIds : function() {
            var dsIds = [];
            for (var i = 0; i < this.conditionTree.items.length; i++) {
               var condition = this.conditionTree.items[i];
               if ( condition.isSelected ) {
                  dsIds.push( condition.datasetId );
               }
            }
            return dsIds;
         },

         /**
          * @private
          */
         afterRender : function() {
            Gemma.Metaheatmap.VisualizationPanel.superclass.afterRender.apply( this, arguments );

            this.mask = new Ext.LoadMask( this.getEl(), {
               msg : "Filtering..."
            } );

            this.hoverWindow.isDocked = false;
            this.hoverWindow.hide();
         },

         /**
          * @private
          * @override
          */
         onRender : function() {
            Gemma.Metaheatmap.VisualizationPanel.superclass.onRender.apply( this, arguments );

            this.addEvents( 'gene_zoom_change', 'condition_zoom_change' );

            this.variableWidthCol.boxTopLabels.on( 'label_mouse_in', function( label, e, t ) {
               var labelType = this.variableWidthCol.boxHeatmap.isGeneOnTop ? 'gene' : 'condition';
               var msg = this.constructHoverWindowContent( labelType, label.item );
               this.hoverWindow.show();
               if ( !this.hoverWindow.isDocked ) {
                  this.hoverWindow.setPagePosition( e.getPageX() + 20, e.getPageY() + 20 );
               }
               this.hoverWindow.update( msg );
            }, this );

            this.fixedWidthCol.boxSideLabels.on( 'label_mouse_in', function( label, e, t ) {
               var labelType = this.variableWidthCol.boxHeatmap.isGeneOnTop ? 'condition' : 'gene';
               var msg = this.constructHoverWindowContent( labelType, label.item );
               this.hoverWindow.show();
               if ( !this.hoverWindow.isDocked ) {
                  this.hoverWindow.setPagePosition( e.getPageX() + 20, e.getPageY() + 20 );
               }
               this.hoverWindow.update( msg );
            }, this );

            this.variableWidthCol.boxTopLabels.on( 'label_mouse_out', function( label, e, t ) {
               if ( this.hoverWindow && !this.hoverWindow.isDocked ) {
                  this.hoverWindow.hide();
               }
            }, this );

            this.fixedWidthCol.boxSideLabels.on( 'label_mouse_out', function( label, e, t ) {
               if ( this.hoverWindow && !this.hoverWindow.isDocked ) {
                  this.hoverWindow.hide();
               }
            }, this );

            this.variableWidthCol.boxHeatmap.on( 'cell_mouse_in', function( cell, e, t ) {
               var msg = this.constructHoverWindowContent( 'cell', cell );

               this.hoverWindow.show();
               if ( !this.hoverWindow.isDocked ) {
                  this.hoverWindow.setPagePosition( e.getPageX() + 20, e.getPageY() + 20 );
               }
               this.hoverWindow.update( msg );

               cell.gene.display.label.drawFn( true );
               cell.condition.display.label.drawFn( true );
            }, this );

            this.variableWidthCol.boxHeatmap.on( 'cell_mouse_out', function( cell, e, t ) {
               cell.gene.display.label.drawFn( false );
               cell.condition.display.label.drawFn( false );
               if ( !this.hoverWindow.isDocked ) {
                  this.hoverWindow.hide();
               }
            }, this );

            this.variableWidthCol.boxHeatmap.on( 'cell_click', function( cell ) {
               if ( cell !== null ) {
                  var expressionDetailsWindow = new Gemma.VisualizationWithThumbsWindow( {
                     title : 'Gene Expression',
                     thumbnails : false,
                     closableAction : 'hide',
                     downloadLink : String.format( Gemma.CONTEXT_PATH + "/dedv/downloadDEDV.html?ee={0}&g={1}",
                        cell.condition.datasetId, cell.gene.id )
                  } );
                  expressionDetailsWindow.show( {
                     params : [ [ cell.condition.datasetId ], [ cell.gene.id ] ]
                  } );
                  var xy = expressionDetailsWindow.getPosition();
                  expressionDetailsWindow.setPosition( xy[0] + Gemma.MetaVisualizationPopups.cascadeLayoutCounter * 20,
                     xy[1] + Gemma.MetaVisualizationPopups.cascadeLayoutCounter * 20 );
                  Gemma.MetaVisualizationPopups.cascadeLayoutCounter++;
                  if ( Gemma.MetaVisualizationPopups.cascadeLayoutCounter > 4 ) {
                     Gemma.MetaVisualizationPopups.cascadeLayoutCounter = 0;
                  }

               }
            } );
         }

      } );

Ext.reg( 'Metaheatmap.VisualizationPanel', Gemma.Metaheatmap.VisualizationPanel );
