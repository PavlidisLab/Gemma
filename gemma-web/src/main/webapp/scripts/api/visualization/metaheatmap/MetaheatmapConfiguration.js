Ext.namespace( 'Gemma.Metaheatmap' );

Ext.namespace( 'Gemma.Constants' );

Gemma.Constants.DifferentialExpressionQvalueThreshold = 0.05;

Gemma.Metaheatmap.Config = {};

Gemma.Metaheatmap.Config.USE_GENE_COUNTS_FOR_ENRICHMENT = true;

// Sizes
Gemma.Metaheatmap.Config.cellWidth = 14;
Gemma.Metaheatmap.Config.cellHeight = 10;
Gemma.Metaheatmap.Config.groupSeparatorHeight = 4;
Gemma.Metaheatmap.Config.columnSeparatorWidth = 1;
Gemma.Metaheatmap.Config.groupSeparatorWidth = 4;
Gemma.Metaheatmap.Config.geneLabelFontSize = 9;
Gemma.Metaheatmap.Config.columnLabelFontSize = 9;

Gemma.Metaheatmap.Config.ControlPanel = {};
Gemma.Metaheatmap.Config.ControlPanel.width = 200;

Gemma.Metaheatmap.Config.labelAngle = 315.0;
Gemma.Metaheatmap.Config.labelBaseYCoor = 196;
Gemma.Metaheatmap.Config.columnLabelHeight = 210;

Gemma.Metaheatmap.Config.minAppHeight = 600;
Gemma.Metaheatmap.Config.minAppWidth = 600;
Gemma.Metaheatmap.Config.windowPadding = 50;
Gemma.Metaheatmap.Config.toolPanelWidth = 300;

Gemma.Metaheatmap.Config.labelExtraSpace = Math.floor( Gemma.Metaheatmap.Config.labelBaseYCoor
   / Math.tan( (360 - Gemma.Metaheatmap.Config.labelAngle) * Math.PI / 180 ) );

// Colors
Gemma.Metaheatmap.Config.cellHighlightColor = 'red';
Gemma.Metaheatmap.Config.defaultLabelColor = 'black';
Gemma.Metaheatmap.Config.geneLabelHighlightColor = 'red';
Gemma.Metaheatmap.Config.rowHighlightColor = 'pink';
Gemma.Metaheatmap.Config.rowCellSelectColor = 'pink';
Gemma.Metaheatmap.Config.columnHighlightColor = 'pink';
Gemma.Metaheatmap.Config.analysisLabelHighlightColor = 'rgb(255,140,0)';
Gemma.Metaheatmap.Config.baselineFactorValueColor = 'rgb(128, 0, 0)';
Gemma.Metaheatmap.Config.factorValueDefaultColor = 'rgb(0,0,200)';
Gemma.Metaheatmap.Config.analysisLabelBackgroundColor1 = 'rgba(10,100,10, 0.1)';
Gemma.Metaheatmap.Config.analysisLabelBackgroundColor2 = 'rgba(10,100,10, 0.05)';
Gemma.Metaheatmap.Config.columnExpandButtonColor = 'rgba(10,100,10, 0.8)';
Gemma.Metaheatmap.Config.columnExpandButtonHighlightColor = '';

Gemma.Metaheatmap.Config.miniPieColor = 'rgb(95,158,160)';
Gemma.Metaheatmap.Config.miniPieColorInvalid = 'rgb(192,192,192)';

Gemma.Metaheatmap.Config.basicColourRange = new org.systemsbiology.visualization.DiscreteColorRange( 20, {
   min : 0,
   max : 10
}, {
   maxColor : {
      r : 197,
      g : 27,
      b : 138,
      a : 1
   },
   minColor : {
      r : 255,
      g : 255,
      b : 255,
      a : 1
   },
   emptyDataColor : {
      r : 100,
      g : 100,
      b : 100,
      a : 0.8
   },
   passThroughBlack : false
} );

Gemma.Metaheatmap.Config.contrastsColourRange = new org.systemsbiology.visualization.DiscreteColorRange( 20, {
   min : -3,
   max : 3
}, {
   maxColor : {
      r : 255,
      g : 215,
      b : 0,
      a : 1
   },
   minColor : {
      r : 70,
      g : 130,
      b : 180,
      a : 1
   },
   emptyDataColor : {
      r : 100,
      g : 100,
      b : 100,
      a : 0.8
   },
   passThroughBlack : true
} );

Gemma.Metaheatmap.Config.FoldChangeColorScale = {
   "5" : "rgb(142, 1, 82)",
   "4" : "rgb(197, 27, 125)",
   "3" : "rgb(222, 119, 174)",
   "2" : "rgb(241, 182, 218)",
   "1" : "rgb(253, 224, 239)",
   "0" : "rgb(247, 247, 247)",
   "-1" : "rgb(230, 245, 208)",
   "-2" : "rgb(184, 225, 134)",
   "-3" : "rgb(127, 188, 65)",
   "-4" : "rgb(77, 146, 33)",
   "-5" : "rgb(39, 100, 25)"
};

Gemma.Metaheatmap.Config.ColourLegendSettings = {
   discreteColorRangeObject : Gemma.Metaheatmap.Config.basicColourRange,
   discreteColorRangeObject2 : Gemma.Metaheatmap.Config.contrastsColourRange,
   cellHeight : 14,
   cellWidth : 14,
   colorValues : [ [ null, "No Data" ], [ 0, "1~0.5" ], [ 0.1, "0.5~0.25" ], [ 0.2, "0.1" ], [ 0.3, "0.05" ],
                  [ 0.4, "0.01" ], [ 0.6, "0.001" ], [ 0.8, "0.0001" ], [ 0.9, "0.00001" ], [ 1, "< 0.00001" ] ],
   colorValues2 : [ [ null, "No Data" ], [ 3, "Up" ], [ -3, "Down" ] ],
   vertical : true,
   canvasId : 'canvas1',
   canvasId2 : 'canvas12',
   legendTitle : 'q-value',
   legendTitle2 : 'direction',
   textWidthMax : 80,
   textOffset : 1,
   fontSize : 12,
   constrain : true
};
