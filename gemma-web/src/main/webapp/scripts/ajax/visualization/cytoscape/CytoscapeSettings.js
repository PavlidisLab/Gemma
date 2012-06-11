Ext.namespace('Gemma');

Gemma.CytoscapeSettings = {
	
    backgroundColor: "#FFF7FB",

    // node stuff
    labelFontName: 'Arial',
    labelFontColor: "#252525",
    labelFontColorFade: "#BDBDBD",
    labelGlowStrength: 100,
    labelFontWeight: "bold",
    labelFontSize: 11,
    
    labelFontSizeBigger: 18,
    labelFontSizeBiggest: 25,

    labelYOffset: -20,

    labelHorizontalAnchor: "center",

    nodeColor: "#969696",
    nodeColorFade: "#FFF7FB",
    nodeSize: 25,

    nodeQueryColorTrue: "#E41A1C",
    nodeQueryColorFalse: "#6BAED6",

    // edge stuff
    supportColorBoth: "#CCCCCC",
    supportColorPositive: "#E66101",
    supportColorNegative: "#5E3C99",

    selectionGlowColor: "#0000FF",

    selectionGlowOpacity: 1,    
    
    zoomLevelBiggerFont: 0.7,    
    zoomLevelBiggestFont: 0.4,
    
    maxGeneIdsPerCoexVisQuery:200,
    
    //e.g. dark colour is >dark value and <moderate value, darkest is <=0.2
    //darkest : most specificity, lightest: least specificity
    nodeDegreeValue:{
    	lightest:0.7,
    	light:0.6,
    	moderate: 0.35,
    	dark:0.2
    },
    
    nodeDegreeColor: {
    	lightest:{
    		name: "Lowest",
    		value: "#DEDEDE"
    	},
    	light:{
    		name: "Low",
    		value: "#C9C9C9"
    	},
    	moderate:{
    		name: "Moderate",
    		value: "#737373"
    	},
    	dark:{
    		name: "High",
    		value: "#404040"
    	},
    	darkest:{
    		name: "Highest",
    		value: "#000000"
    	},    		
    	
    }
    

};

Gemma.CytoscapeSettings.visualStyleRegular= {
    global: {

        backgroundColor: Gemma.CytoscapeSettings.backgroundColor
    },
    nodes: {
        tooltipText: "Symbol:${id}<br/>Specificity:${nodeDegreeBin}<br/>NCBI Id:${ncbiId}<br/>",
        shape: "ELLIPSE",
        borderWidth: {
            discreteMapper: {
                attrName: "queryflag",
                entries: [{
                    attrValue: true,
                    value: 3
                }, {
                    attrValue: false,
                    value: 0
                }]
            }

        },

        size: {
            defaultValue: Gemma.CytoscapeSettings.nodeSize

        },

        labelFontName: Gemma.CytoscapeSettings.labelFontName,
        labelFontColor: Gemma.CytoscapeSettings.labelFontColor,
        labelGlowStrength: Gemma.CytoscapeSettings.labelGlowStrength,
        labelFontWeight: Gemma.CytoscapeSettings.labelFontWeight,

        labelYOffset: Gemma.CytoscapeSettings.labelYOffset,

        labelHorizontalAnchor: Gemma.CytoscapeSettings.labelHorizontalAnchor,
        borderColor: {
            discreteMapper: {
                attrName: "queryflag",
                entries: [{
                    attrValue: true,
                    value: Gemma.CytoscapeSettings.nodeQueryColorTrue
                }, {
                    attrValue: false,
                    value: Gemma.CytoscapeSettings.nodeQueryColorFalse
                }]
            }

        },

        color: Gemma.CytoscapeSettings.nodeColor,

        selectionGlowColor: Gemma.CytoscapeSettings.selectionGlowColor,

        selectionGlowOpacity: Gemma.CytoscapeSettings.selectionGlowOpacity
    },
    edges: {
        tooltipText: "Edge Nodes: ${target} to ${source}<br/>Positive Support:${positivesupport}<br/>Negative Support:${negativesupport}",
        width: {
            defaultValue: 1,
            continuousMapper: {
                attrName: "support",
                minValue: 1,
                maxValue: 6
            }

        },

        color: {

            discreteMapper: {
                attrName: "supportsign",
                entries: [{
                    attrValue: "both",
                    value: Gemma.CytoscapeSettings.supportColorBoth
                }, {
                    attrValue: "positive",
                    value: Gemma.CytoscapeSettings.supportColorPositive
                }, {
                    attrValue: "negative",
                    value: Gemma.CytoscapeSettings.supportColorNegative
                }]
            }
        },

        selectionGlowColor: Gemma.CytoscapeSettings.selectionGlowColor,

        selectionGlowOpacity: Gemma.CytoscapeSettings.selectionGlowOpacity
    }
};

Gemma.CytoscapeSettings.visualStyleNodeDegree= {
    global: {
        backgroundColor: Gemma.CytoscapeSettings.backgroundColor
    },
    nodes: {
        tooltipText: "Symbol:${id}<br/>Specificity:${nodeDegreeBin}<br/>NCBI Id:${ncbiId}<br/>",
        shape: "ELLIPSE",
        borderWidth: {
            discreteMapper: {
                attrName: "queryflag",
                entries: [{
                    attrValue: true,
                    value: 3
                }, {
                    attrValue: false,
                    value: 0
                }]
            }

        },

        size: {
            defaultValue: Gemma.CytoscapeSettings.nodeSize

        },

        labelFontColor: {
        	discreteMapper: {
                attrName: "nodeDegreeBin",
                entries: [
                          {attrValue: Gemma.CytoscapeSettings.nodeDegreeColor.lightest.name , value: Gemma.CytoscapeSettings.nodeDegreeColor.lightest.value},
                          {attrValue: Gemma.CytoscapeSettings.nodeDegreeColor.light.name , value:Gemma.CytoscapeSettings.nodeDegreeColor.light.value },
                          {attrValue: Gemma.CytoscapeSettings.nodeDegreeColor.moderate.name , value:Gemma.CytoscapeSettings.nodeDegreeColor.moderate.value },
                          {attrValue: Gemma.CytoscapeSettings.nodeDegreeColor.dark.name , value: Gemma.CytoscapeSettings.nodeDegreeColor.dark.value },
                          {attrValue: Gemma.CytoscapeSettings.nodeDegreeColor.darkest.name , value:Gemma.CytoscapeSettings.nodeDegreeColor.darkest.value }
                ]
            }
        },

        labelFontName: Gemma.CytoscapeSettings.labelFontName,

        labelGlowStrength: Gemma.CytoscapeSettings.labelGlowStrength,
        labelFontWeight: Gemma.CytoscapeSettings.labelFontWeight,

        labelYOffset: Gemma.CytoscapeSettings.labelYOffset,

        labelHorizontalAnchor: Gemma.CytoscapeSettings.labelHorizontalAnchor,

        borderColor: {
            discreteMapper: {
                attrName: "queryflag",
                entries: [{
                    attrValue: true,
                    value: Gemma.CytoscapeSettings.nodeQueryColorTrue
                }, {
                    attrValue: false,
                    value: Gemma.CytoscapeSettings.nodeQueryColorFalse
                }]
            }

        },
        color: {
        	
        	discreteMapper: {
                attrName: "nodeDegreeBin",
                entries: [
                          {attrValue: Gemma.CytoscapeSettings.nodeDegreeColor.lightest.name , value: Gemma.CytoscapeSettings.nodeDegreeColor.lightest.value},
                          {attrValue: Gemma.CytoscapeSettings.nodeDegreeColor.light.name , value:Gemma.CytoscapeSettings.nodeDegreeColor.light.value },
                          {attrValue: Gemma.CytoscapeSettings.nodeDegreeColor.moderate.name , value:Gemma.CytoscapeSettings.nodeDegreeColor.moderate.value },
                          {attrValue: Gemma.CytoscapeSettings.nodeDegreeColor.dark.name , value: Gemma.CytoscapeSettings.nodeDegreeColor.dark.value },
                          {attrValue: Gemma.CytoscapeSettings.nodeDegreeColor.darkest.name , value:Gemma.CytoscapeSettings.nodeDegreeColor.darkest.value }
                ]
            }
        },

        selectionGlowColor: Gemma.CytoscapeSettings.selectionGlowColor,

        selectionGlowOpacity: Gemma.CytoscapeSettings.selectionGlowOpacity
    },
    edges: {
        tooltipText: "Edge Nodes: ${target} to ${source}<br/>Positive Support:${positivesupport}<br/>Negative Support:${negativesupport}",
        width: {
            defaultValue: 1,
            continuousMapper: {
                attrName: "support",
                minValue: 1,
                maxValue: 6
            }

        },

        opacity: {

            customMapper: {
                functionName: "edgeOpacityMapper"
            }

        },

        color: {

            discreteMapper: {
                attrName: "supportsign",
                entries: [{
                    attrValue: "both",
                    value: Gemma.CytoscapeSettings.supportColorBoth
                }, {
                    attrValue: "positive",
                    value: Gemma.CytoscapeSettings.supportColorPositive
                }, {
                    attrValue: "negative",
                    value: Gemma.CytoscapeSettings.supportColorNegative
                }]
            }
        },

        selectionGlowColor: Gemma.CytoscapeSettings.selectionGlowColor,

        selectionGlowOpacity: Gemma.CytoscapeSettings.selectionGlowOpacity

    }
};

Gemma.CytoscapeSettings.forceDirectedLayoutCompressed = {
	
	name: "ForceDirected",
	options: {
		mass : 2,
		gravitation :-300,
		tension: 0.3,
		drag: 0.4,
		minDistance: 1,
		maxDistance: 10000,
		iterations: 400,
		maxTime: 30000
	}
	
	
};

Gemma.CytoscapeSettings.defaultForceDirectedLayout = {
	
	name: "ForceDirected"    	    	
	
};



