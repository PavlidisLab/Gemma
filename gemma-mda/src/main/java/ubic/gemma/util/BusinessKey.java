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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

import ubic.gemma.model.common.Describable;
import ubic.gemma.model.common.description.OntologyEntry;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.gene.GeneProduct;

/**
 * Methods to test business-key-related issues on objects. The 'checkValidKey' methods can be used to check whether an
 * object has the required business key values filled in. An exception is thrown if they don't.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class BusinessKey {

    private static Log log = LogFactory.getLog( BusinessKey.class.getName() );

    /**
     * Restricts the query to the provided BioSequence.
     * 
     * @param queryObject
     * @param bioSequence The object used to create the criteria
     * @param propertyName Often this will be 'bioSequence'
     */
    public static void attachCriteria( Criteria queryObject, BioSequence bioSequence, String propertyName ) {
        Criteria innerQuery = queryObject.createCriteria( propertyName );
        addRestrictions( innerQuery, bioSequence );
        log.warn( queryObject.toString() );
    }

    /**
     * Restricts the query to the provided OntologyEntry.
     * 
     * @param queryObject
     * @param ontologyEntry The object used to create the criteria
     * @param propertyName Often this will be 'ontologyEntry'
     */
    public static void attachCriteria( Criteria queryObject, OntologyEntry ontologyEntry, String propertyName ) {
        Criteria innerQuery = queryObject.createCriteria( propertyName );
        addRestrictions( innerQuery, ontologyEntry );
        log.debug( queryObject.toString() );
    }

    /**
     * @param bioSequence
     */
    public static void checkValidKey( BioSequence bioSequence ) {
        if ( bioSequence == null
                || bioSequence.getTaxon() == null
                || ( StringUtils.isBlank( bioSequence.getName() ) && StringUtils.isBlank( bioSequence.getSequence() ) && bioSequence
                        .getSequenceDatabaseEntry() == null ) ) {
            throw new IllegalArgumentException( "Biosequence did not have a valid key" );
        }
    }

    /**
     * @param gene
     */
    public static void checkValidKey( Gene gene ) {
        // FIXME make key.
        return;
    }

    public static void checkValidKey( ArrayDesign arrayDesign ) {
        if ( arrayDesign == null || StringUtils.isBlank( arrayDesign.getName() ) ) {
            throw new IllegalArgumentException( arrayDesign + " did not have a valid key" );
        }
    }

    /**
     * @param geneProduct
     */
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
     * @param taxon
     */
    public static void checkValidKey( Taxon taxon ) {
        if ( taxon == null
                || ( taxon.getNcbiId() == null && StringUtils.isBlank( taxon.getCommonName() ) && StringUtils
                        .isBlank( taxon.getScientificName() ) ) ) {
            throw new IllegalArgumentException( "Taxon " + taxon + " did not have a valid key" );
        }
    }

    /**
     * @param localFile
     */
    public static void checkValidKey( ubic.gemma.model.common.description.LocalFile localFile ) {
        if ( localFile == null || localFile.getLocalURL() == null ) {
            if ( localFile != null )
                log.error( "Localfile without valid key: localURL=" + localFile.getLocalURL() + " remoteUrL="
                        + localFile.getRemoteURL() + " size=" + localFile.getSize() );
            throw new IllegalArgumentException( "localFile was null or had no valid business keys" );
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

    /**
     * @param session
     * @param ontologyEntry
     * @return
     */
    public static Criteria createQueryObject( Session session, OntologyEntry ontologyEntry ) {
        Criteria queryObject = session.createCriteria( OntologyEntry.class );
        addRestrictions( queryObject, ontologyEntry );
        return queryObject;
    }

    /**
     * @param session
     * @param ontologyEntry
     * @return
     */
    public static Criteria createQueryObject( Session session, ArrayDesign arrayDesign ) {
        Criteria queryObject = session.createCriteria( ArrayDesign.class );
        addRestrictions( queryObject, arrayDesign );
        return queryObject;
    }

    public static void addRestrictions( Criteria queryObject, ArrayDesign arrayDesign ) {
        addNameRestriction( queryObject, arrayDesign );

        if ( arrayDesign.getDesignProvider() != null
                && StringUtils.isNotBlank( arrayDesign.getDesignProvider().getName() ) ) {
            queryObject.createCriteria( "designProvider" ).add(
                    Restrictions.eq( "name", arrayDesign.getDesignProvider().getName() ) );
        }
    }

    /**
     * @param queryObject
     * @param arrayDesign
     */
    private static void addNameRestriction( Criteria queryObject, Describable arrayDesign ) {
        queryObject.add( Restrictions.eq( "name", arrayDesign.getName() ) );
    }

    /**
     * @param innerQuery
     * @param bioSequence
     */
    private static void addRestrictions( Criteria queryObject, BioSequence bioSequence ) {
        if ( StringUtils.isNotBlank( bioSequence.getName() ) ) {
            addNameRestriction( queryObject, bioSequence );

            addRestrictions( queryObject, bioSequence.getTaxon() );
        }

        if ( bioSequence.getSequenceDatabaseEntry() != null ) {
            queryObject.createCriteria( "sequenceDatabaseEntry" ).add(
                    Restrictions.eq( "accession", bioSequence.getSequenceDatabaseEntry().getAccession() ) );
        }

        if ( StringUtils.isNotBlank( bioSequence.getSequence() ) ) {
            queryObject.add( Restrictions.eq( "sequence", bioSequence.getSequence() ) );

            addRestrictions( queryObject, bioSequence.getTaxon() );
        }
    }

    /**
     * @param queryObject
     * @param ontologyEntry
     */
    private static void addRestrictions( Criteria queryObject, OntologyEntry ontologyEntry ) {
        if ( ontologyEntry.getAccession() != null && ontologyEntry.getExternalDatabase() != null ) {
            queryObject.add( Restrictions.eq( "accession", ontologyEntry.getAccession() ) ).createCriteria(
                    "externalDatabase" ).add( Restrictions.eq( "name", ontologyEntry.getExternalDatabase().getName() ) );
        } else {
            queryObject.add( Restrictions.ilike( "category", ontologyEntry.getCategory() ) ).add(
                    Restrictions.ilike( "value", ontologyEntry.getValue() ) );
        }
    }

    /**
     * Assumes parameter name is 'taxon'.
     * 
     * @param queryObject
     * @param taxon
     */
    private static void addRestrictions( Criteria queryObject, Taxon taxon ) {
        checkValidKey( taxon );

        if ( taxon.getNcbiId() != null ) {
            queryObject.createCriteria( "taxon" ).add( Restrictions.eq( "ncbiId", taxon.getNcbiId() ) );
        } else if ( StringUtils.isNotBlank( taxon.getCommonName() ) ) {
            queryObject.createCriteria( "taxon" ).add( Restrictions.eq( "commonName", taxon.getCommonName() ) );
        }
    }

}
