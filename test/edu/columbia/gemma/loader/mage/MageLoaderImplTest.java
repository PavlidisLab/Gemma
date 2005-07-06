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
package edu.columbia.gemma.loader.mage;

import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.util.Collection;
import java.util.zip.ZipInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.columbia.gemma.BaseDAOTestCase;
import edu.columbia.gemma.common.auditAndSecurity.PersonDao;
import edu.columbia.gemma.common.description.OntologyEntryDao;
import edu.columbia.gemma.expression.arrayDesign.ArrayDesignDao;
import edu.columbia.gemma.expression.biomaterial.BioMaterialDao;
import edu.columbia.gemma.expression.experiment.ExpressionExperimentDao;
import edu.columbia.gemma.loader.expression.mage.MageLoaderImpl;
import edu.columbia.gemma.loader.expression.mage.MageMLParser;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class MageLoaderImplTest extends BaseDAOTestCase {
    private static Log log = LogFactory.getLog( MageLoaderImplTest.class.getName() );
    MageLoaderImpl ml;

    /*
     * (non-Javadoc)
     * 
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        ml = new MageLoaderImpl();
        ml.setBioMaterialDao( ( BioMaterialDao ) ctx.getBean( "bioMaterialDao" ) );
        ml.setExpressionExperimentDao( ( ExpressionExperimentDao ) ctx.getBean( "expressionExperimentDao" ) );
        ml.setPersonDao( ( PersonDao ) ctx.getBean( "personDao" ) );
        ml.setOntologyEntryDao( ( OntologyEntryDao ) ctx.getBean( "ontologyEntryDao" ) );
        ml.setArrayDesignDao( ( ArrayDesignDao ) ctx.getBean( "arrayDesignDao" ) );
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /*
     * Class under test for void create(Collection)
     */
    public void testCreateCollection() throws Exception {
        log.debug( "Parsing MAGE Jamboree example" );
        ZipInputStream istMageExamples = new ZipInputStream( MageMLParserTest.class
                .getResourceAsStream( "/data/mage/mageml-example.zip" ) );
        istMageExamples.getNextEntry();
        MageMLParser mlp = new MageMLParser();
        mlp.parse( istMageExamples );
        Collection result = mlp.getConvertedData();
        log.info( result.size() + " Objects parsed from the MAGE file." );
        log.info( "Tally:\n" + mlp );
        istMageExamples.close();
        ml.create( result );
    }

    /**
     * A real example of an experimental package.
     * 
     * @throws Exception
     */
    public void testCreateCollectionReal() throws Exception {
        log.debug( "Parsing MAGE from ArrayExpress" );
        InputStream istMageExamples = MageMLParserTest.class.getResourceAsStream( "/data/mage/E-WMIT-4.xml" );
        MageMLParser mlp = new MageMLParser();
        mlp.parse( istMageExamples );
        Collection result = mlp.getConvertedData();
        log.info( result.size() + " Objects parsed from the MAGE file." );
        log.info( "Tally:\n" + mlp );
        istMageExamples.close();
        ml.create( result );
    }

}
