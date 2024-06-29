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
package ubic.gemma.core.loader.expression.arrayDesign;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.security.concurrent.DelegatingSecurityContextRunnable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.analysis.report.ArrayDesignReportService;
import ubic.gemma.core.analysis.sequence.ProbeMapUtils;
import ubic.gemma.core.analysis.sequence.ProbeMapper;
import ubic.gemma.core.analysis.sequence.ProbeMapperConfig;
import ubic.gemma.core.analysis.service.ArrayDesignAnnotationService;
import ubic.gemma.core.analysis.service.ExpressionDataFileService;
import ubic.gemma.core.goldenpath.GoldenPathSequenceAnalysis;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.biosequence.SequenceType;
import ubic.gemma.model.genome.gene.GeneAlias;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.sequenceAnalysis.AnnotationAssociation;
import ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.persistence.persister.Persister;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;
import ubic.gemma.persistence.service.genome.biosequence.BioSequenceService;
import ubic.gemma.persistence.service.genome.gene.GeneProductService;
import ubic.gemma.persistence.service.genome.gene.GeneService;
import ubic.gemma.persistence.service.genome.sequenceAnalysis.AnnotationAssociationService;
import ubic.gemma.persistence.service.genome.sequenceAnalysis.BlatResultService;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * For an array design, generate gene product mappings for the sequences.
 *
 * @author pavlidis
 */
@Component
public class ArrayDesignProbeMapperServiceImpl implements ArrayDesignProbeMapperService {

    private static final int QUEUE_SIZE = 20000;
    private static final Log log = LogFactory.getLog( ArrayDesignProbeMapperServiceImpl.class.getName() );

    private final AnnotationAssociationService annotationAssociationService;
    private final ArrayDesignAnnotationService arrayDesignAnnotationService;
    private final ArrayDesignReportService arrayDesignReportService;
    private final ArrayDesignService arrayDesignService;
    private final BioSequenceService bioSequenceService;
    private final BlatResultService blatResultService;
    private final CompositeSequenceService compositeSequenceService;
    private final ExpressionDataFileService expressionDataFileService;
    private final GeneProductService geneProductService;
    private final GeneService geneService;
    private final Persister persisterHelper;
    private final ProbeMapper probeMapper;
    private final TaskExecutor taskExecutor;

    @Autowired
    public ArrayDesignProbeMapperServiceImpl( AnnotationAssociationService annotationAssociationService,
            ArrayDesignAnnotationService arrayDesignAnnotationService,
            ArrayDesignReportService arrayDesignReportService, ArrayDesignService arrayDesignService,
            ProbeMapper probeMapper, BioSequenceService bioSequenceService, BlatResultService blatResultService,
            CompositeSequenceService compositeSequenceService, ExpressionDataFileService expressionDataFileService,
            GeneProductService geneProductService, GeneService geneService, Persister persisterHelper, TaskExecutor taskExecutor ) {
        this.annotationAssociationService = annotationAssociationService;
        this.arrayDesignAnnotationService = arrayDesignAnnotationService;
        this.arrayDesignReportService = arrayDesignReportService;
        this.arrayDesignService = arrayDesignService;
        this.probeMapper = probeMapper;
        this.bioSequenceService = bioSequenceService;
        this.blatResultService = blatResultService;
        this.compositeSequenceService = compositeSequenceService;
        this.expressionDataFileService = expressionDataFileService;
        this.geneProductService = geneProductService;
        this.geneService = geneService;
        this.persisterHelper = persisterHelper;
        this.taskExecutor = taskExecutor;
    }

    @Override
    public void printResult( CompositeSequence compositeSequence, Collection<BlatAssociation> col ) {
        for ( BlatAssociation blatAssociation : col ) {
            this.printResult( compositeSequence, blatAssociation );
        }
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public void processArrayDesign( ArrayDesign arrayDesign ) {
        this.processArrayDesign( arrayDesign, new ProbeMapperConfig(), true );
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public void processArrayDesign( ArrayDesign arrayDesign, ProbeMapperConfig config, boolean useDB ) {

        assert config != null;

        if ( arrayDesign.getTechnologyType().equals( TechnologyType.GENELIST ) || arrayDesign.getTechnologyType().equals( TechnologyType.SEQUENCING )
                || arrayDesign.getTechnologyType().equals( TechnologyType.OTHER ) ) {
            throw new IllegalArgumentException(
                    "Do not use this service to process platforms that do not use an probe-based technology." );
        }

        Collection<Taxon> taxa = arrayDesignService.getTaxa( arrayDesign );

        Taxon taxon = arrayDesign.getPrimaryTaxon();
        if ( taxa.size() > 1 && taxon == null ) {
            throw new IllegalArgumentException(
                    "Array design has sequence from multiple taxa and has no primary taxon set: " + arrayDesign );
        }

        BlockingQueue<BACS> persistingQueue = new ArrayBlockingQueue<>( ArrayDesignProbeMapperServiceImpl.QUEUE_SIZE );
        AtomicBoolean generatorDone = new AtomicBoolean( false );
        AtomicBoolean loaderDone = new AtomicBoolean( false );

        this.load( persistingQueue, generatorDone, loaderDone, useDB );

        if ( useDB ) {
            ArrayDesignProbeMapperServiceImpl.log.info( "Removing any old alignment-based associations" );
            arrayDesignService.deleteGeneProductAlignmentAssociations( arrayDesign );
        }

        int count = 0;
        int hits = 0;
        int numWithNoResults = 0;
        ArrayDesignProbeMapperServiceImpl.log
                .info( "Start processing " + arrayDesign.getCompositeSequences().size() + " probes ..." );
        try ( GoldenPathSequenceAnalysis goldenPathDb = new GoldenPathSequenceAnalysis( taxon ) ) {
            for ( CompositeSequence compositeSequence : arrayDesign.getCompositeSequences() ) {

                Map<String, Collection<BlatAssociation>> results = this
                        .processCompositeSequence( config, taxon, goldenPathDb, compositeSequence );

                if ( results == null ) {
                    numWithNoResults++;
                    continue;
                }

                for ( Collection<BlatAssociation> col : results.values() ) {
                    for ( BlatAssociation association : col ) {
                        if ( ArrayDesignProbeMapperServiceImpl.log.isDebugEnabled() )
                            ArrayDesignProbeMapperServiceImpl.log.debug( association );
                        persistingQueue.add( new BACS( compositeSequence, association ) );

                    }
                    ++hits;
                }

                if ( ++count % 200 == 0 ) {
                    ArrayDesignProbeMapperServiceImpl.log
                            .info( "Processed " + count + " composite sequences" + " with blat results; " + hits
                                    + " mappings found." );
                }
            }
        }

        generatorDone.set( true );

        ArrayDesignProbeMapperServiceImpl.log.info( "Waiting for loading to complete ..." );
        while ( !loaderDone.get() ) {
            try {
                Thread.sleep( 1000 );
            } catch ( InterruptedException e ) {
                throw new RuntimeException( e );
            }
        }

        ArrayDesignProbeMapperServiceImpl.log
                .info( "Processed " + count + " composite sequences with blat results; " + hits + " mappings found." );

        if ( numWithNoResults > 0 ) {
            ArrayDesignProbeMapperServiceImpl.log.info( numWithNoResults + " had no blat results" );
        }

        arrayDesignReportService.generateArrayDesignReport( arrayDesign.getId() );

        this.deleteOldFiles( arrayDesign );
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public void processArrayDesign( ArrayDesign arrayDesign, Taxon taxon, File source, ExternalDatabase sourceDB,
            boolean ncbiIds ) throws IOException {

        if ( taxon == null && !ncbiIds ) {
            throw new IllegalArgumentException( "You must provide a taxon unless passing ncbiIds = true" );
        }

        if ( arrayDesign.getTechnologyType().equals( TechnologyType.GENELIST ) || arrayDesign.getTechnologyType().equals( TechnologyType.SEQUENCING )
                || arrayDesign.getTechnologyType().equals( TechnologyType.OTHER ) ) {
            throw new IllegalArgumentException(
                    "Do not use this service to process platforms that do not use an probe-based technology." );
        }

        try ( BufferedReader b = new BufferedReader( new FileReader( source ) ) ) {
            String line;
            int numSkipped = 0;

            ArrayDesignProbeMapperServiceImpl.log.info( "Removing any old annotation-based associations" );
            arrayDesignService.deleteGeneProductAnnotationAssociations( arrayDesign );

            while ( ( line = b.readLine() ) != null ) {

                if ( StringUtils.isBlank( line ) ) {
                    continue;
                }
                if ( line.startsWith( "#" ) ) {
                    continue;
                }

                String[] fields = StringUtils.splitPreserveAllTokens( line, '\t' );
                if ( fields.length != 3 ) {
                    throw new IOException( "Illegal format, expected three columns, got " + fields.length );
                }

                String probeId = fields[0];
                String seqName = fields[1];

                /*
                 * FIXME. We have to allow NCBI gene ids here.
                 */
                String geneSymbol = fields[2];

                if ( StringUtils.isBlank( geneSymbol ) ) {
                    numSkipped++;
                    continue;
                }

                CompositeSequence c = compositeSequenceService.findByName( arrayDesign, probeId );

                if ( c == null ) {
                    if ( ArrayDesignProbeMapperServiceImpl.log.isDebugEnabled() )
                        ArrayDesignProbeMapperServiceImpl.log
                                .debug( "No probe found for '" + probeId + "' on " + arrayDesign + ", skipping" );
                    numSkipped++;
                    continue;
                }

                // a probe can have more than one gene associated with it if so they are piped |
                Collection<Gene> geneListProbe = new HashSet<>();

                // indicate multiple genes
                Gene geneDetails;

                StringTokenizer st = new StringTokenizer( geneSymbol, "|" );
                while ( st.hasMoreTokens() ) {
                    String geneToken = st.nextToken().trim();
                    if ( ncbiIds ) {
                        geneDetails = geneService.findByNCBIId( Integer.parseInt( geneToken ) );
                    } else {
                        geneDetails = geneService.findByOfficialSymbol( geneToken, taxon );
                    }
                    if ( geneDetails != null ) {
                        geneListProbe.add( geneDetails );
                    }
                }

                if ( geneListProbe.isEmpty() ) {
                    ArrayDesignProbeMapperServiceImpl.log
                            .warn( "No gene(s) found for '" + geneSymbol + "' in " + taxon + ", skipping" );
                    numSkipped++;
                    continue;
                } else if ( geneListProbe.size() > 1 ) {
                    // this is a common situation, when the geneSymbol actually has |-separated genes, so no need to
                    // make a
                    // lot of fuss.
                    ArrayDesignProbeMapperServiceImpl.log
                            .debug( "More than one gene found for '" + geneSymbol + "' in " + taxon );
                }

                BioSequence bs = c.getBiologicalCharacteristic();

                if ( bs != null ) {
                    if ( StringUtils.isNotBlank( seqName ) ) {
                        bs = bioSequenceService.thawOrFail( bs );
                        if ( !bs.getName().equals( seqName ) ) {
                            ArrayDesignProbeMapperServiceImpl.log
                                    .warn( "Sequence name '" + seqName + "' given for " + probeId
                                            + " does not match existing entry " + bs.getName() + ", skipping" );
                            numSkipped++;
                            continue;
                        }

                    }
                    // otherwise we assume everything is okay.
                } else {
                    // create one based on the text provided.
                    if ( StringUtils.isBlank( seqName ) ) {
                        ArrayDesignProbeMapperServiceImpl.log
                                .warn( "You must provide sequence names for probes which are not already mapped. probeName="
                                        + probeId + " had no sequence associated and no name provided; skipping" );
                        numSkipped++;
                        continue;
                    }

                    bs = BioSequence.Factory.newInstance();
                    bs.setName( seqName );
                    bs.setTaxon( taxon );
                    bs.setDescription( "Imported from annotation file" );

                    // Placeholder.
                    bs.setType( SequenceType.OTHER );

                    bs = bioSequenceService.create( bs );

                    c.setBiologicalCharacteristic( bs );

                    compositeSequenceService.update( c );
                }

                assert bs.getId() != null;
                for ( Gene gene : geneListProbe ) {
                    gene = geneService.thawOrFail( gene );
                    if ( gene.getProducts().isEmpty() ) {
                        ArrayDesignProbeMapperServiceImpl.log.warn( "There are no gene products for " + gene
                                + ", it cannot be mapped to probes. Skipping" );
                        numSkipped++;
                        continue;
                    }
                    for ( GeneProduct gp : gene.getProducts() ) {
                        AnnotationAssociation association = AnnotationAssociation.Factory.newInstance();
                        association.setBioSequence( bs );
                        association.setGeneProduct( gp );
                        association.setSource( sourceDB );
                        association = annotationAssociationService.create( association );
                    }

                }

            }

            arrayDesignReportService.generateArrayDesignReport( arrayDesign.getId() );

            this.deleteOldFiles( arrayDesign );

            ArrayDesignProbeMapperServiceImpl.log
                    .info( "Completed association processing for " + arrayDesign + ", " + numSkipped
                            + " were skipped" );
        }
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public Map<String, Collection<BlatAssociation>> processCompositeSequence( ProbeMapperConfig config, Taxon taxon,
            GoldenPathSequenceAnalysis goldenPathDb, CompositeSequence compositeSequence ) {
        BioSequence bs = compositeSequence.getBiologicalCharacteristic();
        if ( bs == null )
            return null;

        /*
         * It isn't 100% clear what the right thing to do is. But this seems at least _reasonable_ when there is a
         * mismatch
         */
        if ( taxon != null && !bs.getTaxon().equals( taxon ) ) {
            return null;
        }

        final Collection<BlatResult> blatResults = blatResultService.findByBioSequence( bs );

        ProbeMapUtils.removeDuplicates( blatResults );

        if ( blatResults.isEmpty() )
            return null;

        return probeMapper.processBlatResults( goldenPathDb, blatResults, config );
    }

    private void doLoad( final BlockingQueue<BACS> queue, AtomicBoolean generatorDone, AtomicBoolean loaderDone,
            boolean persist ) {
        int loadedAssociationCount = 0;
        while ( !( generatorDone.get() && queue.isEmpty() ) ) {

            try {
                BACS bacs = queue.poll();
                if ( bacs == null ) {
                    continue;
                }

                GeneProduct geneProduct = bacs.ba.getGeneProduct();

                if ( geneProduct.getId() == null ) {
                    GeneProduct existing = geneProductService.find( geneProduct );

                    if ( existing == null ) {

                        existing = this.checkForAlias( geneProduct );
                        if ( existing == null ) {
                            /*
                             * We have to be careful not to cruft up the gene table now that I so carefully cleaned it.
                             * But this is a problem if we aren't adding some other association to the gene at least.
                             * But generally the mRNAs that GP has that NCBI doesn't are "alternative" or "additional".
                             */
                            if ( ArrayDesignProbeMapperServiceImpl.log.isDebugEnabled() )
                                ArrayDesignProbeMapperServiceImpl.log
                                        .debug( "New gene product from GoldenPath is not in Gemma: " + geneProduct
                                                + " skipping association to " + bacs.ba.getBioSequence()
                                                + " [skipping policy in place]" );
                            continue;
                        }
                    }
                    bacs.ba.setGeneProduct( existing );
                }

                if ( persist ) {
                    BlatAssociation ba = ( BlatAssociation ) persisterHelper.persist( bacs.ba );
                    if ( ++loadedAssociationCount % 1001 == 0 ) {
                        ArrayDesignProbeMapperServiceImpl.log
                                .info( "Persisted " + loadedAssociationCount + " blat associations. "
                                        + "Current queue has " + queue.size() + " items." );
                    }
                } else {
                    this.printResult( bacs.cs, bacs.ba );
                }

            } catch ( Exception e ) {
                ArrayDesignProbeMapperServiceImpl.log.error( e, e );
                loaderDone.set( true );
                throw new RuntimeException( e );
            }
        }
        ArrayDesignProbeMapperServiceImpl.log
                .info( "Load thread done: loaded " + loadedAssociationCount + " blat associations. " );
        loaderDone.set( true );
    }

    private GeneProduct checkForAlias( GeneProduct geneProduct ) {
        Collection<GeneProduct> candidates = geneProductService
                .findByName( geneProduct.getName(), geneProduct.getGene().getTaxon() );

        if ( candidates.isEmpty() )
            return null;

        Gene gene = geneProduct.getGene();
        for ( GeneProduct existing2 : candidates ) {
            Collection<GeneAlias> aliases = existing2.getGene().getAliases();
            for ( GeneAlias geneAlias : aliases ) {
                if ( geneAlias.getAlias().equalsIgnoreCase( gene.getOfficialSymbol() ) ) {
                    /*
                     * So, our gene products match, and the genes match but via an alias. That's pretty solid.
                     */
                    ArrayDesignProbeMapperServiceImpl.log.debug( "Associated gene product " + geneProduct
                            + " has a match in Gemma through an aliased gene: " + existing2 );
                    return existing2;
                }
            }

        }
        return null;
    }

    @Override
    public void deleteOldFiles( ArrayDesign arrayDesign ) {
        arrayDesignAnnotationService.deleteExistingFiles( arrayDesign );
        Collection<ExpressionExperiment> ees4platform = arrayDesignService.getExpressionExperiments( arrayDesign );
        ArrayDesignProbeMapperServiceImpl.log.info( "Removing invalidated files for up to " + ees4platform.size()
                + " experiments associated with updated platform " + arrayDesign );
        for ( ExpressionExperiment ee : ees4platform ) {
            try {
                expressionDataFileService.deleteAllFiles( ee );
            } catch ( Exception e ) {
                ArrayDesignProbeMapperServiceImpl.log
                        .error( "Error deleting files for " + ee + " " + e.getMessage(), e );
            }
        }

    }

    /**
     * @param persist true to get results saved to database; otherwise output is to standard out.
     */
    private void load( final BlockingQueue<BACS> queue, final AtomicBoolean generatorDone,
            final AtomicBoolean loaderDone, final boolean persist ) {
        this.taskExecutor.execute( new DelegatingSecurityContextRunnable( () -> ArrayDesignProbeMapperServiceImpl.this.doLoad( queue, generatorDone, loaderDone, persist ) ) );
    }

    /**
     * Print line of result to STDOUT.
     */
    private void printResult( CompositeSequence cs, BlatAssociation blatAssociation ) {
        GeneProduct geneProduct = blatAssociation.getGeneProduct();
        Gene gene = geneProduct.getGene();
        System.out.println(
                cs.getName() + '\t' + blatAssociation.getBioSequence().getName() + '\t' + geneProduct.getName() + '\t'
                        + gene.getOfficialSymbol() + "\t" + gene.getClass().getSimpleName() );
    }

    /**
     * Wrapper
     */
    private static class BACS {
        final BlatAssociation ba;

        final CompositeSequence cs;

        BACS( CompositeSequence compositeSequence, BlatAssociation association ) {
            this.cs = compositeSequence;
            this.ba = association;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ( ( ba == null ) ? 0 : ba.hashCode() );
            result = prime * result + ( ( cs == null ) ? 0 : cs.hashCode() );
            return result;
        }

        @Override
        public boolean equals( Object obj ) {
            if ( this == obj ) {
                return true;
            }
            if ( obj == null ) {
                return false;
            }
            if ( this.getClass() != obj.getClass() ) {
                return false;
            }
            BACS other = ( BACS ) obj;
            if ( ba == null ) {
                if ( other.ba != null ) {
                    return false;
                }
            } else if ( !ba.equals( other.ba ) ) {
                return false;
            }
            if ( cs == null ) {
                return other.cs == null;
            } else
                return cs.equals( other.cs );
        }
    }

}
