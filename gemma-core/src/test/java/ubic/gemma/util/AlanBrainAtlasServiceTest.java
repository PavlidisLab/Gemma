package ubic.gemma.util;

import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.testing.BaseSpringContextTest;


public class AlanBrainAtlasServiceTest  extends BaseSpringContextTest {

    private AlanBrainAtlasService abaService = null;
    private static Log log = LogFactory.getLog( AlanBrainAtlasServiceTest.class.getName() );
    
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
        abaService = ( AlanBrainAtlasService ) getBean( "alanBrainAtlasService" );        
    }

}
