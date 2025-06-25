package ubic.gemma.cli.util;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import ubic.gemma.core.util.ShellUtils;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.common.Describable;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.protocol.Protocol;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.*;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.persistence.service.common.protocol.ProtocolService;
import ubic.gemma.persistence.service.common.quantitationtype.NonUniqueQuantitationTypeByNameException;
import ubic.gemma.persistence.service.common.quantitationtype.QuantitationTypeService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.SingleCellExpressionExperimentService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

@CommonsLog
@Component
public class EntityLocatorImpl implements EntityLocator {

    @Autowired
    private ExpressionExperimentService eeService;
    @Autowired
    private TaxonService taxonService;
    @Autowired
    private ArrayDesignService arrayDesignService;
    @Autowired
    private ProtocolService protocolService;
    @Autowired
    private QuantitationTypeService quantitationTypeService;
    @Autowired
    private SingleCellExpressionExperimentService singleCellExpressionExperimentService;
    @Autowired
    private DifferentialExpressionAnalysisService differentialExpressionAnalysisService;

    @Override
    public Taxon locateTaxon( String identifier ) {
        Assert.isTrue( StringUtils.isNotBlank( identifier ), "Taxon identifier must not be blank." );
        identifier = StringUtils.strip( identifier );
        Taxon taxon;
        try {
            long id = Long.parseLong( identifier );
            if ( ( taxon = taxonService.load( id ) ) != null ) {
                log.info( "Found " + taxon + " by ID" );
                return taxon;
            }
            if ( ( taxon = taxonService.findByNcbiId( Math.toIntExact( id ) ) ) != null ) {
                log.info( "Found " + taxon + " by NCBI ID" );
                return taxon;
            }
            throw new NullPointerException( "No taxon with ID or NCBI ID " + id );
        } catch ( NumberFormatException e ) {
            // ignore
        }
        if ( ( taxon = taxonService.findByCommonName( identifier ) ) != null ) {
            log.info( "Found " + taxon + " by common name." );
            return taxon;
        }
        if ( ( taxon = taxonService.findByScientificName( identifier ) ) != null ) {
            log.info( "Found " + taxon + " by scientific name." );
            return taxon;
        }
        throw new NullPointerException( "Cannot find taxon with name " + identifier + "." + formatPossibleValues( taxonService.loadAll(), true ) );
    }

    @Override
    public ArrayDesign locateArrayDesign( String identifier ) {
        Assert.isTrue( StringUtils.isNotBlank( identifier ), "Platform identifier must not be blank." );
        identifier = StringUtils.strip( identifier );
        ArrayDesign arrayDesign;
        try {
            long id = Long.parseLong( identifier );
            if ( ( arrayDesign = arrayDesignService.load( id ) ) != null ) {
                log.info( "Found " + arrayDesign + " by ID." );
                return arrayDesign;
            } else {
                throw new NullPointerException( "No platform with ID " + id );
            }
        } catch ( NumberFormatException e ) {
            // ignore
        }
        if ( ( arrayDesign = arrayDesignService.findByShortName( identifier ) ) != null ) {
            log.info( "Found " + arrayDesign + " by short name." );
            return arrayDesign;
        }
        if ( ( arrayDesign = arrayDesignService.findOneByName( identifier ) ) != null ) {
            log.info( "Found " + arrayDesign + " by name." );
            return arrayDesign;
        }
        if ( ( arrayDesign = arrayDesignService.findOneByAlternateName( identifier ) ) != null ) {
            log.info( "Found " + arrayDesign + " by alternate name." );
            return arrayDesign;
        }
        throw new NullPointerException( "No platform found with ID or name matching " + identifier + "." );
    }

    /**
     * Attempt to locate an experiment using the given identifier.
     */
    @Override
    public ExpressionExperiment locateExpressionExperiment( String identifier, boolean useReferencesIfPossible ) {
        Assert.isTrue( StringUtils.isNotBlank( identifier ), "Expression experiment ID or short name must be provided" );
        identifier = StringUtils.strip( identifier );
        ExpressionExperiment ee;
        try {
            Long id = Long.parseLong( identifier );
            if ( useReferencesIfPossible ) {
                // this is never null, but may produce ObjectNotFoundException later on
                return eeService.loadReference( id );
            } else if ( ( ee = eeService.load( id ) ) != null ) {
                log.debug( "Found " + ee + " by ID" );
                return ee;
            } else {
                throw new NullPointerException( "No experiment found with ID " + id );
            }
        } catch ( NumberFormatException e ) {
            // can be safely ignored, we'll attempt to use it as a short name
        }
        if ( ( ee = eeService.findByShortName( identifier ) ) != null ) {
            log.debug( "Found " + ee + " by short name" );
            return ee;
        }
        if ( ( ee = eeService.findOneByAccession( identifier ) ) != null ) {
            log.debug( "Found " + ee + " by accession" );
            return ee;
        }
        if ( ( ee = eeService.findOneByName( identifier ) ) != null ) {
            log.debug( "Found " + ee + " by name" );
            return ee;
        }
        throw new NullPointerException( "Could not locate any experiment with identifier or name matching " + identifier + "." );
    }

    @Override
    public Protocol locateProtocol( String protocolName ) {
        Assert.isTrue( StringUtils.isNotBlank( protocolName ), "Protocol identifier must not be blank." );
        protocolName = StringUtils.strip( protocolName );
        try {
            long id = Long.parseLong( protocolName );
            return protocolService.load( id );
        } catch ( NumberFormatException e ) {
            // ignore
        }
        return requireNonNull( protocolService.findByName( protocolName ),
                "Could not locate any protocol with identifier or name matching " + protocolName + "." );
    }

    @Override
    public <T extends DataVector> QuantitationType locateQuantitationType( ExpressionExperiment ee, String qt, Class<? extends T> vectorType ) {
        Assert.isTrue( StringUtils.isNotBlank( qt ), "Quantitation type identifier must not be blank." );
        qt = StringUtils.strip( qt );
        QuantitationType result;
        try {
            if ( ( result = quantitationTypeService.loadByIdAndVectorType( Long.parseLong( qt ), ee, vectorType ) ) != null ) {
                return result;
            }
        } catch ( NumberFormatException e ) {
            // ignore
        }
        try {
            if ( ( result = quantitationTypeService.findByNameAndVectorType( ee, qt, vectorType ) ) != null ) {
                return result;
            }
        } catch ( NonUniqueQuantitationTypeByNameException e ) {
            Collection<QuantitationType> possibleValues = quantitationTypeService.findAllByNameAndVectorType( ee, qt, vectorType );
            throw new RuntimeException( String.format( "More than one quantitation type in %s for %s matching %s. You must use an ID to disambiguate.%s.",
                    ee, vectorType.getSimpleName(), qt, formatPossibleValues( possibleValues, false ) ), e );
        }
        Collection<QuantitationType> possibleValues = quantitationTypeService.findByExpressionExperiment( ee, vectorType );
        throw new NullPointerException( String.format( "No quantitation type in %s for %s matching %s.%s",
                ee, vectorType.getSimpleName(), qt, formatPossibleValues( possibleValues, true ) ) );
    }

    @Override
    public <T extends DataVector> QuantitationType locateQuantitationType( ExpressionExperiment ee, String qt, Collection<Class<? extends T>> vectorTypes ) {
        Assert.isTrue( StringUtils.isNotBlank( qt ), "Quantitation type identifier must not be blank." );
        qt = StringUtils.strip( qt );
        QuantitationType result;
        for ( Class<? extends T> vectorType : vectorTypes ) {
            try {
                if ( ( result = quantitationTypeService.loadByIdAndVectorType( Long.parseLong( qt ), ee, vectorType ) ) != null ) {
                    return result;
                }
            } catch ( NumberFormatException e ) {
                // ignore
            }
            try {
                if ( ( result = quantitationTypeService.findByNameAndVectorType( ee, qt, vectorType ) ) != null ) {
                    return result;
                }
            } catch ( NonUniqueQuantitationTypeByNameException e ) {
                Collection<QuantitationType> possibleValues = quantitationTypeService.findAllByNameAndVectorType( ee, qt, vectorType );
                throw new RuntimeException( String.format( "More than one quantitation type in %s for any of %s matching %s. You must use an ID to disambiguate.%s.",
                        ee, vectorTypes.stream().map( Class::getSimpleName ).collect( Collectors.joining( ", " ) ), qt,
                        formatPossibleValues( possibleValues, false ) ), e );
            }
        }
        Collection<QuantitationType> possibleValues = quantitationTypeService.findByExpressionExperiment( ee, vectorTypes );
        throw new NullPointerException( String.format( "No quantitation type in %s for any of %s matching %s.%s",
                ee, vectorTypes.stream().map( Class::getSimpleName ).collect( Collectors.joining( ", " ) ), qt,
                formatPossibleValues( possibleValues, true ) ) );
    }

    @Override
    public CellTypeAssignment locateCellTypeAssignment( ExpressionExperiment expressionExperiment, QuantitationType qt, String cta ) {
        Assert.isTrue( StringUtils.isNotBlank( cta ), "Cell type assignment name must not be blank." );
        cta = StringUtils.strip( cta );
        try {
            CellTypeAssignment c = singleCellExpressionExperimentService.getCellTypeAssignment( expressionExperiment, qt, Long.parseLong( cta ) );
            if ( c != null ) {
                return c;
            }
        } catch ( NumberFormatException e ) {
            // ignore
        }
        String finalCta = cta;

        return requireNonNull( singleCellExpressionExperimentService.getCellTypeAssignment( expressionExperiment, qt, cta ), () -> {
            List<CellTypeAssignment> possibleValues = singleCellExpressionExperimentService.getCellTypeAssignments( expressionExperiment, qt );
            return "Could not locate any cell type assignment with identifier or name matching " + finalCta + "." + formatPossibleValues( possibleValues, true );
        } );
    }

    @Override
    public CellLevelCharacteristics locateCellLevelCharacteristics( ExpressionExperiment expressionExperiment, QuantitationType qt, String clcIdentifier ) {
        Assert.isTrue( StringUtils.isNotBlank( clcIdentifier ), "Cell level characteristics name must not be blank." );
        clcIdentifier = StringUtils.strip( clcIdentifier );
        CellLevelCharacteristics r = singleCellExpressionExperimentService.getCellLevelCharacteristics( expressionExperiment, qt, Long.parseLong( clcIdentifier ) );
        if ( r != null ) {
            return r;
        } else {
            List<CellLevelCharacteristics> possibleValues = singleCellExpressionExperimentService.getCellLevelCharacteristics( expressionExperiment, qt );
            throw new NullPointerException( "Could not locate any cell level characteristics with identifier matching " + clcIdentifier + "." + formatPossibleValues( possibleValues, false ) );
        }
    }

    @Override
    public ExperimentalFactor locateExperimentalFactor( ExpressionExperiment expressionExperiment, String identifier ) {
        Assert.isTrue( StringUtils.isNotBlank( identifier ), "Experimental factor name must not be blank." );
        identifier = StringUtils.strip( identifier );

        expressionExperiment = eeService.thawLiter( expressionExperiment );

        if ( expressionExperiment.getExperimentalDesign() == null || expressionExperiment.getExperimentalDesign().getExperimentalFactors().isEmpty() ) {
            throw new IllegalStateException( "Experimental design is not populated for " + expressionExperiment + "." );
        }

        ExperimentalFactor factor;
        try {
            Long efId = Long.parseLong( identifier );
            if ( ( factor = matchOneFactor( expressionExperiment, ef -> ef.getId().equals( efId ) ) ) != null ) {
                return factor;
            }
        } catch ( NumberFormatException e ) {
            // ignore
        }

        // exact match
        String finalIdentifier = identifier;
        if ( ( factor = matchOneFactor( expressionExperiment, ef -> ef.getName().equalsIgnoreCase( finalIdentifier ) ) ) != null ) {
            return factor;
        }

        // replacing space with underscores when matching the name
        if ( ( factor = matchOneFactor( expressionExperiment, ef -> ef.getName().replace( ' ', '_' ).equalsIgnoreCase( finalIdentifier ) ) ) != null ) {
            return factor;
        }

        // match by category
        if ( ( factor = matchOneFactor( expressionExperiment, ef -> ef.getCategory() != null && StringUtils.equalsIgnoreCase( ef.getCategory().getCategory(), finalIdentifier ) ) ) != null ) {
            return factor;
        }
        if ( ( factor = matchOneFactor( expressionExperiment, ef -> ef.getCategory() != null && StringUtils.equalsIgnoreCase( ef.getCategory().getCategoryUri(), finalIdentifier ) ) ) != null ) {
            return factor;
        }
        if ( ( factor = matchOneFactor( expressionExperiment, ef -> ef.getCategory() != null && StringUtils.equalsIgnoreCase( ef.getCategory().getValue(), finalIdentifier ) ) ) != null ) {
            return factor;
        }
        if ( ( factor = matchOneFactor( expressionExperiment, ef -> ef.getCategory() != null && StringUtils.equalsIgnoreCase( ef.getCategory().getValueUri(), finalIdentifier ) ) ) != null ) {
            return factor;
        }

        // TODO: print possible values
        throw new NullPointerException( "Could not locate any experimental factor matching '" + identifier + "'."
                + formatPossibleValues( expressionExperiment.getExperimentalDesign().getExperimentalFactors(), true ) );
    }

    @Nullable
    private ExperimentalFactor matchOneFactor( ExpressionExperiment ee, Predicate<ExperimentalFactor> predicate ) {
        if ( ee.getExperimentalDesign() == null ) {
            return null;
        }
        Set<ExperimentalFactor> matches = ee.getExperimentalDesign().getExperimentalFactors().stream()
                .filter( predicate )
                .collect( Collectors.toSet() );
        if ( matches.size() == 1 ) {
            return matches.iterator().next();
        } else {
            return null;
        }
    }

    @Override
    public BioAssay locateBioAssay( ExpressionExperiment ee, String sampleId ) {
        ee = eeService.thawLite( ee );
        return requireNonNull( locateBioAssay( ee.getBioAssays(), sampleId ),
                "Could not locate any assay matching '" + sampleId + "' in " + ee.getShortName() + "." + formatPossibleValues( ee.getBioAssays(), true ) );
    }

    @Override
    public BioAssay locateBioAssay( ExpressionExperiment ee, QuantitationType qt, String sampleId ) {
        BioAssayDimension bad = eeService.getBioAssayDimension( ee, qt );
        if ( bad != null ) {
            return requireNonNull( locateBioAssay( bad.getBioAssays(), sampleId ),
                    "Could not locate any assay matching '" + sampleId + "' in " + ee.getShortName() + " for " + qt + "." + formatPossibleValues( bad.getBioAssays(), true ) );
        }
        SingleCellDimension scd = singleCellExpressionExperimentService.getSingleCellDimension( ee, qt );
        if ( scd != null ) {
            return requireNonNull( locateBioAssay( scd.getBioAssays(), sampleId ),
                    "Could not locate any assay matching '" + sampleId + "' in " + ee.getShortName() + " for " + qt + "." + formatPossibleValues( scd.getBioAssays(), true ) );
        }
        throw new NullPointerException();
    }

    @Override
    public DifferentialExpressionAnalysis locateDiffExAnalysis( ExpressionExperiment ee, String analysisIdentifier ) {
        return requireNonNull( differentialExpressionAnalysisService.findByExperimentAnalyzedAndId( ee, Long.parseLong( analysisIdentifier ), true ),
                () -> String.format( "Could not locate an analysis matching '%s' in %s.%s",
                        analysisIdentifier,
                        ee.getShortName(),
                        formatPossibleValues( differentialExpressionAnalysisService.getAnalyses( ee, true ), false ) ) );
    }

    @Nullable
    private BioAssay locateBioAssay( Collection<BioAssay> ee, String sampleId ) {
        BioAssay ba;
        try {
            Long id = Long.parseLong( sampleId );
            if ( ( ba = matchOneAssay( ee, ba2 -> ba2.getId().equals( id ) ) ) != null ) {
                return ba;
            }
        } catch ( NumberFormatException e ) {
            // ignore
        }
        if ( ( ba = matchOneAssay( ee, ba2 -> ba2.getShortName() != null && ba2.getShortName().equalsIgnoreCase( sampleId ) ) ) != null ) {
            return ba;
        }
        if ( ( ba = matchOneAssay( ee, ba2 -> ba2.getName().equalsIgnoreCase( sampleId ) ) ) != null ) {
            return ba;
        }
        if ( ( ba = matchOneAssay( ee, ba2 -> ba2.getAccession() != null && ba2.getAccession().getAccession().equalsIgnoreCase( sampleId ) ) ) != null ) {
            return ba;
        }
        return null;
    }

    private BioAssay matchOneAssay( Collection<BioAssay> bioAssays, Predicate<BioAssay> ba ) {
        Set<BioAssay> bas = bioAssays.stream().filter( ba ).collect( Collectors.toSet() );
        if ( bas.size() == 1 ) {
            return bas.iterator().next();
        } else {
            return null;
        }
    }

    private String formatPossibleValues( Collection<? extends Identifiable> possibleValues, boolean allowAmbiguousIds ) {
        if ( possibleValues.isEmpty() ) {
            return "";
        }
        return String.format( " Possible values are:\n\t%s",
                possibleValues.stream()
                        .map( q -> formatPossibleValues( q, allowAmbiguousIds ) + " for " + q )
                        .collect( Collectors.joining( "\n\t" ) ) );
    }

    private String formatPossibleValues( Identifiable q, boolean allowAmbiguousIds ) {
        return getPossibleIdentifiers( q, allowAmbiguousIds ).stream()
                .map( ShellUtils::quoteIfNecessary )
                .collect( Collectors.joining( " or " ) );
    }

    private LinkedHashSet<String> getPossibleIdentifiers( Identifiable identifiable, boolean allowAmbiguousIds ) {
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        candidates.add( String.valueOf( identifiable.getId() ) );
        if ( identifiable instanceof BioAssay ) {
            candidates.add( ( ( BioAssay ) identifiable ).getShortName() );
        }
        if ( allowAmbiguousIds ) {
            if ( identifiable instanceof Describable ) {
                Describable d = ( Describable ) identifiable;
                candidates.add( d.getName() );
            }
            if ( identifiable instanceof BioAssay ) {
                BioAssay ba = ( BioAssay ) identifiable;
                if ( ba.getAccession() != null ) {
                    candidates.add( ba.getAccession().getAccession() );
                }
            }
            if ( identifiable instanceof Taxon ) {
                Taxon t = ( Taxon ) identifiable;
                if ( t.getNcbiId() != null ) {
                    candidates.add( String.valueOf( t.getNcbiId() ) );
                }
                candidates.add( t.getCommonName() );
                candidates.add( t.getScientificName() );
            }
            if ( identifiable instanceof ExperimentalFactor ) {
                ExperimentalFactor ef = ( ExperimentalFactor ) identifiable;
                candidates.add( ef.getName().replace( ' ', '_' ) );
                if ( ef.getCategory() != null ) {
                    candidates.add( ef.getCategory().getCategory() );
                    candidates.add( ef.getCategory().getCategoryUri() );
                    candidates.add( ef.getCategory().getValue() );
                    candidates.add( ef.getCategory().getValueUri() );
                }
            }
        }
        // drop nulls
        candidates.removeIf( Objects::isNull );
        return candidates;
    }
}

