package ubic.gemma.apps;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.hibernate.LockMode;
import org.hibernate.SessionFactory;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

import ubic.gemma.loader.expression.arrayDesign.ArrayDesignProbeMapperService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.util.AbstractSpringAwareCLI;

/**
 * Process the blat results for an array design to map them onto genes.
 * <p>
 * Typical workflow would be to run:
 * <ol>
 * <li>ArrayDesignSequenceAssociationCli - attach sequences to array design, fetching from BLAST database if necessary.
 * <li>ArrayDesignBlatCli - runs blat
 * <li>ArrayDesignProbeMapperCli (this class)
 * </ol>
 * 
 * @author pavlidis
 * @version $Id$
 */
public class ArrayDesignProbeMapperCli extends AbstractSpringAwareCLI {
    ArrayDesignProbeMapperService arrayDesignProbeMapperService;
    TaxonService taxonService;
    ArrayDesignService arrayDesignService;
    private String commonName;
    private String arrayDesignName;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#buildOptions()
     */
    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions() {
        Option taxonOption = OptionBuilder.hasArg().isRequired().withArgName( "Taxon name" ).withDescription(
                "Taxon common name, e.g., 'rat'" ).withLongOpt( "taxon" ).create( 't' );

        addOption( taxonOption );

        Option arrayDesignOption = OptionBuilder.hasArg().isRequired().withArgName( "Array design" ).withDescription(
                "Array design name" ).withLongOpt( "array" ).create( 'a' );

        addOption( arrayDesignOption );

    }

    public static void main( String[] args ) {
        ArrayDesignProbeMapperCli p = new ArrayDesignProbeMapperCli();
        try {
            Exception ex = p.doWork( args );
            if ( ex != null ) {
                ex.printStackTrace();
            }
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#doWork(java.lang.String[])
     */
    @Override
    protected Exception doWork( String[] args ) {
        Exception err = processCommandLine( "Array design sequence BLAT", args );
        if ( err != null ) return err;

        Taxon taxon = taxonService.findByCommonName( commonName );

        if ( taxon == null ) {
            log.error( "No taxon " + commonName + " found" );
            bail( ErrorCode.INVALID_OPTION );
        }

        final ArrayDesign arrayDesign = arrayDesignService.findArrayDesignByName( arrayDesignName );

        if ( arrayDesign == null ) {
            log.error( "No arrayDesign " + arrayDesignName + " found" );
            bail( ErrorCode.INVALID_OPTION );
        }

        // All this to avoid lazy load errors.
        // FIXME shared by at least two CLIs - refactor.
        HibernateDaoSupport hds = new HibernateDaoSupport() {
        };
        hds.setSessionFactory( ( SessionFactory ) this.getBean( "sessionFactory" ) );
        HibernateTemplate templ = hds.getHibernateTemplate();
        templ.execute( new org.springframework.orm.hibernate3.HibernateCallback() {
            public Object doInHibernate( org.hibernate.Session session ) throws org.hibernate.HibernateException {
                session.lock( arrayDesign, LockMode.READ );
                arrayDesign.getCompositeSequences().size();
                for ( CompositeSequence cs : arrayDesign.getCompositeSequences() ) {
                    cs.getBiologicalCharacteristic().getTaxon();
                }
                return null;
            }
        }, true );

        arrayDesignProbeMapperService.processArrayDesign( arrayDesign, taxon );

        return null;
    }

    @Override
    protected void processOptions() {
        super.processOptions();
        arrayDesignProbeMapperService = ( ArrayDesignProbeMapperService ) this
                .getBean( "arrayDesignProbeMapperService" );
        taxonService = ( TaxonService ) this.getBean( "taxonService" );
        arrayDesignService = ( ArrayDesignService ) this.getBean( "arrayDesignService" );

        if ( this.hasOption( 't' ) ) {
            commonName = this.getOptionValue( 't' );
        }

        if ( this.hasOption( 'a' ) ) {
            this.arrayDesignName = this.getOptionValue( 'a' );
        }

    }

}
