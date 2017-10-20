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
package ubic.gemma.core.loader.expression.arrayDesign;

import java.io.InputStream;
import java.util.Collection;

import junit.framework.TestCase;
import ubic.gemma.model.expression.designElement.CompositeSequence;

/**
 * @author pavlidis
 *
 */
public class CompositeSequenceParserTest extends TestCase {
    InputStream designElementStream;

    public void testParseInputStream() throws Exception {
        CompositeSequenceParser csp = new CompositeSequenceParser();

        csp.parse( designElementStream );

        Collection<CompositeSequence> results = csp.getResults();

        assertTrue( results.size() == 33 );
        assertTrue( results.iterator().next().getName().endsWith( "_at" ) );
        assertTrue( results.iterator().next().getDescription().startsWith( "\"" ) );
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        designElementStream = this.getClass().getResourceAsStream( "/data/loader/expression/arrayDesign/MG-U74A.txt" );
    }

}
