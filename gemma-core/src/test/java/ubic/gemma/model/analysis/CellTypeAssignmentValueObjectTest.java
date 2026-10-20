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
package ubic.gemma.model.analysis;

import org.junit.jupiter.api.Test;
import ubic.gemma.model.common.description.Categories;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.bioAssayData.CellTypeAssignment;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * The per-cell-type tally. Before it existed, the only way to answer "how many astrocytes" was the
 * one-entry-per-cell {@code cellTypeIds} array — 89,700 entries and 809 KB on one dataset to recover ten
 * numbers (uib, 2026-09-03).
 */
class CellTypeAssignmentValueObjectTest {

    private static CellTypeAssignment newAssignment() {
        Characteristic astrocyte = Characteristic.Factory.newInstance( Categories.CELL_TYPE, "astrocyte", null );
        astrocyte.setId( 1L );
        Characteristic neuron = Characteristic.Factory.newInstance( Categories.CELL_TYPE, "neuron", null );
        neuron.setId( 2L );
        Characteristic pericyte = Characteristic.Factory.newInstance( Categories.CELL_TYPE, "pericyte", null );
        pericyte.setId( 3L );

        CellTypeAssignment cta = new CellTypeAssignment();
        cta.setId( 10L );
        cta.setCellTypes( Arrays.asList( astrocyte, neuron, pericyte ) );
        cta.setNumberOfCellTypes( 3 );
        // 2 astrocytes, 3 neurons, no pericytes, 1 unassigned cell
        cta.setCellTypeIndices( new int[] { 0, 1, 1, 0, 1, CellTypeAssignment.UNKNOWN_CELL_TYPE } );
        return cta;
    }

    @Test
    void tallyCountsCellsPerCellType() {
        CellTypeAssignmentValueObject vo = new CellTypeAssignmentValueObject( newAssignment(), false );

        assertThat( vo.getNumberOfAssignedCellsByCellType() )
                .containsOnly( entry( 1L, 2 ), entry( 2L, 3 ), entry( 3L, 0 ) );
    }

    /** A cell type nobody was assigned to reads zero rather than going missing — ten rows stay ten rows. */
    @Test
    void anUnusedCellTypeIsPresentWithZero() {
        CellTypeAssignmentValueObject vo = new CellTypeAssignmentValueObject( newAssignment(), false );

        assertThat( vo.getNumberOfAssignedCellsByCellType() ).containsEntry( 3L, 0 );
    }

    /** The point of the tally: it survives excluding the per-cell array, which is the whole payload. */
    @Test
    void tallySurvivesExcludingThePerCellArray() {
        CellTypeAssignmentValueObject vo = new CellTypeAssignmentValueObject( newAssignment(), true );

        assertThat( vo.getCellTypeIds() ).isNull();
        assertThat( vo.getNumberOfAssignedCellsByCellType() )
                .containsOnly( entry( 1L, 2 ), entry( 2L, 3 ), entry( 3L, 0 ) );
        assertThat( vo.getNumberOfAssignedCells() ).isEqualTo( 5 );
    }

    /** The tally sums to the assigned-cell total; the unassigned cell is in neither. */
    @Test
    void tallySumsToTheAssignedTotal() {
        CellTypeAssignmentValueObject vo = new CellTypeAssignmentValueObject( newAssignment(), false );

        assertThat( vo.getNumberOfAssignedCellsByCellType().values().stream().mapToInt( Integer::intValue ).sum() )
                .isEqualTo( vo.getNumberOfAssignedCells() );
    }
}
