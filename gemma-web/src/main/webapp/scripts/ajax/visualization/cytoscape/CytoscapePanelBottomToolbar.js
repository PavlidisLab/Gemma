Ext.namespace('Gemma');

Gemma.CytoscapeBottomToolbar = Ext.extend( Ext.Toolbar, {

    initComponent: function () {
        Ext.apply(this, {
            cytoscapePanel : this.cytoscapePanel,

            hidden: true,
            items: [
                {
                    xtype: 'tbtext',
                    text: '',
                    itemId: 'bbarStatus'
                },
                {
                    xtype: 'button',
                    itemId: 'graphSizeMenu',
                    text: '<b>Graph Size</b>',
                    menu: {
                        xtype: 'menu',
                        itemId: 'sizeMenu'
                    }
                },
                {
                    xtype: 'label',
                    id: 'tooltipMenuNotEnabled',
                    html: '&nbsp&nbsp<img ext:qtip="' + Gemma.HelpText.WidgetDefaults.CytoscapePanel.graphSizeMenuTT2 + '" src="/Gemma/images/icons/question_blue.png"/>&nbsp',
                    height: 15
                },
                {
                    xtype: 'label',
                    id: 'tooltipMenuEnabled',
                    html: '&nbsp&nbsp<img ext:qtip="' + Gemma.HelpText.WidgetDefaults.CytoscapePanel.graphSizeMenuTT + '" src="/Gemma/images/icons/question_blue.png"/>&nbsp',
                    height: 15
                }
            ]
        });
        Gemma.CytoscapeBottomToolbar.superclass.initComponent.call(this);
    },

    /**
     * Updates bottom message bar (graph size options, text, help)
     */
    showMessageBar: function (coexpressionGraphData, usedTrimStringency, showGraphSizeMenu) {
        var toolbar = this;
        var graphSizeButton = this.getComponent('graphSizeMenu');
        var graphSizeMenu = graphSizeButton.menu;
        graphSizeMenu.removeAll();
        toolbar.status = this.getComponent('bbarStatus');

        var cytoscapePanel = this.cytoscapePanel;

        function enableTooltipMenu() {
            toolbar.getComponent('tooltipMenuEnabled').setVisible(true);
            toolbar.getComponent('tooltipMenuNotEnabled').setVisible(false);
        }

        function disableTooltipMenu() {
            toolbar.getComponent('tooltipMenuEnabled').setVisible(false);
            toolbar.getComponent('tooltipMenuNotEnabled').setVisible(true);
        }

        function changeGraph(graph) {
            return function () {
                graphSizeButton.setText(graph.label);
                cytoscapePanel.changeGraph(graph.graph);
            };
        }

        if (showGraphSizeMenu) {
            // Message bar construction
            toolbar.status.setText("Edges not involving query genes have been trimmed at stringency: ");

//            // Button menu text
//            if (usedTrimStringency === 0) { /* TODO: Why 0? Special value? */
//                graphSizeButton.setText("No Trimming ");
//            } else {
//                graphSizeButton.setText(usedTrimStringency + " ");
//            }

            enableTooltipMenu();

            // Menu
            var graphOptions = coexpressionGraphData.getGraphData();
            for (var i = 0, len = graphOptions.length; i < len; i++) {
                var graph = graphOptions[i];
                graphSizeMenu.addMenuItem({
                    text: graph.label,
                    handler:changeGraph(graph),
                    group:'graphTrimOption',
                    checked: i === len-1 ? true : false /* Choose the smallest graph option */
                });
            }
            graphSizeButton.setText(graphOptions[graphOptions.length-1].label);
        } else {
            toolbar.status.setText("Edges not involving query genes have been trimmed at stringency: " + usedTrimStringency);
            disableTooltipMenu();
            if (graphSizeButton.isVisible()) {
                graphSizeButton.hide();
            }
        }

        toolbar.show();
        this.doLayout();
    },

    onRender: function () {
        Gemma.CytoscapeBottomToolbar.superclass.onRender.apply(this, arguments);
    }

});

/*
items: [
    {
        itemId: 'graphSizeLarge',
        text: 'Large',
        handler: function () {
            this.changeGraphSize("large");
        }, scope : this.cytoscapePanel
    },
    {
        itemId: 'graphSizeMedium',
        text: 'Medium',
        handler: function() {
            this.changeGraphSize("medium");
        },
        scope: this.cytoscapePanel
    },
    {
        itemId: 'graphSizeSmall',
        text: 'Small',
        handler: function() {
            this.changeGraphSize('small');
        },
        scope: this.cytoscapePanel
    }]
*/

/*
 "No Trimming (" + this.coexpressionGraphData.getGraphData('large').geneResults.length + " edges)");
 */
