/*
 * The Gemma project
 * 
 * Copyright (c) 2006 Columbia University
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
package ubic.gemma.loader.expression.geo;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import ubic.basecode.util.FileTools;
import ubic.gemma.model.common.quantitationtype.GeneralType;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;

/**
 * @author Paul
 * @version $Id$
 */
public class QuantitationTypeParameterGuesserTest extends TestCase {

    private static Log log = LogFactory.getLog( QuantitationTypeParameterGuesserTest.class.getName() );

    QuantitationType qt;

    /**
     * @
     */
    @Test
    public void testAbsCall() {
        String a = "ABS CALL";
        String b = "the call in an absolute analysis that indicates if the transcript was present (P), absent (A), marginal (M), or reverse present (RP)";
        StandardQuantitationType s = QuantitationTypeParameterGuesser.guessType( a.toLowerCase(), b.toLowerCase() );
        assertEquals( StandardQuantitationType.PRESENTABSENT, s );
    }

    @Test
    public void testareaCall() {
        String a = "AREA";
        String b = "Number of pixels used to calculate a feature's intensity";
        StandardQuantitationType s = QuantitationTypeParameterGuesser.guessType( a.toLowerCase(), b.toLowerCase() );
        assertEquals( "got " + s, StandardQuantitationType.OTHER, s );
    }

    @Test
    public void testbackground() {
        String a = "B635_MEDIAN";
        String b = "median Cy5 feature background intensity";
        Boolean s = QuantitationTypeParameterGuesser.guessIsBackground( a.toLowerCase(), b.toLowerCase() );
        assertEquals( "got " + s, Boolean.TRUE, s );
    }

    @Test
    public void testgcrma() {
        ScaleType s = QuantitationTypeParameterGuesser.guessScaleType( "VALUE", "gcRMA-calculated Signal intensity" );
        assertEquals( "got " + s, ScaleType.LOG2, s );
    }

    @Test
    public void testbackgroundB() {
        String a = "CH1_BKD_+2SD";
        String b = "Percent of feature pixels that were greater than two standard deviations of the background over the background signal";
        Boolean s = QuantitationTypeParameterGuesser.guessIsBackground( a.toLowerCase(), b.toLowerCase() )
                && QuantitationTypeParameterGuesser.maybeBackground( a.toLowerCase(), b.toLowerCase() );
        assertEquals( "got " + s, Boolean.FALSE, s );

    }

    @Test
    public void testbkdst() {
        String a = "CH2_BKD_ SD";
        String b = "NChannel 2 background standard deviation";
        StandardQuantitationType s = QuantitationTypeParameterGuesser.guessType( a.toLowerCase(), b.toLowerCase() );
        assertEquals( "got " + s, StandardQuantitationType.CONFIDENCEINDICATOR, s );
    }

    /**
     * Test method for
     * {@link ubic.gemma.loader.expression.geo.QuantitationTypeParameterGuesser#guessQuantitationTypeParameters(ubic.gemma.model.common.quantitationtype.QuantitationType, java.lang.String, java.lang.String)}
     * .
     */
    @Test
    public void testGuessQuantitationTypeParameters() {
        String name = "CH1_MEAN";
        String description = "CH1 Mean Intensity";
        qt.setName( name );
        QuantitationTypeParameterGuesser.guessQuantitationTypeParameters( qt, name, description );

        assertEquals( ScaleType.LINEAR, qt.getScale() );
        assertEquals( GeneralType.QUANTITATIVE, qt.getGeneralType() );
        assertEquals( StandardQuantitationType.AMOUNT, qt.getType() );
        assertEquals( PrimitiveType.DOUBLE, qt.getRepresentation() );
    }

    @Test
    public void testPercent() {
        String a = "%_>_B532+1SD";
        String b = "percentage of feature pixels with intensities more than one standard deviation above the background pixel intensity, at wavelength #2 (532 nm, Cy3)";

        ScaleType s = QuantitationTypeParameterGuesser.guessScaleType( a.toLowerCase(), b.toLowerCase() );
        assertEquals( "got " + s, ScaleType.PERCENT, s );
    }

    public void testPixels() {
        String a = "B Pixels";
        String b = "number of background pixels";
        PrimitiveType s = QuantitationTypeParameterGuesser.guessPrimitiveType( a.toLowerCase(), b.toLowerCase(), null );
        assertEquals( "got " + s, PrimitiveType.INT, s );
    }

    public void testMas5() {
        String a = "MAS5.0 signal intensity";
        ScaleType guessScaleType = QuantitationTypeParameterGuesser.guessScaleType( "VALUE", a.toLowerCase() );
        assertEquals( ScaleType.LOG2, guessScaleType );
    }

    @Test
    public void testPrimitiveType() {
        String a = "VALUE";
        String b = "green_processed_Signal; the signal left after all the Feature extraction processing steps have been completed (e.g. background substraction)";
        PrimitiveType s = QuantitationTypeParameterGuesser.guessPrimitiveType( a.toLowerCase(), b.toLowerCase(), 1.0 );
        assertEquals( "got " + s, PrimitiveType.DOUBLE, s );
    }

    @Test
    public void testPrimitiveTypeDoubleWrong() {
        String a = "VALUE";
        String b = "green_processed_Signal; the signal left after all the Feature extraction processing steps have been completed (e.g. background substraction)";
        PrimitiveType s = QuantitationTypeParameterGuesser.guessPrimitiveType( a.toLowerCase(), b.toLowerCase(), "a" );
        assertFalse( "got " + s, PrimitiveType.DOUBLE.equals( s ) );
    }

    @Test
    public void testPrimitiveTypeInteger() {
        String a = "B Pixels";
        String b = "number of background pixels";
        PrimitiveType s = QuantitationTypeParameterGuesser.guessPrimitiveType( a.toLowerCase(), b.toLowerCase(), 12232 );
        assertTrue( "got " + s, s.equals( PrimitiveType.INT ) );
    }

    @Test
    public void testPrimitiveTypeIntegerWrong() {
        String a = "B Pixels";
        String b = "number of background pixels";
        PrimitiveType s = QuantitationTypeParameterGuesser.guessPrimitiveType( a.toLowerCase(), b.toLowerCase(),
                12232.00 );
        assertTrue( "got " + s, s.equals( PrimitiveType.DOUBLE ) );
    }

    @Test
    public void testRatio() {
        String a = "RAT1_MEAN";
        String b = "ratio of CH1D_MEAN to CH2D_MEAN";
        StandardQuantitationType s = QuantitationTypeParameterGuesser.guessType( a.toLowerCase(), b.toLowerCase() );
        assertEquals( "got " + s, StandardQuantitationType.AMOUNT, s );
    }

    @Test
    public void testBadRatio() {
        String a = "VALUE";
        String b = "Log2 ratio (CH_1 Median-CH1_B/CH2_Median-CH2_B)";
        PrimitiveType s = QuantitationTypeParameterGuesser
                .guessPrimitiveType( a.toLowerCase(), b.toLowerCase(), "null" );
        assertEquals( "got " + s, PrimitiveType.DOUBLE, s );
    }

    /**
     * @
     */
    @Test
    public void testTortureQuantitationTypes() throws Exception {

        InputStream is = new FileInputStream(
                FileTools.resourceToPath( "/data/loader/expression/quantitationTypes.txt" ) );
        BufferedReader dis = new BufferedReader( new InputStreamReader( is ) );
        dis.readLine(); // throw away header.
        String line = null;

        Collection<String> failed = new ArrayList<String>();
        Collection<String> passed = new ArrayList<String>();
        int lineNum = 1;
        while ( ( line = dis.readLine() ) != null ) {
            String[] fields = line.split( "\\t" );
            String name = fields[1];
            String description = fields[2];
            Boolean isBackground = fields[3].equals( "0" ) ? Boolean.FALSE : Boolean.TRUE;
            PrimitiveType representation = PrimitiveType.fromString( fields[4] );
            GeneralType generalType = GeneralType.fromString( fields[5] );
            StandardQuantitationType type = StandardQuantitationType.fromString( fields[6] );
            ScaleType scale = ScaleType.fromString( fields[7] );

            Boolean isPreferred = fields[8].equals( "0" ) ? Boolean.FALSE : Boolean.TRUE;
            Boolean isNormalized = fields[9].equals( "0" ) ? Boolean.FALSE : Boolean.TRUE;
            Boolean isBackgroundSubtracted = fields[10].equals( "0" ) ? Boolean.FALSE : Boolean.TRUE;
            Boolean isRatio = fields[11].equals( "0" ) ? Boolean.FALSE : Boolean.TRUE;

            qt = QuantitationType.Factory.newInstance();
            qt.setName( name );
            qt.setDescription( description );
            QuantitationTypeParameterGuesser.guessQuantitationTypeParameters( qt, name, description );

            if ( qt.getGeneralType() != generalType ) {
                failed.add( line );
                log.info( ">>> Line " + lineNum + ": Failed general type for '" + fields[1] + " (" + fields[2] + ")"
                        + "', got " + qt.getGeneralType() );
            } else if ( qt.getType() != type ) {
                failed.add( line );
                log.info( ">>> Line " + lineNum + ": Failed standard type for '" + fields[1] + " (" + fields[2] + ")"
                        + "', got " + qt.getType() );
            } else if ( qt.getRepresentation() != representation ) {
                failed.add( line );
                log.info( ">>> Line " + lineNum + ": Failed representation type for '" + fields[1] + " (" + fields[2]
                        + ")" + "', got " + qt.getRepresentation() );
            } else if ( qt.getIsBackground() != isBackground ) {
                failed.add( line );
                log.info( ">>> Line " + lineNum + ": Failed isBackground for '" + fields[1] + " (" + fields[2] + ")"
                        + "', got " + qt.getIsBackground() );
            } else if ( qt.getScale() != scale ) {
                failed.add( line );
                log.info( ">>> Line " + lineNum + ": Failed scale type for '" + fields[1] + " (" + fields[2] + ")"
                        + "', got " + qt.getScale() );
            } else if ( qt.getIsBackgroundSubtracted() != isBackgroundSubtracted ) {
                failed.add( line );
                log.info( ">>> Line " + lineNum + ": Failed isBackgroundSubtracted for '" + fields[1] + " ("
                        + fields[2] + ")" + "', got " + qt.getIsBackgroundSubtracted() );
            } else if ( qt.getIsPreferred() != isPreferred ) {
                failed.add( line );
                log.info( ">>> Line " + lineNum + ": Failed isPreferred for '" + fields[1] + " (" + fields[2] + ")"
                        + "', got " + qt.getIsPreferred() );
            } else if ( qt.getIsNormalized() != isNormalized ) {
                failed.add( line );
                log.info( ">>> Line " + lineNum + ": Failed isNormalized for '" + fields[1] + " (" + fields[2] + ")"
                        + "', got " + qt.getIsNormalized() );
            } else if ( qt.getIsRatio() != isRatio ) {
                failed.add( line );
                log.info( ">>> Line " + lineNum + ": Failed isRatio for '" + fields[1] + " (" + fields[2] + ")"
                        + "', got " + qt.getIsRatio() );
            } else {
                passed.add( line );
            }
            lineNum++;

        }

        StringBuilder buf = new StringBuilder();
        buf.append( "\n***** PASSED: " + passed.size() + " *******\n" );

        buf.append( "***** FAILED " + failed.size() + " *******\n" );

        log.info( buf );

        if ( failed.size() > 0 ) {
            // fail();
        }

    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        qt = QuantitationType.Factory.newInstance();
        qt.setIsBackground( false );
        qt.setScale( ScaleType.LINEAR );
        qt.setRepresentation( PrimitiveType.DOUBLE );
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( StandardQuantitationType.AMOUNT );
    }
}
