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
package edu.columbia.gemma;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import junit.framework.TestCase;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.SystemConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanFactory;

import edu.columbia.gemma.common.auditAndSecurity.AuditTrailService;
import edu.columbia.gemma.common.auditAndSecurity.ContactService;
import edu.columbia.gemma.common.auditAndSecurity.PersonService;
import edu.columbia.gemma.common.description.DatabaseEntryService;
import edu.columbia.gemma.common.description.ExternalDatabaseService;
import edu.columbia.gemma.common.description.LocalFileService;
import edu.columbia.gemma.common.description.OntologyEntryService;
import edu.columbia.gemma.common.measurement.MeasurementService;
import edu.columbia.gemma.common.protocol.HardwareService;
import edu.columbia.gemma.common.protocol.ProtocolService;
import edu.columbia.gemma.common.protocol.SoftwareService;
import edu.columbia.gemma.common.quantitationtype.QuantitationTypeService;
import edu.columbia.gemma.expression.arrayDesign.ArrayDesignService;
import edu.columbia.gemma.expression.bioAssay.BioAssayService;
import edu.columbia.gemma.expression.bioAssayData.BioAssayDimensionService;
import edu.columbia.gemma.expression.bioAssayData.BioMaterialDimensionService;
import edu.columbia.gemma.expression.bioAssayData.DesignElementDimensionService;
import edu.columbia.gemma.expression.biomaterial.BioMaterialService;
import edu.columbia.gemma.expression.biomaterial.CompoundService;
import edu.columbia.gemma.expression.designElement.CompositeSequenceService;
import edu.columbia.gemma.expression.designElement.ReporterService;
import edu.columbia.gemma.expression.experiment.ExpressionExperimentService;
import edu.columbia.gemma.expression.experiment.FactorValueService;
import edu.columbia.gemma.genome.TaxonService;
import edu.columbia.gemma.genome.biosequence.BioSequenceService;
import edu.columbia.gemma.loader.loaderutils.PersisterHelper;
import edu.columbia.gemma.util.SpringContextUtil;

/**
 * Base class for running DAO tests. Based partly on code from Appfuse.
 * 
 * @author mraible
 * @author pavlidis
 * @version $Id$
 */
public class BaseDAOTestCase extends TestCase {
    protected final Log log = LogFactory.getLog( getClass() );
    protected final static BeanFactory ctx = SpringContextUtil.getXmlWebApplicationContext( true );
    protected ResourceBundle rb;
    protected CompositeConfiguration config;

    /*
     * (non-Javadoc)
     * 
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        SpringContextUtil.grantAuthorityForTests();
    }

    public BaseDAOTestCase() {
        // Since a ResourceBundle is not required for each class, just
        // do a simple check to see if one exists
        String className = this.getClass().getName();

        try {
            config = new CompositeConfiguration();
            config.addConfiguration( new SystemConfiguration() );
            config.addConfiguration( new PropertiesConfiguration( "build.properties" ) );
            rb = ResourceBundle.getBundle( className ); // will look for <className>.properties
        } catch ( MissingResourceException mre ) {
            // log.warn("No resource bundle found for: " + className);
        } catch ( ConfigurationException e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * Utility method to populate a javabean-style object with values from a Properties file
     * 
     * @param obj
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    protected Object populate( Object obj ) throws Exception {
        // loop through all the beans methods and set its properties from
        // its .properties file
        Map map = new HashMap();

        for ( Enumeration keys = rb.getKeys(); keys.hasMoreElements(); ) {
            String key = ( String ) keys.nextElement();
            map.put( key, rb.getString( key ) );
        }

        BeanUtils.copyProperties( obj, map );

        return obj;
    }

    /**
     * Supply a configured PersisterHelper.
     * 
     * @return
     */
    protected PersisterHelper getPersisterHelper() {
        PersisterHelper ml = new PersisterHelper();
        ml.setBioMaterialService( ( BioMaterialService ) ctx.getBean( "bioMaterialService" ) );
        ml
                .setExpressionExperimentService( ( ExpressionExperimentService ) ctx
                        .getBean( "expressionExperimentService" ) );
        ml.setPersonService( ( PersonService ) ctx.getBean( "personService" ) );
        ml.setOntologyEntryService( ( OntologyEntryService ) ctx.getBean( "ontologyEntryService" ) );
        ml.setArrayDesignService( ( ArrayDesignService ) ctx.getBean( "arrayDesignService" ) );
        ml.setExternalDatabaseService( ( ExternalDatabaseService ) ctx.getBean( "externalDatabaseService" ) );
        ml.setReporterService( ( ReporterService ) ctx.getBean( "reporterService" ) );
        ml.setCompositeSequenceService( ( CompositeSequenceService ) ctx.getBean( "compositeSequenceService" ) );
        ml.setProtocolService( ( ProtocolService ) ctx.getBean( "protocolService" ) );
        ml.setHardwareService( ( HardwareService ) ctx.getBean( "hardwareService" ) );
        ml.setSoftwareService( ( SoftwareService ) ctx.getBean( "softwareService" ) );
        ml.setTaxonService( ( TaxonService ) ctx.getBean( "taxonService" ) );
        ml.setBioAssayService( ( BioAssayService ) ctx.getBean( "bioAssayService" ) );
        ml.setQuantitationTypeService( ( QuantitationTypeService ) ctx.getBean( "quantitationTypeService" ) );
        ml.setLocalFileService( ( LocalFileService ) ctx.getBean( "localFileService" ) );
        ml.setCompoundService( ( CompoundService ) ctx.getBean( "compoundService" ) );
        ml.setDatabaseEntryService( ( DatabaseEntryService ) ctx.getBean( "databaseEntryService" ) );
        ml.setContactService( ( ContactService ) ctx.getBean( "contactService" ) );
        ml.setBioSequenceService( ( BioSequenceService ) ctx.getBean( "bioSequenceService" ) );
        ml.setFactorValueService( ( FactorValueService ) ctx.getBean( "factorValueService" ) );
        ml.setBioAssayDimensionService( ( BioAssayDimensionService ) ctx.getBean( "bioAssayDimensionService" ) );
        ml.setAuditTrailService( ( AuditTrailService ) ctx.getBean( "auditTrailService" ) );
        ml.setDesignElementDimensionService( ( DesignElementDimensionService ) ctx
                .getBean( "designElementDimensionService" ) );
        ml
                .setBioMaterialDimensionService( ( BioMaterialDimensionService ) ctx
                        .getBean( "bioMaterialDimensionService" ) );
        ml.setMeasurementService( ( MeasurementService ) ctx.getBean( "measurementService" ) );
        return ml;
    }
}