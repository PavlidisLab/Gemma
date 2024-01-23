/*
 * The Gemma project
 *
 * Copyright (c) 2006 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.core.loader.expression.geo.util;

import java.util.Collection;
import java.util.HashSet;

/**
 * Constants used to help decipher GEO data files.
 *
 * @author pavlidis
 */
public class GeoConstants {

    private static final Collection<String> idNames;
    /**
     * Collection of strings used to identify the column holding external database identifiers, usually Genbank
     * accession or ORF numbers. This must be added to when new ones are encountered.
     */
    private static final Collection<String> extRefNames;
    private static final Collection<String> descriptionNames;
    private static final Collection<String> sequenceColumnNames;
    private static final Collection<String> probeOrganismColumnNames;

    static {
        idNames = new HashSet<>();
        GeoConstants.idNames.add( "ID" );
        GeoConstants.idNames.add( "TAG" ); // SAGE.

        extRefNames = new HashSet<>();
        GeoConstants.extRefNames.add( "GB_ACC" );
        GeoConstants.extRefNames.add( "GB_LIST" );
        GeoConstants.extRefNames.add( "ORF" );
        GeoConstants.extRefNames.add( "Genbank" );
        GeoConstants.extRefNames.add( "GenBank" );
        // extRefNames.add( "CLONE_ID" ); // IMAGE...

        descriptionNames = new HashSet<>();
        GeoConstants.descriptionNames.add( "GENE_SYMBOL" );
        GeoConstants.descriptionNames.add( "GENE_NAME" );
        GeoConstants.descriptionNames.add( "Gene Symbol" );
        GeoConstants.descriptionNames.add( "Gene Title" );
        GeoConstants.descriptionNames.add( "description" );
        GeoConstants.descriptionNames.add( "DESCRIPTION" );
        GeoConstants.descriptionNames.add( "Symbol" );
        GeoConstants.descriptionNames.add( "PRIMARY_NAME" ); // agilent.

        sequenceColumnNames = new HashSet<>();
        GeoConstants.sequenceColumnNames.add( "SEQUENCE" ); // agilent.
        GeoConstants.sequenceColumnNames.add( "PROBE_SEQUENCE" ); // e.g. GPL7182

        //LMD 24/07/09 Bug 1647
        probeOrganismColumnNames = new HashSet<>();
        GeoConstants.probeOrganismColumnNames.add( "ORGANISM" );
        GeoConstants.probeOrganismColumnNames.add( "Species" );
        GeoConstants.probeOrganismColumnNames.add( "org" );
        GeoConstants.probeOrganismColumnNames.add( "Species Scientific name" );//agilent?
        GeoConstants.probeOrganismColumnNames.add( "Taxon" );

    }

    public static boolean likelyId( String tag ) {
        return GeoConstants.idNames.contains( tag );
    }

    public static boolean likelyExternalReference( String id ) {
        return GeoConstants.extRefNames.contains( id );
    }

    public static boolean likelySequence( String id ) {
        return GeoConstants.sequenceColumnNames.contains( id );
    }

    public static boolean likelyProbeOrganism( String id ) {
        return GeoConstants.probeOrganismColumnNames.contains( id );
    }

    public static boolean likelyProbeDescription( String id ) {
        return GeoConstants.descriptionNames.contains( id );
    }

}
