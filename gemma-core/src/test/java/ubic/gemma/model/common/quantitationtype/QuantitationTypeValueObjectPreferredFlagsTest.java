/*
 * The Gemma project.
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.model.common.quantitationtype;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code isPreferred} on the wire is an OR of three distinct flags, so it does not name one quantitation type
 * per experiment. A client picking "the live subset group" by it alone resolved the wrong group on 2 of 92
 * single-cell datasets (uib, 2026-09-03): after a re-aggregation the superseded cut keeps its masked-preferred
 * quantitation type, and both groups then answer true. {@code isSingleCellPreferred} was the one of the three a
 * client could not see.
 */
class QuantitationTypeValueObjectPreferredFlagsTest {

    private static QuantitationType newQt() {
        QuantitationType qt = new QuantitationType();
        qt.setId( 1L );
        qt.setName( "counts" );
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( StandardQuantitationType.COUNT );
        qt.setRepresentation( PrimitiveType.DOUBLE );
        qt.setScale( ScaleType.COUNT );
        return qt;
    }

    @Test
    void singleCellPreferredIsVisibleAndDistinctFromPreferred() {
        QuantitationType qt = newQt();
        qt.setIsSingleCellPreferred( true );

        QuantitationTypeValueObject vo = new QuantitationTypeValueObject( qt );

        assertThat( vo.getIsSingleCellPreferred() ).isTrue();
        assertThat( vo.getIsMaskedPreferred() ).isFalse();
        assertThat( vo.getIsPreferred() ).isTrue(); // the OR
    }

    @Test
    void maskedPreferredAloneAlsoSetsTheOr() {
        QuantitationType qt = newQt();
        qt.setIsMaskedPreferred( true );

        QuantitationTypeValueObject vo = new QuantitationTypeValueObject( qt );

        assertThat( vo.getIsMaskedPreferred() ).isTrue();
        assertThat( vo.getIsSingleCellPreferred() ).isFalse();
        assertThat( vo.getIsPreferred() ).isTrue();
    }

    /**
     * The processed-data sense on its own — what a client wanting exactly one quantitation type per experiment
     * has to test, and what it could not express while only two of the three flags were readable.
     */
    @Test
    void theThreeFlagsAreSeparableOnTheWire() {
        QuantitationType processed = newQt();
        processed.setIsPreferred( true );
        QuantitationType masked = newQt();
        masked.setIsMaskedPreferred( true );
        QuantitationType singleCell = newQt();
        singleCell.setIsSingleCellPreferred( true );

        assertThat( isProcessedPreferred( new QuantitationTypeValueObject( processed ) ) ).isTrue();
        assertThat( isProcessedPreferred( new QuantitationTypeValueObject( masked ) ) ).isFalse();
        assertThat( isProcessedPreferred( new QuantitationTypeValueObject( singleCell ) ) ).isFalse();
    }

    private static boolean isProcessedPreferred( QuantitationTypeValueObject vo ) {
        return vo.getIsPreferred() && !vo.getIsMaskedPreferred() && !vo.getIsSingleCellPreferred();
    }
}
