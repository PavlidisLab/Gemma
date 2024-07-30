package ubic.gemma.model.analysis.expression.diff;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.Hibernate;
import ubic.gemma.model.common.IdentifiableValueObject;
import ubic.gemma.model.expression.experiment.FactorValueBasicValueObject;

/**
 * Represents a contrast result.
 * @author poirigui
 */
@Getter
@Setter
@ToString
public class ContrastResultValueObject extends IdentifiableValueObject<ContrastResult> {

    private Double pvalue;
    private Double tStat;
    private Double coefficient;
    private Double logFoldChange;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long factorValueId;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private FactorValueBasicValueObject factorValue;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long secondFactorValueId;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private FactorValueBasicValueObject secondFactorValue;

    public ContrastResultValueObject() {
        super();
    }

    /**
     * Create a contrast value object from a given {@link ContrastResult}.
     */
    public ContrastResultValueObject( ContrastResult contrastResult ) {
        super( contrastResult );
        this.pvalue = contrastResult.getPvalue();
        this.tStat = contrastResult.getTstat();
        this.coefficient = contrastResult.getCoefficient();
        this.logFoldChange = contrastResult.getLogFoldChange();
        if ( contrastResult.getFactorValue() != null ) {
            if ( Hibernate.isInitialized( contrastResult.getFactorValue() ) ) {
                this.factorValue = new FactorValueBasicValueObject( contrastResult.getFactorValue() );
            } else {
                this.factorValueId = contrastResult.getFactorValue().getId();
            }
        } else {
            this.factorValue = null;
        }
        // not all contrast results have a second factor value
        if ( contrastResult.getSecondFactorValue() != null ) {
            if ( Hibernate.isInitialized( contrastResult.getSecondFactorValue() ) ) {
                this.secondFactorValue = new FactorValueBasicValueObject( contrastResult.getSecondFactorValue() );
            } else {
                this.secondFactorValueId = contrastResult.getSecondFactorValue().getId();
            }
        } else {
            this.secondFactorValue = null;
        }
    }
}
