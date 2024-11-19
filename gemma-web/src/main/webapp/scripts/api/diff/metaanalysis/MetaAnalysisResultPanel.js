/**
 * Panel for showing meta-analysis result
 * 
 * @author frances
 *
 */
Ext.namespace( 'Gemma' );

Gemma.MetaAnalysisResultPanel = Ext
   .extend(
      Ext.Panel,
      {
         metaAnalysis : null,
         defaultQvalueThreshold : null,
         showLimitDisplayCombo : true,
         showDownloadButton : true,
         numResultsLimit : 500,
         border : false,
         layout : 'border',
         initComponent : function() {
            var GENE_SYMBOL_COLUMN_TITLE = 'Symbol';
            var GENE_NAME_COLUMN_TITLE = 'Name';
            var P_VALUE_COLUMN_TITLE = 'p-value';
            var Q_VALUE_COLUMN_TITLE = 'q-value';
            var DIRECTION_COLUMN_TITLE = 'Direction';

            var MAX_CHARACTERS_IN_COLUMN = 100;

            var totalNumberOfResults = 0;

            var currentThreshold = null;

            var summaryLabel = new Ext.form.Label();

            var metaAnalysisUtilities = new Gemma.MetaAnalysisUtilities();

            var limitDisplayCombo = this.showLimitDisplayCombo ? new Ext.form.ComboBox( {
               width : 180,
               editable : false,
               triggerAction : 'all',
               mode : 'local',
               store : new Ext.data.ArrayStore( {
                  fields : [ 'shouldLimit', 'displayText' ],
                  data : [ [ true, 'Display top ' + this.numResultsLimit + ' results' ],
                          [ false, 'Display all results' ] ]
               } ),
               value : true, // By default, we should limit number of results shown.
               valueField : 'shouldLimit',
               displayField : 'displayText',
               listeners : {
                  select : function( combo, record, index ) {
                     this.showResults();
                  },
                  scope : this
               }
            } ) : null;

            var downloadButton = this.showDownloadButton ? new Ext.Button( {
               text : '<b>Download</b>',
               icon : Gemma.CONTEXT_PATH + '/images/download.gif',
               handler : function() {
                  var downloadData = [ [ GENE_SYMBOL_COLUMN_TITLE, GENE_NAME_COLUMN_TITLE, P_VALUE_COLUMN_TITLE,
                                        Q_VALUE_COLUMN_TITLE, DIRECTION_COLUMN_TITLE ] ];

                  for ( var i = 0; i < this.metaAnalysis.results.length; i++) {
                     var resultRow = getResultRow( i );
                     if ( currentThreshold == null || resultRow.metaQvalue < currentThreshold ) {
                        downloadData.push( [ resultRow.geneSymbol, resultRow.geneName, resultRow.metaPvalue,
                                            resultRow.metaQvalue, resultRow.upperTail ? 'up' : 'down' ] );
                     }
                  }

                  var downloadDataHeader = 'Results for Meta-analysis '
                     + (this.metaAnalysis.name == null ? '' : this.metaAnalysis.name) + ' (q-value < '
                     + (currentThreshold == null ? this.defaultQvalueThreshold : currentThreshold) + ')';
                  var textWindow = new Gemma.DownloadWindow( {
                     windowTitleSuffix : downloadDataHeader,
                     downloadDataHeader : downloadDataHeader,
                     downloadData : downloadData,
                     modal : true
                  } );
                  textWindow.convertToText();
                  textWindow.show();
               },
               scope : this
            } ) : null;

            var headerPanelItems = [ summaryLabel ];
            if ( this.showDownloadButton ) {
               headerPanelItems.push( downloadButton );
            }
            if ( this.showLimitDisplayCombo ) {
               headerPanelItems.push( limitDisplayCombo );
            }

            var headerPanel = new Ext.Panel( {
               region : 'north',
               layout : 'vbox',
               align : 'stretch',
               border : false,
               height : 90, // It must be set because it is in the north region.
               defaults : {
                  margins : '10 0 0 10',
                  style : 'white-space: nowrap;'
               },
               items : headerPanelItems
            } );

            var resultLabel = new Ext.form.Label( {
               region : 'center',
               autoScroll : true,
               style : 'background-color: #FFFFFF;' // By default, the background color is blue.
            } );

            var getResultRow = function( index ) {
               var result = this.metaAnalysis.results[index];
               var row = {
                  geneSymbol : result.geneSymbol,
                  geneName : result.geneName,
                  metaPvalue : result.metaPvalue.toExponential( 2 ),
                  metaQvalue : result.metaQvalue.toExponential( 2 ),
                  upperTail : result.upperTail
               };

               return row;
            }.createDelegate( this );

            var showResultsWithoutMask = function( threshold ) {
               currentThreshold = threshold;

               var resultText = '';

               if ( resultLabel.loadMask ) {
                  resultLabel.loadMask.hide();
               }

               if ( this.metaAnalysis ) {
                  if ( threshold != null && threshold <= 0 ) {
                     resultLabel.setText( '' );
                     summaryLabel.setText( '' );
                  } else {
                     // Sort results by p-value.
                     this.metaAnalysis.results.sort( function( result1, result2 ) {
                        return result1.metaPvalue < result2.metaPvalue ? -1
                           : result1.metaPvalue == result2.metaPvalue ? 0 : 1;
                     } );

                     // Show limitDisplayCombo only when we have results more than this.numResultsLimit.
                     var shouldLimitDisplayComboBeShown = this.showLimitDisplayCombo
                        && this.metaAnalysis.results.length > this.numResultsLimit;

                     var height = 40;
                     if ( this.showDownloadButton ) {
                        height += 40;
                        downloadButton.show();
                     }
                     if ( shouldLimitDisplayComboBeShown ) {
                        height += 40;
                        limitDisplayCombo.show();
                     } else if ( limitDisplayCombo ) {
                        limitDisplayCombo.hide();
                     }
                     headerPanel.setHeight( height );

                     var stringStyle = 'style="padding: 0 10px 0 10px; vertical-align: top;"';
                     var numberStyle = 'style="padding: 0 10px 0 10px; vertical-align: top; text-align: right; white-space: nowrap;"';

                     var directionStyle = 'style="padding: 0 10px 0 10px; text-align: center; vertical-align: top;"';

                     resultText += '<table>' + '<tr>' + '<th ' + stringStyle + '>' + GENE_SYMBOL_COLUMN_TITLE + '</th>'
                        + '<th ' + stringStyle + '>' + GENE_NAME_COLUMN_TITLE + '</th>' + '<th ' + numberStyle + '>'
                        + P_VALUE_COLUMN_TITLE + '</th>' + '<th ' + numberStyle + '>' + Q_VALUE_COLUMN_TITLE + '</th>'
                        + '<th ' + stringStyle + '>' + DIRECTION_COLUMN_TITLE + '</th>' + '</tr>';

                     var metaAnalysisMaxIndex = shouldLimitDisplayComboBeShown && limitDisplayCombo.getValue() ? this.numResultsLimit
                        : this.metaAnalysis.results.length;

                     var numResultsDisplayed = 0;

                     for ( var i = 0; i < metaAnalysisMaxIndex; i++) {
                        var resultRow = getResultRow( i );

                        // If threshold is null, we don't do any "filtering".
                        if ( threshold == null || resultRow.metaQvalue < threshold ) {
                           numResultsDisplayed++;
                           resultText += '<tr>' + '<td ' + stringStyle + '>' + resultRow.geneSymbol + '</td>' + '<td '
                              + stringStyle + '>'
                              + Ext.util.Format.ellipsis( resultRow.geneName, MAX_CHARACTERS_IN_COLUMN, true )
                              + '</td>' + '<td ' + numberStyle + '>' + resultRow.metaPvalue + '</td>' + '<td '
                              + numberStyle + '>' + resultRow.metaQvalue + '</td>' + '<td ' + directionStyle + '>'
                              + metaAnalysisUtilities.generateDirectionHtml( resultRow.upperTail ) + '</td>' + '</tr>';
                        }
                     }

                     resultText += '</table>';

                     resultLabel.setText( resultText, false );

                     totalNumberOfResults = (threshold == null ? this.metaAnalysis.results.length : numResultsDisplayed);

                     downloadButton.setDisabled( totalNumberOfResults <= 0 );

                     summaryLabel.setText( '<b>Number of genes analyzed</b>: ' + this.metaAnalysis.numGenesAnalyzed
                        + '<br />' + '<b>Number of genes with q-value < '
                        + (threshold == null ? this.defaultQvalueThreshold : threshold) + '</b>: '
                        + totalNumberOfResults, false );
                  }
               } else {
                  summaryLabel.setText( '<b>No results were significant.</b>', false );
               }

               this.doLayout();
            }.createDelegate( this );

            Ext.apply( this, {
               getTotalNumberOfResults : function() {
                  return totalNumberOfResults;
               },
               setMetaAnalysis : function( metaAnalysis ) {
                  this.metaAnalysis = metaAnalysis;
                  showResultsWithoutMask();
               },
               // Reset components responsible for displaying results.
               clear : function( errorMessage ) {
                  summaryLabel.setText( errorMessage ? '<b>' + errorMessage + '</b>' : '', false );
                  resultLabel.setText( '', false );

                  if ( limitDisplayCombo ) {
                     limitDisplayCombo.hide();
                  }

                  if ( downloadButton ) {
                     downloadButton.hide();
                  }

                  this.doLayout();
               },
               showResults : function( threshold ) {
                  if ( resultLabel.getEl() ) {
                     if ( !resultLabel.loadMask ) {
                        resultLabel.loadMask = new Ext.LoadMask( resultLabel.getEl(), {
                           msg : "Loading ..."
                        } );
                     }
                     resultLabel.loadMask.show();

                     // Defer the call. Otherwise, the loading mask does not show.
                     Ext.defer( showResultsWithoutMask, // function to call
                     10, // delay in milliseconds
                     this, // scope
                     [ threshold ] ); // arguments to the function
                  } else {
                     showResultsWithoutMask( threshold );
                  }
               },
               items : [ headerPanel, resultLabel ]
            } );

            if ( this.metaAnalysis ) {
               showResultsWithoutMask();
            }

            Gemma.MetaAnalysisResultPanel.superclass.initComponent.call( this );
         }
      } );
