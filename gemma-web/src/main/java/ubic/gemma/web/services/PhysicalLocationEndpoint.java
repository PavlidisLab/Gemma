/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
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
package ubic.gemma.web.services;

import java.util.Collection;

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import ubic.gemma.model.genome.Chromosome;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.PhysicalLocation;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.gene.GeneService;

/**
 * Given a gene id, will return physical location (chromosome #, nucleotide range: start and end) Note: There may be >1
 * gene products for a particular gene id and hence >1 physical locations associated with gene id. Therefore, the
 * nucleotide range will be the global max and min nucleotides for all the gene products. The max and min do not
 * necessarily correspond to start and end because each gene product can be transcribed on either the +ve strand or the
 * -ve strand (ie. opposite directions of transcription).
 * 
 * @author gavin, klc package ubic.gemma.web.services;
 */

public class PhysicalLocationEndpoint extends AbstractGemmaEndpoint {

    private static Log log = LogFactory.getLog( PhysicalLocationEndpoint.class );

    private GeneService geneService;

    private Long minNT;
    private Long maxNT;
    private String chromId;
    private PhysicalLocation pLoc;
    /**
     * The local name of the expected Request/Response.
     */
    public static final String PLOC_LOCAL_NAME = "physicalLocation";

    /**
     * Sets the "business service" to delegate to.
     */
    public void setGeneService( GeneService geneS ) {
        this.geneService = geneS;
    }

    /**
     * Reads the given <code>requestElement</code>, and sends a the response back.
     * 
     * @param requestElement the contents of the SOAP message as DOM elements
     * @param document a DOM document to be used for constructing <code>Node</code>s
     * @return the response element
     */
    protected Element invokeInternal( Element requestElement, Document document ) {
        StopWatch watch = new StopWatch();
        watch.start();
        
        setLocalName( PLOC_LOCAL_NAME );
        Collection<String> geneResults = getSingleNodeValue( requestElement, "gene_id" );
        String geneId = "";

        for ( String id : geneResults ) {
            geneId = id;
        }

        log.info( "XML input read: gene id, "+geneId );
        // get the physical location of gene using GeneService
        Gene gene = geneService.load( Long.parseLong( geneId ) );

        if ( gene == null ) {
            String msg = "No gene with id, " + geneId + " can be found.";
            return buildBadResponse( document, msg );
        }

        // thaw the gene
        geneService.thaw( gene );

        // get gene products
        Collection<GeneProduct> gpCollection = gene.getProducts();

        if ( gpCollection == null ) {
            String msg = "No gene products for the gene, " + gene.getName() + " can be found.";
            return buildBadResponse( document, msg );
        }

        Long nt;
        int ntLength;
        boolean chromCheck = false;
        String strand = null;
        int gpCount = 0; // see how many gene products have physical locations annotated

        // each Gene Product

        for ( GeneProduct gp : gpCollection ) {
            // use PhysicalLocation accessor methods to get:
            // do we use getCdsPhysicalLocation() (from GeneProduct) or getPhysicalLocation() (from ChromosomeFeature)?
            pLoc = gp.getPhysicalLocation();
            if ( pLoc != null ) {
                // nucleotide length
                ntLength = pLoc.getNucleotideLength();

                // nucleotide
                nt = pLoc.getNucleotide();

                // strand (+ or -)
                strand = pLoc.getStrand();

                // see if start/end is max/min, then set minNT and maxNT
                if ( ( nt != null ) || ( ntLength != 0 ) || ( strand != null ) ) {
                    if ( ( maxNT != null ) ) {
                        if ( maxNT < getMaxNT( strand, nt, ntLength ) ) maxNT = getMaxNT( strand, nt, ntLength );
                    } else
                        // if (maxNT == null)
                        maxNT = getMaxNT( strand, nt, ntLength );

                    if ( ( minNT != null ) ) {
                        if ( minNT > getMinNT( strand, nt, ntLength ) ) minNT = getMinNT( strand, nt, ntLength );
                    } else
                        minNT = getMinNT( strand, nt, ntLength );
                }

                // chromosome id
                if ( chromCheck == false ) {
                    Chromosome chrom = pLoc.getChromosome();
                    chromId = chrom.getName();
                    chromCheck = true;
                }

                gpCount++;

            }

        } // for each gene product

        // if (minNT == -1 || maxNT == -1)
        // responseElement.appendChild(document.createTextNode("No nucleotide range for this physical location."));

        // build results in the form of a collection
        // Collection<String> physLoc = new ArrayList<String>();
        // physLoc.add(Long.toString(chromId));
        // physLoc.add(Long.toString(minNT));
        // physLoc.add(Long.toString(maxNT));
        Element wrapper = buildLocationWrapper( document, chromId, Long.toString( minNT ), Long.toString( maxNT ) );
        
        watch.stop();
        Long time = watch.getTime();
        log.info( "XML response for physical location result built in " + time + "ms." );
        return wrapper;
    }

    private Element buildLocationWrapper( Document document, String chrom, String min, String max ) {
       

        Element responseWrapper = document.createElementNS( NAMESPACE_URI, PLOC_LOCAL_NAME );
        Element responseElement = document.createElementNS( NAMESPACE_URI, PLOC_LOCAL_NAME + RESPONSE );
        responseWrapper.appendChild( responseElement );

        Element e1 = document.createElement( "chromId" );
        e1.appendChild( document.createTextNode( chrom ) );
        responseElement.appendChild( e1 );

        Element e2 = document.createElement( "minNT" );
        e2.appendChild( document.createTextNode( min ) );
        responseElement.appendChild( e2 );

        Element e3 = document.createElement( "maxNT" );
        e3.appendChild( document.createTextNode( max ) );
        responseElement.appendChild( e3 );

        

        return responseWrapper;
    }

    private Long getMaxNT( String strand, Long nt, int ntLength ) {
        if ( strand.equals( "-" ) )
            return nt;
        else {
            return ( nt + ntLength );
        }
    }

    private Long getMinNT( String strand, Long nt, int ntLength ) {
        if ( strand.equals( "-" ) )
            return ( nt - ntLength );
        else {
            return nt;
        }
    }

}
