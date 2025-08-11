package ubic.gemma.corej11.visualization;

import com.sun.imageio.plugins.png.PNGMetadata;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.jfree.chart.encoders.ImageEncoder;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Add support for writing various PNG metadata.
 * @see org.jfree.chart.encoders.SunPNGEncoderAdapter
 * @author poirigui
 */
@Getter
@Setter
class SunPNGEncoderWithMetadataAdapter implements ImageEncoder {

    public final double METER_PER_INCH = 39.37;

    private float quality = 0.0f;

    private boolean isEncodingAlpha = false;

    /**
     * Pixel per meter.
     * <p>
     * If zero or less, no pHYs chunk will be written.
     */
    private double ppm = 0.0;

    private final Map<String, String> keywords = new HashMap<>();

    /**
     * Obtain the DPI (dots per inch) of the image.
     * <p>
     * DPI is actually stored as PPM (pixels per meter) in the PNG format in a pHYs chunk.
     */
    public double getDpi() {
        return this.ppm / METER_PER_INCH;
    }

    public void setDpi( double dpi ) {
        this.ppm = dpi * METER_PER_INCH;
    }

    public void setTitle( String title ) {
        setKeyword( "Title", title );
    }

    public void setAuthor( String author ) {
        setKeyword( "Author", author );
    }

    public void setDescription( String description ) {
        setKeyword( "Description", description );
    }

    public void setCopyright( String s ) {
        setKeyword( "Copyright", s );
    }

    public void setCreationTime( Date creationTime ) {
        setKeyword( "Creation Time", DateTimeFormatter.RFC_1123_DATE_TIME.format( creationTime.toInstant() ) );
    }

    public void setSoftware( String software ) {
        setKeyword( "Software", software );
    }

    public void setDisclaimer( String disclaimer ) {
        setKeyword( "Disclaimer", disclaimer );
    }

    public void setKeyword( String keyword, String value ) {
        if ( StringUtils.isBlank( value ) ) {
            keywords.remove( keyword );
        } else {
            keywords.put( keyword, value );
        }
    }

    @Override
    public byte[] encode( BufferedImage bufferedImage ) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        encode( bufferedImage, baos );
        return baos.toByteArray();
    }

    @Override
    public void encode( BufferedImage bufferedImage, OutputStream outputStream ) throws IOException {
        PNGMetadata pngMetadata = new PNGMetadata();
        if ( ppm > 0 ) {
            pngMetadata.pHYs_present = true;
            pngMetadata.pHYs_pixelsPerUnitXAxis = ( int ) Math.ceil( ppm );
            pngMetadata.pHYs_pixelsPerUnitYAxis = ( int ) Math.ceil( ppm );
            pngMetadata.pHYs_unitSpecifier = PNGMetadata.PHYS_UNIT_METER;
        }
        for ( Map.Entry<String, String> entry : keywords.entrySet() ) {
            pngMetadata.tEXt_keyword.add( entry.getKey() );
            pngMetadata.tEXt_text.add( entry.getValue() );
        }
        IIOImage iioImage = new IIOImage( bufferedImage, null, pngMetadata );
        ImageWriter writer = ImageIO.getImageWritersByFormatName( "png" ).next();
        try ( ImageOutputStream out = ImageIO.createImageOutputStream( outputStream ) ) {
            writer.setOutput( out );
            writer.write( iioImage );
        } finally {
            writer.dispose();
        }
    }
}
