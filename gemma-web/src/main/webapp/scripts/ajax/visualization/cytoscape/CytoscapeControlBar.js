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
Ext.namespace('Gemma');

/**
 *
 *
 */
Gemma.CytoscapeControlBar = Ext.extend(Ext.Toolbar, {
    initComponent: function () {
        var display = this.display;
        var cytoscapePanel = this.cytoscapePanel;

        this.visualOptionsMenu = new Ext.menu.Menu({
            items: [
                 {
                    itemId: 'zoomToFitButton',
                    text: Gemma.HelpText.WidgetDefaults.CytoscapePanel.zoomToFitText,
                    handler: function () {
                    	display.zoomToFit();
                    }
                },
                {
                    itemId: 'refreshLayoutButton',
                    text: Gemma.HelpText.WidgetDefaults.CytoscapePanel.refreshLayoutText,
                    handler: function () {
                        
                    	cytoscapePanel.refreshLayout();
                    }
                },
                {
                    itemId: 'nodeLabelsButton',
                    text: Gemma.HelpText.WidgetDefaults.CytoscapePanel.nodeLabelsText,
                    checked: true,
                    handler: function (menuItem) {
                        display.toggleNodeLabels( !menuItem.checked );
                    }
                }
            ]
        });

        this.geneSetOverlayPicker = new Gemma.GeneSetOverlayPicker({
            display: this.display,
            taxonId: this.coexpressionSearchData.getTaxonId()
        });

        
        this.actionsMenu = new Ext.menu.Menu({
            items: [
                {
                    itemId: 'extendSelectedNodesButton',
                    text: Gemma.HelpText.WidgetDefaults.CytoscapePanel.extendNodeText,
                    disabled: false,
                    handler: function () {
                        cytoscapePanel.extendSelectedNodes();
                    }
                },
                {
                    itemId: 'searchWithSelectedNodesButton',
                    text: Gemma.HelpText.WidgetDefaults.CytoscapePanel.searchWithSelectedText,
                    disabled: false,
                    handler: function () {
                        cytoscapePanel.searchWithSelectedNodes();
                    }
                },
                {
                    itemId: 'applyGeneListOverlayButton',
                    text: "Gene List Overlay",
                    checked: false,
                    handler: function (item) {
                        if (!item.checked) {
                            this.geneSetOverlayPicker.show();
                        } else {
                            this.coexDisplaySettings.setOverlayGeneIds([]);
                        }
                    },
                    scope: this
                }
            ]
        });

        this.actionsButton = new Ext.Button({
            text: '<b>Actions</b>',
            itemId: 'actionsButton',
            menu: this.actionsMenu
        });

        Ext.apply(this, {
            items: [
                {
                    xtype: 'tbtext',
                    text: 'Stringency:'
                },
                ' ',
                {
                    xtype: 'spinnerfield',
                    ref: 'stringencySpinner',
                    itemId: 'stringencySpinner',
                    decimalPrecision: 1,
                    incrementValue: 1,
                    accelerate: false,
                    allowBlank: false,
                    allowDecimals: false,
                    allowNegative: false,
                    minValue: Gemma.MIN_STRINGENCY,
                    maxValue: 999,
                    fieldLabel: 'Stringency ',
                    value: this.coexDisplaySettings.getStringency(),
                    width: 60,
                    enableKeyEvents: true,
                    listeners: {
                        "spin" : {
                            fn: function (spinner) {
                                var stringency = spinner.field.getValue();
                                if (stringency < 2) {
                                    spinner.field.setValue( 2 );
                                } else {
                                    this.onStringencyChange( stringency );
                                }
                            },
                            scope: this
                        },
                        "keyup": {
                            fn: function (field) {
                                var value = field.getValue();
                                if (Ext.isNumber(value) && value > 1) {
                                    this.onStringencyChange( value );
                                }
                            },
                            scope: this,
                            delay: 500
                        }
                    }
                },
                {
                    xtype: 'label',
                    html: '&nbsp&nbsp<img ext:qtip="' +
                        Gemma.HelpText.WidgetDefaults.CytoscapePanel.stringencySpinnerTT +
                        '" src="/Gemma/images/icons/question_blue.png"/>',
                    height: 15
                },
                ' ',
                ' ',
                {
                    xtype: 'textfield',
                    ref: 'searchInCytoscapeBox',
                    itemId: 'searchInCytoscapeBox',
                    tabIndex: 1,
                    enableKeyEvents: true,
                    value: this.coexDisplaySettings.getSearchTextValue(),
                    emptyText: 'Find gene in results',
                    listeners: {
                        "keyup": {
                            fn: function (textField) {
                                    this.coexDisplaySettings.setSearchTextValue( textField.getValue() );
                            },
                            scope: this,
                            delay: 500
                        }
                    }
                },
                ' ',
                ' ',
                {
                    xtype: 'checkbox',
                    itemId: 'queryGenesOnly',
                    boxLabel: 'Query Genes Only',
                    handler: function (checkbox, checked) {
                        this.coexDisplaySettings.setQueryGenesOnly(checked);
                    },
                    checked: false,
                    scope: this
                },
                '->',
                '-',
                {
                    xtype: 'button',
                    text: '<b>Help</b>',
                    tooltip: Gemma.HelpText.WidgetDefaults.CytoscapePanel.widgetHelpTT,
                    handler: function () {
                        window.open('http://gemma-chibi-doc.sites.olt.ubc.ca/documentation/search-page/#GemmaQuickGuide-CoexpressionView');
                    },
                    scope: this
                },
                '->',
                '-',
                {
                    xtype: 'button',
                    text: '<b>Save As</b>',
                    menu: new Ext.menu.Menu({
                        items: [
                            {
                                text: 'Save as PNG',
                                handler: function () {
                                    this.display.exportPNG();
                                },
                                scope: this
                            },
                            {
                                text: 'Save as text',
                                handler: function () {
                                    this.display.exportText();
                                },
                                scope: this
                            }
                            /*,
                            {
                                text: 'Save as GraphML',
                                handler: function () {
                                    this.display.exportGraphML();
                                },
                                scope: this
                            },
                            {
                                text: 'Save as XGMML',
                                handler: function () {
                                    this.display.exportXGMML();
                                },
                                scope: this
                            },
                            {
                                text: 'Save as SIF',
                                handler: function () {
                                    this.display.exportSIF();
                                },
                                scope: this
                            },
                            {
                                text: 'Save as SVG',
                                handler: function () {
                                    this.display.exportSVG();
                                },
                                scope: this
                            }*/
                        ]
                    })
                },
                '->',
                '-',
                {
                    xtype: 'button',
                    text: '<b>Visual Options</b>',
                    menu: this.visualOptionsMenu
                },
                '->',
                '-',
                this.actionsButton,
                '->',
                '-',
                {
                    xtype: 'button',
                    itemId: 'nodeDegreeEmphasis',
                    ref: 'nodeDegreeEmphasis',
                    text: '<b>' + Gemma.HelpText.WidgetDefaults.CytoscapePanel.nodeDegreeEmphasisText + '</b>',
                    enableToggle: 'true',
                    pressed: 'true',
                    toggleHandler: function (button, pressed) {
                        display.nodeDegreeEmphasize(pressed);
                    }
                },
                {
                    xtype: 'label',
                    html: '&nbsp&nbsp<img ext:qtip="' +
                        Gemma.HelpText.WidgetDefaults.CytoscapePanel.nodeDegreeEmphasisTT +
                        '" src="/Gemma/images/icons/question_blue.png"/>&nbsp',
                    height: 15
                }
            ]
        });

        Gemma.CytoscapeControlBar.superclass.initComponent.apply(this, arguments);

        this.display.on('selection_available', function() {
            this.actionsMenu.getComponent('extendSelectedNodesButton').setDisabled(false);
            this.actionsMenu.getComponent('searchWithSelectedNodesButton').setDisabled(false);
        }, this);

        this.display.on('selection_unavailable', function() {
            this.actionsMenu.getComponent('extendSelectedNodesButton').setDisabled(true);
            this.actionsMenu.getComponent('searchWithSelectedNodesButton').setDisabled(true);
        }, this);

        this.coexDisplaySettings.on('stringency_change', function( value ) {
            this.setStringencySpinnerValue( value );
        }, this);

        this.coexDisplaySettings.on('query_genes_only_change', function( value ) {
            this.setQueryGenesOnlyCheckBox( value );
        }, this);

        this.coexDisplaySettings.on('search_text_change', function( text ) {
            this.setSearchText( text );
        }, this);
    },

    onStringencyChange: function (requestedDisplayStringency) {
        var controlBar = this;

        var savedDisplayStringency = this.coexDisplaySettings.getStringency();
        var resultsStringency = this.coexpressionSearchData.getResultsStringency();

        if (requestedDisplayStringency < resultsStringency && !this.cytoscapePanel.coexpressionSearchData.searchCommandUsed.queryGenesOnly) {
            Ext.Msg.show({
                title: 'New Search',
                msg: Gemma.HelpText.WidgetDefaults.CytoscapePanel.lowStringencyWarning,
                buttons: {
                    ok: 'Proceed',
                    cancel: 'Cancel'
                },
                fn: function (button) {
                    if (button === 'ok') {
                        controlBar.coexpressionSearchData.searchForCytoscapeDataWithStringency(requestedDisplayStringency);
                        controlBar.coexDisplaySettings.setStringency(requestedDisplayStringency);
                    } else {
                        // restore spinner field
                        controlBar.setStringencySpinnerValue( savedDisplayStringency );
                    }
                }
            });
        }else if(this.cytoscapePanel.coexpressionSearchData.searchCommandUsed.queryGenesOnly){
        	/*
        	 * not sure if alerting the user is the best idea
        	Ext.Msg.show({
                title: 'Low stringency results have been trimmed',
                msg: 'Because of the number of genes in your search, low stringency results have been removed because of browser performance limitations, you cannot lower the stringency any lower',
                buttons: {
                    ok: 'Proceed'
                    
                },
                fn: function (button) {
                	controlBar.setStringencySpinnerValue( savedDisplayStringency );
                }
            });*/
        	
        	controlBar.coexDisplaySettings.setStringency(requestedDisplayStringency);
        }        
        else {
            controlBar.coexDisplaySettings.setStringency(requestedDisplayStringency);
        }
    },

    setStringencySpinnerValue: function (stringency) {
        this.getComponent('stringencySpinner').setValue(stringency);
    },

    setSearchText: function(text) {
        this.getComponent('searchInCytoscapeBox').setValue( text );
    },

    setQueryGenesOnlyCheckBox: function (checked) {
        this.getComponent('queryGenesOnly').setValue(checked);
    },

    disableQueryGenesOnlyCheckBox: function (disabled) {
        this.getComponent('queryGenesOnly').setDisabled(disabled);
    }
});