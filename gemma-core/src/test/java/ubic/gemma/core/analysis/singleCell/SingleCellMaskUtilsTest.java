package ubic.gemma.core.analysis.singleCell;

import org.junit.jupiter.api.Test;
import ubic.gemma.model.expression.bioAssayData.CellLevelCharacteristics;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SingleCellMaskUtilsTest {

    @Test
    public void test() {
        Random r = new Random();
        boolean[] originalMask = new boolean[1000];
        for ( int i = 0; i < originalMask.length; i++ ) {
            originalMask[i] = r.nextBoolean();
        }
        CellLevelCharacteristics mask = SingleCellMaskUtils.createMask( originalMask );
        SingleCellMaskUtils.validateMask( mask );
        boolean[] maskB = SingleCellMaskUtils.parseMask( mask );
        assertArrayEquals( originalMask, maskB );
    }

    @Test
    public void testMaskWithUnassignedCells() {
        Random r = new Random();
        boolean[] originalMask = new boolean[1000];
        for ( int i = 0; i < originalMask.length; i++ ) {
            originalMask[i] = r.nextBoolean();
        }
        CellLevelCharacteristics mask = SingleCellMaskUtils.createMask( originalMask );
        mask.getIndices()[500] = CellLevelCharacteristics.UNKNOWN_CHARACTERISTIC;
        assertThrows( IllegalArgumentException.class, () -> SingleCellMaskUtils.validateMask( mask ) );
        assertThrows( IllegalArgumentException.class, () -> SingleCellMaskUtils.parseMask( mask ) );
    }
}