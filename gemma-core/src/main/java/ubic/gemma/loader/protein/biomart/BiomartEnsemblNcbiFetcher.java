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
package ubic.gemma.loader.protein.biomart;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.util.FileTools;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.util.Settings;

/**
 * BioMart is a query-oriented data management system. In our particular case we are using it to map ensembl, ncbi and
 * hgnc ids. To construct the query we pass the taxon and the attributes we wish to query for. Note the formating of
 * taxon for biomart consists of latin name without the point e.g. hsapiens For more information visit the site:
 * {@link http://www.biomart.org/martservice.html}
 * <p>
 * Note that Gemma now includes Ensembl ids imported for NCBI genes, using the gene2ensembl file provided by NCBI.
 * 
 * @author ldonnison
 * @version $Id$
 */
public class BiomartEnsemblNcbiFetcher {

    private String urlBiomartService = "";
    public final static String BIOMARTPATH = "protein.biomart.remotepath";
    private static final String BIOMART = "biomart";
    private static final String FILESEPARATOR = "\t";
    private static final int READ_TIMEOUT_SECONDS = 30;
    private static Log log = LogFactory.getLog( BiomartEnsemblNcbiFetcher.class );

    public BiomartEnsemblNcbiFetcher() {
        this.initConfig();
    }

    /**
     * Main method that iterates through each taxon supplied and calls the fetch method for each taxon. Which returns a
     * biomart file for each taxon supplied.
     * 
     * @param taxa Collection of taxa to retrieve biomart files for.
     * @return A map of biomart files as stored on local file system keyed on taxon.
     * @throws IOException
     */
    public Map<Taxon, File> fetch( Collection<Taxon> taxa ) throws IOException {
        Map<Taxon, File> taxonFileMap = new HashMap<Taxon, File>();
        String taxonName = "";
        File taxonFile = null;

        for ( Taxon taxon : taxa ) {
            taxonName = this.getBiomartTaxonName( taxon );
            if ( taxonName != null ) {
                taxonFile = fetchFileForProteinQuery( taxonName );
                taxonFileMap.put( taxon, taxonFile );
                log.debug( "Downloading file " + taxonFile + "for taxon " + taxon );
            }
        }
        return taxonFileMap;
    }

    /**
     * Given a biomart taxon formatted name fetch the file from biomart and save as a local file.
     * 
     * @return
     * @throws Exception
     */
    public File fetchFileForProteinQuery( String bioMartTaxonName ) throws IOException {
        log.info( "Retrieving biomart file for taxon " + bioMartTaxonName + " from url " + urlBiomartService );
        String xmlQueryString = getXmlQueryAsStringForProteinQuery( bioMartTaxonName );
        URL url;
        String data;
        try {
            url = new URL( urlBiomartService );
            data = URLEncoder.encode( "query", "UTF-8" ) + "=" + URLEncoder.encode( xmlQueryString, "UTF-8" );
        } catch ( MalformedURLException e ) {
            throw new RuntimeException( e );
        } catch ( UnsupportedEncodingException e ) {
            throw new RuntimeException( e );
        }

        try (BufferedReader biomartBufferedReader = this.readFile( url, data );) {
            File EnsemblEntrezHGCNProteinMappingFile = this.getFileName( bioMartTaxonName );
            String headerForEnsemblEntrezHGCNProteinMapping = getHeaderForBiomartFileForProteinQuery( bioMartTaxonName );

            return this.writeFile( EnsemblEntrezHGCNProteinMappingFile, headerForEnsemblEntrezHGCNProteinMapping,
                    biomartBufferedReader );
        } catch ( Exception e ) {
            throw new IOException( "Could not download: " + url, e );
        }

    }

    /**
     * Constructs an xml query for biomart. This can be generated from the biomart site. The attributes sit under
     * attributes filter external
     * 
     * @param
     * @return String of xml populated with taxon
     */
    protected String getXmlQueryAsStringForProteinQuery( String biomartTaxonName ) {
        StringBuilder xmlQuery = new StringBuilder( "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" );
        xmlQuery.append( "<!DOCTYPE Query>" );
        xmlQuery.append( "<Query  virtualSchemaName = \"default\" formatter = \"TSV\" header = \"0\" uniqueRows = \"0\" count = \"\" datasetConfigVersion = \"0.6\" >" );
        xmlQuery.append( "<Dataset name = \"" + biomartTaxonName + "_gene_ensembl\" interface = \"default\" >" );
        for ( String attributes : attributesToRetrieveFromBioMartForProteinQuery( biomartTaxonName ) ) {
            if ( attributes != null && !( attributes.isEmpty() ) ) {
                xmlQuery.append( "<Attribute name = \"" + attributes + "\" />" );
            }
        }
        xmlQuery.append( "</Dataset>" );
        xmlQuery.append( "</Query>" );
        if ( log.isDebugEnabled() ) log.debug( "Biomart query was:\n" + xmlQuery.toString() );
        return xmlQuery.toString();
    }

    /**
     * Method that based on the taxon supplied constructs an array of attributes that can be queried on. For example if
     * hsapiens is supplied then hgnc_id can be supplied as a query parameter.
     * 
     * @param biomartTaxonName Biomart formatted taxon name
     * @return An Array of strings representing the attributes that can be used to query biomart.
     */
    public String[] attributesToRetrieveFromBioMartForProteinQuery( String biomartTaxonName ) {
        String[] attributesToGet = new String[] { "ensembl_gene_id", "ensembl_transcript_id", "entrezgene",
                "ensembl_peptide_id", "" };
        // only add hgnc if it is human taxon
        if ( biomartTaxonName.equals( "hsapiens" ) || biomartTaxonName.equals( "H.sapiens" ) ) {
            attributesToGet[attributesToGet.length - 1] = "hgnc_id";
        }
        return attributesToGet;
    }

    /**
     * Method to construct a header for the downloaded biomart file. The file that comes from biomart has no header so
     * this documents what attributes have been queried for, this can be taxon specific
     * 
     * @param biomartTaxonName The taxon queried for
     * @return Header line for biomart file e.g. ensembl_gene_id ensembl_transcript_id entrezgene ensembl_peptide_id
     */
    protected String getHeaderForBiomartFileForProteinQuery( String biomartTaxonName ) {
        StringBuilder header = new StringBuilder();
        for ( String attributes : attributesToRetrieveFromBioMartForProteinQuery( biomartTaxonName ) ) {
            header.append( attributes ).append( FILESEPARATOR );
        }
        // removes any white space at end
        return header.toString().trim();
    }

    /**
     * Submit a xml query to biomart service return the returned data as a bufferedreader
     * 
     * @param urlToRead Biomart configured URL
     * @param data The query data for biomart
     * @return BufferedReader Stream to read data from
     */
    private BufferedReader readFile( URL urlToRead, String data ) {
        URLConnection conn = null;
        try {
            conn = urlToRead.openConnection();
            conn.setReadTimeout( 1000 * READ_TIMEOUT_SECONDS );
            conn.setDoOutput( true );
            try (Writer writer = new OutputStreamWriter( conn.getOutputStream() );) {
                writer.write( data );
                writer.flush();
                return new BufferedReader( new InputStreamReader( conn.getInputStream() ) );
            }
        } catch ( IOException e ) {
            log.error( e );
            throw new RuntimeException( e );
        }
    }

    /**
     * Method reads data returned from biomart and writes to file adding a header containing the queried attributes.
     * 
     * @param biomartTaxonName Taxon name configured for biomart
     * @param reader The reader for reading data returned from biomart
     * @return File the biomart data written to file
     * @throws IOException Problem writing to file
     */
    private File writeFile( File file, String headerForFile, BufferedReader reader ) throws IOException {

        try (BufferedWriter writer = new BufferedWriter( new FileWriter( file ) );) {
            writer.append( headerForFile + "\n" );
            String line;
            while ( ( line = reader.readLine() ) != null ) {
                if ( line.contains( "ERROR" ) && line.contains( "Exception" ) ) {
                    throw new IOException( "Error from BioMart: " + line );
                }
                writer.append( line + "\n" );
            }
        }
        reader.close();
        return file;

    }

    /**
     * Biomart taxon names are formated as the scientific name all lowercase with the genus name shortened to one letter
     * and appended to species name E.g. Homo sapiens >hsapiens
     * 
     * @param Gemma taxon object
     * @return Biomart taxon formated name.
     * @exception The taxon does not contain a valid scientific name.
     */
    public String getBiomartTaxonName( Taxon gemmaTaxon ) {
        String biomartTaxonName = null;
        if ( gemmaTaxon == null || gemmaTaxon.getScientificName().isEmpty() ) {
            log.error( "Taxon not valid no scientific name set" + gemmaTaxon );
        } else {
            String[] taxonName = gemmaTaxon.getScientificName().split( " " );

            if ( taxonName.length == 2 ) {
                // take first character of genus
                biomartTaxonName = taxonName[0].substring( 0, 1 );
                // take full species name and trim
                biomartTaxonName = biomartTaxonName.concat( taxonName[1].trim() );
                biomartTaxonName = biomartTaxonName.toLowerCase();

            } else {
                throw new RuntimeException( "Taxon scientific name is not the correct formatt" );
            }
        }
        return biomartTaxonName;
    }

    /**
     * Method that gets the configured download path and constructs the file name of the biomart file Which is biomart +
     * biomarttaxonaname + .txt. If a biomart directory does not exist then create it.
     * 
     * @param biomartTaxonName The biomart configured taxon name
     * @return File path to newly created biomart file on local system.
     */
    protected File getFileName( String biomartTaxonName ) {
        String localBasePath = Settings.getDownloadPath();
        String directory = localBasePath + File.separator + BIOMART + File.separator;
        String fileName = BIOMART + biomartTaxonName + ".txt";
        FileTools.createDir( directory );
        return new File( directory + fileName );
    }

    /**
     * Configure the URL for biomart
     * 
     * @throws ConfigurationException one of the file download paths in the properties file was not configured
     *         correctly.
     */
    public void initConfig() {

        urlBiomartService = Settings.getString( BIOMARTPATH );
        if ( urlBiomartService == null || urlBiomartService.length() == 0 )
            throw new RuntimeException( new ConfigurationException( BIOMARTPATH + " was null or empty" ) );
    }

}
