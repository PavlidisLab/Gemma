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
package ubic.gemma.core.loader.association;

import org.apache.commons.lang3.StringUtils;
import ubic.gemma.core.loader.util.QueuingParser;
import ubic.gemma.core.loader.util.parser.BasicLineParser;
import ubic.gemma.core.ontology.OntologyUtils;
import ubic.gemma.model.association.GOEvidenceCode;
import ubic.gemma.model.association.Gene2GOAssociation;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.DatabaseType;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.core.config.Settings;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.BlockingQueue;

/**
 * This parses GO annotations from NCBI. See <a href="ftp://ftp.ncbi.nih.gov/gene/DATA/README">readme</a>.
 *
 * <pre>
 * tax_id:
 * the unique identifier provided by NCBI Taxonomy
 * for the species or strain/isolate
 * GeneID:
 * the unique identifier for a gene
 * --note:  for genomes previously available from LocusLink,
 * the identifiers are equivalent
 * GO ID:
 * the GO ID, formatted as GO:0000000
 * Evidence:
 * the evidence code in the gene_association file
 * Qualifier:
 * a qualifier for the relationship between the gene
 * and the GO term
 * GO term:
 * the term indicated by the GO ID
 * PubMed:
 * pipe-delimited set of PubMed uids reported as evidence
 * for the association
 * Category:
 * the GO category (Function, Process, or Component)
 * </pre>
 *
 * @author keshav
 * @author pavlidis
 */
public class NCBIGene2GOAssociationParser extends BasicLineParser<Gene2GOAssociation> implements QueuingParser<Gene2GOAssociation> {

    private static final String COMMENT_INDICATOR = "#";
    private static final Set<String> ignoredEvidenceCodes = new HashSet<>();

    static {
        // these are 'NOT association' codes, or (ND) one that means "nothing known", which we don't use. See
        // http://www.geneontology.org/GO.evidence.shtml.
        NCBIGene2GOAssociationParser.ignoredEvidenceCodes.add( "IMR" );
        NCBIGene2GOAssociationParser.ignoredEvidenceCodes.add( "IKR" );
        NCBIGene2GOAssociationParser.ignoredEvidenceCodes.add( "IRD" );
        NCBIGene2GOAssociationParser.ignoredEvidenceCodes.add( "ND" );
    }

    private final int TAX_ID = Settings.getInt( "gene2go.tax_id" );
    private final int EVIDENCE_CODE = Settings.getInt( "gene2go.evidence_code" );
    private final int GENE_ID = Settings.getInt( "gene2go.gene_id" );
    private final int GO_ID = Settings.getInt( "gene2go.go_id" );

    private final int GO_TERM_LABEL = Settings.getInt( "gene2go.goterm" );

    private BlockingQueue<Gene2GOAssociation> queue;

    private int count = 0;

    /**
     * NCBI Ids of available taxa.
     */
    private Map<Integer, Taxon> taxaNcbiIds;

    /**
     * @param taxa to consider (usually we pass in all)
     */
    public NCBIGene2GOAssociationParser( Collection<Taxon> taxa ) {
        ExternalDatabase goDb = ExternalDatabase.Factory.newInstance();
        goDb.setName( "GO" );
        goDb.setType( DatabaseType.ONTOLOGY );

        ExternalDatabase ncbiGeneDb = ExternalDatabase.Factory.newInstance();
        ncbiGeneDb.setName( "Entrez Gene" );

        this.taxaNcbiIds = new HashMap<>();
        for ( Taxon taxon : taxa ) {
            this.taxaNcbiIds.put( taxon.getNcbiId(), taxon );
            if ( taxon.getSecondaryNcbiId() != null ) {
                this.taxaNcbiIds.put( taxon.getSecondaryNcbiId(), taxon );
            }
        }
    }

    public int getCount() {
        return count;
    }

    @Override
    public Collection<Gene2GOAssociation> getResults() {
        return null;
    }

    @Override
    protected void addResult( Gene2GOAssociation obj ) {
        count++;
    }

    /**
     * Note that "-" means a missing value, which in practice only occurs in the "qualifier" and "pubmed" columns.
     *
     * @param  line line
     * @return Object
     */
    @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
    public Gene2GOAssociation mapFromGene2GO( String line ) {

        String[] values = StringUtils.splitPreserveAllTokens( line, "\t" );

        if ( line.startsWith( NCBIGene2GOAssociationParser.COMMENT_INDICATOR ) ) return null;

        if ( values.length < 8 ) return null;

        Integer taxonId;
        try {
            taxonId = Integer.parseInt( values[TAX_ID] );
        } catch ( NumberFormatException e ) {
            throw new RuntimeException( e );
        }

        if ( !taxaNcbiIds.containsKey( taxonId ) ) {
            return null;
        }

        Gene gene = Gene.Factory.newInstance();
        gene.setNcbiGeneId( Integer.parseInt( values[GENE_ID] ) );

        gene.setTaxon( taxaNcbiIds.get( taxonId ) );
        Characteristic oe = Characteristic.Factory.newInstance();
        String value = values[GO_ID].replace( ":", "_" );
        oe.setValueUri( OntologyUtils.BASE_PURL_URI + value );
        oe.setValue( values[GO_TERM_LABEL] );

        // g2GOAss.setSource( ncbiGeneDb );

        GOEvidenceCode evcode = null;
        String evidenceCode = values[EVIDENCE_CODE];

        if ( !( StringUtils.isBlank( evidenceCode ) || evidenceCode.equals( "-" ) ) ) {

            if ( NCBIGene2GOAssociationParser.ignoredEvidenceCodes.contains( evidenceCode ) ) {
                return null;
            }

            try {
                evcode = GOEvidenceCode.valueOf( evidenceCode );
            } catch ( IllegalArgumentException e ) {
                log.warn( String.format( "Unknown GO evidence code %s, will be mapping it as %s.", evidenceCode, GOEvidenceCode.OTHER ) );
                evcode = GOEvidenceCode.OTHER;
            }
        }
        Gene2GOAssociation g2GOAss = Gene2GOAssociation.Factory.newInstance( gene, oe, evcode );

        try {
            queue.put( g2GOAss );
        } catch ( InterruptedException e ) {
            throw new RuntimeException( e );
        }

        return g2GOAss;
    }

    @Override
    public void parse( InputStream inputStream, BlockingQueue<Gene2GOAssociation> aqueue ) throws IOException {
        if ( inputStream == null ) throw new IllegalArgumentException( "InputStream was null" );
        this.queue = aqueue;
        super.parse( inputStream );

    }

    @Override
    public Gene2GOAssociation parseOneLine( String line ) {
        return this.mapFromGene2GO( line );
    }

}
