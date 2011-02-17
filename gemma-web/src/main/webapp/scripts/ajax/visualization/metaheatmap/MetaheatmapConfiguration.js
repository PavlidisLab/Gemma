Ext.namespace('Gemma');

Gemma.MetaVisualizationConfig = {};
Gemma.MetaVisualizationConfig.cellWidth = 10;
Gemma.MetaVisualizationConfig.cellHeight = 10;
Gemma.MetaVisualizationConfig.groupSeparatorHeight = 4;
Gemma.MetaVisualizationConfig.columnSeparatorWidth = 1;
Gemma.MetaVisualizationConfig.groupSeparatorWidth = 4;
Gemma.MetaVisualizationConfig.geneLabelFontSize = 9;

Gemma.MetaVisualizationConfig.cellHighlightColor = 'yellow';
Gemma.MetaVisualizationConfig.geneLabelHighlightColor = 'rgb(255,140,0)';
Gemma.MetaVisualizationConfig.analysisLabelHighlightColor = 'rgb(255,140,0)';
Gemma.MetaVisualizationConfig.baselineColor = '';
Gemma.MetaVisualizationConfig.miniPieColor = 'rgb(95,158,160)';

Gemma.MetaVisualizationConfig.basicColourRange = new org.systemsbiology.visualization.DiscreteColorRange(
		20,
		{ min: -1, max: 1},            										
		{ maxColor: {r:255, g:215, b:0, a:1},
		  minColor: {r:255, g:255, b:0, a:1} ,
		  emptyDataColor: {r:100, g:100, b:100, a:0.8},
		  passThroughBlack: true
		});

Gemma.MetaVisualizationConfig.contrastsColourRange = new org.systemsbiology.visualization.DiscreteColorRange(
		20,
			{ min: -1, max: 1},
		{ maxColor: {r:0, g:200, b:255, a:1},
			  minColor: {r:255, g:255, b:0, a:1} ,
			  emptyDataColor: {r:100, g:100, b:100, a:0.8},
			  passThroughBlack: true
	    });
