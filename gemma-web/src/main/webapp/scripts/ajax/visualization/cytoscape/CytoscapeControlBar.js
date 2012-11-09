/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
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

Gemma.CytoscapeControlBar = Ext.extend(Ext.Toolbar, {

    initComponent: function () {

        this.isLayoutCompressed = false;
        this.isNodeLabelsVisible = true;
        this.isNodeDegreeEmphasis = true;

        this.visualOptionsMenu = new Ext.menu.Menu({
            items: [{
                itemId: 'refreshLayoutButton',
                text: Gemma.HelpText.WidgetDefaults.CytoscapePanel.refreshLayoutText,                
                handler: function () {
                    this.display.refreshLayout();
                },
                scope: this
            }, {
                itemId: 'compressGraphButton',
                text: Gemma.HelpText.WidgetDefaults.CytoscapePanel.compressGraphText,                
                handler: function () {

                    this.isLayoutCompressed = this.isLayoutCompressed ? false : true;

                    this.display.compressGraph(this.isLayoutCompressed);

                    this.visualOptionsMenu.getComponent('compressGraphButton').setText(this.isLayoutCompressed ? Gemma.HelpText.WidgetDefaults.CytoscapePanel.unCompressGraphText : Gemma.HelpText.WidgetDefaults.CytoscapePanel.compressGraphText);


                },
                scope: this
            }, {
                itemId: 'nodeLabelsButton',
                text: Gemma.HelpText.WidgetDefaults.CytoscapePanel.noNodeLabelsText,                
                handler: function () {

                    this.isNodeLabelsVisible = this.isNodeLabelsVisible ? false : true;
                    this.display.toggleNodeLabels(this.isNodeLabelsVisible);
                    this.visualOptionsMenu.getComponent('nodeLabelsButton').setText(this.isNodeLabelsVisible ? Gemma.HelpText.WidgetDefaults.CytoscapePanel.noNodeLabelsText : Gemma.HelpText.WidgetDefaults.CytoscapePanel.nodeLabelsText);

                },
                scope: this
            }]
        });

        this.actionsMenu = new Ext.menu.Menu({
            items: [{
                itemId: 'extendSelectedNodesButton',
                text: Gemma.HelpText.WidgetDefaults.CytoscapePanel.extendNodeText,                
                disabled: true,
                handler: function () {
                    this.display.extendSelectedNodesHandler();
                },
                scope: this
            }, {
                itemId: 'searchWithSelectedNodesButton',
                text: Gemma.HelpText.WidgetDefaults.CytoscapePanel.searchWithSelectedText,                
                disabled: true,
                handler: function () {
                    this.display.reRunSearchWithSelectedNodesHandler();
                },
                scope: this
            }, {
                itemId: 'applyGeneListOverlayButton',
                text: Gemma.HelpText.WidgetDefaults.CytoscapePanel.applyGeneListOverlayText,                                
                handler: function () {   
                	if (!this.geneSetOverlayPicker){
                		this.geneSetOverlayPicker = new Gemma.GeneSetOverlayPicker({
                			display: this.display,
                			taxonId: this.taxonId
                		});
                		this.relayEvents(this.geneSetOverlayPicker, ['nodesMatched']);
                		
                		
                	}
                	this.geneSetOverlayPicker.reactivateOverlayPicker();
                },
                scope: this
            }, {
                itemId: 'clearGeneListOverlayButton',                
                text: Gemma.HelpText.WidgetDefaults.CytoscapePanel.clearGeneListOverlayText,
                hidden: true,
                handler: function () {
                	this.display.clearGeneListOverlay();
                	this.actionsMenu.getComponent('clearGeneListOverlayButton').hide();
                },
                scope: this
            }]
        });

        this.actionsButton = new Ext.Button({
            text: '<b>Actions</b>',
            itemId: 'actionsButton',
            menu: this.actionsMenu
        });

        Ext.apply(this, {

            items: [{
                xtype: 'tbtext',
                text: 'Stringency:'
            }, {
                xtype: 'tbspacer'
            }, {
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
                value: 2,
                width: 60,
                enableKeyEvents: true,
                listeners : {
					"keyup" : {
						fn : function(){
							
							var spinnerValue = this.getComponent('stringencySpinner').getValue();
							
							if (Ext.isNumber(spinnerValue) && spinnerValue>1){							
								this.display.stringencyChange(spinnerValue);
							}
						},
						scope : this,																											
						delay : 500													
					}
				}

            }, {
                xtype: 'label',
                html: '&nbsp&nbsp<img ext:qtip="' + Gemma.HelpText.WidgetDefaults.CytoscapePanel.stringencySpinnerTT + '" src="/Gemma/images/icons/question_blue.png"/>',
                height: 15
            },' ',' ',{
				xtype : 'textfield',
				ref: 'searchInCytoscapeBox',				
				tabIndex : 1,
				enableKeyEvents : true,
				emptyText : 'Find gene in results',
				listeners : {
					"keyup" : {
						fn : this.searchForText,
						scope : this,
						delay : 500
					}
				}
			},' ',' ',
            {
                xtype: 'checkbox',								                
                itemId: 'queryGenesOnly',
                boxLabel: 'Query Genes Only',
                handler: function (){
                	this.display.filterQueryGenesOnly();
                },
                checked: false,								                
                scope: this
            }, '->', '-',
            {
                xtype: 'button',
                text: '<b>Help</b>',
                tooltip: Gemma.HelpText.WidgetDefaults.CytoscapePanel.widgetHelpTT,
	      		handler : function() {
	      		  window.open('http://gemma-chibi-doc.sites.olt.ubc.ca/documentation/search-page/#GemmaQuickGuide-CoexpressionView'); 
	      	  	},
	      	  	scope	: this		      	 
            }, '->', '-',
            {
                xtype: 'button',
                text: '<b>Save As</b>',
                menu: new Ext.menu.Menu({
                    items: [{
                        text: 'Save as PNG',
                        
                        handler: function () {
                            this.display.exportPNG();
                        },
                        scope: this
                    }, {
                        text: 'Save as GraphML',
                        
                        handler: function () {
                            this.display.exportGraphML();
                        },
                        scope: this
                    }, {
                        text: 'Save as XGMML',
                        
                        handler: function () {
                            this.display.exportXGMML();
                        },
                        scope: this
                    }, {
                        text: 'Save as SIF',
                        
                        handler: function () {
                            this.display.exportSIF();
                        },
                        scope: this
                    }, {
                        text: 'Save as SVG',
                        
                        handler: function () {
                            this.display.exportSVG();
                        },
                        scope: this
                    }]
                })
            }, '->', '-',
            {
                xtype: 'button',
                text: '<b>Visual Options</b>',
                menu: this.visualOptionsMenu
            }, '->', '-', this.actionsButton, '->', '-',
            {
                xtype: 'button',
                itemId: 'nodeDegreeEmphasis',
                ref: 'nodeDegreeEmphasis',
                text: '<b>' + Gemma.HelpText.WidgetDefaults.CytoscapePanel.nodeDegreeEmphasisText + '</b>',
                enableToggle: 'true',
                pressed: 'true',
                handler: function () {
                    this.display.nodeDegreeEmphasis(this.getComponent('nodeDegreeEmphasis').pressed);
                },
                scope: this
            }, {
                xtype: 'label',
                html: '&nbsp&nbsp<img ext:qtip="' + Gemma.HelpText.WidgetDefaults.CytoscapePanel.nodeDegreeEmphasisTT + '" src="/Gemma/images/icons/question_blue.png"/>&nbsp',
                height: 15
            }]

        });
        
        


        Gemma.CytoscapeControlBar.superclass.initComponent.apply(this, arguments);
       
        
        this.on('nodesMatched', function () {
        	
        	this.actionsMenu.getComponent('clearGeneListOverlayButton').show();
        	
        }, this);

        this.getComponent('stringencySpinner').addListener('spin', function (field, e) {
            this.display.stringencyChange(this.getComponent('stringencySpinner').getValue());
        }, this);

    },

    updateActionsButtons: function (isEnabled) {
        if (isEnabled) {
            this.actionsMenu.getComponent('extendSelectedNodesButton').setDisabled(false);
            this.actionsMenu.getComponent('searchWithSelectedNodesButton').setDisabled(false);
        } else {
            this.actionsMenu.getComponent('extendSelectedNodesButton').setDisabled(true);
            this.actionsMenu.getComponent('searchWithSelectedNodesButton').setDisabled(true);
        }
    },

    setStringency: function (stringency) {
        this.getComponent('stringencySpinner').setValue(stringency);
    },
    searchForText : function(button, keyev) {
		this.display.selectSearchMatchesFromControlBar(this.searchInCytoscapeBox.getValue());
		
	}

});