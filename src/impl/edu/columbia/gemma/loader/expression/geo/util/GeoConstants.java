package edu.columbia.gemma.loader.expression.geo.util;

import java.util.Collection;
import java.util.HashSet;

public class GeoConstants {

    public static Collection<String> idNames;
    public static Collection<String> extRefNames;
    public static Collection<String> descriptionNames;

    static {
        idNames = new HashSet<String>();
        idNames.add( "ID" );
        idNames.add( "TAG" );

        extRefNames = new HashSet<String>();
        extRefNames.add( "GB_ACC" );
        extRefNames.add( "ORF" );

        descriptionNames = new HashSet<String>();
        descriptionNames.add( "GENE_SYMBOL" );
        descriptionNames.add( "GENE_NAME" );
        descriptionNames.add( "Gene Symbol" );
        descriptionNames.add( "Gene Title" );
        descriptionNames.add( "description" );
        descriptionNames.add( "DESCRIPTION" );
    }

    /**
     * @param tag
     * @return
     */
    public static boolean likelyId( String tag ) {
        return idNames.contains( tag );
    }

    /**
     * @param id
     * @return
     */
    public static boolean likelyExternalReference( String id ) {
        return extRefNames.contains( id );
    }

    /**
     * @param id
     * @return
     */
    public static boolean likelyProbeDescription( String id ) {
        return descriptionNames.contains( id );
    }

}
