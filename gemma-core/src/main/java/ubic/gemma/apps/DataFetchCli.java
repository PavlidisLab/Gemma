package ubic.gemma.apps;

import java.util.Collection;
import java.util.Map;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;

import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.util.AbstractSpringAwareCLI;

public class DataFetchCli extends AbstractSpringAwareCLI {

    private DesignElementDataVectorService dedvs;
    private GeneService geneService;
    private String geneName;

    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions() {

        Option geneOpt = OptionBuilder.hasArg().withArgName( "gene" ).create( 'g' );

        addOption( geneOpt );
    }

    public static void main( String[] args ) {
        DataFetchCli d = new DataFetchCli();
        d.doWork( args );
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Exception doWork( String[] args ) {

        processCommandLine( "Data fetching for genes", args );

        Collection<Gene> genes = geneService.findByOfficialSymbol( geneName );

        Map<DesignElementDataVector, Collection<Gene>> geneCoexpressionPattern = dedvs.getVectors( null, genes );
        return null;
    }

    @Override
    protected void processOptions() {
        super.processOptions();

        this.geneService = ( GeneService ) getBean( "geneService" );
        this.dedvs = ( DesignElementDataVectorService ) getBean( "designElementDataVectorService" );

        if ( this.hasOption( 'g' ) ) {
            geneName = this.getOptionValue( 'g' );
        }

    }

}
