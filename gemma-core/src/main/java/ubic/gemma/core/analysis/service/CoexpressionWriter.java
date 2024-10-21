package ubic.gemma.core.analysis.service;

import ubic.gemma.core.util.Constants;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.association.coexpression.CoexpressionValueObject;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.Date;

import static ubic.gemma.core.util.TsvUtils.format;

public class CoexpressionWriter {

    public void write( ExpressionExperiment ee, Collection<CoexpressionValueObject> geneLinks, Writer writer ) throws IOException {
        Date timestamp = new Date( System.currentTimeMillis() );
        // Write header information
        for ( String line : Constants.GEMMA_CITATION_NOTICE ) {
            writer.append( "# " ).append( line ).append( "\n" );
        }
        writer.append( "# Coexpression data for:  " ).append( ee.getShortName() ).append( " : " ).append( ee.getName() ).append( " \n" );
        writer.append( "# Generated On: " ).append( timestamp.toString() ).append( " \n" );
        writer.append( "# Links are listed in an arbitrary order with an indication of positive or negative correlation\n" );
        writer.append( "GeneSymbol1\tGeneSymbol2\tDirection\tSupport\n" );
        // Data
        for ( CoexpressionValueObject link : geneLinks ) {
            writer.append( format( link.getQueryGeneSymbol() ) ).append( "\t" )
                    .append( format( link.getCoexGeneSymbol() ) ).append( "\t" )
                    .append( link.isPositiveCorrelation() ? "+" : "-" ).append( "\n" );
        }
    }
}
