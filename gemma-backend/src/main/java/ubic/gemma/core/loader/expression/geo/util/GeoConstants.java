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
 * @version $Id$
 */
public class GeoConstants {

    public static Collection<String> idNames;

    /**
     * Collection of strings used to identify the column holding external database identifiers, usually Genbank
     * accession or ORF numbers. This must be added to when new ones are encountered.
     */
    public static Collection<String> extRefNames;

    public static Collection<String> descriptionNames;
    public static Collection<String> sequenceColumnNames;
    public static Collection<String> probeOrganismColumnNames;

    static {
        idNames = new HashSet<String>();
        idNames.add( "ID" );
        idNames.add( "TAG" ); // SAGE.

        extRefNames = new HashSet<String>();
        extRefNames.add( "GB_ACC" );
        extRefNames.add( "GB_LIST" );
        extRefNames.add( "ORF" );
        extRefNames.add( "Genbank" );
        extRefNames.add( "GenBank" );
        // extRefNames.add( "CLONE_ID" ); // IMAGE...

        descriptionNames = new HashSet<String>();
        descriptionNames.add( "GENE_SYMBOL" );
        descriptionNames.add( "GENE_NAME" );
        descriptionNames.add( "Gene Symbol" );
        descriptionNames.add( "Gene Title" );
        descriptionNames.add( "description" );
        descriptionNames.add( "DESCRIPTION" );
        descriptionNames.add( "Symbol" );
        descriptionNames.add( "PRIMARY_NAME" ); // agilent.

        sequenceColumnNames = new HashSet<String>();
        sequenceColumnNames.add( "SEQUENCE" ); // agilent.
        
        //LMD 24/07/09 Bug 1647
        probeOrganismColumnNames = new HashSet<String>();
        probeOrganismColumnNames.add( "ORGANISM" ); 
        probeOrganismColumnNames.add( "Species" );  
        probeOrganismColumnNames.add( "org" ); 
        probeOrganismColumnNames.add( "Species Scientific name" );//agilent?
        probeOrganismColumnNames.add( "Taxon" );
        
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

    public static boolean likelySequence( String id ) {
        return sequenceColumnNames.contains( id );
    }
    
    
    public static boolean likelyProbeOrganism( String id ) {
        return probeOrganismColumnNames.contains( id );
    }

    /**
     * @param id
     * @return
     */
    public static boolean likelyProbeDescription( String id ) {
        return descriptionNames.contains( id );
    }

}
