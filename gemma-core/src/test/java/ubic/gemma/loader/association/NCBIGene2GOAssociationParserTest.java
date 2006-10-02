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
package ubic.gemma.loader.association;

import java.io.InputStream;
import java.util.Collection;
import java.util.zip.GZIPInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.association.Gene2GOAssociation;
import ubic.gemma.testing.BaseTransactionalSpringContextTest;

/**
 * Tests multiple both parsing and loading.
 * 
 * @author keshav
 * @version $Id$
 */
public class NCBIGene2GOAssociationParserTest extends BaseTransactionalSpringContextTest {
    protected Log log = LogFactory.getLog( NCBIGene2GOAssociationParserTest.class );

    NCBIGene2GOAssociationParser gene2GOAssParser = null;

    NCBIGene2GOAssociationLoader gene2GOAssLoader = null;

    /**
     * Tests both the parser and the loader. This is more of an integration test, but since its dependencies are
     * localized to the Gemma project it has been added to the test suite.
     */
    public void testParseAndLoad() throws Exception {

        InputStream is = this.getClass().getResourceAsStream( "/data/loader/association/gene2go.gz" );

        GZIPInputStream gZipIs = new GZIPInputStream( is );

        gene2GOAssParser.parse( gZipIs );

        Collection<Collection<Gene2GOAssociation>> results = gene2GOAssParser.getResults();

        Collection<Gene2GOAssociation> finalResults = gene2GOAssLoader.load( results );

        assertEquals( 21, finalResults.size() );

    }

    /**
     * Configure parser and loader. Provide "tomcat-esque" functionality by injecting the parser and loader with their
     * dependencies.
     */
    @Override
    protected void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();
        gene2GOAssParser = new NCBIGene2GOAssociationParser();
        gene2GOAssLoader = new NCBIGene2GOAssociationLoader();
        gene2GOAssLoader.setPersisterHelper( persisterHelper );
    }

}
