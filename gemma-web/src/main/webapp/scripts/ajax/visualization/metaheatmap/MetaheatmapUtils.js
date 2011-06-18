Ext.namespace('Gemma');

Gemma.MetaVisualizationUtils = {};

Gemma.MetaVisualizationUtils.calculateColumnHeight = function (geneNames) {
	var initialHeight = 20;
	for (var i = 0; i < geneNames.length; i++) {
		initialHeight += geneNames[i].length * Gemma.MetaVisualizationConfig.cellHeight;
		initialHeight += Gemma.MetaVisualizationConfig.groupSeparatorHeight;
	}
	return initialHeight;
};

Gemma.MetaVisualizationUtils.shortenText = function (text, maxLength) {
	var shortenedText = "";
	
	
	return shortenedText;
};
/**
 * Function: (private) constructCanvas
 * 
 * Initializes a canvas. When the browser is IE, we make use of excanvas.
 * 
 * from Heatmap.js
 * 
 * Parameters: canvas
 * 
 * Returns: ctx
 */
Gemma.MetaVisualizationUtils.getCanvasContext = function(canvas){
	// using !!! to turn into boolean, won't work in IE otherwise
	if (!!!document.createElement("canvas").getContext) { 
		canvas = $(window.G_vmlCanvasManager.initElement(canvas));
	}
	
	return canvas.getContext('2d');
};