Ext.namespace('Gemma');

Gemma.CytoscapePanelUtil = {};

Gemma.CytoscapePanelUtil.ttSubstring= function (tString) {

    if (!tString) {
        return null;
    }

    var maxLength = 60;

    if (tString.length > maxLength) {
        return tString.substr(0, maxLength) + "...";
    }

    return tString;
};


// inputs should be two node degrees between 0 and 1, if
// null(missing data) return 1 as nodes/edges with 1 fade
// into the background
Gemma.CytoscapePanelUtil.getMaxWithNull = function (n1, n2) {

    // missing data check
    if (n1 == null || n2 == null) {
        return 1;
    }

    return Math.max(n1, n2);

};

Gemma.CytoscapePanelUtil.decimalPlaceRounder = function (number) {

    if (number == null) {
        return null;
    }
    return Ext.util.Format.round(number, 4);

};

Gemma.CytoscapePanelUtil.nodeDegreeBinMapper = function (nodeDegree) {

	 // no data for some genes
	 if (nodeDegree == null) {
	     return null;
	 }	

	 
	 if (nodeDegree > 0.6) {
	     return Gemma.CytoscapeSettings.nodeDegreeColor.lightest.name;
	 } else if (nodeDegree > 0.4) {
		 return Gemma.CytoscapeSettings.nodeDegreeColor.light.name;
	 } else if (nodeDegree > 0.2) {
		 return Gemma.CytoscapeSettings.nodeDegreeColor.dark.name;
	 } else  {
		 return Gemma.CytoscapeSettings.nodeDegreeColor.darkest.name;
	 } 
     
};

Gemma.CytoscapePanelUtil.restrictResultsStringency = function (displayStringency) {
	
	 if (displayStringency > 5) {
         return displayStringency - Math.round(displayStringency / 4);
     }
	 
	 return 2;
	
	
};