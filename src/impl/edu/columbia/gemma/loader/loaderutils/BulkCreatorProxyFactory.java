package edu.columbia.gemma.loader.loaderutils;

import edu.columbia.gemma.loader.genome.GeneLoaderService;
import edu.columbia.gemma.loader.genome.TaxonLoaderService;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University Factory containing the different BulkCreator Proxies
 * 
 * @author keshav
 * @version $Id$
 * @spring.bean id="bulkCreatorProxyFactory"
 * @spring.property name="taxonLoaderService" ref="taxonLoaderService"
 * @spring.property name="geneLoaderService" ref="geneLoaderService"
 */
public class BulkCreatorProxyFactory {
    private GeneLoaderService geneLoaderService;
    private TaxonLoaderService taxonLoaderService;

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