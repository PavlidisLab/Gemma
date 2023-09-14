package ubic.gemma.core.loader.expression;

import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.biosequence.BioSequence;

public interface ExpressionExperimentPlatformSwitchService {

    void switchExperimentToArrayDesign( ExpressionExperiment ee, ArrayDesign arrayDesign );

    void switchExperimentToMergedPlatform( ExpressionExperiment expExp );
}
