package ubic.gemma.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.cli.util.AbstractAuthenticatedCLI;
import ubic.gemma.cli.util.CLI;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.persistence.service.expression.experiment.ExperimentalDesignService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.util.IdentifiableUtils;

import java.util.*;

/**
 * @author paul
 */
public class ExperimentalDesignViewCli extends AbstractAuthenticatedCLI {

    @Autowired
    private ExperimentalDesignService eds;
    @Autowired
    private ExpressionExperimentService ees;

    @Override
    public String getCommandName() {
        return "viewExpDesigns";
    }

    @Override
    public String getShortDesc() {
        return "Dump a view of experimental design(s)";
    }

    @Override
    public CommandGroup getCommandGroup() {
        return CLI.CommandGroup.ANALYSIS;
    }

    @Override
    protected void buildOptions( Options options ) {
    }

    @Override
    protected void processOptions( CommandLine commandLine ) {

    }

    @Override
    protected void doAuthenticatedWork() throws Exception {
        Collection<ExpressionExperimentValueObject> experiments = ees.loadValueObjectsByIds(
                IdentifiableUtils.getIds( ees.loadAll() ) );

        Map<Long, ExpressionExperimentValueObject> ed2ee = new HashMap<>();

        for ( ExpressionExperimentValueObject expressionExperiment : experiments ) {
            ed2ee.put( expressionExperiment.getExperimentalDesign(), expressionExperiment );
        }

        Collection<ExperimentalDesign> designs = eds.loadAll();

        Map<Long, Long> factor2Design = new HashMap<>();

        Map<String, Map<String, Collection<FactorValueBasicValueObject>>> categoryMap = new TreeMap<>();

        for ( ExperimentalDesign experimentalDesign : designs ) {

            if ( !ed2ee.containsKey( experimentalDesign.getId() ) ) continue;

            for ( ExperimentalFactor factor : experimentalDesign.getExperimentalFactors() ) {

                factor2Design.put( factor.getId(), experimentalDesign.getId() );

                String category;
                if ( factor.getCategory() != null )
                    category = factor.getCategory().getValue();
                else
                    category = " ** NO CATEGORY ** ";

                if ( !categoryMap.containsKey( category ) ) {
                    categoryMap.put( category, new TreeMap<String, Collection<FactorValueBasicValueObject>>() );
                }

                for ( FactorValue f : factor.getFactorValues() ) {
                    if ( f.getMeasurement() != null ) continue; // don't list individual quantitative values.

                    if ( !f.getCharacteristics().isEmpty() ) {
                        for ( Characteristic c : f.getCharacteristics() ) {
                            if ( Objects.equals( c.getCategory(), category ) ) {

                                String value = c.getValue();

                                if ( value == null ) continue;

                                if ( !categoryMap.get( category ).containsKey( value ) ) {
                                    categoryMap.get( category ).put( value, new HashSet<FactorValueBasicValueObject>() );
                                }

                                categoryMap.get( category ).get( value ).add( new FactorValueBasicValueObject( f ) );
                            }
                        }
                    } else if ( f.getValue() != null ) {
                        if ( !categoryMap.get( category ).containsKey( f.getValue() ) ) {
                            categoryMap.get( category ).put( f.getValue(), new HashSet<FactorValueBasicValueObject>() );
                        }
                        categoryMap.get( category ).get( f.getValue() ).add( new FactorValueBasicValueObject( f ) );
                    }

                }
            }

        }

        for ( String category : categoryMap.keySet() ) {

            log.info( "Category: " + category );

            if ( category.equals( "Time" ) || category.equals( "SamplingTimePoint" ) || category.equals( "Age" ) ) {
                log.info( " *****  Details not shown for this category" );
            }

            for ( String value : categoryMap.get( category ).keySet() ) {

                log.info( "     Value: " + value );

                for ( FactorValueBasicValueObject fv : categoryMap.get( category ).get( value ) ) {
                    if ( fv.getMeasurementObject() != null ) continue; // don't list individual values.

                    Long factor = fv.getId();
                    ExpressionExperimentValueObject expressionExperimentValueObject = ed2ee.get( factor2Design
                            .get( factor ) );

                    if ( expressionExperimentValueObject == null ) {
                        log.warn( "       NO EE for Factor=" + factor );
                        continue;
                    }

                    log.info( "           " + fv.getSummary() );

                }
            }
        }
    }

}
