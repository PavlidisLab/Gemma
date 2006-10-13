/*
 * The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.apps;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.hibernate.LockMode;
import org.hibernate.SessionFactory;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

import ubic.gemma.loader.expression.arrayDesign.ArrayDesignSequenceAlignmentService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.util.AbstractSpringAwareCLI;

/**
 * Command line interface to run blat on the sequences for a microarray; the results are persisted in the DB. You must
 * start the BLAT server first before using this.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class ArrayDesignBlatCli extends ArrayDesignSequenceManipulatingCli {
    ArrayDesignSequenceAlignmentService arrayDesignSequenceAlignmentService;
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
        ArrayDesignBlatCli p = new ArrayDesignBlatCli();
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
        Exception err = processCommandLine( "Array design sequence BLAT - only works if server is already started!",
                args );
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

        unlazifyArrayDesign( arrayDesign );
         

        arrayDesignSequenceAlignmentService.processArrayDesign( arrayDesign, taxon );

        return null;
    }

    @Override
    protected void processOptions() {
        super.processOptions();
        arrayDesignSequenceAlignmentService = ( ArrayDesignSequenceAlignmentService ) this
                .getBean( "arrayDesignSequenceAlignmentService" );
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
