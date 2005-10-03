package edu.columbia.gemma.loader.association;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.columbia.gemma.association.GOEvidenceCode;
import edu.columbia.gemma.association.Gene2GOAssociation;
import edu.columbia.gemma.genome.Taxon;
import edu.columbia.gemma.genome.TaxonDao;
import edu.columbia.gemma.loader.loaderutils.ParserAndLoaderTools;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @version $Id$
 */
public class Gene2GOAssociationMappings {
    protected static final Log log = LogFactory.getLog( Gene2GOAssociationMappings.class );
    Configuration conf = new PropertiesConfiguration( "Gemma.properties" );

    private final int TAX_ID = conf.getInt( "gene2go.tax_id" );

    private final int EVIDENCE_CODE = conf.getInt( "gene2go.evidence_code" );

    private TaxonDao taxonDao = null;

    Map<Integer, Taxon> taxaMap = null;

    Collection<String> evidenceCodes = null;

    public Gene2GOAssociationMappings() throws ConfigurationException {
        initializeEvidenceCodes();
    }

    /**
     * @param taxonDao
     * @throws ConfigurationException
     * @spring.constructor-arg id="gene2GOAssociationMappings" ref="taxonDao"
     */
    public Gene2GOAssociationMappings( TaxonDao taxonDao ) throws ConfigurationException {
        if ( taxonDao == null ) throw new IllegalArgumentException();
        this.taxonDao = taxonDao;
        initializeTaxa();
        initializeEvidenceCodes();
    }

    /**
     * initializes the taxa.
     */
    @SuppressWarnings("unchecked")
    private void initializeTaxa() {
        Collection<Taxon> taxa = taxonDao.loadAll();
        taxaMap = new HashMap<Integer, Taxon>();

        for ( Taxon t : taxa ) {
            taxaMap.put( t.getNcbiId(), t );
        }
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

        for ( String evidenceCode : evidenceCodes ) {

            if ( values[EVIDENCE_CODE].equalsIgnoreCase( evidenceCode ) ) {
                g2GOAss.setEvidenceCode( GOEvidenceCode.fromString( evidenceCode ) );
                // log.info("Evidence code is: " + evidenceCode + " value: " + values[EVIDENCE_CODE]);
                break;
            }
        }

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
