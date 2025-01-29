package ubic.gemma.core.loader.util.anndata;

import ubic.gemma.core.loader.util.hdf5.H5Dataset;
import ubic.gemma.core.loader.util.hdf5.H5Location;

import java.util.Objects;

class Utils {

    static void checkEncoding( H5Location loc, String encodingType ) throws MissingEncodingAttributeException {
        if ( !loc.hasAttribute( "encoding-type" ) ) {
            throw new MissingEncodingAttributeException( loc + " does not have an 'encoding-type' attribute set." );
        }
        if ( !Objects.equals( loc.getStringAttribute( "encoding-type" ), encodingType ) ) {
            throw new InvalidEncodingAttributeException( loc + " does not have its 'encoding-type' set to '" + encodingType + "'." );
        }
        if ( !loc.hasAttribute( "encoding-version" ) ) {
            throw new MissingEncodingAttributeException( loc + " does not have an 'encoding-version' attribute set." );
        }
    }

    static void checkEncoding( H5Dataset loc, String encodingType ) throws MissingEncodingAttributeException {
        if ( !loc.hasAttribute( "encoding-type" ) ) {
            throw new MissingEncodingAttributeException( loc + " does not have an 'encoding-type' attribute set." );
        }
        if ( !Objects.equals( loc.getStringAttribute( "encoding-type" ), encodingType ) ) {
            throw new InvalidEncodingAttributeException( loc + " does not have its 'encoding-type' set to '" + encodingType + "'." );
        }
        if ( !loc.hasAttribute( "encoding-version" ) ) {
            throw new MissingEncodingAttributeException( loc + " does not have an 'encoding-version' attribute set." );
        }
    }
}
