package edu.columbia.gemma.loader.loaderutils;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

import edu.columbia.gemma.loader.sequence.gene.GeneLoaderService;
import edu.columbia.gemma.loader.sequence.gene.TaxonLoaderService;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @version $Id$
 */
public class BulkCreatorProxyFactory {
    private GeneLoaderService geneLoaderService;
    private TaxonLoaderService taxonLoaderService;
    Configuration conf;
    String geneFilename;
    String taxonFilename;

    /**
     * @throws ConfigurationException
     */
    public BulkCreatorProxyFactory() throws ConfigurationException {
        conf = new PropertiesConfiguration( "loader.properties" );
        taxonFilename = conf.getString( "loader.filename.taxon" );
        geneFilename = conf.getString( "loader.filename.gene" );
    }
    
    /**
     * 
     * @param filename
     * @return BulkCreator
     */
    public final BulkCreator getBulkCreatorProxy( String filename ) {
        String sArray[] = filename.split( "/" );
        filename = sArray[sArray.length - 1];
        if ( filename.startsWith( taxonFilename ) ) {
            return getTaxonLoaderService();
        } else if ( filename.startsWith( geneFilename ) ) {
            return getGeneLoaderService();
        } else {
            return getTaxonLoaderService();
        }
    }

    /**
     * @return Returns the geneLoaderService.
     */
    public GeneLoaderService getGeneLoaderService() {
        return geneLoaderService;
    }

    /**
     * @return Returns the taxonLoaderService.
     */
    public TaxonLoaderService getTaxonLoaderService() {
        return taxonLoaderService;
    }

    /**
     * @param geneLoaderService The geneLoaderService to set.
     */
    public void setGeneLoaderService( GeneLoaderService geneLoaderService ) {
        this.geneLoaderService = geneLoaderService;
    }

    /**
     * @param taxonLoaderService The taxonLoaderService to set.
     */
    public void setTaxonLoaderService( TaxonLoaderService taxonLoaderService ) {
        this.taxonLoaderService = taxonLoaderService;
    }
}