package edu.columbia.gemma.loader.loaderutils;

import edu.columbia.gemma.loader.sequence.gene.GeneLoaderService;
import edu.columbia.gemma.loader.sequence.gene.TaxonLoaderService;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University Factory containing the different BulkCreator Proxies
 * 
 * @author keshav
 * @version $Id$
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