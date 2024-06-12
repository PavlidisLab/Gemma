package ubic.gemma.model.expression.experiment;

import gemma.gsec.model.Securable;

public class ExpressionExperimentSubsetValueObject extends ExpressionExperimentValueObject {

    private Long sourceExperiment;

    private String sourceExperimentShortName;

    public ExpressionExperimentSubsetValueObject() {
        super();
    }

    public ExpressionExperimentSubsetValueObject( ExpressionExperimentSubSet ees ) {
        super( ees.getId() );
        this.sourceExperiment = ees.getSourceExperiment().getId();
        this.sourceExperimentShortName = ees.getSourceExperiment().getShortName();
        this.numberOfBioAssays = ees.getBioAssays() != null ? ees.getBioAssays().size() : null;
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

    public void setSourceExperiment( Long sourceExperiment ) {
        this.sourceExperiment = sourceExperiment;
    }

    public String getSourceExperimentShortName() {
        return sourceExperimentShortName;
    }
}
