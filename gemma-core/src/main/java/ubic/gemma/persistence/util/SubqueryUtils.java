package ubic.gemma.persistence.util;

import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SubqueryUtils {

    /**
     * Given a prefix and an object alias, guess a reasonable sequence of aliases to use in a query.
     * <p>
     * FIXME: the prefix is not always a valid association path
     * <p>
     * If the prefix is something like: 'experimentalDesign.experimentalFactors.factorValues.' with the 'fv' alias, it
     * is converted into:
     * <p>
     * {@code join experimentalDesign as alias1 join alias1.experimentalFactors as alias2 join alias2.factorValues as fv}
     * @param prefix      prefix under which the supplied object alias is accessible
     * @param objectAlias ultimate alias to declare
     */
    public static List<Subquery.Alias> guessAliases( String prefix, String objectAlias ) {
        Assert.isTrue( prefix.isEmpty() || prefix.endsWith( "." ), "A valid prefix must either be empty or end with a '.'." );
        if ( prefix.isEmpty() ) {
            return Collections.emptyList();
        }
        String[] parts = prefix.split( "\\." );
        List<Subquery.Alias> aliases = new ArrayList<>();
        for ( int i = 0; i < parts.length - 1; i++ ) {
            String part = parts[i];
            aliases.add( new Subquery.Alias( i > 0 ? "alias" + i : null, part, "alias" + ( i + 1 ) ) );
        }
        if ( parts.length > 1 ) {
            aliases.add( new Subquery.Alias( "alias" + ( parts.length - 1 ), parts[parts.length - 1], objectAlias ) );
        } else {
            aliases.add( new Subquery.Alias( null, parts[0], objectAlias ) );
        }
        return aliases;
    }
}
