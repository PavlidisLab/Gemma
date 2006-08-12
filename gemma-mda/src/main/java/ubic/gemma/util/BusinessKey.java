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
package ubic.gemma.util;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.gene.GeneProduct;

/**
 * Methods to test business-key-related issues on objects.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class BusinessKey {

    /**
     * @param bioSequence
     * @return
     */
    public static void checkValidKey( BioSequence bioSequence ) {
        if ( bioSequence == null
                || ( StringUtils.isBlank( bioSequence.getName() ) && StringUtils.isBlank( bioSequence.getSequence() ) && bioSequence
                        .getSequenceDatabaseEntry() == null ) ) {
            throw new IllegalArgumentException( "Biosequence did not have a valid key" );
        }

        // FIXME: add check for non-null taxon, even though it should not be nullable.
    }

    public static void checkValidKey( Taxon taxon ) {
        if ( taxon == null || ( taxon.getNcbiId() == null && StringUtils.isBlank( taxon.getCommonName() ) ) ) {
            throw new IllegalArgumentException( "Taxon " + taxon + " did not have a valid key" );
        }
    }

    /**
     * @param gene
     */
    public static void checkValidKey( Gene gene ) {
        return;
    }

    public static void checkValidKey( GeneProduct geneProduct ) {
        boolean ok = true;

        if ( geneProduct == null ) ok = false;

        if ( StringUtils.isBlank( geneProduct.getNcbiId() ) && StringUtils.isBlank( geneProduct.getName() ) )
            ok = false;

        if ( !ok ) {
            throw new IllegalArgumentException( "GeneProduct did not have a valid key" );
        }
    }

    /**
     * Assumes that the queryObject was created for a class with a parameter named "bioSequence", and restricts the
     * query to the provided BioSequence.
     * 
     * @param queryObject
     * @param string
     */
    public static void attachCriteria( Criteria queryObject, BioSequence bioSequence, String propertyName ) {
        Criteria innerQuery = queryObject.createCriteria( propertyName );
        addRestrictions( innerQuery, bioSequence );
    }

    /**
     * @param innerQuery
     * @param bioSequence
     */
    private static void addRestrictions( Criteria queryObject, BioSequence bioSequence ) {
        if ( StringUtils.isNotBlank( bioSequence.getName() ) ) {
            queryObject.add( Restrictions.eq( "name", bioSequence.getName() ) );

            addTaxonRestriction( queryObject, bioSequence.getTaxon() );
        }

        if ( bioSequence.getSequenceDatabaseEntry() != null ) {
            queryObject.createCriteria( "sequenceDatabaseEntry" ).add(
                    Restrictions.eq( "accession", bioSequence.getSequenceDatabaseEntry().getAccession() ) );
        }

        if ( StringUtils.isNotBlank( bioSequence.getSequence() ) ) {
            queryObject.add( Restrictions.eq( "sequence", bioSequence.getSequence() ) );

            addTaxonRestriction( queryObject, bioSequence.getTaxon() );
        }
    }

    /**
     * @param queryObject
     * @param taxon
     */
    private static void addTaxonRestriction( Criteria queryObject, Taxon taxon ) {
        checkValidKey( taxon );

        if ( taxon.getNcbiId() != null ) {
            queryObject.createCriteria( "taxon" ).add( Restrictions.eq( "ncbiId", taxon.getNcbiId() ) );
        } else if ( StringUtils.isNotBlank( taxon.getCommonName() ) ) {
            queryObject.createCriteria( "taxon" ).add( Restrictions.eq( "commonName", taxon.getCommonName() ) );
        }
    }

    /**
     * @param session
     * @param bioSequence
     * @return
     */
    public static Criteria createQueryObject( Session session, BioSequence bioSequence ) {
        Criteria queryObject = session.createCriteria( BioSequence.class );
        addRestrictions( queryObject, bioSequence );
        return queryObject;
    }

    public static void checkValidKey( ubic.gemma.model.common.description.LocalFile localFile ) {
        if ( localFile == null || localFile.getLocalURI() == null
                || ( localFile.getRemoteURI() == null && localFile.getSize() == 0 ) ) {
            throw new IllegalArgumentException( "localFile was null or had no valid business keys" );
        }
    }

}
