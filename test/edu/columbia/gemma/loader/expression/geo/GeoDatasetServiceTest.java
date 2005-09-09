/*
 * The Gemma project
 * 
 * Copyright (c) 2005 Columbia University
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
package edu.columbia.gemma.loader.expression.geo;

import java.io.File;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;

import edu.columbia.gemma.BaseDAOTestCase;
import edu.columbia.gemma.common.auditAndSecurity.PersonDao;
import edu.columbia.gemma.common.description.ExternalDatabaseDao;
import edu.columbia.gemma.common.description.OntologyEntryDao;
import edu.columbia.gemma.expression.arrayDesign.ArrayDesignDao;
import edu.columbia.gemma.expression.biomaterial.BioMaterialDao;
import edu.columbia.gemma.expression.designElement.DesignElementDao;
import edu.columbia.gemma.expression.experiment.ExpressionExperimentDao;
import edu.columbia.gemma.loader.expression.PersisterHelper;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class GeoDatasetServiceTest extends BaseDAOTestCase {

    /** This is an integration test */
    public void testFetchAndLoad() throws Exception {

        Configuration config = new PropertiesConfiguration( "Gemma.properties" );
        String path = ( String ) config.getProperty( "geo.local.datafile.basepath" );
        if ( !( new File( path ).canRead() ) ) {
            log.warn( "Skipping test, required path " + path + "not writable" );
            return;
        }

        GeoDatasetService gds = new GeoDatasetService();
        PersisterHelper ml = new PersisterHelper();
        GeoConverter geoConv = new GeoConverter();
        ml.setBioMaterialDao( ( BioMaterialDao ) ctx.getBean( "bioMaterialDao" ) );
        ml.setExpressionExperimentDao( ( ExpressionExperimentDao ) ctx.getBean( "expressionExperimentDao" ) );
        ml.setPersonDao( ( PersonDao ) ctx.getBean( "personDao" ) );
        ml.setOntologyEntryDao( ( OntologyEntryDao ) ctx.getBean( "ontologyEntryDao" ) );
        ml.setArrayDesignDao( ( ArrayDesignDao ) ctx.getBean( "arrayDesignDao" ) );
        ml.setExternalDatabaseDao( ( ExternalDatabaseDao ) ctx.getBean( "externalDatabaseDao" ) );
        ml.setDesignElementDao( ( DesignElementDao ) ctx.getBean( "designElementDao" ) );
        gds.setPersister( ml );
        gds.setConverter( geoConv );
        gds.fetchAndLoad( "GDS100" );
    }
}
