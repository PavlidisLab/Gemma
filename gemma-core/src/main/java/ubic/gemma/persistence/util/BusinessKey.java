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
import org.hibernate.Session;
import org.springframework.util.Assert;
import ubic.gemma.model.association.Gene2GOAssociation;
import ubic.gemma.model.common.Describable;
import ubic.gemma.model.common.auditAndSecurity.Contact;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.measurement.Unit;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.AlternateName;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.Statement;
import ubic.gemma.model.genome.Chromosome;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.PhysicalLocation;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.gene.GeneProduct;

import org.springframework.lang.Nullable;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

/**
 * Methods to test business-key-related issues on objects. The 'checkValidKey' methods can be used to check whether an
 * object has the required business key values filled in. An exception is thrown if they don't.
 * <p>
 * Hibernate 6 port: the Criteria-based API has been replaced with JPA Criteria. Each entity type exposes a
 * {@code matches(...)} helper that contributes predicates to a query the caller already built (so the caller stays
 * in control of read-only/flush-mode hints), and a convenience {@code find(Session, X)} that wraps the whole thing.
 * The {@code checkKey} / {@code checkValidKey} methods are unchanged.
 *
 * @author pavlidis
 */
public class BusinessKey {

    private static final Log log = LogFactory.getLog( BusinessKey.class.getName() );

    // ===== ArrayDesign =====

    @SuppressWarnings("unused") // public utility API
    public static List<Predicate> matches( CriteriaBuilder cb, From<?, ArrayDesign> from, ArrayDesign arrayDesign ) {
        List<Predicate> preds = new ArrayList<>();
        if ( arrayDesign.getPrimaryTaxon() != null && arrayDesign.getPrimaryTaxon().getId() != null ) {
            preds.add( cb.equal( from.get( "primaryTaxon" ), arrayDesign.getPrimaryTaxon() ) );
        }

        if ( !arrayDesign.getExternalReferences().isEmpty() ) {
            Join<ArrayDesign, DatabaseEntry> ext = from.join( "externalReferences" );
            List<Predicate> ors = new ArrayList<>();
            for ( DatabaseEntry de : arrayDesign.getExternalReferences() ) {
                ors.add( cb.equal( ext.get( "accession" ), de.getAccession() ) );
            }
            preds.add( cb.or( ors.toArray( new Predicate[0] ) ) );
            // Note: original short-circuited here; we preserve that by skipping shortName/name fall-throughs
            return preds;
        } else if ( !arrayDesign.getAlternateNames().isEmpty() ) {
            Join<ArrayDesign, AlternateName> alt = from.join( "alternateNames" );
            List<Predicate> ors = new ArrayList<>();
            for ( AlternateName an : arrayDesign.getAlternateNames() ) {
                ors.add( cb.equal( alt.get( "name" ), an.getName() ) );
            }
            preds.add( cb.or( ors.toArray( new Predicate[0] ) ) );
            return preds;
        } else if ( arrayDesign.getShortName() != null ) {
            preds.add( cb.equal( from.get( "shortName" ), arrayDesign.getShortName() ) );
        } else {
            addNameRestriction( cb, from, arrayDesign, preds );
        }

        if ( arrayDesign.getDesignProvider() != null
                && StringUtils.isNotBlank( arrayDesign.getDesignProvider().getName() ) ) {
            preds.add( cb.equal( from.join( "designProvider" ).get( "name" ),
                    arrayDesign.getDesignProvider().getName() ) );
        }
        return preds;
    }

    public static ArrayDesign find( Session session, ArrayDesign arrayDesign ) {
        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaQuery<ArrayDesign> cq = cb.createQuery( ArrayDesign.class );
        Root<ArrayDesign> root = cq.from( ArrayDesign.class );
        List<Predicate> preds = matches( cb, root, arrayDesign );
        cq.select( root ).distinct( true );
        if ( !preds.isEmpty() ) {
            cq.where( preds.toArray( new Predicate[0] ) );
        }
        return session.createQuery( cq ).uniqueResult();
    }

    // ===== BioAssay =====

    @SuppressWarnings("unused")
    public static List<Predicate> matches( CriteriaBuilder cb, From<?, BioAssay> from, BioAssay bioAssay ) {
        List<Predicate> preds = new ArrayList<>();
        if ( bioAssay.getId() != null ) {
            preds.add( cb.equal( from.get( "id" ), bioAssay.getId() ) );
        } else if ( bioAssay.getAccession() != null ) {
            attachDatabaseEntry( cb, from.join( "accession" ), bioAssay.getAccession(), preds );
        }
        preds.add( cb.equal( from.get( "name" ), bioAssay.getName() ) );
        return preds;
    }

    public static BioAssay find( Session session, BioAssay bioAssay ) {
        checkKey( bioAssay );
        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaQuery<BioAssay> cq = cb.createQuery( BioAssay.class );
        Root<BioAssay> root = cq.from( BioAssay.class );
        cq.select( root ).where( matches( cb, root, bioAssay ).toArray( new Predicate[0] ) );
        return session.createQuery( cq ).uniqueResult();
    }

    // ===== BioMaterial =====

    public static List<Predicate> matches( CriteriaBuilder cb, From<?, BioMaterial> from, BioMaterial bioMaterial ) {
        List<Predicate> preds = new ArrayList<>();
        if ( bioMaterial.getName() != null ) {
            preds.add( cb.equal( from.get( "name" ), bioMaterial.getName() ) );
        }
        if ( bioMaterial.getExternalAccession() != null ) {
            preds.add( cb.equal( from.join( "externalAccession" ).get( "accession" ),
                    bioMaterial.getExternalAccession().getAccession() ) );
        } else if ( StringUtils.isNotBlank( bioMaterial.getDescription() ) ) {
            preds.add( cb.equal( from.get( "description" ), bioMaterial.getDescription() ) );
        }
        if ( bioMaterial.getSourceTaxon() != null ) {
            preds.add( cb.equal( from.get( "sourceTaxon" ), bioMaterial.getSourceTaxon() ) );
        }
        return preds;
    }

    public static BioMaterial find( Session session, BioMaterial bioMaterial ) {
        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaQuery<BioMaterial> cq = cb.createQuery( BioMaterial.class );
        Root<BioMaterial> root = cq.from( BioMaterial.class );
        List<Predicate> preds = matches( cb, root, bioMaterial );
        cq.select( root );
        if ( !preds.isEmpty() ) {
            cq.where( preds.toArray( new Predicate[0] ) );
        }
        return session.createQuery( cq ).uniqueResult();
    }

    // ===== BioSequence =====

    @SuppressWarnings("unused")
    public static List<Predicate> matches( CriteriaBuilder cb, From<?, BioSequence> from, BioSequence bioSequence ) {
        List<Predicate> preds = new ArrayList<>();
        if ( bioSequence.getId() != null ) {
            preds.add( cb.equal( from.get( "id" ), bioSequence.getId() ) );
            return preds;
        }
        if ( StringUtils.isNotBlank( bioSequence.getName() ) ) {
            addNameRestriction( cb, from, bioSequence, preds );
        }
        attachTaxon( cb, from.join( "taxon" ), bioSequence.getTaxon(), preds );
        return preds;
    }

    public static BioSequence find( Session session, BioSequence bioSequence ) {
        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaQuery<BioSequence> cq = cb.createQuery( BioSequence.class );
        Root<BioSequence> root = cq.from( BioSequence.class );
        List<Predicate> preds = matches( cb, root, bioSequence );
        cq.select( root );
        if ( !preds.isEmpty() ) {
            cq.where( preds.toArray( new Predicate[0] ) );
        }
        return session.createQuery( cq ).uniqueResult();
    }

    /**
     * Restricts a parent query so that property {@code propertyName} matches a BioSequence by business key.
     * Used by e.g. BlatResult / BlatAssociation finders.
     */
    public static void attachBioSequence( CriteriaBuilder cb, From<?, ?> parent, String propertyName,
            BioSequence bioSequence, List<Predicate> preds ) {
        Join<?, BioSequence> j = parent.join( propertyName );
        preds.addAll( matches( cb, j, bioSequence ) );
    }

    // ===== Chromosome =====

    @SuppressWarnings("unused")
    public static List<Predicate> matches( CriteriaBuilder cb, From<?, Chromosome> from, Chromosome chromosome ) {
        List<Predicate> preds = new ArrayList<>();
        preds.add( cb.equal( from.get( "name" ), chromosome.getName() ) );
        attachTaxon( cb, from.join( "taxon" ), chromosome.getTaxon(), preds );
        if ( chromosome.getAssemblyDatabase() != null ) {
            attachExternalDatabase( cb, from.join( "assemblyDatabase" ), chromosome.getAssemblyDatabase(), preds );
        }
        if ( chromosome.getSequence() != null ) {
            attachBioSequence( cb, from, "sequence", chromosome.getSequence(), preds );
        }
        return preds;
    }

    /**
     * Find an existing {@link Chromosome} by its business key (name + taxon, optionally tightened by
     * assemblyDatabase + sequence when present on the input).
     *
     * @throws IllegalArgumentException if the chromosome lacks a name or a taxon
     * @throws org.hibernate.NonUniqueResultException if more than one row matches the key
     */
    public static Chromosome find( Session session, Chromosome chromosome ) {
        checkValidKey( chromosome );
        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaQuery<Chromosome> cq = cb.createQuery( Chromosome.class );
        Root<Chromosome> root = cq.from( Chromosome.class );
        List<Predicate> preds = matches( cb, root, chromosome );
        cq.select( root );
        if ( !preds.isEmpty() ) {
            cq.where( preds.toArray( new Predicate[0] ) );
        }
        return session.createQuery( cq ).uniqueResult();
    }

    // ===== Contact =====

    public static List<Predicate> matches( CriteriaBuilder cb, From<?, ? extends Contact> from, Contact contact ) {
        List<Predicate> preds = new ArrayList<>();
        if ( contact instanceof User ) {
            preds.add( cb.equal( from.get( "userName" ), ( (User) contact ).getUserName() ) );
            return preds;
        }
        if ( StringUtils.isNotBlank( contact.getEmail() ) ) {
            preds.add( cb.equal( from.get( "email" ), contact.getEmail() ) );
        }
        if ( StringUtils.isNotBlank( contact.getName() ) ) {
            preds.add( cb.equal( from.get( "name" ), contact.getName() ) );
        }
        return preds;
    }

    public static Contact find( Session session, Contact contact ) {
        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaQuery<Contact> cq = cb.createQuery( Contact.class );
        Root<Contact> root = cq.from( Contact.class );
        List<Predicate> preds = matches( cb, root, contact );
        cq.select( root );
        if ( !preds.isEmpty() ) {
            cq.where( preds.toArray( new Predicate[0] ) );
        }
        return session.createQuery( cq ).uniqueResult();
    }

    // ===== ExperimentalFactor =====

    public static List<Predicate> matches( CriteriaBuilder cb, From<?, ExperimentalFactor> from,
            ExperimentalFactor experimentalFactor ) {
        List<Predicate> preds = new ArrayList<>();
        if ( experimentalFactor.getId() != null ) {
            preds.add( cb.equal( from.get( "id" ), experimentalFactor.getId() ) );
            return preds;
        }
        if ( StringUtils.isNotBlank( experimentalFactor.getName() ) ) {
            preds.add( cb.equal( from.get( "name" ), experimentalFactor.getName() ) );
        }
        if ( experimentalFactor.getCategory() != null ) {
            Join<ExperimentalFactor, Characteristic> j = from.join( "category" );
            addCharacteristicRestrictions( cb, j, experimentalFactor.getCategory(), preds );
        }
        return preds;
    }

    public static ExperimentalFactor find( Session session, ExperimentalFactor experimentalFactor ) {
        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaQuery<ExperimentalFactor> cq = cb.createQuery( ExperimentalFactor.class );
        Root<ExperimentalFactor> root = cq.from( ExperimentalFactor.class );
        List<Predicate> preds = matches( cb, root, experimentalFactor );
        cq.select( root );
        if ( !preds.isEmpty() ) {
            cq.where( preds.toArray( new Predicate[0] ) );
        }
        return session.createQuery( cq ).uniqueResult();
    }

    // ===== FactorValue =====

    public static FactorValue find( Session session, FactorValue factorValue ) {
        checkKey( factorValue );
        ExperimentalFactor ef = factorValue.getExperimentalFactor();
        if ( ef == null ) {
            throw new IllegalArgumentException( "Cannot find a factor value lacking an experimental factor." );
        }
        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaQuery<FactorValue> cq = cb.createQuery( FactorValue.class );
        Root<FactorValue> root = cq.from( FactorValue.class );
        List<Predicate> preds = new ArrayList<>();

        Join<FactorValue, ExperimentalFactor> efJoin = root.join( "experimentalFactor" );
        preds.addAll( matches( cb, efJoin, ef ) );

        if ( factorValue.getMeasurement() != null ) {
            preds.add( cb.equal( root.get( "measurement" ), factorValue.getMeasurement() ) );
        } else if ( !factorValue.getCharacteristics().isEmpty() ) {
            preds.add( cb.equal( cb.size( root.get( "characteristics" ) ),
                    factorValue.getCharacteristics().size() ) );
            Join<FactorValue, Statement> stJoin = root.join( "characteristics" );
            List<Predicate> ors = new ArrayList<>();
            for ( Statement st : factorValue.getCharacteristics() ) {
                ors.add( cb.and( buildStatementPredicates( cb, stJoin, st ).toArray( new Predicate[0] ) ) );
            }
            preds.add( cb.or( ors.toArray( new Predicate[0] ) ) );
        } else if ( factorValue.getValue() != null ) {
            preds.add( cb.equal( root.get( "value" ), factorValue.getValue() ) );
        } else {
            throw new IllegalArgumentException( "No suitable fields defined to find a matching FactorValue." );
        }

        cq.select( root ).distinct( true ).where( preds.toArray( new Predicate[0] ) );
        return session.createQuery( cq ).uniqueResult();
    }

    // ===== Gene =====

    public static List<Predicate> matches( CriteriaBuilder cb, From<?, Gene> from, Gene gene, boolean stricter ) {
        List<Predicate> preds = new ArrayList<>();
        if ( gene.getId() != null ) {
            preds.add( cb.equal( from.get( "id" ), gene.getId() ) );
        } else if ( gene.getNcbiGeneId() != null ) {
            if ( StringUtils.isNotBlank( gene.getPreviousNcbiGeneId() ) ) {
                Collection<Integer> ncbiIds = new HashSet<>();
                ncbiIds.add( gene.getNcbiGeneId() );
                for ( String previousId : StringUtils.split( gene.getPreviousNcbiGeneId(), "," ) ) {
                    try {
                        ncbiIds.add( Integer.parseInt( previousId ) );
                    } catch ( NumberFormatException e ) {
                        log.warn( "Previous Ncbi id wasn't parseable to an int: " + previousId );
                    }
                }
                preds.add( from.get( "ncbiGeneId" ).in( ncbiIds ) );
            } else {
                preds.add( cb.equal( from.get( "ncbiGeneId" ), gene.getNcbiGeneId() ) );
            }
        } else if ( StringUtils.isNotBlank( gene.getOfficialSymbol() ) ) {
            preds.add( cb.equal( from.get( "officialSymbol" ), gene.getOfficialSymbol() ) );
            attachTaxon( cb, from.join( "taxon" ), gene.getTaxon(), preds );
            if ( stricter ) {
                if ( StringUtils.isNotBlank( gene.getOfficialName() ) ) {
                    preds.add( cb.equal( from.get( "officialName" ), gene.getOfficialName() ) );
                }
                if ( gene.getPhysicalLocation() != null ) {
                    attachPhysicalLocation( cb, from.join( "physicalLocation" ), gene.getPhysicalLocation(), preds );
                }
            }
        } else {
            throw new IllegalArgumentException( "No valid key " + gene );
        }
        return preds;
    }

    public static Gene find( Session session, Gene gene ) {
        checkKey( gene );
        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaQuery<Gene> cq = cb.createQuery( Gene.class );
        Root<Gene> root = cq.from( Gene.class );
        List<Predicate> preds;
        if ( gene.getId() != null ) {
            preds = new ArrayList<>();
            preds.add( cb.equal( root.get( "id" ), gene.getId() ) );
        } else {
            preds = matches( cb, root, gene, true );
        }
        cq.select( root );
        if ( !preds.isEmpty() ) {
            cq.where( preds.toArray( new Predicate[0] ) );
        }
        return session.createQuery( cq ).uniqueResult();
    }

    // ===== Gene2GOAssociation =====

    public static List<Predicate> matches( CriteriaBuilder cb, From<?, Gene2GOAssociation> from,
            Gene2GOAssociation g2g ) {
        List<Predicate> preds = new ArrayList<>();
        Join<Gene2GOAssociation, Gene> geneJoin = from.join( "gene" );
        preds.addAll( matches( cb, geneJoin, g2g.getGene(), true ) );
        Join<Gene2GOAssociation, Characteristic> ontJoin = from.join( "ontologyEntry" );
        addCharacteristicRestrictions( cb, ontJoin, g2g.getOntologyEntry(), preds );
        return preds;
    }

    public static Gene2GOAssociation find( Session session, Gene2GOAssociation g2g ) {
        checkValidKey( g2g );
        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaQuery<Gene2GOAssociation> cq = cb.createQuery( Gene2GOAssociation.class );
        Root<Gene2GOAssociation> root = cq.from( Gene2GOAssociation.class );
        cq.select( root ).where( matches( cb, root, g2g ).toArray( new Predicate[0] ) );
        return session.createQuery( cq ).uniqueResult();
    }

    // ===== GeneProduct =====

    public static GeneProduct find( Session session, GeneProduct geneProduct ) {
        checkValidKey( geneProduct );
        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaQuery<GeneProduct> cq = cb.createQuery( GeneProduct.class );
        Root<GeneProduct> root = cq.from( GeneProduct.class );
        List<Predicate> preds = new ArrayList<>();

        if ( geneProduct.getId() != null ) {
            preds.add( cb.equal( root.get( "id" ), geneProduct.getId() ) );
        } else if ( StringUtils.isNotBlank( geneProduct.getNcbiGi() ) ) {
            preds.add( cb.equal( root.get( "ncbiGi" ), geneProduct.getNcbiGi() ) );
        } else if ( StringUtils.isNotBlank( geneProduct.getName() ) ) {
            preds.add( cb.equal( root.get( "name" ), geneProduct.getName() ) );
            if ( geneProduct.getGene() != null ) {
                Join<GeneProduct, Gene> geneJoin = root.join( "gene" );
                preds.addAll( matches( cb, geneJoin, geneProduct.getGene(), false ) );
            }
        }

        cq.select( root ).distinct( true );
        if ( !preds.isEmpty() ) {
            cq.where( preds.toArray( new Predicate[0] ) );
        }
        return session.createQuery( cq ).uniqueResult();
    }

    // ===== Taxon =====

    public static List<Predicate> matches( CriteriaBuilder cb, From<?, Taxon> from, Taxon taxon ) {
        checkValidKey( taxon );
        List<Predicate> preds = new ArrayList<>();
        attachTaxon( cb, from, taxon, preds );
        return preds;
    }

    public static Taxon find( Session session, Taxon taxon ) {
        checkValidKey( taxon );
        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaQuery<Taxon> cq = cb.createQuery( Taxon.class );
        Root<Taxon> root = cq.from( Taxon.class );
        List<Predicate> preds = new ArrayList<>();
        attachTaxon( cb, root, taxon, preds );
        cq.select( root );
        if ( !preds.isEmpty() ) {
            cq.where( preds.toArray( new Predicate[0] ) );
        }
        return session.createQuery( cq ).setReadOnly( true ).uniqueResult();
    }

    // ===== Characteristic =====

    public static List<Predicate> matches( CriteriaBuilder cb, From<?, ? extends Characteristic> from,
            Characteristic ontologyEntry ) {
        List<Predicate> preds = new ArrayList<>();
        addCharacteristicRestrictions( cb, from, ontologyEntry, preds );
        return preds;
    }

    @SuppressWarnings("unused")
    public static Characteristic find( Session session, Characteristic ontologyEntry ) {
        checkKey( ontologyEntry );
        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaQuery<Characteristic> cq = cb.createQuery( Characteristic.class );
        Root<Characteristic> root = cq.from( Characteristic.class );
        List<Predicate> preds = new ArrayList<>();
        addCharacteristicRestrictions( cb, root, ontologyEntry, preds );
        cq.select( root );
        if ( !preds.isEmpty() ) {
            cq.where( preds.toArray( new Predicate[0] ) );
        }
        return session.createQuery( cq ).uniqueResult();
    }

    // ===== Unit =====

    public static Unit find( Session session, Unit unit ) {
        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaQuery<Unit> cq = cb.createQuery( Unit.class );
        Root<Unit> root = cq.from( Unit.class );
        List<Predicate> preds = new ArrayList<>();
        if ( unit.getId() != null ) {
            preds.add( cb.equal( root.get( "id" ), unit.getId() ) );
        } else if ( unit.getUnitNameCV() != null ) {
            preds.add( cb.equal( root.get( "unitNameCV" ), unit.getUnitNameCV() ) );
        }
        cq.select( root );
        if ( !preds.isEmpty() ) {
            cq.where( preds.toArray( new Predicate[0] ) );
        }
        return session.createQuery( cq ).uniqueResult();
    }

    // ===== ExpressionExperimentSubSet =====

    public static ExpressionExperimentSubSet find( Session session, ExpressionExperimentSubSet entity ) {
        checkKey( entity );
        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaQuery<ExpressionExperimentSubSet> cq = cb.createQuery( ExpressionExperimentSubSet.class );
        Root<ExpressionExperimentSubSet> root = cq.from( ExpressionExperimentSubSet.class );
        List<Predicate> preds = new ArrayList<>();
        preds.add( cb.equal( root.get( "sourceExperiment" ), entity.getSourceExperiment() ) );
        preds.add( cb.equal( cb.size( root.get( "bioAssays" ) ), entity.getBioAssays().size() ) );
        Join<ExpressionExperimentSubSet, BioAssay> baJoin = root.join( "bioAssays" );
        preds.add( baJoin.get( "id" ).in( IdentifiableUtils.getIds( entity.getBioAssays() ) ) );
        cq.select( root ).distinct( true ).where( preds.toArray( new Predicate[0] ) );
        return session.createQuery( cq ).uniqueResult();
    }

    // ===== QuantitationType =====
    //
    // The business key matches QuantitationType#equals: name + generalType + type + scale + representation
    // + the five boolean flags (isBackground, isBackgroundSubtracted, isRatio, isNormalized, isBatchCorrected)
    // + isRecomputedFromRawData. This mirrors QuantitationTypeDaoImpl#qtMatchClause.
    //
    // Note: in practice the persister uses create() rather than findOrCreate() for QT, because QTs are
    // experiment-scoped and intentionally not shared. The find(...) here is still useful for callers
    // that need to dedupe within a single experiment.

    public static List<Predicate> matches( CriteriaBuilder cb, From<?, QuantitationType> from, QuantitationType qt ) {
        List<Predicate> preds = new ArrayList<>();
        preds.add( cb.equal( from.get( "name" ), qt.getName() ) );
        preds.add( cb.equal( from.get( "generalType" ), qt.getGeneralType() ) );
        preds.add( cb.equal( from.get( "type" ), qt.getType() ) );
        preds.add( cb.equal( from.get( "scale" ), qt.getScale() ) );
        preds.add( cb.equal( from.get( "representation" ), qt.getRepresentation() ) );
        preds.add( cb.equal( from.get( "isBackground" ), qt.getIsBackground() ) );
        preds.add( cb.equal( from.get( "isBackgroundSubtracted" ), qt.getIsBackgroundSubtracted() ) );
        preds.add( cb.equal( from.get( "isRatio" ), qt.getIsRatio() ) );
        preds.add( cb.equal( from.get( "isNormalized" ), qt.getIsNormalized() ) );
        preds.add( cb.equal( from.get( "isBatchCorrected" ), qt.getIsBatchCorrected() ) );
        preds.add( cb.equal( from.get( "isRecomputedFromRawData" ), qt.getIsRecomputedFromRawData() ) );
        return preds;
    }

    /**
     * Find an existing {@link QuantitationType} whose full set of identifying fields equals the input's.
     *
     * @throws IllegalArgumentException if the QT lacks a name
     * @throws org.hibernate.NonUniqueResultException if more than one row matches the key
     */
    public static QuantitationType find( Session session, QuantitationType qt ) {
        checkKey( qt );
        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaQuery<QuantitationType> cq = cb.createQuery( QuantitationType.class );
        Root<QuantitationType> root = cq.from( QuantitationType.class );
        cq.select( root ).where( matches( cb, root, qt ).toArray( new Predicate[0] ) );
        return session.createQuery( cq ).uniqueResult();
    }

    // ===== BioAssayDimension =====
    //
    // The natural key is the (ordered) list of BioAssays. Candidates are pre-filtered by size and
    // "shares at least one BioAssay id with the input" (mirroring BioAssayDimensionDaoImpl#find), then
    // the caller's BioAssayDimension.equals(...) (which compares the bioAssays lists) is applied in
    // Java to enforce identity-and-order. All BioAssays must be persistent (have an id).

    public static BioAssayDimension find( Session session, BioAssayDimension bioAssayDimension ) {
        checkKey( bioAssayDimension );

        Collection<Long> bioAssayIds = new HashSet<>();
        for ( BioAssay ba : bioAssayDimension.getBioAssays() ) {
            bioAssayIds.add( ba.getId() );
        }

        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaQuery<BioAssayDimension> cq = cb.createQuery( BioAssayDimension.class );
        Root<BioAssayDimension> root = cq.from( BioAssayDimension.class );
        List<Predicate> preds = new ArrayList<>();
        preds.add( cb.equal( cb.size( root.get( "bioAssays" ) ), bioAssayDimension.getBioAssays().size() ) );
        if ( !bioAssayIds.isEmpty() ) {
            Join<BioAssayDimension, BioAssay> baJoin = root.join( "bioAssays" );
            preds.add( baJoin.get( "id" ).in( bioAssayIds ) );
        }
        cq.select( root ).distinct( true ).where( preds.toArray( new Predicate[0] ) );

        List<BioAssayDimension> candidates = session.createQuery( cq ).list();
        for ( BioAssayDimension candidate : candidates ) {
            // BioAssayDimension.equals checks list-equality of bioAssays (preserving order).
            if ( candidate.getBioAssays().equals( bioAssayDimension.getBioAssays() ) ) {
                return candidate;
            }
        }
        return null;
    }

    public static void checkKey( BioAssayDimension bioAssayDimension ) {
        if ( bioAssayDimension == null || bioAssayDimension.getBioAssays() == null
                || bioAssayDimension.getBioAssays().isEmpty() ) {
            throw new IllegalArgumentException( "BioAssayDimension must have at least one BioAssay." );
        }
        for ( BioAssay ba : bioAssayDimension.getBioAssays() ) {
            if ( ba == null || ba.getId() == null ) {
                throw new IllegalArgumentException(
                        "BioAssayDimension's BioAssays must all be persistent (have an id) to be findable." );
            }
        }
    }

    public static void checkKey( QuantitationType quantitationType ) {
        if ( quantitationType == null || StringUtils.isBlank( quantitationType.getName() ) ) {
            throw new IllegalArgumentException( "QuantitationType must have a name." );
        }
    }

    // ===== DatabaseEntry =====

    /**
     * Restrict a parent query so a DatabaseEntry-typed property at {@code propertyName} matches by business key.
     * Used by callers that build a query on a different entity referencing the DatabaseEntry.
     */
    public static void attachDatabaseEntry( CriteriaBuilder cb, From<?, ?> parent, String propertyName,
            DatabaseEntry databaseEntry, List<Predicate> preds ) {
        Join<?, DatabaseEntry> j = parent.join( propertyName );
        attachDatabaseEntry( cb, j, databaseEntry, preds );
    }

    private static void attachDatabaseEntry( CriteriaBuilder cb, From<?, DatabaseEntry> from,
            DatabaseEntry databaseEntry, List<Predicate> preds ) {
        preds.add( cb.equal( from.get( "accession" ), databaseEntry.getAccession() ) );
        preds.add( cb.equal( from.join( "externalDatabase" ).get( "name" ),
                databaseEntry.getExternalDatabase().getName() ) );
    }

    // ===== checkKey / checkValidKey (unchanged) =====

    public static void checkKey( BibliographicReference bibliographicReference ) {
        if ( bibliographicReference == null || bibliographicReference.getPubAccession() == null
                || bibliographicReference.getPubAccession().getAccession() == null ) {
            throw new IllegalArgumentException(
                    "BibliographicReference was null or had no accession : " + bibliographicReference );
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static void checkKey( Characteristic ontologyEntry ) {
        if ( ontologyEntry.getValue() == null )
            throw new IllegalArgumentException();
    }

    public static void checkKey( Contact contact ) {
        if ( contact == null || ( StringUtils.isBlank( contact.getName() ) && StringUtils
                .isBlank( contact.getEmail() ) ) ) {
            throw new IllegalArgumentException( "Contact must have at least some information filled in!" );
        }
    }

    public static void checkKey( DatabaseEntry accession ) {
        if ( accession.getId() != null )
            return;
        if ( StringUtils.isBlank( accession.getAccession() ) ) {
            throw new IllegalArgumentException( accession + " did not have an accession" );
        }
        checkKey( accession.getExternalDatabase() );
    }

    public static void checkKey( DesignElementDataVector designElementDataVector ) {
        if ( designElementDataVector == null || designElementDataVector.getDesignElement() == null
                || designElementDataVector.getExpressionExperiment() == null ) {
            throw new IllegalArgumentException(
                    "DesignElementDataVector did not have complete business key " + designElementDataVector );
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static void checkKey( ExternalDatabase externalDatabase ) {
        if ( externalDatabase.getId() != null )
            return;
        if ( StringUtils.isBlank( externalDatabase.getName() ) ) {
            throw new IllegalArgumentException( externalDatabase + " did not have a name" );
        }
    }

    public static void checkKey( FactorValue factorValue ) {
        if ( factorValue.getMeasurement() == null && factorValue.getCharacteristics().isEmpty()
                && factorValue.getValue() == null ) {
            throw new IllegalArgumentException(
                    "FactorValue must have a value (or associated measurement or characteristics)." );
        }
    }

    public static void checkKey( Gene gene ) {
        if ( gene == null )
            throw new IllegalArgumentException( "Gene cannot be null" );
        if ( ( ( gene.getOfficialSymbol() == null || gene.getTaxon() == null ) && gene.getPhysicalLocation() == null
                && ( gene.getProducts() == null || gene.getProducts().isEmpty() ) ) && gene.getNcbiGeneId() == null ) {
            throw new IllegalArgumentException( "No valid key for " + gene
                    + ": Gene must have official symbol and name with taxon + physical location or gene products, or ncbiId" );
        }
    }

    public static void checkKey( User user ) {
        if ( user == null || StringUtils.isBlank( user.getUserName() ) ) {
            throw new IllegalArgumentException( "User was null or had no userName defined" );
        }
    }

    private static void checkKey( BioAssay bioAssay ) {
        if ( bioAssay.getId() == null && bioAssay.getAccession() == null ) {
            throw new IllegalArgumentException( "Bioassay must have id or accession" );
        }
    }

    public static void checkKey( ExpressionExperimentSubSet entity ) {
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

    public static void checkValidKey( ArrayDesign arrayDesign ) {
        if ( arrayDesign == null || ( StringUtils.isBlank( arrayDesign.getName() ) && StringUtils
                .isBlank( arrayDesign.getShortName() ) && arrayDesign.getExternalReferences().isEmpty() ) ) {
            throw new IllegalArgumentException( arrayDesign + " did not have a valid key" );
        }
    }

    public static void checkValidKey( BioSequence bioSequence ) {
        if ( bioSequence == null || bioSequence.getTaxon() == null || StringUtils.isBlank( bioSequence.getName() ) ) {
            throw new IllegalArgumentException( bioSequence + " did not have a valid key" );
        }
    }

    @SuppressWarnings({ "unused", "WeakerAccess" })
    public static void checkValidKey( Chromosome chromosome ) {
        if ( StringUtils.isBlank( chromosome.getName() ) ) {
            throw new IllegalArgumentException( "Chromosome did not have a valid key" );
        }
        checkValidKey( chromosome.getTaxon() );
    }

    public static void checkValidKey( DatabaseEntry databaseEntry ) {
        if ( databaseEntry == null || databaseEntry.getAccession() == null
                || databaseEntry.getExternalDatabase() == null ) {
            throw new IllegalArgumentException( "DatabaseEntry does not have valid key" );
        }
    }

    public static void checkValidKey( ExperimentalFactor experimentalFactor ) {
        if ( StringUtils.isBlank( experimentalFactor.getName() ) && experimentalFactor.getCategory() == null ) {
            throw new IllegalArgumentException( "Experimental factor must have name or category" );
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static void checkValidKey( Gene gene ) {
        if ( gene == null || ( gene.getNcbiGeneId() == null && ( StringUtils.isBlank( gene.getOfficialSymbol() )
                || gene.getTaxon() == null || StringUtils.isBlank( gene.getOfficialName() ) ) ) ) {
            throw new IllegalArgumentException(
                    "Gene does not have valid key (needs NCBI numeric id or Official Symbol + Official Name + Taxon" );
        }
    }

    public static void checkValidKey( Gene2GOAssociation gene2GOAssociation ) {
        checkValidKey( gene2GOAssociation.getGene() );
    }

    public static void checkValidKey( GeneProduct geneProduct ) {
        if ( geneProduct.getId() != null )
            return;

        boolean ok = StringUtils.isNotBlank( geneProduct.getNcbiGi() )
                || StringUtils.isNotBlank( geneProduct.getName() );

        if ( !ok ) {
            throw new IllegalArgumentException( "GeneProduct did not have a valid key - requires name or NCBI GI" );
        }

        if ( geneProduct.getGene() != null ) {
            checkKey( geneProduct.getGene() );
        }
    }

    public static void checkValidKey( Taxon taxon ) {
        if ( taxon == null || ( taxon.getNcbiId() == null && StringUtils.isBlank( taxon.getCommonName() ) && StringUtils
                .isBlank( taxon.getScientificName() ) ) ) {
            throw new IllegalArgumentException( "Taxon " + taxon + " did not have a valid key" );
        }
    }

    public static void checkValidKey( Unit unit ) {
        if ( unit == null || StringUtils.isBlank( unit.getUnitNameCV() ) ) {
            throw new IllegalArgumentException( unit + " did not have a valid key" );
        }
    }

    // ===== private helpers =====

    private static <X extends Describable> void addNameRestriction( CriteriaBuilder cb, From<?, X> from,
            X describable, List<Predicate> preds ) {
        if ( describable.getName() != null ) {
            preds.add( cb.equal( from.get( "name" ), describable.getName() ) );
        }
    }

    private static void attachExternalDatabase( CriteriaBuilder cb, From<?, ExternalDatabase> from,
            ExternalDatabase assemblyDatabase, List<Predicate> preds ) {
        if ( assemblyDatabase.getId() != null ) {
            preds.add( cb.equal( from.get( "id" ), assemblyDatabase.getId() ) );
            return;
        }
        if ( StringUtils.isNotBlank( assemblyDatabase.getName() ) ) {
            preds.add( cb.equal( from.get( "name" ), assemblyDatabase.getName() ) );
        }
    }

    private static void attachTaxon( CriteriaBuilder cb, From<?, Taxon> from, Taxon taxon, List<Predicate> preds ) {
        if ( taxon == null )
            throw new IllegalArgumentException( "Taxon was null" );
        if ( taxon.getId() != null ) {
            preds.add( cb.equal( from.get( "id" ), taxon.getId() ) );
        } else if ( taxon.getNcbiId() != null ) {
            List<Predicate> ors = new ArrayList<>();
            ors.add( cb.equal( from.get( "ncbiId" ), taxon.getNcbiId() ) );
            ors.add( cb.equal( from.get( "secondaryNcbiId" ), taxon.getNcbiId() ) );
            if ( taxon.getSecondaryNcbiId() != null ) {
                ors.add( cb.equal( from.get( "ncbiId" ), taxon.getSecondaryNcbiId() ) );
            }
            preds.add( cb.or( ors.toArray( new Predicate[0] ) ) );
        } else if ( StringUtils.isNotBlank( taxon.getScientificName() ) ) {
            preds.add( cb.equal( from.get( "scientificName" ), taxon.getScientificName() ) );
        } else if ( StringUtils.isNotBlank( taxon.getCommonName() ) ) {
            preds.add( cb.equal( from.get( "commonName" ), taxon.getCommonName() ) );
        }
    }

    private static void attachPhysicalLocation( CriteriaBuilder cb, From<?, PhysicalLocation> nested,
            PhysicalLocation physicalLocation, List<Predicate> preds ) {
        if ( physicalLocation.getChromosome() == null ) {
            throw new IllegalArgumentException();
        }
        Join<PhysicalLocation, Chromosome> chrJoin = nested.join( "chromosome" );
        if ( physicalLocation.getChromosome().getId() != null ) {
            preds.add( cb.equal( chrJoin.get( "id" ), physicalLocation.getChromosome().getId() ) );
        } else {
            preds.add( cb.equal( chrJoin.get( "name" ), physicalLocation.getChromosome().getName() ) );
        }
        if ( physicalLocation.getNucleotide() != null ) {
            preds.add( cb.equal( nested.get( "nucleotide" ), physicalLocation.getNucleotide() ) );
        }
        if ( physicalLocation.getNucleotideLength() != null ) {
            preds.add( cb.equal( nested.get( "nucleotideLength" ), physicalLocation.getNucleotideLength() ) );
        }
    }

    private static void addCharacteristicRestrictions( CriteriaBuilder cb, From<?, ? extends Characteristic> from,
            Characteristic characteristic, List<Predicate> preds ) {
        if ( characteristic.getCategory() != null ) {
            addOntologyTermRestrictions( cb, from, characteristic.getCategoryUri(),
                    characteristic.getCategory(), "category", preds );
        }
        addOntologyTermRestrictions( cb, from, characteristic.getValueUri(),
                characteristic.getValue(), "value", preds );
    }

    /**
     * @see ubic.gemma.model.common.description.CharacteristicUtils#equals(String, String, String, String)
     */
    private static void addOntologyTermRestrictions( CriteriaBuilder cb, From<?, ?> from, @Nullable String uri,
            String value, String propertyNamePrefix, List<Predicate> preds ) {
        Assert.notNull( value, String.format( "An ontology term (for properties %s and %sUri) must at least have a value.",
                propertyNamePrefix, propertyNamePrefix ) );
        if ( uri != null ) {
            preds.add( cb.equal( from.get( propertyNamePrefix + "Uri" ), uri ) );
        } else {
            preds.add( cb.equal( from.get( propertyNamePrefix ), value ) );
        }
    }

    private static List<Predicate> buildStatementPredicates( CriteriaBuilder cb,
            From<?, ? extends Characteristic> from, Statement statement ) {
        List<Predicate> preds = new ArrayList<>();
        addCharacteristicRestrictions( cb, from, statement, preds );
        if ( statement.getPredicate() != null ) {
            addOntologyTermRestrictions( cb, from, statement.getPredicateUri(), statement.getPredicate(),
                    "predicate", preds );
            if ( statement.getObject() != null ) {
                addOntologyTermRestrictions( cb, from, statement.getObjectUri(), statement.getObject(),
                        "object", preds );
            }
        }
        if ( statement.getSecondPredicate() != null ) {
            addOntologyTermRestrictions( cb, from, statement.getSecondPredicateUri(), statement.getSecondPredicate(),
                    "secondPredicate", preds );
            if ( statement.getSecondObject() != null ) {
                addOntologyTermRestrictions( cb, from, statement.getSecondObjectUri(), statement.getSecondObject(),
                        "secondObject", preds );
            }
        }
        return preds;
    }
}
