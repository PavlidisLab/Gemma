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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.TreeSet;

import ubic.basecode.ontology.model.OntologyTerm;
import ubic.basecode.ontology.ncbo.AnnotatorResponse;
import ubic.basecode.ontology.providers.DiseaseOntologyService;
import ubic.basecode.ontology.providers.HumanPhenotypeOntologyService;
import ubic.gemma.genome.gene.service.GeneService;
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

    protected static final String WRITE_FOLDER = Settings.getString( "gemma.appdata.home" );

    // the disease ontology file, to get the OMIM ID it is used for now
    protected static final String DISEASE_ONT_PATH = "http://rest.bioontology.org/bioportal/virtual/download/";
    protected static final String DISEASE_ONT_FILE = "1009?apikey=68835db8-b142-4c7d-9509-3c843849ad67";

    // ********************************************************************************
    // the services that will be needed
    protected DiseaseOntologyService diseaseOntologyService = null;
    protected HumanPhenotypeOntologyService humanPhenotypeOntologyService = null;
    protected OntologyService ontologyService = null;
    protected GeneService geneService = null;

    // keep all errors to show unique ones at the end
    protected TreeSet<String> errorMessages = new TreeSet<String>();

    // *********************************************************************************************

    // load all needed services
    protected synchronized void loadServices( String[] args ) throws Exception {

        // this get the context, so we can access beans
        Exception err = processCommandLine( "ExternalDatabaseEvidenceImporterCLI", args );
        if ( err != null ) throw err;

        this.ontologyService = this.getBean( OntologyService.class );
        this.diseaseOntologyService = this.ontologyService.getDiseaseOntologyService();
        this.humanPhenotypeOntologyService = this.ontologyService.getHumanPhenotypeOntologyService();
        this.geneService = this.getBean( GeneService.class );

        while ( this.diseaseOntologyService.isOntologyLoaded() == false ) {
            wait( 3000 );
            log.info( "waiting for the Disease Ontology to load" );
        }

        while ( this.humanPhenotypeOntologyService.isOntologyLoaded() == false ) {
            wait( 3000 );
            log.info( "waiting for the HP Ontology to load" );
        }
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

    // creates the folder where the output files will be put
    protected String createWriteFolder( String externalDatabaseUse ) throws Exception {

        // where to put the final results
        String folderToWrite = WRITE_FOLDER + File.separator + externalDatabaseUse + "_" + getTodayDate();

        File folder = new File( folderToWrite );

        if ( !folder.mkdir() ) {
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

    // checks if a value uri usualy found with the annotator exists or obsolete
    protected Collection<AnnotatorResponse> removeNotExistAndObsolete( Collection<AnnotatorResponse> annotatorResponses ) {

        Collection<AnnotatorResponse> annotatorResponseWithNoObsolete = new TreeSet<AnnotatorResponse>();

        for ( AnnotatorResponse annotatorResponse : annotatorResponses ) {

            if ( !isObsoleteOrNotExist( annotatorResponse.getValueUri() ) ) {
                annotatorResponseWithNoObsolete.add( annotatorResponse );
            }
        }
        return annotatorResponseWithNoObsolete;
    }

}
