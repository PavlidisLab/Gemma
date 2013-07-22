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
package ubic.gemma.association.phenotype.externalDatabaseUpload;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.TreeSet;

import ubic.basecode.ontology.providers.DiseaseOntologyService;
import ubic.basecode.ontology.providers.HumanPhenotypeOntologyService;
import ubic.gemma.genome.gene.service.GeneService;
import ubic.gemma.model.genome.gene.phenotype.valueObject.GenericEvidenceValueObject;
import ubic.gemma.ontology.OntologyService;
import ubic.gemma.util.AbstractCLIContextCLI;

/**
 * TODO Document Me
 * 
 * @author Paul
 * @version $Id$
 */
public abstract class ExternalDatabaseEvidenceImporterAbstractCLI extends AbstractCLIContextCLI {

    public static final String CURRENT_PATH = "./gemma-core/src/main/java/ubic/gemma/association/phenotype/externalDatabaseUpload/Files/";
    // the annotator to search the NBCI web service
    protected DiseaseOntologyService diseaseOntologyService = null;
    protected HumanPhenotypeOntologyService humanPhenotypeOntologyService = null;
    protected OntologyService ontologyService = null;
    protected GeneService geneService = null;

    protected TreeSet<String> errorMessages = new TreeSet<String>();

    // *********************************************************************************************
    // OMIM VARIABLES
    public static final String OMIM_DONWLOAD = CURRENT_PATH + "OMIM/Downloaded/";
    public static final String OMIM_STATIC = CURRENT_PATH + "OMIM/StaticFiles/";

    // the disease ontology file will be use to find OMIM id
    public static final String DISEASE_ONT_PATH = "http://rest.bioontology.org/bioportal/virtual/download/";
    public static final String DISEASE_ONT_FILE = "1009?apikey=68835db8-b142-4c7d-9509-3c843849ad67";

    // the OMIM files
    public static final String OMIM_URL_PATH = "ftp://grcf.jhmi.edu/OMIM/";
    public static final String OMIM_FILE_MORBID = "morbidmap";
    public static final String OMIM_FILE_MIM = "mim2gene.txt";

    // manual static file mapping
    public static final String MANUAL_MAPPING_OMIM = OMIM_STATIC + "ManualDescriptionMapping.tsv";
    // terms to exclude when we search for a phenotype the second time
    public static final String EXCLUDE_KEYWORDS_OMIM = OMIM_STATIC + "KeyWordsToExclude.tsv";
    // results we exclude, we know those results are not good
    public static final String RESULTS_TO_IGNORE = OMIM_STATIC + "ResultsToIgnore.tsv";
    // when we find this description ingore it
    public static final String DESCRIPTION_TO_IGNORE = OMIM_STATIC + "DescriptionToIgnore.tsv";

    // *********************************************************************************************

    protected HashMap<String, ArrayList<GenericEvidenceValueObject>> omimIDGeneToEvidence = new HashMap<String, ArrayList<GenericEvidenceValueObject>>();

    protected synchronized void loadOntologyServices( String[] args ) throws Exception {

        // this get the context, so we can access beans
        Exception err = processCommandLine( "ExternalDatabaseEvidenceImporterCLI", args );
        if ( err != null ) throw err;

        // beans that we are interested in
        this.ontologyService = this.getBean( OntologyService.class );
        this.diseaseOntologyService = this.ontologyService.getDiseaseOntologyService();
        this.humanPhenotypeOntologyService = this.ontologyService.getHumanPhenotypeOntologyService();
        this.geneService = this.getBean( GeneService.class );

        while ( this.diseaseOntologyService.isOntologyLoaded() == false ) {
            wait( 3000 );
            System.out.println( "waiting for the Disease Ontology to load" );
        }

        while ( this.humanPhenotypeOntologyService.isOntologyLoaded() == false ) {
            wait( 3000 );
            System.out.println( "waiting for the HP Ontology to load" );
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

    protected String createWriteFolder( String externalDatabaseUse ) throws Exception {
        // where to put the final results
        String folderToWrite = null;

        if ( externalDatabaseUse.equalsIgnoreCase( "OMIM" ) ) {
            folderToWrite = OMIM_DONWLOAD + "OMIM" + "_" + getTodayDate();
        }

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

}
