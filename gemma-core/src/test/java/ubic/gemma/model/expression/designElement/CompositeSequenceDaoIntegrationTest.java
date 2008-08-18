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
package ubic.gemma.model.expression.designElement;

import java.io.File;
import java.io.InputStream;
import java.util.Collection;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import ubic.gemma.apps.Blat;
import ubic.gemma.loader.expression.arrayDesign.AbstractArrayDesignProcessingTest;
import ubic.gemma.loader.expression.arrayDesign.ArrayDesignProbeMapperService;
import ubic.gemma.loader.expression.arrayDesign.ArrayDesignSequenceAlignmentService;
import ubic.gemma.loader.expression.arrayDesign.ArrayDesignSequenceProcessingService;
import ubic.gemma.loader.genome.gene.ncbi.NcbiGeneLoader;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.model.genome.biosequence.SequenceType;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.persistence.PersisterHelper;
import ubic.gemma.persistence.TableMaintenanceUtil;
import ubic.gemma.util.ConfigUtils;

/**
 * @author pavlidis
 * @version $Id$
 */
public class CompositeSequenceDaoIntegrationTest extends AbstractArrayDesignProcessingTest {

    static Blat blat = new Blat();
    static CompositeSequenceService compositeSequenceService;
    static boolean setupDone = false;

    /**
     * The test files have only ~100 genes. This is still a very slow test to run, because it does many steps of
     * processing the data. This is a good candidate for a test that would be better done with a mini-database.
     * 
     * @see ubic.gemma.loader.expression.arrayDesign.AbstractArrayDesignProcessingTest#onSetUpInTransaction()
     */
    @Override
    protected void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();
        compositeSequenceService = ( CompositeSequenceService ) this.getBean( "compositeSequenceService" );
        Taxon taxon = ( ( TaxonService ) getBean( "taxonService" ) ).findByScientificName( "Homo sapiens" );

        if ( !setupDone ) {
            // insert the needed genes and geneproducts into the system.(can use NCBI gene loader, but for subset)
            NcbiGeneLoader loader = new NcbiGeneLoader();
            loader.setPersisterHelper( ( PersisterHelper ) this.getBean( "persisterHelper" ) );
            String filePath = ConfigUtils.getString( "gemma.home" ) + File.separatorChar;
            filePath = filePath + "gemma-core/src/test/resources/data/loader/genome/gene";
            String geneInfoFile = filePath + File.separatorChar + "selected_gene_info.gz";
            String gene2AccFile = filePath + File.separatorChar + "selected_gene2accession.gz";
            String geneHistoryFile = filePath + File.separatorChar + "selected_geneHistory.gz";

            loader.load( geneInfoFile, gene2AccFile, geneHistoryFile, true );

            // needed to fill in the sequence information for blat scoring.
            InputStream sequenceFile = this.getClass().getResourceAsStream(
                    "/data/loader/genome/gpl140.sequences.fasta" );
            ArrayDesignSequenceProcessingService app = ( ArrayDesignSequenceProcessingService ) getBean( "arrayDesignSequenceProcessingService" );
            app.processArrayDesign( getAd(), sequenceFile, SequenceType.EST );

            // fill in the blat results. Note that each time you run this test you
            // get the results loaded again (so they
            // pile up)
            ArrayDesignSequenceAlignmentService aligner = ( ArrayDesignSequenceAlignmentService ) getBean( "arrayDesignSequenceAlignmentService" );

            InputStream blatResultInputStream = new GZIPInputStream( this.getClass().getResourceAsStream(
                    "/data/loader/genome/gpl140.blatresults.psl.gz" ) );

            Collection<BlatResult> results = blat.processPsl( blatResultInputStream, taxon );

            aligner.processArrayDesign( getAd(), results );

            // real stuff.
            ArrayDesignProbeMapperService arrayDesignProbeMapperService = ( ArrayDesignProbeMapperService ) this
                    .getBean( "arrayDesignProbeMapperService" );
            arrayDesignProbeMapperService.processArrayDesign( getAd() );
            setupDone = true;
        }
        endTransaction();

    }

    @SuppressWarnings("unchecked")
    public void testFindByGene() {
        GeneService geneService = ( GeneService ) this.getBean( "geneService" );
        Collection<Gene> genes = geneService.findByOfficialSymbol( "PON2" );
        Gene g = genes.iterator().next();
        Collection<CompositeSequence> collection = compositeSequenceService.findByGene( g );
        assertEquals( 1, collection.size() );

    }

    @SuppressWarnings("unchecked")
    public void testFindByGeneAndArrayDesign() {
        GeneService geneService = ( GeneService ) this.getBean( "geneService" );
        Collection<Gene> genes = geneService.findByOfficialSymbol( "PON2" );
        Gene g = genes.iterator().next();
        Collection<CompositeSequence> collection = compositeSequenceService.findByGene( g, getAd() );
        assertEquals( 1, collection.size() );
    }

    @SuppressWarnings("unchecked")
    public void testHandleGetGenesCompositeSequence() {

        Collection<CompositeSequence> css = compositeSequenceService.findByName( "C277" );
        CompositeSequence cs = css.iterator().next();
        Collection<Gene> genes = compositeSequenceService.getGenes( cs );
        assertEquals( 1, genes.size() );
    }

    @SuppressWarnings("unchecked")
    public void testHandleGetGenesCompositeSequences() {

        TableMaintenanceUtil tu = ( TableMaintenanceUtil ) this.getBean( "tableMaintenanceUtil" );
        tu.updateGene2CsEntries();

        Collection<CompositeSequence> css = compositeSequenceService.findByName( "C277" );

        Map<CompositeSequence, Collection<Gene>> genes = compositeSequenceService.getGenes( css );
        assertEquals( 1, genes.size() );
    }

}
