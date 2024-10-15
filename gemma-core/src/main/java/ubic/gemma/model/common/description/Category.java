package ubic.gemma.model.common.description;

import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Represents a category.
 * <p>
 * We intend to <a href="https://github.com/PavlidisLab/Gemma/issues/913">revamp the the characteristic hierarchy</a>
 * which will make categories persistent alongside {@link ubic.gemma.model.expression.experiment.Statement} and terms.
 * @author poirigui
 * @see Categories
 */
public class Category {

    private final String category;
    @Nullable
    private final String categoryUri;

    public Category( String category, String categoryUri ) {
        if ( category == null && categoryUri != null ) {
            throw new IllegalArgumentException( "A category with a non-blank URI must have a label." );
        }
        this.category = category;
        this.categoryUri = categoryUri;
    }

    public String getCategory() {
        return category;
    }

    @Nullable
    public String getCategoryUri() {
        return categoryUri;
    }

    public boolean equals( Object object ) {
        if ( this == object ) {
            return true;
        }
        if ( !( object instanceof Category ) ) {
            return false;
        }
        Category other = ( Category ) object;
        return CharacteristicUtils.equals( category, categoryUri, other.category, other.categoryUri );
    }

    public int hashCode() {
        return Objects.hash( StringUtils.lowerCase( categoryUri != null ? categoryUri : category ) );
    }

    public String toString() {
        StringBuilder b = new StringBuilder( "Category" );
        if ( category != null ) {
            b.append( " Category=" ).append( category );
            if ( categoryUri != null ) {
                b.append( " [" ).append( categoryUri ).append( "]" );
            }
        } else if ( categoryUri != null ) {
            b.append( " Category URI=" ).append( categoryUri );
        } else {
            b.append( " [No Category]" );
        }
        return b.toString();
    }
}
