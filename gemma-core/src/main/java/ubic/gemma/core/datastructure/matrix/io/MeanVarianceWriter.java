package ubic.gemma.core.datastructure.matrix.io;

import org.springframework.util.Assert;
import ubic.gemma.core.util.BuildInfo;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssayData.MeanVarianceRelation;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.util.EntityUrlBuilder;

import java.io.IOException;
import java.io.Writer;

public class MeanVarianceWriter {

    private final BuildInfo buildInfo;

    private final EntityUrlBuilder entityUrlBuilder;

    public MeanVarianceWriter( BuildInfo buildInfo, EntityUrlBuilder entityUrlBuilder ) {
        this.buildInfo = buildInfo;
        this.entityUrlBuilder = entityUrlBuilder;
    }

    /**
     *
     * @param ee
     * @param qt quantitation type that was used to generate the mean-variance relation, usually the one attached to the processed vectors
     * @param writer
     * @throws IOException
     */
    public void write( ExpressionExperiment ee, QuantitationType qt, Writer writer ) throws IOException {
        Assert.notNull( ee.getMeanVarianceRelation(), ee + " has no mean-variance relation." );
        ExpressionDataWriterUtils.appendBaseHeader( ee, qt, "mean-variance relation",
                entityUrlBuilder.fromHostUrl().entity( ee ).web().toUriString(), buildInfo, writer );
        MeanVarianceRelation mvr = ee.getMeanVarianceRelation();
        writer.write( "mean\tvariance\n" );
        for ( int i = 0; i < mvr.getMeans().length; i++ ) {
            writer.write( mvr.getMeans()[i] + "\t" + mvr.getVariances()[i] );
            writer.write( "\n" );
        }
    }
}
