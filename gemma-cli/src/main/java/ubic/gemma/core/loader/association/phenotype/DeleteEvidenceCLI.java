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
package ubic.gemma.core.loader.association.phenotype;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import ubic.gemma.core.apps.GemmaCLI.CommandGroup;
import ubic.gemma.core.association.phenotype.PhenotypeAssociationManagerService;
import ubic.gemma.core.util.AbstractAuthenticatedCLI;
import ubic.gemma.core.util.AbstractCLI;
import ubic.gemma.model.association.phenotype.PhenotypeAssociation;
import ubic.gemma.model.genome.gene.phenotype.valueObject.EvidenceValueObject;

import java.util.Collection;

/**
 * When we need to remove all evidence from an external database, usually to reimport them after
 *
 * @author nicolas
 */
public class DeleteEvidenceCLI extends AbstractAuthenticatedCLI {

    private String externalDatabaseName = "";
    private PhenotypeAssociationManagerService phenotypeAssociationService = null;

    @Override
    public CommandGroup getCommandGroup() {
        return CommandGroup.PHENOTYPES;
    }

    @Override
    public String getCommandName() {
        return "deletePhenotypes";
    }

    @Override
    public String getShortDesc() {
        return "Use to remove all evidence from an external database, usually to reimport them after";
    }

    @Override
    protected void buildOptions( Options options ) {
        @SuppressWarnings("static-access")
        Option databaseOption = Option.builder( "d" ).desc( "External database name (e.g. 'GWAS_Catalog', 'DGA' etc.)" ).hasArg()
                .argName( "name" ).required().build();
        options.addOption( databaseOption );

    }

    @Override
    protected void doWork() throws Exception {
        try {
            this.loadServices();
        } catch ( Exception e ) {
            AbstractCLI.log.info( e.getMessage() );
        }

        Integer limit = 1000;

        AbstractCLI.log.info( "Loading " + limit + " evidences to delete ..." );

        Collection<EvidenceValueObject<? extends PhenotypeAssociation>> evidenceToDelete = this.phenotypeAssociationService
                .loadEvidenceWithExternalDatabaseName( externalDatabaseName, limit, 0 );
        int i = 0;

        while ( evidenceToDelete.size() > 0 ) {
            for ( EvidenceValueObject<?> e : evidenceToDelete ) {
                this.phenotypeAssociationService.remove( e.getId() );
                //   AbstractCLI.log.info( i++ );
                i++;
            }

            // WTF?
            evidenceToDelete = this.phenotypeAssociationService
                    .loadEvidenceWithExternalDatabaseName( externalDatabaseName, limit, 0 );
        }
    }

    @Override
    protected void processOptions( CommandLine commandLine ) {
        this.externalDatabaseName = commandLine.getOptionValue( 'd' );
    }

    private synchronized void loadServices() {
        this.phenotypeAssociationService = this.getBean( PhenotypeAssociationManagerService.class );
    }

}
