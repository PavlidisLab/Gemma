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
 * 
 * 
 */
Gemma.CytoscapeControlBar = Ext
   .extend(
      Ext.Toolbar,
      {

         /**
          * @memberOf Gemma.CytoscapeControlBar
          */
         initComponent : function() {
            var display = this.display;
            var cytoscapePanel = this.cytoscapePanel;

            this.visualOptionsMenu = new Ext.menu.Menu( {
               items : [ {
                  itemId : 'zoomToFitButton',
                  text : Gemma.HelpText.WidgetDefaults.CytoscapePanel.zoomToFitText,
                  handler : function() {
                     display.zoomToFit();
                  }
               }, {
                  itemId : 'refreshLayoutButton',
                  text : Gemma.HelpText.WidgetDefaults.CytoscapePanel.refreshLayoutText,
                  handler : function() {

                     cytoscapePanel.refreshLayout();
                  }
               }, {
                  itemId : 'nodeLabelsButton',
                  text : Gemma.HelpText.WidgetDefaults.CytoscapePanel.nodeLabelsText,
                  checked : true,
                  handler : function( menuItem ) {
                     display.toggleNodeLabels( !menuItem.checked );
                  }
               } ]
            } );

            this.geneSetOverlayPicker = new Gemma.GeneSetOverlayPicker( {
               display : this.display,
               taxonId : this.coexpressionSearchData.getTaxonId()
            } );

            this.actionsMenu = new Ext.menu.Menu( {
               items : [ {
                  itemId : 'extendSelectedNodesButton',
                  text : Gemma.HelpText.WidgetDefaults.CytoscapePanel.extendNodeText,
                  disabled : false,
                  handler : function() {
                     cytoscapePanel.extendSelectedNodes();
                  }
               }, {
                  itemId : 'searchWithSelectedNodesButton',
                  text : Gemma.HelpText.WidgetDefaults.CytoscapePanel.searchWithSelectedText,
                  disabled : false,
                  handler : function() {
                     cytoscapePanel.searchWithSelectedNodes();
                  }
               }, {
                  itemId : 'applyGeneListOverlayButton',
                  text : "Gene List Overlay",
                  handler : function( item ) {
                     this.geneSetOverlayPicker.show();
                  },
                  scope : this
               } ]
            } );

            this.actionsButton = new Ext.Button( {
               text : '<b>Actions</b>',
               itemId : 'actionsButton',
               menu : this.actionsMenu
            } );

            Ext
               .apply(
                  this,
                  {
                     items : [
                              {
                                 xtype : 'tbtext',
                                 text : 'Stringency:'
                              },
                              ' ',
                              {
                                 xtype : 'spinnerfield',
                                 ref : 'stringencySpinner',
                                 itemId : 'stringencySpinner',
                                 decimalPrecision : 1,
                                 incrementValue : 1,
                                 accelerate : false,
                                 allowBlank : false,
                                 allowDecimals : false,
                                 allowNegative : false,
                                 minValue : Gemma.MIN_STRINGENCY,
                                 maxValue : 999,
                                 fieldLabel : 'Stringency ',
                                 value : this.coexDisplaySettings.getStringency(),
                                 width : 60,
                                 enableKeyEvents : true,
                                 listeners : {
                                    "spin" : {
                                       fn : this.onStringencyChange,
                                       scope : this
                                    },
                                    "keyup" : {
                                       fn : this.onStringencyChange,
                                       scope : this,
                                       delay : 500
                                    }
                                 }
                              },
                              {
                                 xtype : 'label',
                                 html : '&nbsp&nbsp<img ext:qtip="'
                                    + Gemma.HelpText.WidgetDefaults.CytoscapePanel.stringencySpinnerTT
                                    + '" src="' + Gemma.CONTEXT_PATH + '/images/icons/question_blue.png"/>',
                                 height : 15
                              },
                              ' ',
                              ' ',
                              {
                                 xtype : 'textfield',
                                 ref : 'searchInCytoscapeBox',
                                 itemId : 'searchInCytoscapeBox',
                                 tabIndex : 1,
                                 enableKeyEvents : true,
                                 value : this.coexDisplaySettings.getSearchTextValue(),
                                 emptyText : 'Find gene in results',
                                 listeners : {
                                    "keyup" : {
                                       fn : function( textField ) {
                                          this.coexDisplaySettings.setSearchTextValue( textField.getValue() );
                                       },
                                       scope : this,
                                       delay : 500
                                    }
                                 }
                              },
                              ' ',
                              ' ',
                              {
                                 xtype : 'checkbox',
                                 itemId : 'queryGenesOnly',
                                 boxLabel : 'Query Genes Only',
                                 handler : function( checkbox, checked ) {
                                    this.coexDisplaySettings.setQueryGenesOnly( checked );
                                 },
                                 checked : false,
                                 scope : this
                              },
                              '->',
                              '-',
                              {
                                 xtype : 'button',
                                 text : '<b>Help</b>',
                                 tooltip : Gemma.HelpText.WidgetDefaults.CytoscapePanel.widgetHelpTT,
                                 handler : function() {
                                    window
                                       .open( 'https://pavlidislab.github.io/Gemma/search.html#coexpression-results' );
                                 },
                                 scope : this
                              },
                              '->',
                              '-',
                              {
                                 xtype : 'button',
                                 text : '<b>Save As</b>',
                                 menu : new Ext.menu.Menu( {
                                    items : [ {
                                       text : 'Save as PNG',
                                       handler : function() {
                                          this.display.exportPNG();
                                       },
                                       scope : this
                                    }, {
                                       text : 'Save as text',
                                       handler : function() {
                                          this.display.exportText();
                                       },
                                       scope : this
                                    }
                                    /*
                                     * , { text: 'Save as GraphML', handler: function () { this.display.exportGraphML(); },
                                     * scope: this }, { text: 'Save as XGMML', handler: function () {
                                     * this.display.exportXGMML(); }, scope: this }, { text: 'Save as SIF', handler:
                                     * function () { this.display.exportSIF(); }, scope: this }, { text: 'Save as SVG',
                                     * handler: function () { this.display.exportSVG(); }, scope: this }
                                     */
                                    ]
                                 } )
                              },
                              '->',
                              '-',
                              {
                                 xtype : 'button',
                                 text : '<b>Visual Options</b>',
                                 menu : this.visualOptionsMenu
                              },
                              '->',
                              '-',
                              this.actionsButton,
                              '->',
                              '-',
                              {
                                 xtype : 'button',
                                 itemId : 'nodeDegreeEmphasis',
                                 ref : 'nodeDegreeEmphasis',
                                 text : '<b>' + Gemma.HelpText.WidgetDefaults.CytoscapePanel.nodeDegreeEmphasisText
                                    + '</b>',
                                 enableToggle : 'true',
                                 pressed : 'true',
                                 toggleHandler : function( button, pressed ) {
                                    display.nodeDegreeEmphasize( pressed );
                                 }
                              },
                              {
                                 xtype : 'label',
                                 html : '&nbsp&nbsp<img ext:qtip="'
                                    + Gemma.HelpText.WidgetDefaults.CytoscapePanel.nodeDegreeEmphasisTT
                                    + '" src="' + Gemma.CONTEXT_PATH + '/images/icons/question_blue.png"/>&nbsp',
                                 height : 15
                              } ]
                  } );

            Gemma.CytoscapeControlBar.superclass.initComponent.apply( this, arguments );

            this.display.on( 'selection_available', function() {
               this.actionsMenu.getComponent( 'extendSelectedNodesButton' ).setDisabled( false );
               this.actionsMenu.getComponent( 'searchWithSelectedNodesButton' ).setDisabled( false );
            }, this );

            this.display.on( 'selection_unavailable', function() {
               this.actionsMenu.getComponent( 'extendSelectedNodesButton' ).setDisabled( true );
               this.actionsMenu.getComponent( 'searchWithSelectedNodesButton' ).setDisabled( true );
            }, this );

            this.coexDisplaySettings.on( 'stringency_change', function( value ) {
               this.setStringencySpinnerValue( value );
            }, this );

            this.coexDisplaySettings.on( 'query_genes_only_change', function( value ) {
               this.setQueryGenesOnlyCheckBox( value );
            }, this );

            this.coexDisplaySettings.on( 'search_text_change', function( text ) {
               this.setSearchText( text );
            }, this );
         },

         /**
          * @private handler
          * @param requestedDisplayStringency
          *           (this is the same as for the coexpressiongrid)
          */
         onStringencyChange : function( spinner ) {
            var spinnerValue = spinner.field.getValue();
            /*
             * Don't allow the stringency to go lower than that used in the query
             */
            var appliedStringency = this.coexpressionSearchData.getQueryStringency();

            if ( spinnerValue >= appliedStringency ) {
               this.coexDisplaySettings.setStringency( spinnerValue );
            } else {
               spinner.field.setValue( appliedStringency );
            }

            // var requestedDisplayStringency = spinner.field.getValue();
            //
            // /*
            // * Don't allow the stringency to go lower than that used in the query
            // */
            // var appliedStringency = this.coexpressionSearchData.searchResults.appliedStringency;
            //
            // if ( requestedDisplayStringency < appliedStringency ) {
            // spinner.field.setValue( appliedStringency );
            // this.coexDisplaySettings.setStringency( appliedStringency );
            // } else {
            // this.coexDisplaySettings.setStringency( requestedDisplayStringency );
            // }

         },

         setStringencySpinnerValue : function( stringency ) {
            this.getComponent( 'stringencySpinner' ).setValue( stringency );
         },

         setSearchText : function( text ) {
            this.getComponent( 'searchInCytoscapeBox' ).setValue( text );
         },

         setQueryGenesOnlyCheckBox : function( checked ) {
            this.getComponent( 'queryGenesOnly' ).setValue( checked );
         },

         disableQueryGenesOnlyCheckBox : function( disabled ) {
            this.getComponent( 'queryGenesOnly' ).setDisabled( disabled );
         }
      } );