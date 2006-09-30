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
package ubic.gemma.loader;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.commons.lang.RandomStringUtils;

import ubic.basecode.io.ByteArrayConverter;
import ubic.gemma.loader.util.parser.TabDelimParser;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.testing.BaseTransactionalSpringContextTest;

/**
 * @author keshav
 * @version $Id$
 */
public class PersistRealDataIntegrationTest extends BaseTransactionalSpringContextTest {

    // TODO move/remove me as well as the data set.

    private String filename = "aov.results-2-monocyte-data-bytime.bypat.data.sort";

    private ByteArrayConverter bArrayConverter = new ByteArrayConverter();

    private Collection<String[]> parseData( boolean headerExists ) throws IOException {

        InputStream is = PersistRealDataIntegrationTest.class.getResourceAsStream( "/data/loader/" + filename );

        TabDelimParser parser = new TabDelimParser();
        parser.parse( is );

        Collection<String[]> results = parser.getResults();

        if ( headerExists ) {
            /* not using generics. easier to get header with iterator */
            Iterator iter = results.iterator();
            String[] header = ( String[] ) iter.next();
            parser.setHeader( header );
            log.debug( "header is " + header );

            log.debug( "size before before header removal: " + results.size() );
            results.remove( header );
        }

        log.debug( "size after header removal: " + results.size() );
        return results;
    }

    /**
     * @param dataCol
     */
    private void prepareData( Collection<String[]> dataCol ) {
        // FIXME associations must be persistent ie. see TestPersistentObjectHelper.

        /* TODO: Step 1. This either exists in system or is user supplied */
        ArrayDesign arrayDesign = ArrayDesign.Factory.newInstance();
        arrayDesign.setName( "custom" );
        arrayDesign.setDescription( "an array design based on " + filename );

        /*
         * TODO: Step 2. Assuming this is not in the system, read the probes from file. For now I am creating this.
         */
        for ( String[] data : dataCol ) {
            CompositeSequence cs = CompositeSequence.Factory.newInstance();
            cs.setName( data[0] );
            cs.setDescription( "probe set " + data[0] + " from " + filename );

            DesignElementDataVector vector = DesignElementDataVector.Factory.newInstance();

            double[] ddata = new double[data.length - 1];
            log.debug( "num doubles: " + ddata.length );

            for ( int j = 1; j < ddata.length; j++ ) {
                ddata[j] = new Double( data[j] );
            }

            byte[] bdata = bArrayConverter.doubleArrayToBytes( ddata );
            log.debug( "num bytes: " + bdata.length );
            vector.setData( bdata );
            Collection<DesignElementDataVector> vectors = new HashSet<DesignElementDataVector>();
            cs.setDesignElementDataVectors( vectors );

            /* TODO: Step 3. This also either exists or is user supplied. For now I am creating this. */
            BioSequence sequence = BioSequence.Factory.newInstance();
            sequence.setSequence( RandomStringUtils.random( 40, "ATCG" ) );
            cs.setBiologicalCharacteristic( sequence );
        }
    }

    /**
     * 
     *
     */
    private void loadData() {

    }

    /**
     * 
     *
     */
    public void testParseAndLoad() {
        boolean fail = false;
        try {
            Collection<String[]> results = parseData( true );
            prepareData( results );
            loadData();
        } catch ( IOException e ) {
            e.printStackTrace();
        } finally {
            assertFalse( fail );
        }
    }
}
