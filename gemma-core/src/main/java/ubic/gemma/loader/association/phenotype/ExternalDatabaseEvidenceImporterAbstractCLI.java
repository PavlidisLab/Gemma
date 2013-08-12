/*
 * The Gemma project
 * 
 * Copyright (c) 2013 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.loader.association.phenotype;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeSet;

import ubic.basecode.ontology.model.OntologyTerm;
import ubic.basecode.ontology.ncbo.AnnotatorResponse;
import ubic.basecode.ontology.providers.DiseaseOntologyService;
import ubic.basecode.ontology.providers.HumanPhenotypeOntologyService;
import ubic.gemma.genome.gene.service.GeneService;
import ubic.gemma.genome.taxon.service.TaxonService;
import ubic.gemma.ontology.OntologyService;
import ubic.gemma.util.AbstractCLIContextCLI;
import ubic.gemma.util.Settings;

/**
 * TODO Document Me
 * 
 * @author Paul
 * @version $Id$
 */
public abstract class ExternalDatabaseEvidenceImporterAbstractCLI extends AbstractCLIContextCLI {

    // this is where the results and files downloaded are put
    protected static final String WRITE_FOLDER = Settings.getString( "gemma.appdata.home" );
    // those are where the static resources are kept, like manual mapping files
    protected static final String RESOURCE_PATH = File.separator + "neurocarta" + File.separator
            + "externalDatabaseImporter" + File.separator;

    // ********************************************************************************
    // the services that will be needed
    protected DiseaseOntologyService diseaseOntologyService = null;
    protected HumanPhenotypeOntologyService humanPhenotypeOntologyService = null;
    protected OntologyService ontologyService = null;
    protected GeneService geneService = null;
    protected TaxonService taxonService = null;

    // keep all errors and show unique ones at the end of the program
    protected TreeSet<String> errorMessages = new TreeSet<String>();

    // *********************************************************************************************
    // the disease ontology file, we are interested in the MESH and OMIM Id in it ( we dont have this information in
    // gemma)
    protected static final String DISEASE_ONT_PATH = "http://rest.bioontology.org/bioportal/virtual/download/";
    protected static final String DISEASE_ONT_FILE = "1009?apikey=68835db8-b142-4c7d-9509-3c843849ad67";
    // found using the disease ontology file, OMIM ID --> ValuesUri
    protected HashMap<String, HashSet<String>> omimIdToPhenotypeMapping = new HashMap<String, HashSet<String>>();
    // found using the disease ontology file, MESH ID --> ValuesUri
    protected HashMap<String, HashSet<String>> meshIdToPhenotypeMapping = new HashMap<String, HashSet<String>>();

    // load all needed services
    protected synchronized void loadServices( String[] args ) throws Exception {

        // this gets the context, so we can access beans
        Exception err = processCommandLine( "ExternalDatabaseEvidenceImporterCLI", args );
        if ( err != null ) throw err;

        this.ontologyService = this.getBean( OntologyService.class );
        this.diseaseOntologyService = this.ontologyService.getDiseaseOntologyService();
        this.humanPhenotypeOntologyService = this.ontologyService.getHumanPhenotypeOntologyService();
        this.geneService = this.getBean( GeneService.class );
        this.taxonService = this.getBean( TaxonService.class );

        while ( this.diseaseOntologyService.isOntologyLoaded() == false ) {
            wait( 3000 );
            log.info( "waiting for the Disease Ontology to load" );
        }

        while ( this.humanPhenotypeOntologyService.isOntologyLoaded() == false ) {
            wait( 3000 );
            log.info( "waiting for the HP Ontology to load" );
        }
    }

    // the loadServices is in the constructor, we always need those
    public ExternalDatabaseEvidenceImporterAbstractCLI( String[] args ) throws Exception {
        super();
        loadServices( args );
    }

    @Override
    protected Exception doWork( String[] args ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected void buildOptions() {
        // TODO Auto-generated method stub
    }

    // creates the folder where the output files will be put, with today date
    protected String createWriteFolder( String externalDatabaseUse ) throws Exception {

        // where to put the final results
        String folderToWrite = WRITE_FOLDER + File.separator + externalDatabaseUse + "_" + getTodayDate();

        File folder = new File( folderToWrite );

        if ( !folder.mkdir() ) {
            throw new Exception( "having trouble to create a folder" );
        }
        return folderToWrite;
    }

    // creates the folder where the output files will be put, use this one if file is too big
    protected String createWriteFolderIfDoesntExist( String externalDatabaseUse ) throws Exception {

        // where to put the final results
        String folderToWrite = WRITE_FOLDER + File.separator + externalDatabaseUse;

        File folder = new File( folderToWrite );
        folder.mkdir();

        if ( !folder.exists() ) {
            throw new Exception( "having trouble to create a folder" );
        }
        return folderToWrite;
    }

    private String getTodayDate() {
        DateFormat dateFormat = new SimpleDateFormat( "yyyy-MM-dd_HH:mm" );
        Calendar cal = Calendar.getInstance();
        return dateFormat.format( cal.getTime() );
    }

    /**
     * Goes on the specified urlPath and download the file to place it into the writeFolder
     */
    protected String downloadFileFromWeb( String writeFolder, String pathName, String fileUrlName ) {

        String fileName = fileUrlName;

        String fullPathToDownload = pathName + fileUrlName;

        // here we change the name of this specific file so itcan be human readable
        if ( fileName.equalsIgnoreCase( DISEASE_ONT_FILE ) ) {
            fileName = "diseaseOntology.txt";
        }

        String pathFileName = writeFolder + File.separator + fileName;

        try {

            log.info( "Trying to download : " + fullPathToDownload );

            URL url = new URL( fullPathToDownload );
            url.openConnection();
            InputStream reader = url.openStream();

            FileOutputStream writer = new FileOutputStream( pathFileName );
            byte[] buffer = new byte[153600];
            int bytesRead = 0;

            while ( ( bytesRead = reader.read( buffer ) ) > 0 ) {
                writer.write( buffer, 0, bytesRead );
                buffer = new byte[153600];
            }

            writer.close();
            reader.close();
            log.info( "Download Completed" );

        } catch ( MalformedURLException e ) {
            e.printStackTrace();
        } catch ( IOException e ) {
            e.printStackTrace();
        }

        return pathFileName;
    }

    // is the valueUri existing or obsolete ?
    protected boolean isObsoleteOrNotExist( String valueUri ) {

        OntologyTerm o = this.diseaseOntologyService.getTerm( valueUri );
        if ( o == null ) {
            o = this.humanPhenotypeOntologyService.getTerm( valueUri );
        }

        if ( o == null || o.isTermObsolete() ) {
            return true;
        }

        return false;
    }

    // is the valueUri existing or obsolete ?
    protected OntologyTerm findOntologyTermExistAndNotObsolote( String valueUri ) {

        OntologyTerm o = this.diseaseOntologyService.getTerm( valueUri );
        if ( o == null ) {
            o = this.humanPhenotypeOntologyService.getTerm( valueUri );
        }

        if ( o == null || o.isTermObsolete() ) {
            return null;
        }

        return o;
    }

    // checks if a value uri usualy found with the annotator exists or obsolete, filters the results
    protected Collection<AnnotatorResponse> removeNotExistAndObsolete( Collection<AnnotatorResponse> annotatorResponses ) {

        Collection<AnnotatorResponse> annotatorResponseWithNoObsolete = new TreeSet<AnnotatorResponse>();

        for ( AnnotatorResponse annotatorResponse : annotatorResponses ) {

            if ( !isObsoleteOrNotExist( annotatorResponse.getValueUri() ) ) {
                annotatorResponseWithNoObsolete.add( annotatorResponse );
            }
        }
        return annotatorResponseWithNoObsolete;
    }

    /**
     * parse the disease ontology file to create the structure needed (omimIdToPhenotypeMapping and
     * meshIdToPhenotypeMapping)
     **/
    protected void findOmimAndMeshMappingUsingOntologyFile( String writeFolder ) throws IOException {

        // download the disease Ontology File
        String diseaseOntologyFile = downloadFileFromWeb( writeFolder, DISEASE_ONT_PATH, DISEASE_ONT_FILE );

        HashSet<String> omimIds = new HashSet<String>();
        HashSet<String> meshIds = new HashSet<String>();
        String valueUri = null;

        BufferedReader br = new BufferedReader( new FileReader( diseaseOntologyFile ) );

        String line = "";

        boolean foundTerm = false;

        while ( ( line = br.readLine() ) != null ) {

            String[] tokens = null;

            line = line.trim();

            // found a term
            if ( line.equalsIgnoreCase( "[Term]" ) ) {
                foundTerm = true;
                valueUri = null;
                omimIds = new HashSet<String>();
                meshIds = new HashSet<String>();
            } else if ( foundTerm ) {

                if ( line.startsWith( "id:" ) ) {

                    tokens = line.split( ":" );

                    String diseaseId = tokens[2].trim();
                    // will throw exception if a number is not found
                    Integer.parseInt( diseaseId );
                    // disease id
                    valueUri = "http://purl.obolibrary.org/obo/DOID_" + diseaseId;

                } else if ( line.indexOf( "xref: OMIM" ) != -1 ) {

                    tokens = line.split( ":" );
                    omimIds.add( tokens[2].trim() );
                } else if ( line.indexOf( "xref: MSH" ) != -1 ) {

                    tokens = line.split( ":" );
                    meshIds.add( tokens[2].trim() );
                }

                // end of a term
                else if ( line.equalsIgnoreCase( "" ) ) {

                    foundTerm = false;

                    for ( String omimId : omimIds ) {

                        HashSet<String> h = new HashSet<String>();

                        if ( omimIdToPhenotypeMapping.get( omimId ) == null ) {
                            if ( !isObsoleteOrNotExist( valueUri ) ) {
                                h.add( valueUri );
                            }
                        } else {
                            h = omimIdToPhenotypeMapping.get( omimId );
                            if ( !isObsoleteOrNotExist( valueUri ) ) {
                                h.add( valueUri );
                            }
                        }
                        omimIdToPhenotypeMapping.put( omimId, h );
                    }

                    for ( String meshId : meshIds ) {

                        HashSet<String> h = new HashSet<String>();

                        if ( meshIdToPhenotypeMapping.get( meshId ) == null ) {
                            if ( !isObsoleteOrNotExist( valueUri ) ) {
                                h.add( valueUri );
                            }
                        } else {
                            h = meshIdToPhenotypeMapping.get( meshId );
                            if ( !isObsoleteOrNotExist( valueUri ) ) {
                                h.add( valueUri );
                            }
                        }
                        meshIdToPhenotypeMapping.put( meshId, h );
                    }
                }
            }
        }
    }

    // transform the given line to its valueURi, ex: OMIM:1234= 1234-->valueUri
    protected String findValueUriWithDiseaseId( String diseaseId ) {

        Collection<String> valuesUri = new HashSet<String>();
        String allValueUri = "";

        if ( diseaseId.indexOf( "OMIM" ) != -1 ) {
            if ( omimIdToPhenotypeMapping.get( findId( diseaseId ) ) != null ) {
                valuesUri = omimIdToPhenotypeMapping.get( findId( diseaseId ) );
            }
        } else if ( diseaseId.indexOf( "MESH" ) != -1 ) {

            if ( meshIdToPhenotypeMapping.get( findId( diseaseId ) ) != null ) {
                valuesUri = meshIdToPhenotypeMapping.get( findId( diseaseId ) );
            }
        }

        if ( !valuesUri.isEmpty() ) {

            for ( String valueUri : valuesUri ) {
                allValueUri = allValueUri + valueUri + ";";
            }
        }

        return allValueUri;

    }

    private String findId( String txt ) {
        return txt.substring( txt.indexOf( ":" ) + 1, txt.length() );
    }

}
