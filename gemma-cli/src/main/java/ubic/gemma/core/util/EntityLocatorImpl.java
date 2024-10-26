package ubic.gemma.core.util;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import ubic.gemma.model.common.protocol.Protocol;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssayData.CellTypeAssignment;
import ubic.gemma.model.expression.bioAssayData.DataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.common.protocol.ProtocolService;
import ubic.gemma.persistence.service.common.quantitationtype.NonUniqueQuantitationTypeByNameException;
import ubic.gemma.persistence.service.common.quantitationtype.QuantitationTypeService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.SingleCellExpressionExperimentService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

import java.util.Optional;

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

    @Override
    public Taxon locateTaxon( String identifier ) {
        Assert.isTrue( StringUtils.isNotBlank( identifier ), "Taxon name must be be blank." );
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
        throw new NullPointerException( "Cannot find taxon with name " + identifier );
    }

    @Override
    public ArrayDesign locateArrayDesign( String identifier ) {
        Assert.isTrue( StringUtils.isNotBlank( identifier ), "Platform name must not be blank." );
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
        throw new NullPointerException( "No platform found with ID or name matching " + identifier );
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
        throw new NullPointerException( "Could not locate any experiment with identifier or name matching " + identifier );
    }

    @Override
    public Protocol locateProtocol( String protocolName ) {
        try {
            long id = Long.parseLong( protocolName );
            return protocolService.load( id );
        } catch ( NumberFormatException e ) {
            // ignore
        }
        return requireNonNull( protocolService.findByName( protocolName ),
                "Could not locate any protocol with identifier or name matching " + protocolName );
    }

    @Override
    public QuantitationType locateQuantitationType( ExpressionExperiment ee, String qt, Class<? extends DataVector> vectorType ) {
        try {
            quantitationTypeService.loadByIdAndVectorType( Long.parseLong( qt ), ee, vectorType );
        } catch ( NumberFormatException e ) {
            // ignore
        }
        try {
            return requireNonNull( quantitationTypeService.findByNameAndVectorType( ee, qt, vectorType ) );
        } catch ( NonUniqueQuantitationTypeByNameException e ) {
            throw new RuntimeException( e );
        }
    }

    @Override
    public CellTypeAssignment locateCellTypeAssignment( ExpressionExperiment expressionExperiment, QuantitationType qt, String cta ) {
        try {
            Optional<CellTypeAssignment> c = singleCellExpressionExperimentService.getCellTypeAssignment( expressionExperiment, qt, Long.parseLong( cta ) );
            if ( c.isPresent() ) {
                return c.get();
            }
        } catch ( NumberFormatException e ) {
            // ignore
        }
        return singleCellExpressionExperimentService.getCellTypeAssignment( expressionExperiment, qt, cta )
                .orElseThrow( () -> new NullPointerException( "Could not locate any cell type assignment with identifier or name matching " + cta ) );
    }
}
