package ubic.gemma.model.analysis.expression.diff;

import ubic.gemma.model.util.ModelUtils;
import org.springframework.util.Assert;
import ubic.gemma.model.expression.experiment.FactorType;
import ubic.gemma.model.expression.experiment.FactorValue;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Represents a baseline for a single factor or an interaction of factors.
 * <p>
 * This class is proxy-safe as long as {@link #hashCode()} isn't being used.
 * @author poirigui
 */
public class Baseline {

    /**
     * Create a baseline for a single categorical factor.
     */
    public static Baseline categorical( FactorValue fv ) {
        if ( ModelUtils.isInitialized( fv ) && ModelUtils.isInitialized( fv.getExperimentalFactor() ) ) {
            Assert.isTrue( fv.getExperimentalFactor().getType().equals( FactorType.CATEGORICAL ),
                    "A categorical baseline must belong to a categorical factor." );
        }
        return new Baseline( fv );
    }

    /**
     * Create a baseline for an interaction of factors.
     */
    public static Baseline interaction( FactorValue fv1, FactorValue fv2 ) {
        if ( ModelUtils.isInitialized( fv1 ) && ModelUtils.isInitialized( fv2 ) ) {
            // IDs can be safely retrieved for proxies
            Assert.isTrue( !Objects.equals( fv1.getExperimentalFactor().getId(), fv2.getExperimentalFactor().getId() ),
                    "An interaction must be of two different experimental factors." );
        }
        if ( ModelUtils.isInitialized( fv1 ) && ModelUtils.isInitialized( fv1.getExperimentalFactor() ) ) {
            Assert.isTrue( fv1.getExperimentalFactor().getType().equals( FactorType.CATEGORICAL ),
                    "A categorical baseline must belong to a categorical factor." );
        }
        if ( ModelUtils.isInitialized( fv2 ) && ModelUtils.isInitialized( fv2.getExperimentalFactor() ) ) {
            Assert.isTrue( fv2.getExperimentalFactor().getType().equals( FactorType.CATEGORICAL ),
                    "A categorical baseline must belong to a categorical factor." );
        }
        return new Baseline( fv1, fv2 );
    }

    private final FactorValue factorValue;
    @Nullable
    private final FactorValue secondFactorValue;

    private Baseline( FactorValue fv ) {
        this.factorValue = fv;
        this.secondFactorValue = null;
    }

    private Baseline( FactorValue fv1, @Nullable FactorValue fv2 ) {
        this.factorValue = fv1;
        this.secondFactorValue = fv2;
    }

    public FactorValue getFactorValue() {
        return factorValue;
    }

    @Nullable
    public FactorValue getSecondFactorValue() {
        return secondFactorValue;
    }

    public boolean isInteraction() {
        return secondFactorValue != null;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( obj == this ) {
            return true;
        }
        if ( !( obj instanceof Baseline ) ) {
            return false;
        }
        Baseline that = ( Baseline ) obj;
        return Objects.equals( factorValue, that.factorValue )
                && Objects.equals( secondFactorValue, that.secondFactorValue );
    }

    @Override
    public int hashCode() {
        return Objects.hash( factorValue, secondFactorValue );
    }

    @Override
    public String toString() {
        return "Baseline for " + formatFactorValue( factorValue ) + ( secondFactorValue != null ? ":" + formatFactorValue( secondFactorValue ) : "" );
    }

    private String formatFactorValue( FactorValue fv ) {
        if ( ModelUtils.isInitialized( fv ) ) {
            return String.valueOf( fv.toString() );
        } else if ( fv.getId() != null ) {
            return "FactorValue Id=" + fv.getId();
        } else {
            return "FactorValue";
        }
    }
}
