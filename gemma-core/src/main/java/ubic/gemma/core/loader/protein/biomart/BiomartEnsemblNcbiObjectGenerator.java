/*
 * The Gemma project
 * 
 * Copyright (c) 2010 University of British Columbia
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
package ubic.gemma.core.loader.protein.biomart;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ubic.gemma.core.loader.protein.biomart.model.Ensembl2NcbiValueObject;
import ubic.gemma.core.loader.util.parser.LineParser;
import ubic.gemma.model.genome.Taxon;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Class that is responsible for generating a map of BioMartEnsembleNcbiObject value objects which are keyed on ensemble
 * protein id. This BioMartEnsembleNcbiObject object represents a mapping between ensemble protein ids, ensemble gene
 * ids and entrez gene ids.
 * If a bioMartFileName is supplied then biomart fetcher is not called and provided filename is used for parsing, in
 * this scenario only 1 taxon can be processed. If the bioMartFileName is null then all eligible taxa files are
 * downloaded from biomart. Eligible taxa are those that are in gemma and that have usable genes and that are species.
 * Once files have been downloaded or located then those files are parsed into BioMartEnsembleNcbi value objects
 * Note that Gemma now includes Ensembl ids imported for NCBI genes, using the gene2ensembl file provided by NCBI.
 *
 * @author ldonnison
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
public class BiomartEnsemblNcbiObjectGenerator {

    protected final Log log = LogFactory.getLog( BiomartEnsemblNcbiObjectGenerator.class );
    /**
     * Fetcher is called to download files if bioMartFileName is null
     */
    protected BiomartEnsemblNcbiFetcher biomartEnsemblNcbiFetcher;
    /**
     * A biomart parser which is constructed a new for each taxon due to slight file taxon differences
     */
    protected LineParser<Ensembl2NcbiValueObject> bioMartEnsemblNcbiParser;
    /**
     * If this file name is set then implies that file is local and no remote call should be made to biomart service
     */
    protected File bioMartFileName = null;

    /**
     * Constructor ensuring that fetcher is set. Even if not fetching files should be set so as to get attribute header
     * information for that particular taxon.
     */
    public BiomartEnsemblNcbiObjectGenerator() {
        this.biomartEnsemblNcbiFetcher = new BiomartEnsemblNcbiFetcher();
    }

    /**
     * Main method to generate a map of biomartEnsembleNcbiIds, involves optional fetch from biomart if no file is
     * provided then returns results of parse method. If the fetcher is called then all then all files for eligible
     * taxon are retrieved and the results returned as a map keyed on taxon. This map can be iterated through and files
     * parsed with the specific parser generated for that particular taxon. Currently only different for human. The
     * results for each taxon parsing are combined into a map of BioMartEnsembleNcbi value objects. * If a
     * bioMartFileName file is provided then no iteration is needed and the file is directly parsed.
     *
     * @param validTaxa taxa Taxa to retrieve biomart files for.
     * @return Map of BioMartEnsembleNcbi value objects keyed on ensemble peptide id.
     * @throws IOException if there is a problem while manipulating the file
     */
    public Map<String, Ensembl2NcbiValueObject> generate( Collection<Taxon> validTaxa ) throws IOException {
        // fetch the biomart files to process keyed on taxon
        if ( bioMartFileName == null || !bioMartFileName.canRead() ) {
            log.info( "No file name set or unreadable; fetching files from biomart " );
            return generateRemote( validTaxa );
        }
        log.info( "Processing: " + bioMartFileName );
        return parseTaxonBiomartFile( validTaxa.iterator().next(), bioMartFileName );

    }

    /**
     * @return Generates file from remote biomart location
     * @throws IOException if there is a problem while manipulating the file
     */
    public Map<String, Ensembl2NcbiValueObject> generateRemote( Collection<Taxon> validTaxa ) throws IOException {
        Map<String, Ensembl2NcbiValueObject> bioMartEnsemblNcbiIdsForValidAllGemmaTaxa = new HashMap<>();
        Map<Taxon, File> taxaBiomartFiles = this.biomartEnsemblNcbiFetcher.fetch( validTaxa );

        if ( taxaBiomartFiles != null && !taxaBiomartFiles.isEmpty() ) {
            for ( Taxon taxon : taxaBiomartFiles.keySet() ) {
                File fileForTaxon = taxaBiomartFiles.get( taxon );
                if ( fileForTaxon != null ) {
                    log.info( "Starting processing taxon " + taxon + " for file " + fileForTaxon );
                    Map<String, Ensembl2NcbiValueObject> map = parseTaxonBiomartFile( taxon, fileForTaxon );
                    bioMartEnsemblNcbiIdsForValidAllGemmaTaxa.putAll( map );
                } else {
                    log.error( "No biomart file retrieved for taxon " + taxon );
                }

            }
        } else {
            throw new RuntimeException( "No files could be downloaded from Biomart for provided taxon" );
        }
        return bioMartEnsemblNcbiIdsForValidAllGemmaTaxa;
    }

    /**
     * Method calls the parse method to parse a biomart file. The parser is configurable based on the taxon.
     *
     * @param taxon            Taxon for which file is for.
     * @param taxonBiomartFile The biomart file for given taxon
     * @return Map of BioMartEnsembleNcbi value objects for the given taxon keyed on ensembl peptide id
     */
    public Map<String, Ensembl2NcbiValueObject> parseTaxonBiomartFile( Taxon taxon, File taxonBiomartFile ) {
        // get the attributes in the file and set for this taxon
        String biomartTaxonName = this.biomartEnsemblNcbiFetcher.getBiomartTaxonName( taxon );
        Map<String, Ensembl2NcbiValueObject> map;
        if ( biomartTaxonName != null ) {
            String[] bioMartHeaderFields = this.biomartEnsemblNcbiFetcher
                    .attributesToRetrieveFromBioMartForProteinQuery( biomartTaxonName );
            // parse the file dependant upon the header files for this taxon
            BiomartEnsembleNcbiParser parser = new BiomartEnsembleNcbiParser( taxon, bioMartHeaderFields );

            // get the biomart file for this taxon
            try {
                parser.parse( taxonBiomartFile );
                map = parser.getMap();
                if ( map == null || map.isEmpty() ) {
                    throw new RuntimeException(
                            "No valid objects could be parsed from biomart for taxon " + taxon + " using file "
                                    + taxonBiomartFile );
                }

            } catch ( IOException e ) {
                throw new RuntimeException( e );
            }
        } else {
            throw new RuntimeException( "A biomart taxon name could not be constructed from supplied taxon " + taxon );
        }

        return map;
    }

    /**
     * @return Get the biomart file name can be null if retrieving all taxons
     */
    public File getBioMartFileName() {
        return bioMartFileName;
    }

    /**
     * Set a biomart file name can be null if retrieving all taxons
     *
     * @param bioMartFileName biomart file name
     */
    public void setBioMartFileName( File bioMartFileName ) {
        this.bioMartFileName = bioMartFileName;
    }

    /**
     * Should be set
     *
     * @return the bioMartEnsemblNcbiFetcher
     */
    public BiomartEnsemblNcbiFetcher getBioMartEnsemblNcbiFetcher() {
        return biomartEnsemblNcbiFetcher;
    }

    /**
     * @param biomartEnsemblNcbiFetcher the bioMartEnsemblNcbiFetcher to set
     */
    public void setBioMartEnsemblNcbiFetcher( BiomartEnsemblNcbiFetcher biomartEnsemblNcbiFetcher ) {
        this.biomartEnsemblNcbiFetcher = biomartEnsemblNcbiFetcher;
    }

}
