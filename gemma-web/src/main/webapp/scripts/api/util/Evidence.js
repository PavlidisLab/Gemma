Ext.namespace( 'Gemma' );

Gemma.Evidence = {}

function isRunningOutsideOfGemma() {
   var hostname = window.location.hostname;

   return (hostname.indexOf( 'msl.ubc.ca' ) < 0 && hostname.indexOf( 'chibi.ubc.ca' ) < 0 && hostname.indexOf( 'localhost' ) < 0);
};

Gemma.Evidence.convertToEvidenceError = function( validateEvidenceValueObject ) {
   var isWarning = false;
   var errorMessage = '';

   if ( validateEvidenceValueObject.userNotLoggedIn ) {
      isWarning = false;
      errorMessage = Gemma.HelpText.WidgetDefaults.PhenotypeAssociationForm.ErrorMessage.userNotLoggedIn;
   } else if ( validateEvidenceValueObject.accessDenied ) {
      isWarning = false;
      errorMessage = Gemma.HelpText.WidgetDefaults.PhenotypeAssociationForm.ErrorMessage.accessDenied;
   } else if ( validateEvidenceValueObject.lastUpdateDifferent ) {
      isWarning = false;
      errorMessage = Gemma.HelpText.WidgetDefaults.PhenotypeAssociationForm.ErrorMessage.lastUpdateDifferent;
   } else if ( validateEvidenceValueObject.evidenceNotFound ) {
      isWarning = false;
      errorMessage = Gemma.HelpText.WidgetDefaults.PhenotypeAssociationForm.ErrorMessage.evidenceNotFound;
   } else if ( validateEvidenceValueObject.pubmedIdInvalid ) {
      isWarning = false;
      errorMessage = Gemma.HelpText.WidgetDefaults.PhenotypeAssociationForm.ErrorMessage.pubmedIdInvalid;
   } else if ( validateEvidenceValueObject.sameGeneAndPhenotypesAnnotated ) {
      isWarning = true;
      errorMessage = Gemma.HelpText.WidgetDefaults.PhenotypeAssociationForm.ErrorMessage.sameGeneAndPhenotypesAnnotated;
   } else if ( validateEvidenceValueObject.sameGeneAnnotated ) {
      isWarning = true;
      errorMessage = Gemma.HelpText.WidgetDefaults.PhenotypeAssociationForm.ErrorMessage.sameGeneAnnotated;
   } else if ( validateEvidenceValueObject.sameGeneAndOnePhenotypeAnnotated ) {
      isWarning = true;
      errorMessage = Gemma.HelpText.WidgetDefaults.PhenotypeAssociationForm.ErrorMessage.sameGeneAndOnePhenotypeAnnotated;
   } else if ( validateEvidenceValueObject.sameGeneAndPhenotypeChildOrParentAnnotated ) {
      isWarning = true;
      errorMessage = Gemma.HelpText.WidgetDefaults.PhenotypeAssociationForm.ErrorMessage.sameGeneAndPhenotypeChildOrParentAnnotated;
   } else if ( validateEvidenceValueObject.sameEvidenceFound ) {
      isWarning = false;
      errorMessage = Gemma.HelpText.WidgetDefaults.PhenotypeAssociationForm.ErrorMessage.sameEvidenceFound;
   } else if ( validateEvidenceValueObject.descriptionInvalidSymbol ) {
      isWarning = false;
      errorMessage = Gemma.HelpText.WidgetDefaults.PhenotypeAssociationForm.ErrorMessage.descriptionInvalidSymbol;
   } else {
      isWarning = false;
      errorMessage = Gemma.HelpText.WidgetDefaults.PhenotypeAssociationForm.ErrorMessage.errorUnknown;
   }

   return {
      isWarning : isWarning,
      errorMessage : errorMessage
   };
};
