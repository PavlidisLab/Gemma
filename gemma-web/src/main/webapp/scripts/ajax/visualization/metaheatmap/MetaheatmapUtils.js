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