/*
 * The gemma-core project
 *
 * Copyright (c) 2018 University of British Columbia
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

package ubic.gemma.core.loader.expression.geo;

import org.junit.Test;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.biomaterial.BioMaterial;

import static org.junit.Assert.*;

/**
 *
 *
 * @author paul
 */
public class GeoCharacteristicParseTest {
    @Test
    public final void testParseGEOSampleCharacteristic() throws Exception {
        GeoConverterImpl g = new GeoConverterImpl();

        BioMaterial t = BioMaterial.Factory.newInstance();
        g.parseGEOSampleCharacteristicString( "Sex: M", t );
        Characteristic c = t.getCharacteristics().iterator().next();
        assertEquals( "biological sex", c.getCategory() );
        assertEquals( "male", c.getValue() );
        assertEquals( "http://purl.obolibrary.org/obo/PATO_0000047", c.getCategoryUri() );
        assertEquals( "http://purl.obolibrary.org/obo/PATO_0000384", c.getValueUri() );

        t = BioMaterial.Factory.newInstance();
        g.parseGEOSampleCharacteristicString( "Sex: M,Tissue=brain", t );
        check2chars( t );

        t = BioMaterial.Factory.newInstance();
        g.parseGEOSampleCharacteristicString( "Sex : M; Tissue:brain ", t );
        check2chars( t );

        t = BioMaterial.Factory.newInstance();
        g.parseGEOSampleCharacteristicString( "Sex=M", t );
        c = t.getCharacteristics().iterator().next();
        assertEquals( "biological sex", c.getCategory() );
        assertEquals( "male", c.getValue() );
        assertEquals( "http://purl.obolibrary.org/obo/PATO_0000047", c.getCategoryUri() );
        assertEquals( "http://purl.obolibrary.org/obo/PATO_0000384", c.getValueUri() );

        t = BioMaterial.Factory.newInstance();
        g.parseGEOSampleCharacteristicString( "Genotype: wild type", t );
        c = t.getCharacteristics().iterator().next();
        assertEquals( "genotype", c.getCategory() );
        assertEquals( "wild type genotype", c.getValue() );
        assertEquals( "http://www.ebi.ac.uk/efo/EFO_0000513", c.getCategoryUri() );
        assertEquals( "http://www.ebi.ac.uk/efo/EFO_0005168", c.getValueUri() );

        t = BioMaterial.Factory.newInstance();
        g.parseGEOSampleCharacteristicString( "Developmental stage: adult", t );
        c = t.getCharacteristics().iterator().next();
        assertEquals( "developmental stage", c.getCategory() );
        assertEquals( "adult", c.getValue() );
        assertEquals( "http://www.ebi.ac.uk/efo/EFO_0000399", c.getCategoryUri() );
        assertEquals( "http://www.ebi.ac.uk/efo/EFO_0001272", c.getValueUri() );

        // case we can't parse reliably.
        t = BioMaterial.Factory.newInstance();
        g.parseGEOSampleCharacteristicString( "Clinical info: Submitting diagnosis: DLBCL", t );
        c = t.getCharacteristics().iterator().next();
        assertEquals( "Clinical info", c.getCategory() );
        assertNull( c.getCategoryUri() );
        assertEquals( "Submitting diagnosis: DLBCL", c.getValue() );
        assertNull( c.getValueUri() );

        // test trimming of species names
        t = BioMaterial.Factory.newInstance();
        g.parseGEOSampleCharacteristicString( "cell type: human fibroblast", t );
        c = t.getCharacteristics().iterator().next();
        assertEquals( "cell type", c.getCategory() );
        assertEquals( "fibroblast", c.getValue() );
        assertEquals( "http://www.ebi.ac.uk/efo/EFO_0000324", c.getCategoryUri() );
        assertEquals( "http://purl.obolibrary.org/obo/CL_0000057", c.getValueUri() );
    }

    /**
     */
    void check2chars( BioMaterial t ) {
        assertEquals( 2, t.getCharacteristics().size() );
        boolean found1 = false;
        boolean found2 = false;
        for ( Characteristic ch : t.getCharacteristics() ) {
            if ( ch.getCategory().equals( "biological sex" ) ) {
                assertEquals( "male", ch.getValue() );
                assertEquals( "http://purl.obolibrary.org/obo/PATO_0000047", ch.getCategoryUri() );
                assertEquals( "http://purl.obolibrary.org/obo/PATO_0000384", ch.getValueUri() );
                found1 = true;
            } else if ( ch.getCategory().equals( "organism part" ) ) {
                assertEquals( "brain", ch.getValue() );
                assertEquals( "http://www.ebi.ac.uk/efo/EFO_0000635", ch.getCategoryUri() );
                assertEquals( "http://purl.obolibrary.org/obo/UBERON_0000955", ch.getValueUri() );
                found2 = true;
            }
        }
        assertTrue( found1 && found2 );
    }
}
