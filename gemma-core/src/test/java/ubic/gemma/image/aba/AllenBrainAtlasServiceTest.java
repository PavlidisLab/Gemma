package ubic.gemma.image.aba;

import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.image.aba.AbaGene;
import ubic.gemma.image.aba.AllenBrainAtlasService;
import ubic.gemma.image.aba.Image;
import ubic.gemma.image.aba.ImageSeries;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * Alan brain Atlas service test.
 *  
 * @version $Id$ @author kelsey
 *
 */
public class AllenBrainAtlasServiceTest  extends BaseSpringContextTest {

    private AllenBrainAtlasService abaService = null;
    private static Log log = LogFactory.getLog( AllenBrainAtlasServiceTest.class.getName() );
    
    public void testGetGene() throws Exception {

        AbaGene grin1 = abaService.getGene( "Grin1" );
        Collection<ImageSeries> representativeSaggitalImages = new HashSet<ImageSeries>();
        
        for ( ImageSeries is : grin1.getImageSeries() ) {
            if (is.getPlane().equalsIgnoreCase( "sagittal" )){
              
                Collection<Image> images = abaService.getImageseries( is.getImageSeriesId() );              
              Collection<Image> representativeImages = new HashSet<Image>();
              
              for(Image img : images){
                  if ((2600 > img.getPosition()) && (img.getPosition() > 2200)){
                      representativeImages.add( img );
                  }
              }
              
              if (representativeImages.isEmpty())
                  continue;
              
              //Only add if there is something to add
              is.setImages( representativeImages );
              representativeSaggitalImages.add( is );
            }
        }
        grin1.setImageSeries( representativeSaggitalImages );
            
        
        log.info( grin1 );
   }
        
    
    
    protected void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();
        abaService = ( AllenBrainAtlasService ) getBean( "alanBrainAtlasService" );        
    }

}
