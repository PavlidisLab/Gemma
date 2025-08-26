package ubic.gemma.core.datastructure.matrix.io;

import org.springframework.util.Assert;
import ubic.gemma.core.util.BuildInfo;
import ubic.gemma.core.util.TsvUtils;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssayData.MeanVarianceRelation;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.util.EntityUrlBuilder;

import java.io.IOException;
import java.io.Writer;
import java.util.Date;

/**
 * @author poirigui
 */
public class MeanVarianceWriter {

    private final BuildInfo buildInfo;

    private final EntityUrlBuilder entityUrlBuilder;

    public MeanVarianceWriter( BuildInfo buildInfo, EntityUrlBuilder entityUrlBuilder ) {
        this.buildInfo = buildInfo;
        this.entityUrlBuilder = entityUrlBuilder;
    }

    /**
     * @param qt quantitation type that was used to generate the mean-variance relation, usually the one attached to the
     *           processed vectors
     */
    public void write( ExpressionExperiment ee, QuantitationType qt, Writer writer ) throws IOException {
        Assert.notNull( ee.getMeanVarianceRelation(), ee + " has no mean-variance relation." );
        ExpressionDataWriterUtils.appendBaseHeader( ee, qt, ProcessedExpressionDataVector.class, "Mean-variance relation",
                entityUrlBuilder.fromHostUrl().entity( ee ).web().toUriString(), buildInfo, new Date(), writer );
        MeanVarianceRelation mvr = ee.getMeanVarianceRelation();
        writer.write( "mean\tvariance\n" );
        for ( int i = 0; i < mvr.getMeans().length; i++ ) {
            writer.write( TsvUtils.format( mvr.getMeans()[i] ) );
            writer.write( "\t" );
            writer.write( TsvUtils.format( mvr.getVariances()[i] ) );
            writer.write( "\n" );
        }
    }
}
