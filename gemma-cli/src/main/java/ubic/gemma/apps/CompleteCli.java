package ubic.gemma.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.cli.completion.CompletionType;
import ubic.gemma.cli.util.EnumeratedByCommandConverter;
import ubic.gemma.cli.util.AbstractAuthenticatedCLI;
import ubic.gemma.cli.util.EnumConverter;
import ubic.gemma.core.util.TsvUtils;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.common.protocol.Protocol;
import ubic.gemma.model.expression.arrayDesign.AlternateName;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.common.protocol.ProtocolService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentSetService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

import javax.annotation.Nullable;
import java.util.Collection;

/**
 * This CLI provide various runtime completions that can be re-used by other CLIs.
 * @author poirigui
 * @see EnumeratedByCommandConverter
 */
public class CompleteCli extends AbstractAuthenticatedCLI {

    @Autowired
    private TaxonService taxonService;

    @Autowired
    private ArrayDesignService arrayDesignService;

    @Autowired
    private ExpressionExperimentSetService expressionExperimentSetService;

    @Autowired
    private ProtocolService protocolService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    /**
     * The type of completion to produce.
     */
    private CompletionType completionType;
    /**
     * Additional arguments for the completion command.
     * <p>
     * This is specific to each completion type.
     */
    @Nullable
    private String[] completeArgs;

    public CompleteCli() {
        setAllowPositionalArguments();
    }

    @Override
    protected void processOptions( CommandLine commandLine ) throws ParseException {
        super.processOptions( commandLine );
        if ( commandLine.getArgList().isEmpty() ) {
            throw new ParseException( "Expects at least one positional argument indicating the completion type." );
        }
        try {
            completionType = EnumConverter.of( CompletionType.class ).apply( commandLine.getArgList().get( 0 ) );
        } catch ( IllegalArgumentException e ) {
            throw new ParseException( "Failed to parse the completion type." );
        }
        if ( commandLine.getArgList().size() > 1 ) {
            completeArgs = commandLine.getArgList().subList( 1, commandLine.getArgList().size() ).toArray( new String[0] );
        } else {
            completeArgs = new String[0];
        }
    }

    @Override
    public String getCommandName() {
        return "complete";
    }

    @Override
    protected String getUsage() {
        return "gemma-cli [options] complete <type> [completeArgs...]";
    }

    @Override
    protected void doAuthenticatedWork() throws Exception {
        switch ( completionType ) {
            case TAXON:
                for ( Taxon taxon : taxonService.loadAll() ) {
                    String description = taxon.getScientificName();
                    printCompletion( String.valueOf( taxon.getId() ), description );
                    if ( taxon.getNcbiId() != null ) {
                        printCompletion( String.valueOf( taxon.getNcbiId() ), description );
                    }
                    if ( taxon.getCommonName() != null ) {
                        printCompletion( taxon.getCommonName(), description );
                    }
                    if ( taxon.getScientificName() != null ) {
                        printCompletion( taxon.getScientificName(), description );
                    }
                }
                break;
            case PLATFORM:
                Collection<ArrayDesign> ads;
                if ( ArrayUtils.contains( completeArgs, "generic" ) ) {
                    ads = arrayDesignService.loadAllGenericGenePlatforms();
                } else {
                    ads = arrayDesignService.loadAll();
                }
                for ( ArrayDesign ad : ads ) {
                    String description = ad.getName();
                    printCompletion( String.valueOf( ad.getId() ), description );
                    printCompletion( String.valueOf( ad.getShortName() ), description );
                    printCompletion( String.valueOf( description ), description );
                    for ( AlternateName alternateName : ad.getAlternateNames() ) {
                        printCompletion( alternateName.getName(), description );
                    }
                }
                break;
            case PROTOCOL:
                for ( Protocol protocol : protocolService.loadAllUniqueByName() ) {
                    printCompletion( String.valueOf( protocol.getId() ), protocol.getName() );
                    printCompletion( protocol.getName(), protocol.getName() );
                }
                break;
            case EESET:
                for ( ExpressionExperimentSet eeSet : expressionExperimentSetService.loadAll() ) {
                    printCompletion( String.valueOf( eeSet.getId() ), eeSet.getName() );
                    printCompletion( eeSet.getName(), eeSet.getName() );
                }
                break;
            case DATASET:
                expressionExperimentService.loadAllIdAndName()
                        .forEach( ( id, name ) -> printCompletion( String.valueOf( id ), name ) );
                expressionExperimentService.loadAllShortNameAndName()
                        .forEach( this::printCompletion );
                expressionExperimentService.loadAllName()
                        .forEach( name -> printCompletion( name, name ) );
                expressionExperimentService.loadAllAccessionAndName()
                        .forEach( this::printCompletion );
                break;
            default:
                throw new UnsupportedOperationException( "Unsupported completion type " + completionType );
        }
    }

    private void printCompletion( String value, @Nullable String description ) {
        getCliContext().getOutputStream().printf( "%s\t%s%n", value, TsvUtils.format( description ) );
    }
}
