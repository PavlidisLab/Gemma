/*
 * The Gemma project
 *
 * Copyright (c) 2026 University of British Columbia
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
 */
package ubic.gemma.persistence.util;

import org.junit.Test;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.genome.Chromosome;
import ubic.gemma.model.genome.Taxon;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertThrows;

/**
 * Pure unit tests for the validator portion of {@link BusinessKey} added during the Phase 3
 * persister retirement. The {@code matches(...)} and {@code find(Session, ...)} methods require
 * a live JPA Criteria session and are exercised by integration tests against gemdtest in a
 * separate pass.
 *
 * @author phase3
 */
public class BusinessKeyTest {

    // ----- Chromosome -----

    @Test
    public void checkValidKeyChromosome_happyPath() {
        Taxon t = new Taxon();
        t.setNcbiId( 9606 );
        Chromosome chr = new Chromosome();
        chr.setName( "1" );
        chr.setTaxon( t );
        // Should not throw.
        BusinessKey.checkValidKey( chr );
    }

    @Test
    public void checkValidKeyChromosome_blankNameRejected() {
        Taxon t = new Taxon();
        t.setNcbiId( 9606 );
        Chromosome chr = new Chromosome();
        chr.setName( "" );
        chr.setTaxon( t );
        assertThrows( IllegalArgumentException.class, () -> BusinessKey.checkValidKey( chr ) );
    }

    // ----- QuantitationType -----

    @Test
    public void checkKeyQuantitationType_happyPath() {
        QuantitationType qt = new QuantitationType();
        qt.setName( "expression value" );
        BusinessKey.checkKey( qt );
    }

    @Test
    public void checkKeyQuantitationType_blankNameRejected() {
        QuantitationType qt = new QuantitationType();
        qt.setName( "   " );
        assertThrows( IllegalArgumentException.class, () -> BusinessKey.checkKey( qt ) );
    }

    @Test
    public void checkKeyQuantitationType_nullRejected() {
        assertThrows( IllegalArgumentException.class, () -> BusinessKey.checkKey( (QuantitationType) null ) );
    }

    // ----- BioAssayDimension -----

    @Test
    public void checkKeyBioAssayDimension_happyPath() {
        BioAssay ba = new BioAssay();
        ba.setId( 42L );
        List<BioAssay> bas = new ArrayList<>();
        bas.add( ba );
        BioAssayDimension bad = BioAssayDimension.Factory.newInstance( bas );
        BusinessKey.checkKey( bad );
    }

    @Test
    public void checkKeyBioAssayDimension_emptyRejected() {
        BioAssayDimension bad = BioAssayDimension.Factory.newInstance();
        assertThrows( IllegalArgumentException.class, () -> BusinessKey.checkKey( bad ) );
    }

    @Test
    public void checkKeyBioAssayDimension_transientBioAssayRejected() {
        BioAssay ba = new BioAssay();
        // no id set -> transient
        List<BioAssay> bas = new ArrayList<>();
        bas.add( ba );
        BioAssayDimension bad = BioAssayDimension.Factory.newInstance( bas );
        assertThrows( IllegalArgumentException.class, () -> BusinessKey.checkKey( bad ) );
    }
}
