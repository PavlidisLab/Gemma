package ubic.gemma.model.genome.gene.phenotype.valueObject;

import java.util.Collection;

import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.association.phenotype.DifferentialExpressionEvidence;

//TODO stgeorgn
public class DiffExpressionEvidenceValueObject extends EvidenceValueObject {

    // TODO need to populate this as an valueObject...
    private DifferentialExpressionAnalysisResult differentialExpressionAnalysisResult = null;

    public DiffExpressionEvidenceValueObject( String description, CharacteristicValueObject associationType,
            Boolean isNegativeEvidence, String evidenceCode, Collection<CharacteristicValueObject> phenotypes,
            DifferentialExpressionAnalysisResult differentialExpressionAnalysisResult ) {
        super( description, associationType, isNegativeEvidence, evidenceCode, phenotypes );
        this.differentialExpressionAnalysisResult = differentialExpressionAnalysisResult;
    }

    /** Entity to Value Object */
    public DiffExpressionEvidenceValueObject( DifferentialExpressionEvidence differentialExpressionEvidence ) {
        super( differentialExpressionEvidence );

        this.differentialExpressionAnalysisResult = differentialExpressionEvidence
                .getDifferentialExpressionAnalysisResult();
    }

}
