// Set up inheritance structures for value objects
// see http://directwebremoting.org/dwr/documentation/server/configuration/dwrxml/converters/bean.html#interfacesAndAbstractClasses

SessionBoundExpressionExperimentSetValueObject.prototype = new ExpressionExperimentSetValueObject();
SessionBoundExpressionExperimentSetValueObject.prototype.constructor = SessionBoundExpressionExperimentSetValueObject;

FreeTextExpressionExperimentResultsValueObject.prototype = new SessionBoundExpressionExperimentSetValueObject();
FreeTextExpressionExperimentResultsValueObject.prototype.constructor = FreeTextExpressionExperimentResultsValueObject;

DatabaseBackedGeneSetValueObject.prototype = new GeneSetValueObject();
DatabaseBackedGeneSetValueObject.prototype.constructor = DatabaseBackedGeneSetValueObject;

SessionBoundGeneSetValueObject.prototype = new GeneSetValueObject();
SessionBoundGeneSetValueObject.prototype.constructor = SessionBoundGeneSetValueObject;

FreeTextGeneResultsValueObject.prototype = new SessionBoundGeneSetValueObject();
FreeTextGeneResultsValueObject.prototype.constructor = FreeTextGeneResultsValueObject;

GOGroupValueObject.prototype = new SessionBoundGeneSetValueObject();
GOGroupValueObject.prototype.constructor = GOGroupValueObject;

PhenotypeGroupValueObject.prototype = new SessionBoundGeneSetValueObject();
PhenotypeGroupValueObject.prototype.constructor = PhenotypeGroupValueObject;

ExperimentalEvidenceValueObject.prototype = new EvidenceValueObject();
ExperimentalEvidenceValueObject.prototype.constructor = ExperimentalEvidenceValueObject;

LiteratureEvidenceValueObject.prototype = new EvidenceValueObject();
LiteratureEvidenceValueObject.prototype.constructor = LiteratureEvidenceValueObject;
