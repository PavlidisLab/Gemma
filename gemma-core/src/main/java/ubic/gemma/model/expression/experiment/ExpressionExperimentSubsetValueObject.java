package ubic.gemma.model.expression.experiment;

import gemma.gsec.model.Securable;

public class ExpressionExperimentSubsetValueObject extends ExpressionExperimentValueObject {

    private final Long sourceExperiment;

    public ExpressionExperimentSubsetValueObject( ExpressionExperimentSubSet ees ) {
        super( ees.getId() );
        this.sourceExperiment = ees.getSourceExperiment().getId();
        this.bioAssayCount = ees.getBioAssays() != null ? ees.getBioAssays().size() : null;
        this.name = ees.getName();
        this.description = ees.getDescription();
    }

    @Override
    public Class<? extends Securable> getSecurableClass() {
        return ExpressionExperimentSubSet.class;
    }

    public Long getSourceExperiment() {
        return sourceExperiment;
    }
}
