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
package ubic.gemma.persistence.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.*;
import ubic.gemma.model.association.Gene2GOAssociation;
import ubic.gemma.model.common.Describable;
import ubic.gemma.model.common.auditAndSecurity.Contact;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.description.*;
import ubic.gemma.model.common.measurement.Unit;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.AlternateName;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.genome.Chromosome;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.PhysicalLocation;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.gene.GeneProduct;

import java.util.Collection;
import java.util.HashSet;

import static ubic.gemma.persistence.util.Specifications.byIdentifiable;

/**
 * Methods to test business-key-related issues on objects. The 'checkValidKey' methods can be used to check whether an
 * object has the required business key values filled in. An exception is thrown if they don't.
 * This class contains some important code that determines our rules for how entities are detected as being the same as
 * another (in queries from the database; this is on top of basic 'equals', but should be compatible).
 *
 * @author pavlidis
 */
public class BusinessKey {

    private static final Log log = LogFactory.getLog( BusinessKey.class.getName() );

    public static void addADRestrictions( Criteria queryObject, Specification<ArrayDesign> spec ) {
        ArrayDesign arrayDesign = spec.getEntity();

        /*
         * Test whether ANY of the associated external references match any of the given external references.
         */
        if ( arrayDesign.getPrimaryTaxon() != null && arrayDesign.getPrimaryTaxon().getId() != null ) {
            queryObject.add( Restrictions.eq( "primaryTaxon", arrayDesign.getPrimaryTaxon() ) );
        }

        if ( arrayDesign.getExternalReferences().size() != 0 ) {
            Criteria externalRef = queryObject.createCriteria( "externalReferences" );
            Disjunction disjunction = Restrictions.disjunction();
            for ( DatabaseEntry databaseEntry : arrayDesign.getExternalReferences() ) {
                disjunction.add( Restrictions.eq( "accession", databaseEntry.getAccession() ) );
                // FIXME this should include the ExternalDatabase in the criteria.
            }
            externalRef.add( disjunction );
            return;
        } else if ( arrayDesign.getAlternateNames().size() != 0 ) {
            Criteria externalRef = queryObject.createCriteria( "alternateNames" );
            Disjunction disjunction = Restrictions.disjunction();
            for ( AlternateName alternateName : arrayDesign.getAlternateNames() ) {
                disjunction.add( Restrictions.eq( "name", alternateName.getName() ) );
            }
            externalRef.add( disjunction );
            return;
        } else if ( arrayDesign.getShortName() != null ) {
            // this might not be such a good idea, because we can edit the short name.
            queryObject.add( Restrictions.eq( "shortName", arrayDesign.getShortName() ) );
        } else {
            BusinessKey.addNameRestriction( queryObject, arrayDesign );
        }

        if ( arrayDesign.getDesignProvider() != null && StringUtils
                .isNotBlank( arrayDesign.getDesignProvider().getName() ) ) {
            queryObject.createCriteria( "designProvider" )
                    .add( Restrictions.eq( "name", arrayDesign.getDesignProvider().getName() ) );
        }

    }

    @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
    public static void addBARestrictions( Criteria queryObject, Specification<BioAssay> spec ) {
        BioAssay bioAssay = spec.getEntity();
        if ( bioAssay.getId() != null ) {
            queryObject.add( Restrictions.eq( "id", bioAssay.getId() ) );
        } else if ( bioAssay.getAccession() != null ) {
            BusinessKey.attachDECriteria( queryObject, byIdentifiable( bioAssay.getAccession() ), "accession" );
        }
        queryObject.add( Restrictions.eq( "name", bioAssay.getName() ) );
    }

    public static void addBMRestrictions( Criteria queryObject, Specification<BioMaterial> spec ) {
        BioMaterial bioMaterial = spec.getEntity();

        if ( bioMaterial.getName() != null ) {
            queryObject.add( Restrictions.eq( "name", bioMaterial.getName() ) );
        }

        if ( bioMaterial.getExternalAccession() != null ) {
            // this is not completely foolproof.
            queryObject.createCriteria( "externalAccession" )
                    .add( Restrictions.eq( "accession", bioMaterial.getExternalAccession().getAccession() ) );
        } else if ( StringUtils.isNotBlank( bioMaterial.getDescription() ) ) {
            // The description is generally only filled in by Gemma, and contains the experiment short name.
            queryObject.add( Restrictions.eq( "description", bioMaterial.getDescription() ) );
        }

        if ( bioMaterial.getSourceTaxon() != null ) {
            queryObject.add( Restrictions.eq( "sourceTaxon", bioMaterial.getSourceTaxon() ) );
        }
    }

    /**
     * Note: The finder has to do the additional checking for equality of sequence and/or database entry - we don't know
     * until we get the sequences. Due to the following issues:
     * <ul>
     * <li>Sometimes the sequence in the database lacks the DatabaseEntry
     * <li>Sometimes the old entry lacks the actual sequence (ATCG..)
     * </ul>
     * This means that we can't use those criteria up front. Instead we match by name and taxon. If those match, the
     * caller has to sort through possible multiple results to find the correct one.
     */
    @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
    public static void addBSRestrictions( Criteria queryObject, Specification<BioSequence> spec ) {
        BioSequence bioSequence = spec.getEntity();

        if ( bioSequence.getId() != null ) {
            queryObject.add( Restrictions.eq( "id", bioSequence.getId() ) );
            return;
        }

        if ( StringUtils.isNotBlank( bioSequence.getName() ) ) {
            BusinessKey.addNameRestriction( queryObject, bioSequence );
        }

        BusinessKey.attachTaxonCriteria( queryObject, byIdentifiable( bioSequence.getTaxon() ), "taxon" );
    }

    @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
    public static void addCRestrictions( Criteria queryObject, Specification<Characteristic> spec ) {
        Characteristic characteristic = spec.getEntity();

        if ( characteristic.getCategoryUri() != null ) {
            queryObject.add( Restrictions.eq( "categoryUri", characteristic.getCategoryUri() ) );
        } else if ( characteristic.getCategory() != null ) {
            queryObject.add( Restrictions.eq( "category", characteristic.getCategory() ) );
        }

        if ( StringUtils.isNotBlank( characteristic.getValueUri() ) ) {
            queryObject.add( Restrictions.eq( "valueUri", characteristic.getValueUri() ) );
        } else {
            assert characteristic.getValue() != null;
            queryObject.add( Restrictions.eq( "value", characteristic.getValue() ) );
        }

    }

    @SuppressWarnings({ "WeakerAccess", "unused" }) // Possible external use
    public static void addChromosomeRestrictions( Criteria queryObject, Specification<Chromosome> spec ) {
        Chromosome chromosome = spec.getEntity();
        queryObject.add( Restrictions.eq( "name", chromosome.getName() ) );
        BusinessKey.attachTaxonCriteria( queryObject, byIdentifiable( chromosome.getTaxon() ), "taxon" );
        if ( chromosome.getAssemblyDatabase() != null ) {
            BusinessKey.attachCriteria( queryObject, chromosome.getAssemblyDatabase() );
        }
        if ( chromosome.getSequence() != null ) {
            BusinessKey.attachBSCriteria( queryObject, byIdentifiable( chromosome.getSequence() ), "sequence" );
        }
    }

    public static void addContactRestrictions( Criteria queryObject, Specification<Contact> spec ) {
        Contact contact = spec.getEntity();

        if ( contact instanceof User ) {
            queryObject.add( Restrictions.eq( "userName", ( ( User ) contact ).getUserName() ) );
            return;
        }

        if ( StringUtils.isNotBlank( contact.getEmail() ) ) {
            // email is NOT unique.
            queryObject.add( Restrictions.eq( "email", contact.getEmail() ) );
        }

        if ( StringUtils.isNotBlank( contact.getName() ) )
            queryObject.add( Restrictions.eq( "name", contact.getName() ) );

    }

    public static void addEFRestrictions( Criteria queryObject, Specification<ExperimentalFactor> spec ) {
        ExperimentalFactor experimentalFactor = spec.getEntity();

        if ( experimentalFactor.getId() != null ) {
            queryObject.add( Restrictions.eq( "id", experimentalFactor.getId() ) );
            return;
        }

        if ( StringUtils.isNotBlank( experimentalFactor.getName() ) ) {
            queryObject.add( Restrictions.eq( "name", experimentalFactor.getName() ) );
        }

        if ( experimentalFactor.getCategory() != null ) {
            BusinessKey.attachCCriteria( queryObject, byIdentifiable( experimentalFactor.getCategory() ), "category" );
        }
    }

    @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
    public static void addGeneRestrictions( Criteria queryObject, Specification<Gene> spec, boolean stricter ) {
        Gene gene = spec.getEntity();
        if ( gene.getId() != null ) {
            queryObject.add( Restrictions.eq( "id", gene.getId() ) );
        } else if ( gene.getNcbiGeneId() != null ) {
            /*
             * These are unambiguous identifiers.
             */

            if ( StringUtils.isNotBlank( gene.getPreviousNcbiId() ) ) {
                Collection<Integer> ncbiIds = new HashSet<>();
                ncbiIds.add( gene.getNcbiGeneId() );
                for ( String previousId : StringUtils.split( gene.getPreviousNcbiId(), "," ) ) {
                    /*
                     * Check to see if the new gene used to use an id that is in the system. This is needed to deal with
                     * the case where NCBI changes a gene id (which is common, from gene_history).
                     */
                    try {
                        ncbiIds.add( Integer.parseInt( previousId ) );
                    } catch ( NumberFormatException e ) {
                        BusinessKey.log.warn( "Previous Ncbi id wasn't parseable to an int: " + previousId );
                    }
                }
                queryObject.add( Restrictions.in( "ncbiGeneId", ncbiIds ) );

            } else {
                queryObject.add( Restrictions.eq( "ncbiGeneId", gene.getNcbiGeneId() ) );
            }

        } else if ( StringUtils.isNotBlank( gene.getOfficialSymbol() ) ) {
            /*
             * Second choice, but not unambiguous even within a taxon unless we know the physical location
             */
            queryObject.add( Restrictions.eq( "officialSymbol", gene.getOfficialSymbol() ) );

            BusinessKey.attachTaxonCriteria( queryObject, byIdentifiable( gene.getTaxon() ), "taxon" );

            if ( stricter ) {
                // Need either the official name AND the location to be unambiguous. But if we are already restricted on
                // some other characteristic such as the gene, this causes too many false negatives. Typical case: NCBI
                // vs. GoldenPath.
                if ( StringUtils.isNotBlank( gene.getOfficialName() ) ) {
                    queryObject.add( Restrictions.eq( "officialName", gene.getOfficialName() ) );
                }

                if ( gene.getPhysicalLocation() != null ) {
                    BusinessKey.attachPLCriteria( queryObject, byIdentifiable( gene.getPhysicalLocation() ), "physicalLocation" );
                }
            }
        } else {
            throw new IllegalArgumentException( "No valid key " + gene );
        }
    }

    public static void addG2GRestrictions( Criteria queryObject, Specification<Gene2GOAssociation> gene2GOAssociation ) {
        BusinessKey.attachGeneCriteria( queryObject, byIdentifiable( gene2GOAssociation.getEntity().getGene() ), "gene" );
        BusinessKey.attachCriteria( queryObject, byIdentifiable( gene2GOAssociation.getEntity().getOntologyEntry() ) );
    }

    public static void addQTRestrictions( Criteria queryObject, Specification<QuantitationType> spec ) {
        QuantitationType quantitationType = spec.getEntity();
        queryObject.add( Restrictions.eq( "name", quantitationType.getName() ) );

        queryObject.add( Restrictions.eq( "description", quantitationType.getDescription() ) );

        queryObject.add( Restrictions.eq( "generalType", quantitationType.getGeneralType() ) );

        queryObject.add( Restrictions.eq( "type", quantitationType.getType() ) );

        queryObject.add( Restrictions.eq( "isBackground", quantitationType.getIsBackground() ) );

        if ( quantitationType.getRepresentation() != null )
            queryObject.add( Restrictions.eq( "representation", quantitationType.getRepresentation() ) );

        if ( quantitationType.getScale() != null )
            queryObject.add( Restrictions.eq( "scale", quantitationType.getScale() ) );

        queryObject.add( Restrictions.eq( "isBackgroundSubtracted", quantitationType.getIsBackgroundSubtracted() ) );

        queryObject.add( Restrictions.eq( "isPreferred", quantitationType.getIsPreferred() ) );

        queryObject.add( Restrictions.eq( "isNormalized", quantitationType.getIsNormalized() ) );

    }

    public static void addTaxonRestrictions( Criteria queryObject, Specification<Taxon> taxon ) {
        BusinessKey.checkValidTaxonKey( taxon );
        BusinessKey.attachTaxonCriteria( queryObject, taxon );
    }

    @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
    public static void addDERestrictions( DetachedCriteria queryObject, Specification<DatabaseEntry> databaseEntry ) {
        queryObject.add( Restrictions.eq( "accession", databaseEntry.getEntity().getAccession() ) )
                .createCriteria( "externalDatabase" )
                .add( Restrictions.eq( "name", databaseEntry.getEntity().getExternalDatabase().getName() ) );
    }

    /**
     * Restricts the query to the provided BioSequence.
     *
     * @param bioSequence  The object used to create the criteria
     * @param propertyName Often this will be 'bioSequence'
     * @param queryObject  query object
     */
    public static void attachBSCriteria( Criteria queryObject, Specification<BioSequence> bioSequence, String propertyName ) {
        Criteria innerQuery = queryObject.createCriteria( propertyName );
        BusinessKey.addBSRestrictions( innerQuery, bioSequence );
    }

    /**
     * Restricts the query to the provided OntologyEntry.
     *
     * @param ontologyEntry The object used to create the criteria
     * @param propertyName  Often this will be 'ontologyEntry'
     * @param queryObject   query object
     */
    @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
    public static void attachCCriteria( Criteria queryObject, Specification<Characteristic> ontologyEntry, String propertyName ) {
        Criteria innerQuery = queryObject.createCriteria( propertyName );
        BusinessKey.addCRestrictions( innerQuery, ontologyEntry );
    }

    /**
     * Restricts query to the given DatabaseEntry association
     *
     * @param databaseEntry to match
     * @param propertyName  often "accession"
     * @param queryObject   query object
     */
    public static void attachDECriteria( Criteria queryObject, Specification<DatabaseEntry> databaseEntry, String propertyName ) {
        Criteria innerQuery = queryObject.createCriteria( propertyName );
        BusinessKey.attachDECriteria( innerQuery, databaseEntry );
    }

    /**
     * Restricts the query to the provided Gene.
     *
     * @param queryObject  query object
     * @param gene         gene
     * @param propertyName property name
     */
    @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
    public static void attachGeneCriteria( Criteria queryObject, Specification<Gene> gene, String propertyName ) {
        Criteria innerQuery = queryObject.createCriteria( propertyName );
        BusinessKey.addGeneRestrictions( innerQuery, gene, true );
    }

    @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
    public static void attachPLCriteria( Criteria queryObject, Specification<PhysicalLocation> spec, String attributeName ) {
        PhysicalLocation physicalLocation = spec.getEntity();
        Criteria nestedCriteria = queryObject.createCriteria( attributeName );

        if ( physicalLocation.getChromosome() == null ) {
            throw new IllegalArgumentException();
        }

        if ( physicalLocation.getChromosome().getId() != null ) {
            nestedCriteria.createCriteria( "chromosome" )
                    .add( Restrictions.eq( "id", physicalLocation.getChromosome().getId() ) );
        } else {
            // FIXME should add taxon to this.
            nestedCriteria.createCriteria( "chromosome" )
                    .add( Restrictions.eq( "name", physicalLocation.getChromosome().getName() ) );
        }

        if ( physicalLocation.getNucleotide() != null )
            nestedCriteria.add( Restrictions.eq( "nucleotide", physicalLocation.getNucleotide() ) );

        if ( physicalLocation.getNucleotideLength() != null )
            nestedCriteria.add( Restrictions.eq( "nucleotideLength", physicalLocation.getNucleotideLength() ) );

    }

    /**
     * Restricts query to the given Taxon.
     *
     * @param propertyName often "taxon"
     * @param queryObject  query object
     * @param taxon        taxon
     */
    @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
    public static void attachTaxonCriteria( Criteria queryObject, Specification<Taxon> taxon, String propertyName ) {
        Criteria innerQuery = queryObject.createCriteria( propertyName );
        BusinessKey.attachCriteria( innerQuery, taxon );
    }

    @SuppressWarnings({ "WeakerAccess", "unused" }) // Possible external use
    public static void attachDECriteria( DetachedCriteria queryObject, Specification<DatabaseEntry> databaseEntry,
            String attributeName ) {
        DetachedCriteria externalRef = queryObject.createCriteria( attributeName );
        BusinessKey.addDERestrictions( externalRef, databaseEntry );
    }

    public static void checkBRKey( Specification<BibliographicReference> bibliographicReference ) {
        if ( bibliographicReference == null || bibliographicReference.getPubAccession() == null
                || bibliographicReference.getPubAccession().getAccession() == null ) {
            throw new IllegalArgumentException(
                    "BibliographicReference was null or had no accession : " + bibliographicReference );
        }
    }

    @SuppressWarnings("WeakerAccess") // Consistency
    public static void checkCKey( Specification<Characteristic> ontologyEntry ) {
        if ( ontologyEntry.getEntity().getValue() == null )
            throw new IllegalArgumentException();
    }

    public static void checkContactKey( Specification<Contact> contact ) {
        if ( contact == null || ( StringUtils.isBlank( contact.getName() ) && StringUtils
                .isBlank( contact.getEmail() ) ) ) {
            throw new IllegalArgumentException( "Contact must have at least some information filled in!" );
        }
    }

    public static void checkDEKey( Specification<DatabaseEntry> spec ) {
        DatabaseEntry accession = spec.getEntity();
        if ( accession.getId() != null )
            return;
        if ( StringUtils.isBlank( accession.getAccession() ) ) {
            throw new IllegalArgumentException( accession + " did not have an accession" );
        }
        BusinessKey.checkEESSKey( accession.getExternalDatabase() );
    }

    public static void checkDEDVKey( Specification<? extends DesignElementDataVector> designElementDataVector ) {
        if ( designElementDataVector.getEntity() == null || designElementDataVector.getEntity().getDesignElement() == null
                || designElementDataVector.getEntity().getExpressionExperiment() == null ) {
            throw new IllegalArgumentException(
                    "DesignElementDataVector did not have complete business key " + designElementDataVector );
        }
    }

    @SuppressWarnings("WeakerAccess") // Consistency
    public static void checkEDKey( Specification<ExternalDatabase> spec ) {
        ExternalDatabase externalDatabase = spec.getEntity();
        if ( externalDatabase.getId() != null )
            return;
        if ( StringUtils.isBlank( externalDatabase.getName() ) ) {
            throw new IllegalArgumentException( externalDatabase + " did not have a name" );
        }
    }

    public static void checkFVKey( Specification<FactorValue> spec ) {
        FactorValue factorValue = spec.getEntity();
        if ( factorValue.getValue() == null && factorValue.getMeasurement() == null
                && factorValue.getCharacteristics().size() == 0 ) {
            throw new IllegalArgumentException(
                    "FactorValue must have a value (or associated measurement or characteristics)." );
        }
    }

    public static void checkGeneKey( Specification<Gene> spec ) {
        Gene gene = spec.getEntity();
        if ( gene == null )
            throw new IllegalArgumentException( "Gene cannot be null" );
        if ( ( ( gene.getOfficialSymbol() == null || gene.getTaxon() == null ) && gene.getPhysicalLocation() == null
                && ( gene.getProducts() == null || gene.getProducts().isEmpty() ) ) && gene.getNcbiGeneId() == null ) {
            throw new IllegalArgumentException( "No valid key for " + gene
                    + ": Gene must have official symbol and name with taxon + physical location or gene products, or ncbiId" );
        }
    }

    public static void checkUserKey( Specification<User> user ) {
        if ( user.getEntity() == null || StringUtils.isBlank( user.getEntity().getUserName() ) ) {
            throw new IllegalArgumentException( "User was null or had no userName defined" );
        }
    }

    public static void checkValidArrayDesignKey( Specification<ArrayDesign> arrayDesign ) {
        if ( arrayDesign.getEntity() == null || ( StringUtils.isBlank( arrayDesign.getEntity().getName() ) && StringUtils
                .isBlank( arrayDesign.getEntity().getShortName() ) && arrayDesign.getEntity().getExternalReferences().size() == 0 ) ) {
            throw new IllegalArgumentException( arrayDesign + " did not have a valid key" );
        }
    }

    public static void checkValidBioSequenceKey( Specification<BioSequence> bioSequence ) {
        if ( bioSequence.getEntity() == null || bioSequence.getEntity().getTaxon() == null || StringUtils.isBlank( bioSequence.getEntity().getName() ) ) {
            throw new IllegalArgumentException( bioSequence + " did not have a valid key" );
        }
    }

    @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
    public static void checkValidChromosomeKey( Specification<Chromosome> chromosome ) {
        if ( chromosome.getEntity() == null || StringUtils.isBlank( chromosome.getEntity().getName() ) ) {
            throw new IllegalArgumentException( "Chromosome did not have a valid key" );
        }
        BusinessKey.checkValidTaxonKey( byIdentifiable( chromosome.getEntity().getTaxon() ) );
    }

    public static void checkValidDatabaseEntryKey( Specification<DatabaseEntry> databaseEntry ) {
        if ( databaseEntry.getEntity() == null || databaseEntry.getEntity().getAccession() == null
                || databaseEntry.getEntity().getExternalDatabase() == null ) {
            throw new IllegalArgumentException( "DatabaseEntry does not have valid key" );
        }
    }

    public static void checkValidEFKey( ExperimentalFactor experimentalFactor ) {
        if ( StringUtils.isBlank( experimentalFactor.getName() ) && experimentalFactor.getCategory() == null ) {
            throw new IllegalArgumentException( "Experimental factor must have name or category" );
        }
    }

    @SuppressWarnings("WeakerAccess") // Consistency
    public static void checkValidGeneKey( Specification<Gene> gene ) {
        if ( gene == null || ( gene.getNcbiGeneId() == null && ( StringUtils.isBlank( gene.getOfficialSymbol() )
                || gene.getTaxon() == null || StringUtils.isBlank( gene.getOfficialName() ) ) ) ) {
            throw new IllegalArgumentException(
                    "Gene does not have valid key (needs NCBI numeric id or Official Symbol + Official Name + Taxon" );
        }
    }

    public static void checkValidG2GKey( Specification<Gene2GOAssociation> gene2GOAssociation ) {
        BusinessKey.checkValidUnitKey( gene2GOAssociation.getGene() );
    }

    public static void checkValidGPKey( Specification<GeneProduct> geneProduct ) {
        if ( geneProduct.getId() != null )
            return;

        boolean ok = false;

        if ( StringUtils.isNotBlank( geneProduct.getNcbiGi() ) || StringUtils.isNotBlank( geneProduct.getName() ) )
            ok = true;

        if ( !ok ) {
            throw new IllegalArgumentException( "GeneProduct did not have a valid key - requires name or NCBI GI" );
        }

        if ( geneProduct.getGene() != null ) {
            BusinessKey.checkEESSKey( geneProduct.getGene() );
        }

    }

    public static void checkValidTaxonKey( Specification<Taxon> taxon ) {
        if ( taxon == null || ( taxon.getNcbiId() == null && StringUtils.isBlank( taxon.getCommonName() ) && StringUtils
                .isBlank( taxon.getScientificName() ) ) ) {
            throw new IllegalArgumentException( "Taxon " + taxon + " did not have a valid key" );
        }
    }

    public static void checkValidLFKey( Specification<LocalFile> localFile ) {
        if ( localFile.getEntity() == null || ( localFile.getEntity().getLocalURL() == null && localFile.getEntity().getRemoteURL() == null ) ) {
            if ( localFile.getEntity() != null )
                BusinessKey.log
                        .error( "Localfile without valid key: localURL=" + localFile.getEntity().getLocalURL() + " remoteUrL="
                                + localFile.getEntity().getRemoteURL() + " size=" + localFile.getEntity().getSize() );
            throw new IllegalArgumentException( "localFile was null or had no valid business keys" );
        }
    }

    public static void checkValidUnitKey( Specification<Unit> unit ) {
        if ( unit.getEntity() == null || StringUtils.isBlank( unit.getEntity().getUnitNameCV() ) ) {
            throw new IllegalArgumentException( unit + " did not have a valid key" );
        }
    }

    public static void createFVQueryObject( Criteria queryObject, Specification<FactorValue> spec ) {
        FactorValue factorValue = spec.getEntity();

        ExperimentalFactor ef = factorValue.getExperimentalFactor();

        if ( ef == null )
            throw new IllegalArgumentException( "Must have experimentalfactor on factorvalue to search" );

        Criteria innerQuery = queryObject.createCriteria( "experimentalFactor" );
        BusinessKey.addEFRestrictions( innerQuery, byIdentifiable( ef ) );

        if ( factorValue.getValue() != null ) {
            queryObject.add( Restrictions.eq( "value", factorValue.getValue() ) );
        } else if ( factorValue.getCharacteristics().size() > 0 ) {

            /*
             * All the characteristics have to match ones in the result, and the result cannot have any extras. In other
             * words there has to be a one-to-one match between the characteristics.
             */

            // this takes care of the size check
            queryObject.add( Restrictions.sizeEq( "characteristics", factorValue.getCharacteristics().size() ) );

            // now the equivalence.
            Criteria characteristicsCriteria = queryObject.createCriteria( "characteristics" );

            /*
             * Note that this isn't exactly correct, but it should work okay: "If all the characteristics in the
             * candidate are also in the query", along with the size restriction. The only problem would be if the same
             * characteristic were added to an object more than once - so the sizes would be the same, but a
             * characteristic in the query might not show up in the candidate. Multiple entries of the same
             * characteristic shouldn't be allowed, and even if it did happen the chance of a problem is small.... but a
             * formal possibility.
             */
            Disjunction vdj = Restrictions.disjunction();
            for ( Characteristic characteristic : factorValue.getCharacteristics() ) {

                Conjunction c = Restrictions.conjunction();

                if ( StringUtils.isNotBlank( characteristic.getCategoryUri() ) ) {
                    c.add( Restrictions.eq( "categoryUri", characteristic.getCategoryUri() ) );
                }
                if ( StringUtils.isNotBlank( characteristic.getValueUri() ) ) {
                    c.add( Restrictions.eq( "valueUri", characteristic.getValueUri() ) );
                }

                if ( StringUtils.isNotBlank( characteristic.getValue() ) )
                    c.add( Restrictions.eq( "value", characteristic.getValue() ) );

                if ( StringUtils.isNotBlank( characteristic.getCategory() ) )
                    c.add( Restrictions.eq( "category", characteristic.getCategory() ) );

                vdj.add( c );
            }
            characteristicsCriteria.add( vdj );

        } else if ( factorValue.getMeasurement() != null ) {
            queryObject.add( Restrictions.eq( "measurement", factorValue.getMeasurement() ) );
        }

        queryObject.setResultTransformer( CriteriaSpecification.DISTINCT_ROOT_ENTITY );
    }

    public static void createGeneQueryObject( Criteria queryObject, Specification<Gene> gene ) {
        if ( gene.getEntity().getId() != null ) {
            queryObject.add( Restrictions.eq( "id", gene.getEntity().getId() ) );
        } else {
            BusinessKey.addGeneRestrictions( queryObject, gene, true );
        }
    }

    public static void createGPQueryObject( Criteria queryObject, Specification<GeneProduct> spec ) {
        GeneProduct geneProduct = spec.getEntity();
        if ( geneProduct.getId() != null ) {
            queryObject.add( Restrictions.eq( "id", geneProduct.getId() ) );
        } else if ( StringUtils.isNotBlank( geneProduct.getNcbiGi() ) ) {
            queryObject.add( Restrictions.eq( "ncbiGi", geneProduct.getNcbiGi() ) );
        } else if ( StringUtils.isNotBlank( geneProduct.getName() ) ) { // NM_XXXXX etc.
            queryObject.add( Restrictions.eq( "name", geneProduct.getName() ) );
            /*
             * This can cause some problems when golden path and NCBI don't have the same information about the gene
             */
            if ( geneProduct.getGene() != null ) {
                Criteria geneCrits = queryObject.createCriteria( "gene" );
                BusinessKey.addGeneRestrictions( geneCrits, byIdentifiable( geneProduct.getGene() ), false );
            }
        }
    }

    @SuppressWarnings("unused") // Possible external use
    public static Criteria createADQueryObject( Session session, Specification<ArrayDesign> arrayDesign ) {
        Criteria queryObject = session.createCriteria( ArrayDesign.class );
        BusinessKey.addADRestrictions( queryObject, arrayDesign );
        return queryObject;
    }

    public static Criteria createBAQueryObject( Session session, Specification<BioAssay> bioAssay ) {
        Criteria queryObject = session.createCriteria( BioAssay.class );
        BusinessKey.checkBAKey( bioAssay );
        BusinessKey.addBARestrictions( queryObject, bioAssay );
        return queryObject;
    }

    public static Criteria createBSQueryObject( Session session, Specification<BioSequence> bioSequence ) {
        Criteria queryObject = session.createCriteria( BioSequence.class );
        BusinessKey.addBSRestrictions( queryObject, bioSequence );
        return queryObject;
    }

    @SuppressWarnings("unused") // Possible external use
    public static Criteria createCQueryObject( Session session, Specification<Characteristic> ontologyEntry ) {
        Criteria queryObject = session.createCriteria( Characteristic.class );
        BusinessKey.checkCKey( ontologyEntry );
        BusinessKey.addCRestrictions( queryObject, ontologyEntry );
        return queryObject;
    }

    public static Criteria createUnitQueryObject( Session session, Specification<Unit> unit ) {
        Criteria queryObject = session.createCriteria( Unit.class );

        if ( unit.getEntity().getId() != null ) {
            queryObject.add( Restrictions.eq( "id", unit.getEntity().getId() ) );
        } else if ( unit.getEntity().getUnitNameCV() != null ) {
            queryObject.add( Restrictions.eq( "unitNameCV", unit.getEntity().getUnitNameCV() ) );
        }

        return queryObject;
    }

    private static void addNameRestriction( Criteria queryObject, Describable describable ) {
        if ( describable.getName() != null )
            queryObject.add( Restrictions.eq( "name", describable.getName() ) );
    }

    private static void addRestrictions( Criteria queryobject, ExternalDatabase assemblyDatabase ) {
        if ( assemblyDatabase.getId() != null ) {
            queryobject.add( Restrictions.eq( "id", assemblyDatabase.getId() ) );
            return;
        }

        if ( StringUtils.isNotBlank( assemblyDatabase.getName() ) ) {
            BusinessKey.addNameRestriction( queryobject, assemblyDatabase );
        }

    }

    private static void attachCriteria( Criteria queryObject, DatabaseEntry databaseEntry ) {

        queryObject.add( Restrictions.eq( "accession", databaseEntry.getAccession() ) )
                .createCriteria( "externalDatabase" )
                .add( Restrictions.eq( "name", databaseEntry.getExternalDatabase().getName() ) );

    }

    private static void attachCriteria( Criteria queryObject, ExternalDatabase assemblyDatabase ) {
        Criteria innerQuery = queryObject.createCriteria( "assemblyDatabase" );
        BusinessKey.addRestrictions( innerQuery, assemblyDatabase );
    }

    private static void attachCriteria( Criteria queryObject, Taxon taxon ) {
        if ( taxon == null )
            throw new IllegalArgumentException( "Taxon was null" );
        if ( taxon.getId() != null ) {
            queryObject.add( Restrictions.eq( "id", taxon.getId() ) );
        } else if ( taxon.getNcbiId() != null ) {
            Disjunction disjunction = Restrictions.disjunction();
            disjunction.add( Restrictions.eq( "ncbiId", taxon.getNcbiId() ) );
            disjunction.add( Restrictions.eq( "secondaryNcbiId", taxon.getNcbiId() ) );
            if ( taxon.getSecondaryNcbiId() != null ) {
                disjunction.add( Restrictions.eq( "ncbiId", taxon.getSecondaryNcbiId() ) );
            }
            queryObject.add( disjunction );

        } else if ( StringUtils.isNotBlank( taxon.getScientificName() ) ) {
            queryObject.add( Restrictions.eq( "scientificName", taxon.getScientificName() ) );
        } else if ( StringUtils.isNotBlank( taxon.getCommonName() ) ) {
            queryObject.add( Restrictions.eq( "commonName", taxon.getCommonName() ) );
        }
    }

    private static void attachCriteria( Criteria queryObject, Specification<Characteristic> ontologyEntry ) {
        Criteria innerQuery = queryObject.createCriteria( "ontologyEntry" );
        BusinessKey.addCRestrictions( innerQuery, ontologyEntry );
    }

    private static void checkBAKey( Specification<BioAssay> spec ) {
        BioAssay bioAssay = spec.getEntity();
        if ( bioAssay.getId() == null && bioAssay.getAccession() == null ) {
            throw new IllegalArgumentException( "Bioassay must have id or accession" );
        }

    }

    public static void checkEESSKey( Specification<ExpressionExperimentSubSet> spec ) {
        ExpressionExperimentSubSet entity = spec.getEntity();
        if ( entity.getBioAssays().isEmpty() ) {
            throw new IllegalArgumentException( "Subset must have bioassays" );
        }

        if ( entity.getSourceExperiment() == null || entity.getSourceExperiment().getId() == null ) {
            throw new IllegalArgumentException( "Subset must have persistent sourceExperiment" );
        }

        for ( BioAssay ba : entity.getBioAssays() ) {
            if ( ba.getId() == null ) {
                throw new IllegalArgumentException( "Subset must be made from persistent bioassays." );
            }
        }
    }

    public static void createEESSQueryObject( Criteria queryObject, Specification<ExpressionExperimentSubSet> spec ) {
        ExpressionExperimentSubSet entity = spec.getEntity();
        /*
         * Note that we don't match on name.
         */

        queryObject.add( Restrictions.eq( "sourceExperiment", entity.getSourceExperiment() ) );

        queryObject.add( Restrictions.sizeEq( "bioAssays", entity.getBioAssays().size() ) );

        queryObject.createCriteria( "bioAssays" )
                .add( Restrictions.in( "id", EntityUtils.getIds( entity.getBioAssays() ) ) );

    }
}
