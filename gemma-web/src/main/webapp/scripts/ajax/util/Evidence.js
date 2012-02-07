Ext.namespace('Gemma');

// It returns display and tooltip text for the evidence the given evidence code represents.
Gemma.decodeEvidenceCode = function(evidenceCode) {
	var EVIDENCE_TEXT = {
		'EXP': {
			displayText: Gemma.EvidenceCodes.expText,
			tooltipText: Gemma.EvidenceCodes.expTT
		},
		'IC': {
			displayText: Gemma.EvidenceCodes.icText,
			tooltipText: Gemma.EvidenceCodes.icTT
		},
		'TAS': {
			displayText: Gemma.EvidenceCodes.tasText,
			tooltipText: Gemma.EvidenceCodes.tasTT
		}
	};
	
	return EVIDENCE_TEXT[evidenceCode];
};
