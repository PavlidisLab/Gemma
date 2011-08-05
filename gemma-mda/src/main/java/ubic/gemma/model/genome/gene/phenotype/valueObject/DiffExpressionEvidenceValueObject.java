package ubic.gemma.model.genome.gene.phenotype.valueObject;

import java.util.Collection;

import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.association.GOEvidenceCode;
import ubic.gemma.model.association.phenotype.DifferentialExpressionEvidence;

//TODO stgeorgn
public class DiffExpressionEvidenceValueObject extends EvidenceValueObject {

    // TODO need to populate this as an valueObject...
    private DifferentialExpressionAnalysisResult differentialExpressionAnalysisResult = null;

    public DiffExpressionEvidenceValueObject( String name, String description, String characteristic,
            Boolean isNegativeEvidence, GOEvidenceCode evidenceCode, Collection<String> characteristics,
            DifferentialExpressionAnalysisResult differentialExpressionAnalysisResult ) {
        super( name, description, characteristic, isNegativeEvidence, evidenceCode, characteristics );
        this.differentialExpressionAnalysisResult = differentialExpressionAnalysisResult;
    }

    /** Entity to Value Object */
    public DiffExpressionEvidenceValueObject( DifferentialExpressionEvidence differentialExpressionEvidence ) {
        super( differentialExpressionEvidence );

        this.differentialExpressionAnalysisResult = differentialExpressionEvidence
                .getDifferentialExpressionAnalysisResult();
    }

}
