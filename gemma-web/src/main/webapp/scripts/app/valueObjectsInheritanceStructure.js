/**
 * 
 * Set up inheritance structures for value objects see
 * http://directwebremoting.org/dwr/documentation/server/configuration/dwrxml/converters/bean.html#interfacesAndAbstractClasses
 * 
 * @version $Id$
 * 
 */

if ( typeof ExpressionExperimentSetValueObject === 'undefined' ) {
   var ExpressionExperimentSetValueObject = function() {
   };
}
if ( typeof SessionBoundExpressionExperimentSetValueObject === 'undefined' ) {
   var SessionBoundExpressionExperimentSetValueObject = function() {
   };
}
if ( typeof FreeTextExpressionExperimentResultsValueObject === 'undefined' ) {
   var FreeTextExpressionExperimentResultsValueObject = function() {
   };
}
if ( typeof DatabaseBackedGeneSetValueObject === 'undefined' ) {
   var DatabaseBackedGeneSetValueObject = function() {
   };
}
if ( typeof GeneSetValueObject === 'undefined' ) {
   var GeneSetValueObject = function() {
   };
}
if ( typeof FreeTextGeneResultsValueObject === 'undefined' ) {
   var FreeTextGeneResultsValueObject = function() {
   };
}
if ( typeof GOGroupValueObject === 'undefined' ) {
   var GOGroupValueObject = function() {
   };
}
if ( typeof PhenotypeGroupValueObject === 'undefined' ) {
   var PhenotypeGroupValueObject = function() {
   };
}
if ( typeof SessionBoundGeneSetValueObject === 'undefined' ) {
   var SessionBoundGeneSetValueObject = function() {
   };
}
if ( typeof EvidenceValueObject === 'undefined' ) {
   var EvidenceValueObject = function() {
   };
}
if ( typeof LiteratureEvidenceValueObject === 'undefined' ) {
   var LiteratureEvidenceValueObject = function() {
   };
}
if ( typeof ExperimentalEvidenceValueObject === 'undefined' ) {
   var ExperimentalEvidenceValueObject = function() {
   };
}

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
