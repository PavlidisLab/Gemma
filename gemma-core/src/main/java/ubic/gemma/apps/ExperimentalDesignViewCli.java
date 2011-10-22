package ubic.gemma.apps;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;

import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExperimentalDesignService;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.FactorValueValueObject;
import ubic.gemma.util.AbstractSpringAwareCLI;
import ubic.gemma.util.EntityUtils;

/**
 * @author paul
 * @version $Id$
 */
public class ExperimentalDesignViewCli extends AbstractSpringAwareCLI {

    /**
     * @param args
     */
    public static void main( String[] args ) {
        ExperimentalDesignViewCli p = new ExperimentalDesignViewCli();
        try {

            Exception ex = p.doWork( args );
            if ( ex != null ) {
                ex.printStackTrace();
            }
            System.exit( 0 );
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    @Override
    protected void buildOptions() {
        // TODO Auto-generated method stub

    }

    @Override
    protected Exception doWork( String[] args ) {
        Exception err = processCommandLine( "Experimental Design view ", args );
        if ( err != null ) return err;

        ExperimentalDesignService eds = ( ExperimentalDesignService ) getBean( "experimentalDesignService" );

        ExpressionExperimentService ees = ( ExpressionExperimentService ) getBean( "expressionExperimentService" );
        Collection<ExpressionExperimentValueObject> experiments = ees.loadValueObjects( EntityUtils.getIds( ees
                .loadAll() ) );

        Map<Long, ExpressionExperimentValueObject> ed2ee = new HashMap<Long, ExpressionExperimentValueObject>();

        for ( ExpressionExperimentValueObject expressionExperiment : experiments ) {
            ed2ee.put( expressionExperiment.getExperimentalDesign(), expressionExperiment );
        }

        Collection<ExperimentalDesign> designs = eds.loadAll();

        Map<Long, Long> factor2Design = new HashMap<Long, Long>();

        Map<String, Map<String, Collection<FactorValueValueObject>>> categoryMap = new TreeMap<String, Map<String, Collection<FactorValueValueObject>>>();

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
                    categoryMap.put( category, new TreeMap<String, Collection<FactorValueValueObject>>() );
                }

                for ( FactorValue f : factor.getFactorValues() ) {
                    if ( f.getMeasurement() != null ) continue; // don't list individual quantitative values.

                    if ( f.getCharacteristics().size() > 0 ) {
                        for ( Characteristic c : f.getCharacteristics() ) {
                            if ( c.getCategory().equals( category ) ) {

                                String value = c.getValue();

                                if ( value == null ) continue;

                                if ( !categoryMap.get( category ).containsKey( value ) ) {
                                    categoryMap.get( category ).put( value, new HashSet<FactorValueValueObject>() );
                                }

                                categoryMap.get( category ).get( value ).add( new FactorValueValueObject( f, c ) );
                            }
                        }
                    } else if ( f.getValue() != null ) {
                        if ( !categoryMap.get( category ).containsKey( f.getValue() ) ) {
                            categoryMap.get( category ).put( f.getValue(), new HashSet<FactorValueValueObject>() );
                        }
                        categoryMap.get( category ).get( f.getValue() ).add( new FactorValueValueObject( f ) );
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

                for ( FactorValueValueObject fv : categoryMap.get( category ).get( value ) ) {
                    if ( fv.isMeasurement() ) continue; // don't list individual values.

                    Long factor = fv.getFactorId();
                    ExpressionExperimentValueObject expressionExperimentValueObject = ed2ee.get( factor2Design
                            .get( factor ) );

                    if ( expressionExperimentValueObject == null ) {
                        log.warn( "       NO EE for Factor=" + factor );
                        continue;
                    }

                    String ee = expressionExperimentValueObject.getShortName();

                    String uri = StringUtils.isBlank( fv.getValueUri() ) ? "" : " [" + fv.getValueUri() + "]";
                    log.info( "           " + fv.getValue() + uri + " EE=" + ee );

                }
            }
        }

        return null;
    }

}
