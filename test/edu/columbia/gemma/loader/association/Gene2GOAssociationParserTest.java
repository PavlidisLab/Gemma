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
package edu.columbia.gemma.loader.association;

import java.io.InputStream;
import java.util.Collection;
import java.util.zip.GZIPInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.columbia.gemma.BaseDAOTestCase;
import edu.columbia.gemma.association.Gene2GOAssociationDao;
import edu.columbia.gemma.genome.TaxonDao;
import edu.columbia.gemma.loader.loaderutils.PersisterHelper;

/**
 * This test is more representative of integration testing than unit testing as it tests multiple both parsing and
 * 
 * @author keshav
 * @version $Id$
 */
public class Gene2GOAssociationParserTest extends BaseDAOTestCase {
    protected static final Log log = LogFactory.getLog( Gene2GOAssociationParserTest.class );

    Gene2GOAssociationParserImpl gene2GOAssParser = null;

    Gene2GOAssociationLoaderImpl gene2GOAssLoader = null;

    Collection<Object> gene2GOCol = null;

    TaxonDao taxonDao = null;

    /**
     * Tests both the parser and the loader. This is more of an integration test, but since its dependencies are
     * localized to the Gemma project it has been added to the test suite.
     */

    public void testParseAndLoad() throws Exception {

        InputStream is = this.getClass().getResourceAsStream( "/data/loader/association/gene2go.gz" );

        GZIPInputStream gZipIs = new GZIPInputStream( is );

        gene2GOAssParser.parse( gZipIs );

        gene2GOCol = gene2GOAssParser.getResults();

        gene2GOAssLoader.persist( gene2GOCol );

    }

    /**
     * Configure parser and loader. Provide "tomcat-esque" functionality by injecting the parser and loader with their
     * dependencies.
     */
    protected void setUp() throws Exception {
        super.setUp();

        gene2GOAssParser = new Gene2GOAssociationParserImpl();

        gene2GOAssLoader = new Gene2GOAssociationLoaderImpl();

        gene2GOAssLoader.setGene2GOAssociationDao( ( Gene2GOAssociationDao ) ctx.getBean( "gene2GOAssociationDao" ) );
        gene2GOAssLoader.setPersisterHelper( ( PersisterHelper ) ctx.getBean( "persisterHelper" ) );
    }

    /**
     * 
     */
    protected void tearDown() throws Exception {
        super.tearDown();

    }

}
