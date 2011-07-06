Ext.namespace('Gemma');

Gemma.MetaVisualizationUtils = {};

Gemma.MetaVisualizationUtils.calculateGeneLabelColumnHeight = function (geneNames) {
	var initialHeight = 20;
	for (var i = 0; i < geneNames.length; i++) {
		initialHeight += geneNames[i].length * Gemma.MetaVisualizationConfig.cellHeight;
		initialHeight += Gemma.MetaVisualizationConfig.groupSeparatorHeight;
	}
	return initialHeight;
};

Gemma.MetaVisualizationUtils.shortenText = function (text, maxLength) {
	if (text.length <= maxLength) {return text;}			
	return text.substring(0,maxLength - 3) + "...";
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

// Format p value.
Gemma.MetaVisualizationUtils.formatPVal = function(p){
	if (p === null) {
		return '-';
	}
	if (p < 0.001) {
		return sprintf("%.3e", p);
	}
	else {
		return sprintf("%.3f", p);
	}
};

/**
 * Calculates percentage and formats with 1 decimal point
 * @param {Object} n numerator
 * @param {Object} d denominator
 * @param {boolean} round true if percentages should be rounded to integers 
 * @return string (might contain non-digit characters)
 */
Gemma.MetaVisualizationUtils.formatPercent = function(n, d, round){
	if (n === 0) {
		return "0";
	}
	if (d === 0 || d === null || n === null) {
		return "-";
	}
	var  p = n/d*100;
	if(round){
		if (p < 1) {
		return "< 1";
	}
	else {
		return Math.round(p);
	}
	}else{
		if (p < 0.01) {
		return "< 0.01";
	}
	else {
		return sprintf("%.2f", p);
	}
	}
	
};
