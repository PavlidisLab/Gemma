package ubic.gemma.model.common.description;

import lombok.Value;

import javax.annotation.Nullable;

/**
 * Represents a category.
 * <p>
 * We intend to <a href="https://github.com/PavlidisLab/Gemma/issues/913">revamp the the characteristic hierarchy</a>
 * which will make categories persistent alongside {@link ubic.gemma.model.expression.experiment.Statement} and terms.
 * @author poirigui
 * @see Categories
 */
@Value
public class Category {
    String category;
    @Nullable
    String categoryUri;
}
