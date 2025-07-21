package ubic.gemma.core.analysis.singleCell;

import org.springframework.util.Assert;
import ubic.gemma.model.common.description.Categories;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.CharacteristicUtils;
import ubic.gemma.model.expression.bioAssayData.CellLevelCharacteristics;

import java.util.Arrays;

/**
 * Utility class for handling masks in single-cell analysis.
 * @author poirigui
 */
public class SingleCellMaskUtils {

    /**
     * Create a mask from a boolean array.
     */
    public static CellLevelCharacteristics createMask( boolean[] mask ) {
        int[] maskToIndices = new int[mask.length];
        for ( int i = 0; i < mask.length; i++ ) {
            maskToIndices[i] = mask[i] ? 1 : 0;
        }
        return CellLevelCharacteristics.Factory.newInstance( Arrays.asList(
                Characteristic.Factory.newInstance( Categories.MASK, "false", null ), // index 0
                Characteristic.Factory.newInstance( Categories.MASK, "true", null )   // index 1
        ), maskToIndices );
    }

    /**
     * Validate a mask.
     * @throws IllegalArgumentException if the mask is not valid.
     */
    public static void validateMask( CellLevelCharacteristics mask ) throws IllegalArgumentException {
        parseMask( mask, false );
    }

    /**
     * Parse a mask from a {@link CellLevelCharacteristics} object.
     */
    public static boolean[] parseMask( CellLevelCharacteristics mask ) {
        return parseMask( mask, true );
    }

    public static boolean[] parseMask( CellLevelCharacteristics mask, boolean createArray ) {
        Assert.isTrue( mask.getCharacteristics().stream().allMatch( c -> CharacteristicUtils.hasCategory( c, Categories.MASK ) ),
                "All the characteristic of a mask must use the " + Categories.MASK + " category." );
        Assert.isTrue( mask.getCharacteristics().size() == 2, "A mask must have exactly two possible characteristic." );
        int trueIx;
        if ( Boolean.parseBoolean( mask.getCharacteristics().get( 0 ).getValue() ) && !Boolean.parseBoolean( mask.getCharacteristics().get( 1 ).getValue() ) ) {
            trueIx = 0;
        } else if ( !Boolean.parseBoolean( mask.getCharacteristics().get( 0 ).getValue() ) && Boolean.parseBoolean( mask.getCharacteristics().get( 1 ).getValue() ) ) {
            trueIx = 1;
        } else {
            throw new IllegalArgumentException( mask + " is not valid: it must have two characteristics: one indicating true and the other false." );
        }
        if ( createArray ) {
            boolean[] maskB = new boolean[mask.getIndices().length];
            for ( int i = 0; i < mask.getIndices().length; i++ ) {
                if ( mask.getIndices()[i] == CellLevelCharacteristics.UNKNOWN_CHARACTERISTIC ) {
                    throw new IllegalArgumentException( "Mask may not contain unassigned cells." );
                }
                maskB[i] = mask.getIndices()[i] == trueIx;
            }
            return maskB;
        } else {
            for ( int i = 0; i < mask.getIndices().length; i++ ) {
                if ( mask.getIndices()[i] == CellLevelCharacteristics.UNKNOWN_CHARACTERISTIC ) {
                    throw new IllegalArgumentException( "Mask may not contain unassigned cells." );
                }
            }
            return null;
        }
    }
}
