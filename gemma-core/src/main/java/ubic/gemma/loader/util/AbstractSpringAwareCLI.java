/*
 * The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
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
package ubic.gemma.loader.util;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.springframework.beans.factory.BeanFactory;

import ubic.gemma.loader.util.persister.PersisterHelper;
import ubic.gemma.model.common.auditAndSecurity.ContactService;
import ubic.gemma.model.common.auditAndSecurity.PersonService;
import ubic.gemma.model.common.description.DatabaseEntryService;
import ubic.gemma.model.common.description.ExternalDatabaseService;
import ubic.gemma.model.common.description.LocalFileService;
import ubic.gemma.model.common.description.OntologyEntryService;
import ubic.gemma.model.common.protocol.HardwareService;
import ubic.gemma.model.common.protocol.ProtocolService;
import ubic.gemma.model.common.protocol.SoftwareService;
import ubic.gemma.model.common.quantitationtype.QuantitationTypeService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.bioAssay.BioAssayService;
import ubic.gemma.model.expression.biomaterial.BioMaterialService;
import ubic.gemma.model.expression.biomaterial.CompoundService;
import ubic.gemma.model.expression.designElement.CompositeSequenceService;
import ubic.gemma.model.expression.designElement.ReporterService;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.expression.experiment.FactorValueService;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.model.genome.biosequence.BioSequenceService;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.security.authentication.ManualAuthenticationProcessing;
import ubic.gemma.util.SpringContextUtil;

/**
 * Subclass this to create command line interface (CLI) tools that need a Spring context. A standard set of CLI options
 * are provided to manage authentication.
 * 
 * @author pavlidis
 * @version $Id$
 */
public abstract class AbstractSpringAwareCLI extends AbstractCLI {

    protected static BeanFactory ctx = null;
    PersisterHelper ph = null;

    public AbstractSpringAwareCLI() {
        this.buildStandardOptions();
        this.buildOptions();
    }

    @SuppressWarnings("static-access")
    @Override
    protected void buildStandardOptions() {
        // TODO Auto-generated method stub
        super.buildStandardOptions();
        Option usernameOpt = OptionBuilder.withArgName( "user" ).isRequired().withLongOpt( "user" ).hasArg()
                .withDescription( "User name for accessing the system" ).create( 'u' );

        Option passwordOpt = OptionBuilder.withArgName( "passwd" ).isRequired().withLongOpt( "password" ).hasArg()
                .withDescription( "Password for accessing the system" ).create( 'p' );
        options.addOption( usernameOpt );
        options.addOption( passwordOpt );

    }

    /**
     * @return
     */
    protected PersisterHelper getPersisterHelper() {
        if ( ph != null ) {
            return ph;
        }

        assert ctx != null : "Spring context was not initialized";
        ph = new PersisterHelper();
        ph.setBioMaterialService( ( BioMaterialService ) ctx.getBean( "bioMaterialService" ) );
        ph
                .setExpressionExperimentService( ( ExpressionExperimentService ) ctx
                        .getBean( "expressionExperimentService" ) );
        ph.setPersonService( ( PersonService ) ctx.getBean( "personService" ) );
        ph.setOntologyEntryService( ( OntologyEntryService ) ctx.getBean( "ontologyEntryService" ) );
        ph.setArrayDesignService( ( ArrayDesignService ) ctx.getBean( "arrayDesignService" ) );
        ph.setExternalDatabaseService( ( ExternalDatabaseService ) ctx.getBean( "externalDatabaseService" ) );
        ph.setReporterService( ( ReporterService ) ctx.getBean( "reporterService" ) );
        ph.setCompositeSequenceService( ( CompositeSequenceService ) ctx.getBean( "compositeSequenceService" ) );
        ph.setProtocolService( ( ProtocolService ) ctx.getBean( "protocolService" ) );
        ph.setHardwareService( ( HardwareService ) ctx.getBean( "hardwareService" ) );
        ph.setSoftwareService( ( SoftwareService ) ctx.getBean( "softwareService" ) );
        ph.setTaxonService( ( TaxonService ) ctx.getBean( "taxonService" ) );
        ph.setBioAssayService( ( BioAssayService ) ctx.getBean( "bioAssayService" ) );
        ph.setQuantitationTypeService( ( QuantitationTypeService ) ctx.getBean( "quantitationTypeService" ) );
        ph.setLocalFileService( ( LocalFileService ) ctx.getBean( "localFileService" ) );
        ph.setCompoundService( ( CompoundService ) ctx.getBean( "compoundService" ) );
        ph.setDatabaseEntryService( ( DatabaseEntryService ) ctx.getBean( "databaseEntryService" ) );
        ph.setContactService( ( ContactService ) ctx.getBean( "contactService" ) );
        ph.setBioSequenceService( ( BioSequenceService ) ctx.getBean( "bioSequenceService" ) );
        ph.setFactorValueService( ( FactorValueService ) ctx.getBean( "factorValueService" ) );
        ph.setGeneService( ( GeneService ) ctx.getBean( "geneService" ) );
        return ph;
    }

    /** check username and password. */
    static void authenticate() {

        if ( commandLine.hasOption( 'u' ) ) {
            if ( commandLine.hasOption( 'p' ) ) {
                String username = commandLine.getOptionValue( 'u' );
                String password = commandLine.getOptionValue( 'p' );
                ManualAuthenticationProcessing manAuthentication = ( ManualAuthenticationProcessing ) ctx
                        .getBean( "manualAuthenticationProcessing" );
                manAuthentication.validateRequest( username, password );
            }
        } else {
            log.error( "Not authenticated. Make sure you entered a valid username and/or password" );
            System.exit( 0 );
        }

    }

    /** check if using test or production context */
    static void setTestOrProduction() {

        if ( commandLine.hasOption( "testing" ) ) {
            ctx = SpringContextUtil.getApplicationContext( true );
        } else {
            ctx = SpringContextUtil.getApplicationContext( false );
        }

    }

    @Override
    protected void processOptions() {
        setTestOrProduction();
        authenticate();
    }

}
