package ubic.gemma.model.analysis.expression.diff;

import ubic.gemma.model.util.ModelUtils;
import org.springframework.util.Assert;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.FactorType;
import ubic.gemma.model.expression.experiment.FactorValue;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents a contrast.
 * @author poirigui
 */
@ParametersAreNonnullByDefault
public class Contrast {

    /**
     * Create a contrast for a continuous factor.
     */
    public static Contrast continuous( ExperimentalFactor ef ) {
        Assert.isTrue( ef.getType().equals( FactorType.CONTINUOUS ),
                "A continuous baseline must belong to a continuous factor." );
        return new Contrast( ef );
    }

    /**
     * Create a contrast for a categorical factor.
     */
    public static Contrast categorical( FactorValue fv ) {
        if ( ModelUtils.isInitialized( fv.getExperimentalFactor() ) ) {
            Assert.isTrue( fv.getExperimentalFactor().getType().equals( FactorType.CATEGORICAL ),
                    "A categorical baseline must belong to a categorical factor." );
        }
        return new Contrast( fv );
    }

    /**
     * Create an interaction of two categorical factors.
     */
    public static Contrast interaction( FactorValue fv1, FactorValue fv2 ) {
        // IDs can be safely retrieved for proxies
        Assert.isTrue( !Objects.equals( fv1.getExperimentalFactor().getId(), fv2.getExperimentalFactor().getId() ),
                "An interaction must be of two different experimental factors." );
        if ( ModelUtils.isInitialized( fv1.getExperimentalFactor() ) ) {
            Assert.isTrue( fv1.getExperimentalFactor().getType().equals( FactorType.CATEGORICAL ),
                    "A categorical baseline must belong to a categorical factor." );
        }
        if ( ModelUtils.isInitialized( fv2.getExperimentalFactor() ) ) {
            Assert.isTrue( fv2.getExperimentalFactor().getType().equals( FactorType.CATEGORICAL ),
                    "A categorical baseline must belong to a categorical factor." );
        }
        return new Contrast( fv1, fv2 );
    }

    /**
     * Necessary for continuous factors because they lack specific FVs.
     */
    private final ExperimentalFactor experimentalFactor;

    @Nullable
    private final FactorValue factorValue;
    @Nullable
    private final FactorValue secondFactorValue;

    private final List<FactorValue> factorValues;

    private Contrast( ExperimentalFactor experimentalFactor ) {
        this.experimentalFactor = experimentalFactor;
        this.factorValue = null;
        this.secondFactorValue = null;
        this.factorValues = Collections.emptyList();
    }

    private Contrast( FactorValue fv ) {
        this.experimentalFactor = null;
        this.factorValue = fv;
        this.secondFactorValue = null;
        factorValues = Collections.singletonList( fv );
    }

    private Contrast( FactorValue fv, FactorValue fv2 ) {
        this.experimentalFactor = null;
        this.factorValue = fv;
        this.secondFactorValue = fv2;
        factorValues = Arrays.asList( fv, fv2 );
    }

    public ExperimentalFactor getExperimentalFactor() {
        return experimentalFactor;
    }

    @Nullable
    public FactorValue getFactorValue() {
        return factorValue;
    }

    @Nullable
    public FactorValue getSecondFactorValue() {
        return secondFactorValue;
    }

    public List<FactorValue> getFactorValues() {
        return factorValues;
    }

    /**
     * Indicate if this contrast is continuous.
     */
    public boolean isContinuous() {
        return factorValue == null;
    }

    /**
     * Indicate if this contrast is an interaction of two or more factors.
     */
    public boolean isInteraction() {
        return secondFactorValue != null;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( obj == this ) {
            return true;
        }
        if ( !( obj instanceof Contrast ) ) {
            return false;
        }
        Contrast that = ( Contrast ) obj;
        return Objects.equals( experimentalFactor, that.experimentalFactor )
                && Objects.equals( factorValue, that.factorValue )
                && Objects.equals( secondFactorValue, that.secondFactorValue );
    }

    @Override
    public int hashCode() {
        return Objects.hash( factorValue, secondFactorValue );
    }

    @Override
    public String toString() {
        return "Contrast for "
                + ( factorValue != null ? factorValue : experimentalFactor )
                + ( secondFactorValue != null ? ":" + secondFactorValue : "" );
    }
}

