package ubic.gemma.web.taglib.expression.experiment;

import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.servlet.tags.form.TagWriter;
import ubic.gemma.core.visualization.ExpressionDataHeatmap;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.web.util.WebEntityUrlBuilder;

import javax.annotation.Nullable;
import javax.servlet.jsp.JspException;
import java.util.List;
import java.util.stream.Collectors;

@Setter
public class ExpressionDataHeatmapTag extends AbstractHeatmapTag<ExpressionDataHeatmap> {

    private transient WebEntityUrlBuilder entityUrlBuilder;

    /**
     * Font to use for the single-cell boxplot links.
     */
    @Nullable
    private String font;

    // needed for JSP
    @Override
    public void setHeatmap( ExpressionDataHeatmap heatmap ) {
        super.setHeatmap( heatmap );
    }

    @Override
    protected void writeHeatmapImage( TagWriter writer ) throws JspException {
        boolean transpose = heatmap.isTranspose();
        if ( heatmap.getVectors() != null ) {
            super.writeHeatmapImage( writer );
        } else {
            BioAssaySet bioAssaySet = heatmap.getBioAssaySet();
            BioAssayDimension dimension = heatmap.getDimension();
            int cellSize = heatmap.getCellSize();
            // create an URL to retrieve the image from the backend
            String url;
            if ( bioAssaySet instanceof ExpressionExperiment ) {
                url = pageContext.getServletContext().getContextPath() + "/expressionExperiment/visualizeHeatmap.html";
            } else if ( bioAssaySet instanceof ExpressionExperimentSubSet ) {
                url = pageContext.getServletContext().getContextPath() + "/expressionExperiment/visualizeSubSetHeatmap.html";
            } else {
                throw new IllegalArgumentException( "Cannot generate heatmap for " + bioAssaySet.getClass().getSimpleName() + "." );
            }
            url += "?id=" + bioAssaySet.getId() + "&dimension=" + dimension.getId();
            if ( heatmap.getDesignElements() != null ) {
                url += "&offset=" + heatmap.getDesignElements().getOffset() + "&limit=" + heatmap.getDesignElements().getLimit();
            }
            if ( useResizeTrick ) {
                url += "&cellSize=1";
            } else {
                url += "&cellSize=" + cellSize;
            }
            if ( transpose ) {
                url += "&transpose=true";
            }
            int height = cellSize * heatmap.getXLabels().size();
            int width = cellSize * heatmap.getYLabels().size();
            writer.startTag( "img" );
            writer.writeAttribute( "src", url );
            writer.writeAttribute( "class", "heatmap-img" );
            writer.writeOptionalAttributeValue( "alt", htmlEscape( alt ) );
            writer.writeAttribute( "width", String.valueOf( width ) );
            writer.writeAttribute( "height", String.valueOf( height ) );
            writer.endTag();
        }
    }

    @Override
    protected void writeXLabels( TagWriter writer ) throws JspException {
        writer.startTag( "div" );
        writer.writeAttribute( "class", "heatmap-x-labels" );
        writer.writeAttribute( "style", "font-size: " + ( heatmap.getCellSize() - 2 ) + "px; line-height: " + heatmap.getCellSize() + "px;" );
        if ( heatmap.isTranspose() ) {
            writeSamples( writer );
        } else {
            writeGenes( writer );
        }
        writer.endTag();
    }

    @Override
    protected void writeYLabels( TagWriter writer ) throws JspException {
        writer.startTag( "div" );
        writer.writeAttribute( "class", "heatmap-y-labels" );
        writer.writeAttribute( "style", "font-size: " + ( heatmap.getCellSize() - 2 ) + "px; line-height: " + heatmap.getCellSize() + "px;" );
        if ( heatmap.isTranspose() ) {
            writeGenes( writer );
        } else {
            writeSamples( writer );
        }
        writer.endTag();
    }

    public void writeGenes( TagWriter writer ) throws JspException {
        if ( entityUrlBuilder == null ) {
            entityUrlBuilder = getRequestContext().getWebApplicationContext().getBean( WebEntityUrlBuilder.class );
        }
        if ( heatmap.getGenes() != null ) {
            List<Gene> genes = heatmap.getGenes();
            for ( int i = 0; i < genes.size(); i++ ) {
                Gene gene = genes.get( i );
                if ( i > 0 ) {
                    writer.startTag( "br" );
                    writer.endTag(); // </br>
                }
                if ( gene != null ) {
                    writer.startTag( "a" );
                    writer.writeAttribute( "href", entityUrlBuilder.fromContextPath().entity( gene ).toUriString() );
                    writer.appendValue( gene.getOfficialSymbol() );
                    writer.endTag(); // </a>
                    if ( heatmap.getSingleCellQuantitationType() != null ) {
                        CompositeSequence designElement;
                        if ( heatmap.getVectors() != null ) {
                            designElement = heatmap.getVectors().get( i ).getDesignElement();
                        } else if ( heatmap.getDesignElements() != null ) {
                            designElement = heatmap.getDesignElements().get( i );
                        } else {
                            throw new IllegalStateException( "An expression data heatmap must have either vectors or design elements populated." );
                        }
                        ExpressionExperiment ee;
                        if ( heatmap.getBioAssaySet() instanceof ExpressionExperimentSubSet ) {
                            ee = ( ( ExpressionExperimentSubSet ) heatmap.getBioAssaySet() ).getSourceExperiment();
                        } else if ( heatmap.getBioAssaySet() instanceof ExpressionExperiment ) {
                            ee = ( ExpressionExperiment ) heatmap.getBioAssaySet();
                        } else {
                            throw new IllegalStateException( "Unexpected BioAssaySet type in heatmap: " + heatmap.getBioAssaySet().getClass().getName() );
                        }
                        String boxplotUrl = entityUrlBuilder.fromContextPath().entity( ee )
                                .web()
                                .visualizeSingleCellBoxPlot( heatmap.getSingleCellQuantitationType(),
                                        designElement, heatmap.getCellLevelCharacteristics(),
                                        heatmap.getFocusedCellLevelCharacteristic(),
                                        // may be an empty string when set via JSP
                                        StringUtils.stripToNull( font ) )
                                .toUriString();
                        writer.startTag( "a" );
                        writer.writeAttribute( "href", boxplotUrl );
                        writer.appendValue( " (view single-cell expression data)" );
                        writer.endTag(); // </a>
                    }
                } else {
                    writer.startTag( "i" );
                    writer.appendValue( "Unmapped: " );
                    if ( heatmap.getDesignElements() != null ) {
                        CompositeSequence cs = heatmap.getDesignElements().get( i );
                        writer.appendValue( htmlEscape( cs.getName() ) );
                    }
                    writer.endTag();
                }
            }
        } else if ( heatmap.getDesignElements() != null ) {
            boolean first = true;
            for ( CompositeSequence cs : heatmap.getDesignElements() ) {
                if ( !first ) {
                    writer.startTag( "br" );
                    writer.endTag(); // </br>
                }
                first = false;
                writer.appendValue( htmlEscape( cs.getName() ) );
            }
        } else {
            writer.appendValue( heatmap.getXLabels().stream().map( this::htmlEscape ).collect( Collectors.joining( "<br>" ) ) );
        }
    }

    private void writeSamples( TagWriter writer ) throws JspException {
        if ( entityUrlBuilder == null ) {
            entityUrlBuilder = getRequestContext().getWebApplicationContext().getBean( WebEntityUrlBuilder.class );
        }
        boolean first = true;
        for ( BioAssay ba : heatmap.getSamples() ) {
            if ( !first ) {
                writer.startTag( "br" );
                writer.endTag(); // </br>
            }
            first = false;
            writer.startTag( "a" );
            writer.writeAttribute( "href", entityUrlBuilder.fromContextPath().entity( ba ).toUriString() + "&dimension=" + heatmap.getDimension().getId() );
            writer.appendValue( htmlEscape( ba.getName() ) );
            writer.endTag(); // </a>
        }
    }
}
