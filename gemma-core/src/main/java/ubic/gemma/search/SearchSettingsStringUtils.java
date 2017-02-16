package ubic.gemma.search;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.queryParser.QueryParser;

import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.genome.Taxon;

public class SearchSettingsStringUtils {

    /**
     * Add anything that should be removed from the search string here. Lowercase.
     */
    protected static String[] STRINGS_TO_REMOVE = new String[] { "all", "results", "for" };

    public static String stripShortTerms( String query ) {
        String[] searchTerms = query.split( "\\s+" );

        if ( searchTerms.length > 0 ) {
            query = "";
            for ( String sTerm : searchTerms ) {
                if ( sTerm.length() > 1 ) {
                    query = query + " " + sTerm;
                }
            }
            query = query.trim();
        }
        return query;
    }

    /**
     * Checks whether there is a taxon set in the given SearchSettings, and if not, tries to extract a taxon from the
     * SearchSettings query
     * 
     * @param settings
     * @return
     */
    public static SearchSettings processSettings( SearchSettings settings, HashMap<String, Taxon> nameToTaxonMap ) {

        if ( settings != null && settings.getTaxon() == null ) {

            settings = processSearchString( settings );
            String searchString = settings.getQuery();

            // split the query around whitespace characters, limit the splitting to 4 terms (may be excessive)
            String[] searchTerms = searchString.split( "\\s+", 4 );

            List<String> searchTermsList = Arrays.asList( searchTerms );

            // this Set is ordered by insertion order(LinkedHashMap)
            Set<String> keywords = nameToTaxonMap.keySet();

            // only strip out taxon terms if there is more than one search term in query and if the entire search string
            // is not itself a keyword
            if ( searchTerms.length > 1 && !keywords.contains( settings.getQuery().toLowerCase() ) ) {

                for ( String keyword : keywords ) {

                    int termIndex = searchString.toLowerCase().indexOf( keyword );
                    // make sure that the keyword occurs in the searchString
                    if ( termIndex != -1 ) {
                        // make sure that either the keyword is multi-term or that it occurs as a single term(not as
                        // part of another word)

                        if ( keyword.contains( " " ) || searchTermsList.contains( keyword ) ) {
                            searchString = searchString.replaceFirst( "(?i)" + keyword, "" ).trim();
                            settings.setTaxon( nameToTaxonMap.get( keyword ) );
                            // break on first term found in keywords since they should be(more or less) ordered by
                            // precedence
                            break;
                        }
                    }
                }
            }

            settings.setQuery( searchString );
        }

        return settings;
    }

    /**
     * Makes the query lower case, removes quotes and removes all (sub)strings in STRINGS_TO_REMOVE array from it.
     * 
     * @param settings
     * @return
     */
    private static SearchSettings processSearchString( SearchSettings settings ) {
        String searchString = QueryParser.escape( settings.getQuery().toLowerCase() );

        for ( String s : STRINGS_TO_REMOVE ) {
            searchString = searchString.replace( s, "" );
        }

        String newString = "";
        String[] searchTerms = searchString.split( "\\s+" );

        for ( String term : searchTerms ) {
            newString += term.replaceAll( "\'|\"", "" ) + " ";
        }

        settings.setQuery( StringUtils.strip( newString ) );

        return settings;
    }
}
