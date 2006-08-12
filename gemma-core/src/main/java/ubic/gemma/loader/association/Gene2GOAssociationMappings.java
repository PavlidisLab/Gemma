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
package ubic.gemma.loader.association;

import java.util.Collection;
import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.loader.util.ParserAndLoaderTools;
import ubic.gemma.model.association.GOEvidenceCode;
import ubic.gemma.model.association.Gene2GOAssociation;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.DatabaseType;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.OntologyEntry;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;

/**
 * @author keshav
 * @version $Id$
 */
public class Gene2GOAssociationMappings {
    protected static final Log log = LogFactory.getLog( Gene2GOAssociationMappings.class );
    Configuration conf = new PropertiesConfiguration( "Gemma.properties" );

    private final int TAX_ID = conf.getInt( "gene2go.tax_id" );

    private final int EVIDENCE_CODE = conf.getInt( "gene2go.evidence_code" );

    private final int GENE_ID = conf.getInt( "gene2go.gene_id" );

    private final int GO_ID = conf.getInt( "gene2go.go_id" );

    Collection<String> evidenceCodes = null;

    ExternalDatabase goDb;
    ExternalDatabase ncbiDb;

    public Gene2GOAssociationMappings() throws ConfigurationException {
        initializeEvidenceCodes();
        goDb = ExternalDatabase.Factory.newInstance();
        goDb.setName( "GO" );
        goDb.setType( DatabaseType.ONTOLOGY );
        ncbiDb = ExternalDatabase.Factory.newInstance();
        ncbiDb.setName( "NCBI" );
        ncbiDb.setType( DatabaseType.SEQUENCE );
    }

    /**
     * @param line
     * @return Object
     */
    public Object mapFromGene2GO( String line ) {
        String[] values = StringUtils.split( line, "\t" );

        if ( line.startsWith( "#" ) ) return null;

        if ( !ParserAndLoaderTools.validTaxonId( values[TAX_ID] ) ) {
            return null;
        }

        Gene2GOAssociation g2GOAss = Gene2GOAssociation.Factory.newInstance();

        Gene gene = Gene.Factory.newInstance();
        gene.setNcbiId( values[GENE_ID] );
        DatabaseEntry dbe = DatabaseEntry.Factory.newInstance();
        dbe.setAccession( values[GENE_ID] );
        dbe.setExternalDatabase( ncbiDb );
        gene.getAccessions().add( dbe );

        Taxon t = Taxon.Factory.newInstance();
        try {
            t.setNcbiId( Integer.parseInt( values[TAX_ID] ) );
        } catch ( NumberFormatException e ) {
            throw new RuntimeException( e );
        }

        gene.setTaxon( t );

        OntologyEntry oe = OntologyEntry.Factory.newInstance();
        oe.setAccession( values[GO_ID] );
        oe.setExternalDatabase( goDb );

        g2GOAss.setGene( gene );
        g2GOAss.setOntologyEntry( oe );

        g2GOAss.setEvidenceCode( GOEvidenceCode.fromString( values[EVIDENCE_CODE] ) );

        return g2GOAss;
    }

    /**
     * Initialize the evidenceCodes from the enumeration values
     */
    @SuppressWarnings("unchecked")
    private void initializeEvidenceCodes() {
        evidenceCodes = GOEvidenceCode.names();
    }

}
