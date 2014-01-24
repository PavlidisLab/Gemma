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

import java.util.Collection;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;

import ubic.gemma.association.phenotype.PhenotypeAssociationManagerService;
import ubic.gemma.model.genome.gene.phenotype.valueObject.EvidenceValueObject;
import ubic.gemma.util.AbstractCLIContextCLI;

/**
 * When we need to delete all evidence from an external database, usually to reimport them after
 * 
 * @author nicolas
 */
public class DeleteEvidenceCLI extends AbstractCLIContextCLI {

    private PhenotypeAssociationManagerService phenotypeAssociationService = null;
    private String externalDatabaseName = "";

    // initArgument is only call when no argument is given on the command line, (it make it faster to run it in eclipse)
    private static String[] initArguments() {

        String[] args = new String[6];
        // user
        args[0] = "-u";
        args[1] = "administrator";
        // password
        args[2] = "-p";
        args[3] = "administrator";
        // what database name when want to delete
        args[4] = "-d";
        args[5] = "DGA";

        return args;
    }

    public static void main( String[] args ) {

        DeleteEvidenceCLI deleteEvidenceImporterCLI = new DeleteEvidenceCLI();

        try {
            Exception ex = null;

            String[] argsToTake = null;

            if ( args.length == 0 ) {
                argsToTake = initArguments();
            } else {
                argsToTake = args;
            }

            ex = deleteEvidenceImporterCLI.doWork( argsToTake );

            if ( ex != null ) {
                ex.printStackTrace();
            }
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    @Override
    protected Exception doWork( String[] args ) {

        Exception err = processCommandLine( "EvidenceImporterCLI", args );

        if ( err != null ) return err;

        try {
            loadServices();
        } catch ( Exception e ) {
            log.info( e.getMessage() );
        }

        Integer limit = 1000;
        
        log.info( "Loading "+limit+" evidence (this takes some time)" );

        Collection<EvidenceValueObject> evidenceToDelete = this.phenotypeAssociationService
                .loadEvidenceWithExternalDatabaseName( externalDatabaseName, limit );
        int i = 0;

        while ( evidenceToDelete.size() > 0 ) {
            for ( EvidenceValueObject e : evidenceToDelete ) {
                this.phenotypeAssociationService.remove( e.getId() );
                log.info( i++ );
            }
            evidenceToDelete = this.phenotypeAssociationService.loadEvidenceWithExternalDatabaseName(
                    externalDatabaseName, limit );
        }
        System.exit( -1 );
        return null;
    }

    protected synchronized void loadServices() throws Exception {
        this.phenotypeAssociationService = this.getBean( PhenotypeAssociationManagerService.class );
    }

    @Override
    protected void buildOptions() {
        @SuppressWarnings("static-access")
        Option databaseOption = OptionBuilder.withDescription( "External database name to be deleted" ).hasArg()
                .withArgName( "name of external database" ).isRequired().create( "d" );
        addOption( databaseOption );

    }

    @Override
    protected void processOptions() {
        super.processOptions();
        this.externalDatabaseName = new String( getOptionValue( 'd' ) );
    }

}
