/*
 * The Gemma project
 * 
 * Copyright (c) 2010 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.apps;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.lang.StringUtils;

import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.ExternalDatabaseService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.biosequence.BioSequenceService;
import ubic.gemma.model.genome.biosequence.PolymerType;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.gene.GeneProductType;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.model.genome.sequenceAnalysis.AnnotationAssociation;
import ubic.gemma.model.genome.sequenceAnalysis.AnnotationAssociationService;
import ubic.gemma.util.AbstractSpringAwareCLI;

/**
 * Creates an array design based on the current set of transcripts for a taxon.
 * <p>
 * This is used to create a 'platform' for linking non-array based data to the system, or data for which we have only
 * gene or transcript-level information.
 * <p>
 * 
 * @author paul
 * @version $Id$
 */
public class GenericGenelistDesignGenerator extends AbstractSpringAwareCLI {
    private Taxon taxon = null;
    private TaxonService taxonService;
    private BioSequenceService bioSequenceService;
    private GeneService geneService;
    private ArrayDesignService arrayDesignService;
    private CompositeSequenceService compositeSequenceService;
    private AnnotationAssociationService annotationAssociationService;
    private ExternalDatabaseService externalDatabaseService;

    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions() {
        Option taxonOption = OptionBuilder.hasArg().withDescription( "taxon name" ).withDescription(
                "Taxon of the genes" ).withLongOpt( "taxon" ).isRequired().create( 't' );
        addOption( taxonOption );

    }

    @Override
    protected Exception doWork( String[] args ) {
        super.processCommandLine( "Create a new array design based on the genes for the organism", args );

        ExternalDatabase genbank = externalDatabaseService.find( "Genbank" );
        ExternalDatabase ensembl = externalDatabaseService.find( "Ensembl" );

        assert genbank != null;
        assert ensembl != null;

        /*
         * Create the stub array design for the organism. The name and etc. are generated automatically.
         */
        ArrayDesign arrayDesign = ArrayDesign.Factory.newInstance();
        if ( StringUtils.isBlank( taxon.getCommonName() ) ) {
            arrayDesign.setShortName( "Generic_" + taxon.getScientificName().replaceAll( " ", "_" ) );
        } else {
            arrayDesign.setShortName( "Generic_" + taxon.getCommonName().replaceAll( " ", "_" ) );
        }

        // common name
        arrayDesign.setPrimaryTaxon( taxon );
        arrayDesign.setName( "Generic array for " + taxon.getScientificName() );
        arrayDesign.setDescription( "Created by Gemma" );

        if ( arrayDesignService.find( arrayDesign ) != null ) {
            log.info( "Array design for " + taxon + " already exists." );
        }

        arrayDesign = arrayDesignService.findOrCreate( arrayDesign );
        arrayDesign = arrayDesignService.thaw( arrayDesign );

        /*
         * Load up the genes for the organism, exclusing predicted genes (for now) and pars.
         */
        Collection<Gene> knownGenes = geneService.loadKnownGenes( taxon );
        log.info( knownGenes.size() + " genes" );

        /*
         * Create a biosequence for each transcript, if there isn't one.
         */
        Map<GeneProduct, BioSequence> gp2bs = new HashMap<GeneProduct, BioSequence>();
        int count = 0;
        for ( Gene gene : knownGenes ) {
            boolean hasTranscript = false;
            gene = geneService.thaw( gene );
            Collection<GeneProduct> products = gene.getProducts();
            for ( GeneProduct geneProduct : products ) {
                if ( GeneProductType.RNA.equals( geneProduct.getType() ) ) {
                    /*
                     * Name is usually the genbank or ensembl accession
                     */

                    String name = geneProduct.getName();
                    BioSequence bs = BioSequence.Factory.newInstance();
                    Collection<DatabaseEntry> accessions = geneProduct.getAccessions();
                    bs.setName( name );
                    bs.setTaxon( taxon );
                    bs.setPolymerType( PolymerType.RNA );

                    if ( accessions.isEmpty() ) {
                        // this should not be hit.
                        log.warn( "No accession for " + name );
                        DatabaseEntry de = DatabaseEntry.Factory.newInstance();
                        de.setAccession( name );
                        if ( name.startsWith( "ENS" ) && name.length() > 10 ) {
                            de.setExternalDatabase( ensembl );
                        } else {
                            assert name.matches( "^[A-Z]{1,2}(_?)[0-9]+(\\.[0-9]+)?$" ) : "Name doesn't look like genbnak: "
                                    + name;
                            de.setExternalDatabase( genbank );
                        }
                        bs.setSequenceDatabaseEntry( de );
                    } else {
                        if ( accessions.size() > 1 ) {
                            log.warn( "Ambiguous accessions for " + name );
                        }
                        bs.setSequenceDatabaseEntry( accessions.iterator().next() );
                    }

                    BioSequence bioSequence = bioSequenceService.findOrCreate( bs );
                    // BioSequence bioSequence = bioSequenceService.find( bs );
                    //
                    // if ( bioSequence == null ) {
                    // Collection<BioSequence> candidates = bioSequenceService.findByName( name );
                    // log.debug( "No sequence for " + name + " found " + candidates.size() + " by name alone (" + gene
                    // + ")" );
                    // } else {
                    // log.debug( "Found existing sequence for " + name );
                    // }

                    gp2bs.put( geneProduct, bioSequence );

                    CompositeSequence cs = CompositeSequence.Factory.newInstance();
                    cs.setName( name );
                    cs.setArrayDesign( arrayDesign );
                    cs.setDescription( "Generic expression element for " + geneProduct );
                    cs = compositeSequenceService.findOrCreate( cs );
                    arrayDesign.getCompositeSequences().add( cs );

                    AnnotationAssociation aa = AnnotationAssociation.Factory.newInstance();
                    aa.setGeneProduct( geneProduct );
                    aa.setBioSequence( bioSequence );
                    annotationAssociationService.create( aa );

                    /*
                     * For now, only associate with a single transcript. Later we will refine our definition of
                     * transcripts and fix this.
                     */
                    hasTranscript = true;
                    break;
                }
            }
            if ( !hasTranscript ) {
                log.warn( "No transcript for " + gene );
            }

            if ( ++count % 10 == 0 ) log.info( count + " genes processed; " + gp2bs.size() + " transcripts so far" );
        }

        arrayDesignService.update( arrayDesign );

        arrayDesign = arrayDesignService.thawLite( arrayDesign );

        log.info( "Array design has " + arrayDesign.getCompositeSequences().size() + " 'probes'" );

        return null;

    }

    @Override
    protected void processOptions() {
        super.processOptions();

        geneService = ( GeneService ) this.getBean( "geneService" );
        taxonService = ( TaxonService ) getBean( "taxonService" );
        bioSequenceService = ( BioSequenceService ) getBean( "bioSequenceService" );
        arrayDesignService = ( ArrayDesignService ) getBean( "arrayDesignService" );
        compositeSequenceService = ( CompositeSequenceService ) getBean( "compositeSequenceService" );
        annotationAssociationService = ( AnnotationAssociationService ) getBean( "annotationAssociationService" );
        externalDatabaseService = ( ExternalDatabaseService ) getBean( "externalDatabaseService" );
        if ( hasOption( 't' ) ) {
            String taxonName = getOptionValue( 't' );
            this.taxon = taxonService.findByCommonName( taxonName );
            if ( taxon == null ) {
                log.error( "ERROR: Cannot find taxon " + taxonName );
            }
        }
    }

    public static void main( String[] args ) {
        GenericGenelistDesignGenerator b = new GenericGenelistDesignGenerator();
        b.doWork( args );
    }
}
