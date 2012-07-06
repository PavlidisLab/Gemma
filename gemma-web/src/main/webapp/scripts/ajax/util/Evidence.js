Ext.namespace('Gemma');

Gemma.isRunningOutsideOfGemma = function() {
	var hostname = window.location.hostname;

	return hostname.indexOf('chibi.ubc.ca') < 0 && hostname.indexOf('localhost') < 0;
}

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
		},
		'IEP': {
			displayText: Gemma.EvidenceCodes.iepText,
			tooltipText: Gemma.EvidenceCodes.iepTT
		},
		'IMP': {
			displayText: Gemma.EvidenceCodes.impText,
			tooltipText: Gemma.EvidenceCodes.impTT
		},
		'IGI': {
			displayText: Gemma.EvidenceCodes.igiText,
			tooltipText: Gemma.EvidenceCodes.igiTT
		},
		'IED': {
			displayText: Gemma.EvidenceCodes.iedText,
			tooltipText: Gemma.EvidenceCodes.iedTT
		},
		'IAGP': {
			displayText: Gemma.EvidenceCodes.iagpText,
			tooltipText: Gemma.EvidenceCodes.iagpTT
		},
		'IPM': {
			displayText: Gemma.EvidenceCodes.ipmText,
			tooltipText: Gemma.EvidenceCodes.ipmTT
		},
		'QTM': {
			displayText: Gemma.EvidenceCodes.qtmText,
			tooltipText: Gemma.EvidenceCodes.qtmTT
		}
	};
	
	return EVIDENCE_TEXT[evidenceCode];
};

Gemma.convertToEvidenceError = function(validateEvidenceValueObject) {
	var isWarning = false;
	var errorMessage = '';

	if (validateEvidenceValueObject.userNotLoggedIn) {
		isWarning = false;
		errorMessage = Gemma.HelpText.WidgetDefaults.PhenotypeAssociationForm.ErrorMessage.userNotLoggedIn;
	} else if (validateEvidenceValueObject.accessDenied) {
		isWarning = false;
		errorMessage = Gemma.HelpText.WidgetDefaults.PhenotypeAssociationForm.ErrorMessage.accessDenied;
	} else if (validateEvidenceValueObject.lastUpdateDifferent) {
		isWarning = false;
		errorMessage = Gemma.HelpText.WidgetDefaults.PhenotypeAssociationForm.ErrorMessage.lastUpdateDifferent;
	} else if (validateEvidenceValueObject.evidenceNotFound) {
		isWarning = false;
		errorMessage = Gemma.HelpText.WidgetDefaults.PhenotypeAssociationForm.ErrorMessage.evidenceNotFound;
	} else if (validateEvidenceValueObject.pubmedIdInvalid) {
		isWarning = false;
		errorMessage = Gemma.HelpText.WidgetDefaults.PhenotypeAssociationForm.ErrorMessage.pubmedIdInvalid;
	} else if (validateEvidenceValueObject.sameGeneAndPhenotypesAnnotated) {
		isWarning = true;
		errorMessage = Gemma.HelpText.WidgetDefaults.PhenotypeAssociationForm.ErrorMessage.sameGeneAndPhenotypesAnnotated;
	} else if (validateEvidenceValueObject.sameGeneAnnotated) {
		isWarning = true;
		errorMessage = Gemma.HelpText.WidgetDefaults.PhenotypeAssociationForm.ErrorMessage.sameGeneAnnotated;
	} else if (validateEvidenceValueObject.sameGeneAndOnePhenotypeAnnotated) {
		isWarning = true;
		errorMessage = Gemma.HelpText.WidgetDefaults.PhenotypeAssociationForm.ErrorMessage.sameGeneAndOnePhenotypeAnnotated;
	} else if (validateEvidenceValueObject.sameGeneAndPhenotypeChildOrParentAnnotated) {
		isWarning = true;
		errorMessage = Gemma.HelpText.WidgetDefaults.PhenotypeAssociationForm.ErrorMessage.sameGeneAndPhenotypeChildOrParentAnnotated;
	} else if (validateEvidenceValueObject.sameEvidenceFound) {
		isWarning = false;
		errorMessage = Gemma.HelpText.WidgetDefaults.PhenotypeAssociationForm.ErrorMessage.sameEvidenceFound;
	} else {
		isWarning = false;
		errorMessage = Gemma.HelpText.WidgetDefaults.PhenotypeAssociationForm.ErrorMessage.errorUnknown;
	}
			
	return {
		isWarning: isWarning,
		errorMessage: errorMessage
	};
};
