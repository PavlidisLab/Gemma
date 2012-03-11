package ubic.gemma.analysis.service;

import java.util.Collection;

import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

public interface SampleRemoveService {

    /**
     * This does not actually remove the sample; rather, it sets all values to "missing".
     * 
     * @param expExp
     * @param bioAssay
     */
    public abstract void markAsMissing( ExpressionExperiment expExp, BioAssay bioAssay );

    /**
     * This does not actually remove the sample; rather, it sets all values to "missing".
     * 
     * @param expExp
     * @param bioAssay
     */
    public abstract void markAsMissing( BioAssay bioAssay );

    /**
     * This does not actually remove the sample; rather, it sets all values to "missing" in the processed data.
     * 
     * @param expExp
     * @param assaysToRemove
     */
    public abstract void markAsMissing( ExpressionExperiment expExp, Collection<BioAssay> assaysToRemove );

}