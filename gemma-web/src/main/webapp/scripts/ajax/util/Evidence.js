Ext.namespace('Gemma');

// This function returns display and tooltip text for the evidence the given evidence code represents.
Gemma.decodeEvidenceCode = function(evidenceCode) {
	var displayText = "";
	var tooltipText = "";
	
	switch (evidenceCode) {
		case 'EXP' :
        	displayText = Gemma.EvidenceCodes.expText;
        	tooltipText = Gemma.EvidenceCodes.expTT;
			break;
		case 'IC' :
        	displayText = Gemma.EvidenceCodes.icText;
        	tooltipText = Gemma.EvidenceCodes.icTT;
			break;
		case 'TAS' :
        	displayText = Gemma.EvidenceCodes.tasText;
        	tooltipText = Gemma.EvidenceCodes.tasTT;
			break;
	}
	
	return {
		displayText: displayText,
		tooltipText: tooltipText
	}
};