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

import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.model.association.Gene2GOAssociationService;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * Tests multiple both parsing and loading.
 * 
 * @author keshav
 * @version $Id$
 */
public class NCBIGene2GOAssociationParserTest extends BaseSpringContextTest {

    private NCBIGene2GOAssociationLoader gene2GOAssLoader = null;

    @Autowired
    private Gene2GOAssociationService gene2GOAssociationService;

    /**
     * Configure parser and loader. Injecting the parser and loader with their dependencies.
     */
    @Before
    public void setup() {
        gene2GOAssociationService.removeAll();
        gene2GOAssLoader = new NCBIGene2GOAssociationLoader();
        gene2GOAssLoader.setParser( new NCBIGene2GOAssociationParser( taxonService.loadAll() ) );
        gene2GOAssLoader.setPersisterHelper( this.persisterHelper );
    }

    /**
     * Tests both the parser and the loader. This is more of an integration test, but since its dependencies are
     * localized to the Gemma project it has been added to the test suite.
     */
    @Test
    public void testParseAndLoad() throws Exception {

        try (InputStream is = this.getClass().getResourceAsStream( "/data/loader/association/gene2go.gz" );
                InputStream gZipIs = new GZIPInputStream( is );) {
            gene2GOAssLoader.load( gZipIs );

            gZipIs.close();
            is.close();
            int count = gene2GOAssLoader.getCount();

            /*
             * Actual count might vary depending on state of database (which taxa are available)
             */
            assertTrue( count >= 61 );
        }
    }

}
