Ext.namespace('Gemma.Metaheatmap');

Gemma.Metaheatmap.Utils = {};

Gemma.Metaheatmap.Utils.calculateGeneLabelColumnHeight = function (geneNames) {
	var initialHeight = 20;
	for (var i = 0; i < geneNames.length; i++) {
		initialHeight += geneNames[i].length * Gemma.MetaVisualizationConfig.cellHeight;
		initialHeight += Gemma.MetaVisualizationConfig.groupSeparatorHeight;
	}
	return initialHeight;
};

Gemma.Metaheatmap.Utils.shortenText = function (text, maxLength) {
	if (text.length <= maxLength) {return text;}			
	return text.substring (0, maxLength - 1) + "~";
};


Gemma.Metaheatmap.Utils.shortenTextPxl = function (text, maxPxlLength) {
	var pxlLength = CanvasTextFunctions.measure (null, 9, text);
	
	if (text.length <= maxLength) {return text;}			
	return text.substring (0, length - 1) + "~";
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
Gemma.Metaheatmap.Utils.getCanvasContext = function(canvas){
	// using !!! to turn into boolean, won't work in IE otherwise
	if (!!!document.createElement("canvas").getContext) { 
		canvas = $(window.G_vmlCanvasManager.initElement(canvas));
	}
	
	return canvas.getContext('2d');
};




Gemma.Metaheatmap.Utils.createSortByPropertyFunction = function ( property ) {
	return function ( a, b ) {
		if (typeof a[property] == "number") {
			return (a[property] - b[property]);
		} else {
			return ((a[property] < b[property]) ? -1 : ((a[property] > b[property]) ? 1 : 0));
		}
	};
};

// Format p value.
Gemma.Metaheatmap.Utils.formatPVal = function(p){
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
Gemma.Metaheatmap.Utils.formatPercent = function(n, d, round){
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


/**
 * Create a URL that can be used to query the system.
 * 
 * @param state should have format:
 * 	state.geneIds = array of gene ids that occur singly (not in a group): [7,8,9]
 *  state.geneGroupIds = array of db-backed gene group ids: [10,11,12]
 *  ^same for experiments^
 *  state.geneSort
 *  state.eeSort
 *  state.filters = list of filters applied, values listed should be filtered OUT (note this 
 *  	is the opposite heuristic as in viz) (done to minimize url length)
 *  state.taxonId
 * @return url string or null if error or nothing to link to
 */
Gemma.Metaheatmap.Utils.getBookmarkableLink = function(state) {
	if (!state) {
		return null;
	}
	var queryStart = document.URL.indexOf("?");
	var url = queryStart > -1 ? document.URL.substr(0, queryStart) : document.URL;
	url = url.replace('home','metaheatmap');
	url = url.replace('html#','html'); // added by IE sometimes
	
	var noGenes = true;
	var noExperiments = true;
	
	url += "?";
	if( typeof state.geneIds !== 'undefined' &&  state.geneIds !== null &&  state.geneIds.length !== 0){
		url += String.format("g={0}&", state.geneIds.join(","));
		noGenes=false;
	}		
	if (typeof state.geneGroupIds !== 'undefined' && state.geneGroupIds !== null && state.geneGroupIds.length !== 0) {
		url += String.format("gg={0}&", state.geneGroupIds.join(","));
		noGenes=false;
	}
	if( typeof state.eeIds !== 'undefined' &&  state.eeIds !== null &&  state.eeIds.length !== 0){
		url += String.format("e={0}&", state.eeIds.join(","));
		noExperiments = false;
	}	
	if (typeof state.eeGroupIds !== 'undefined' && state.eeGroupIds !== null && state.eeGroupIds.length !== 0) {
		url += String.format("eg={0}&", state.eeGroupIds.join(","));
		noExperiments = false;
	}	
	if (typeof state.experimentSessionGroupQueries !== 'undefined' && 
			!(state.experimentSessionGroupQueries.length === 1 && typeof state.experimentSessionGroupQueries[0] === 'undefined') && 
			state.experimentSessionGroupQueries !== null && 
			state.experimentSessionGroupQueries.length !== 0) {
		url += String.format("eq={0}&", state.experimentSessionGroupQueries.join(","));
		noExperiments = false;
	}
	if (typeof state.geneSessionGroupQueries !== 'undefined' && 
			!(state.geneSessionGroupQueries.length === 1 && typeof state.geneSessionGroupQueries[0] === 'undefined') && 
			state.geneSessionGroupQueries !== null && 
			state.geneSessionGroupQueries.length > 0) {
		url += String.format("gq={0}&", state.geneSessionGroupQueries.join(","));
		noGenes=false;
	}
	if (typeof state.geneSort !== 'undefined' && state.geneSort !== null && state.geneSort.length !== 0) {
		url += String.format("gs={0}&", state.geneSort);
	}
	if (typeof state.eeSort !== 'undefined' && state.eeSort !== null && state.eeSort.length !== 0) {
		url += String.format("es={0}&", state.eeSort);
	}
	if (typeof state.factorFilters !== 'undefined' && state.factorFilters !== null && state.factorFilters.length !== 0) {
		url += String.format("ff={0}&", state.factorFilters.join(','));
	}
	url += String.format("t={0}&", state.taxonId);

	// remove trailing '&'
	url = url.substring(0, url.length-1);

	if(noGenes || noExperiments){
		return null;
	}
	return url.replace('#',''); // added by IE sometimes
};
