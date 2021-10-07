package ubic.gemma.model.analysis.expression.diff;

import lombok.Data;
import ubic.gemma.model.expression.experiment.FactorValueBasicValueObject;

/**
 * Represents a contrast result.
 * @author poirigui
 */
@Data
public class ContrastResultValueObject {

    private final Double pvalue;
    private final Double tStat;
    private final Double coefficient;
    private final Double logFoldChange;
    private final FactorValueBasicValueObject factorValue;
    private final FactorValueBasicValueObject secondFactorValue;

    /**
     * Create a contrast value object from a given {@link ContrastResult}.
     * @param contrastResult
     */
    public ContrastResultValueObject( ContrastResult contrastResult ) {
        this.pvalue = contrastResult.getPvalue();
        this.tStat = contrastResult.getTstat();
        this.coefficient = contrastResult.getCoefficient();
        this.logFoldChange = contrastResult.getLogFoldChange();
        this.factorValue = new FactorValueBasicValueObject( contrastResult.getFactorValue() );
        // not all contrast results have a second factor value
        if ( contrastResult.getSecondFactorValue() != null ) {
            this.secondFactorValue = new FactorValueBasicValueObject( contrastResult.getSecondFactorValue() );
        } else {
            this.secondFactorValue = null;
        }
    }
}
