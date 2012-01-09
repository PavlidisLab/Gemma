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
    return nodeDegree * 10;
    /*
     * //this should stay zero for the opacity use var
     * lowVis = 0; if (nodeDegree > 0.9989) { return lowVis +
     * 10; } else if (nodeDegree > 0.9899) { return lowVis +
     * 9; } else if (nodeDegree > 0.8999) { return lowVis +
     * 8; } else if (nodeDegree > 0.8499) { return lowVis +
     * 7; } else if (nodeDegree > 0.7999) { return lowVis +
     * 6; } else if (nodeDegree > 0.1999) { //this should be
     * bland colour return lowVis + 5; } else if (nodeDegree >
     * 0.1499) { return lowVis + 4; } else if (nodeDegree >
     * 0.0999) { return lowVis + 4; } else if (nodeDegree >
     * 0.0499) { return lowVis + 3; } else if (nodeDegree >
     * 0.0099) { return lowVis + 2; } else if (nodeDegree >
     * 0.0009) { return lowVis + 1; } else { return lowVis; }
     * 
     */
};