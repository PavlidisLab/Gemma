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
package ubic.gemma.loader.expression.mage;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipInputStream;

import ubic.gemma.testing.BaseSpringContextTest;

/**
 * @author pavlidis
 * @version $Id$
 */
public abstract class AbstractMageTest extends BaseSpringContextTest {

    protected static final String MAGE_DATA_RESOURCE_PATH = "/data/loader/expression/mage/";

    /**
     * XSL-transform the mage document. This is only needed for testing. In production, this is done as part of the
     * parsing.
     * 
     * @param mlp
     * @param resourceName
     * @throws IOException
     */
    protected void xslSetup( MageMLParser mlp, String resourceName ) throws IOException {
        InputStream istMageExamples = MageMLParserTest.class.getResourceAsStream( resourceName );
        assert istMageExamples != null;
        // mlp.createSimplifiedXml( istMageExamples );
        istMageExamples.close();
    }

    /**
     * XSL-transform the mage document. This is only needed for testing. In production, this is done as part of the
     * parsing.
     * 
     * @param mlp
     * @param resourceName
     * @throws IOException
     */
    protected void zipXslSetup( MageMLParser mlp, String resourceName ) throws IOException {
        ZipInputStream istMageExamples = new ZipInputStream( MageMLParserTest.class.getResourceAsStream( resourceName ) );
        istMageExamples.getNextEntry();
        // mlp.createSimplifiedXml( istMageExamples );
        istMageExamples.close();
    }

}
