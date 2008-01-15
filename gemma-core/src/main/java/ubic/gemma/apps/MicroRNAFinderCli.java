/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Collection;
import java.util.HashMap;

import java.util.HashSet;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.lang.time.StopWatch;

import ubic.gemma.externalDb.GoldenPathSequenceAnalysis;
import ubic.gemma.model.association.BioSequence2GeneProduct;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.PhysicalLocation;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation;
import ubic.gemma.model.genome.sequenceAnalysis.BlatAssociationService;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.util.AbstractSpringAwareCLI;

/**
 * @author xwan
 * @version $Id$
 */
public class MicroRNAFinderCli extends AbstractSpringAwareCLI {

    private String arrayDesignName = null;
    private String outFileName = null;
    private String taxonName = null;
    private Collection<GeneProduct> miRNAs = new HashSet<GeneProduct>();

    @Override
    protected void processOptions() {
        super.processOptions();
        if ( hasOption( 'a' ) ) {
            this.arrayDesignName = getOptionValue( 'a' );
        }
        if ( hasOption( 'o' ) ) {
            this.outFileName = getOptionValue( 'o' );
        }
        if ( hasOption( 't' ) ) {
            this.taxonName = getOptionValue( 't' );
        }

    }

    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions() {
        Option ADOption = OptionBuilder.hasArg().isRequired().withArgName( "arrayDesign" ).withDescription(
                "Array Design Short Name (GPLXXX) " ).withLongOpt( "arrayDesign" ).create( 'a' );
        addOption( ADOption );
        Option OutOption = OptionBuilder.hasArg().isRequired().withArgName( "outputFile" ).withDescription(
                "The name of the file to save the output " ).withLongOpt( "outputFile" ).create( 'o' );
        addOption( OutOption );

        Option TaxonOption = OptionBuilder.hasArg().isRequired().withArgName( "taxonName" ).withDescription(
                "The name of the speci " ).withLongOpt( "taxonName" ).create( 't' );
        addOption( TaxonOption );

    }

    private Taxon getTaxon( String name ) {
        Taxon taxon = Taxon.Factory.newInstance();
        taxon.setCommonName( name );
        TaxonService taxonService = ( TaxonService ) this.getBean( "taxonService" );
        taxon = taxonService.find( taxon );
        if ( taxon == null ) {
            log.info( "NO Taxon found!" );
        }
        return taxon;
    }

    private Collection<GeneProduct> checkMappedRNAs( PhysicalLocation targetLocation ) {
        Collection<GeneProduct> returnedRNAs = new HashSet<GeneProduct>();
        if ( targetLocation == null ) {
            return returnedRNAs;
        }
        for ( GeneProduct miRNA : miRNAs ) {
            // if(!miRNA.getName().equals("mmu-let-7b")) continue;
            PhysicalLocation location = miRNA.getPhysicalLocation();
            if ( targetLocation.computeOverlap( location ) >= location.getNucleotideLength() )
                returnedRNAs.add( miRNA );
        }
        return returnedRNAs;

    }

    @SuppressWarnings("unchecked")
    @Override
    protected Exception doWork( String[] args ) {
        Exception err = processCommandLine( "MicroRNAFinder", args );
        if ( err != null ) {
            return err;
        }
        ArrayDesignService adService = ( ArrayDesignService ) this.getBean( "arrayDesignService" );
        GeneService geneService = ( GeneService ) this.getBean( "geneService" );
        BlatAssociationService blatAssociationService = ( BlatAssociationService ) this
                .getBean( "blatAssociationService" );

        Taxon taxon = this.getTaxon( this.taxonName );
        if ( taxon == null ) {
            System.err.println( " Taxon " + this.taxonName + " doesn't exist" );
            return null;
        }
        Collection<Gene> genes = geneService.getMicroRnaByTaxon( taxon );

        for ( Gene gene : genes ) {
            miRNAs.addAll( gene.getProducts() );
        }

        ArrayDesign arrayDesign = adService.findByShortName( this.arrayDesignName );
        if ( arrayDesign == null ) {
            System.err.println( " Array Design " + this.arrayDesignName + " doesn't exist" );
            return null;
        }

        HashMap<CompositeSequence, HashSet<GeneProduct>> results = new HashMap<CompositeSequence, HashSet<GeneProduct>>();

        try {

            GoldenPathSequenceAnalysis analysis = new GoldenPathSequenceAnalysis( taxon );
            Collection<CompositeSequence> allCSs = adService.loadCompositeSequences( arrayDesign );

            for ( CompositeSequence cs : allCSs ) {
                // if(!cs.getName().equals("1440357_at")) continue;
                Collection bs2gps = cs.getBiologicalCharacteristic().getBioSequence2GeneProduct();
                HashSet<GeneProduct> mappedRNAs = new HashSet<GeneProduct>();
                for ( Object object : bs2gps ) {
                    BioSequence2GeneProduct bs2gp = ( BioSequence2GeneProduct ) object;
                    if ( bs2gp instanceof BlatAssociation ) {
                        BlatAssociation blatAssociation = ( BlatAssociation ) bs2gp;
                        blatAssociationService.thaw( blatAssociation );
                        GeneProduct geneProduct = blatAssociation.getGeneProduct();
                        PhysicalLocation targetLocation = geneProduct.getPhysicalLocation();
                        mappedRNAs.addAll( checkMappedRNAs( targetLocation ) );

                        BlatResult blatResult = blatAssociation.getBlatResult();
                        if ( blatResult == null ) continue;
                        long start = blatResult.getTargetStart();
                        long end = blatResult.getTargetEnd();
                        String chromosome = blatResult.getTargetChromosome().getName();
                        Collection<Gene> alignedGenes = analysis.findRNAs( chromosome, start, end, blatResult
                                .getStrand() );
                        for ( Gene gene : alignedGenes ) {
                            targetLocation = gene.getPhysicalLocation();
                            mappedRNAs.addAll( checkMappedRNAs( targetLocation ) );
                        }
                    }
                }
                if ( mappedRNAs.size() > 0 ) results.put( cs, mappedRNAs );
            }

            PrintStream output = new PrintStream( new FileOutputStream( new File( this.outFileName ) ) );
            for ( CompositeSequence cs : results.keySet() ) {
                output.print( cs.getName() );
                HashSet<GeneProduct> mappedmiRNAs = results.get( cs );
                for ( GeneProduct miRNA : mappedmiRNAs ) {
                    output.print( "\t" + miRNA.getName() );
                }
                output.println();
            }
            output.close();
        } catch ( Exception e ) {
            return e;
        }
        return null;
    }

    /**
     * @param args
     */
    public static void main( String[] args ) {
        MicroRNAFinderCli finder = new MicroRNAFinderCli();
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            Exception ex = finder.doWork( args );
            if ( ex != null ) {
                ex.printStackTrace();
            }
            watch.stop();
            log.info( "Elapsed time: " + watch.getTime() / 1000 + " seconds" );
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

}
