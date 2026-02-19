import ExternalDatabaseUtils from "./ExternalDatabaseUtils";

let htmlEncode = Ext.util.Format.htmlEncode;

function isHttpUrl( uri ) {
   return uri.startsWith( 'http://' ) || uri.startsWith( 'https://' );
}

/**
 * Simple component for rendering a database entry.
 *
 * This should be kept as much as possible in sync with the DatabaseEntryTag.java.
 * @author poirigui
 */
export default class DatabaseEntryTag {
   /**
    *
    * @param {DatabaseEntryValueObject} databaseEntry
    */
   constructor( databaseEntry ) {
      this.databaseEntry = databaseEntry;
   }

   render() {
      if ( !this.databaseEntry ) {
         return "<i>No accession available</i>";
      }
      let edMeta = ExternalDatabaseUtils.externalDatabases
         .find( ed => ed.name === this.databaseEntry.externalDatabase.name );
      let s;
      if ( edMeta ) {
         if ( this.databaseEntry.label ) {
            s = htmlEncode( this.databaseEntry.label ) + ' ';
         } else {
            s = '';
         }
         let externalDatabaseLogo = '<img src="' + edMeta.logo + '" height="16" alt="' + htmlEncode( edMeta.name ) + ' logo"/>';
         if ( this.databaseEntry.uri !== null && isHttpUrl( this.databaseEntry.uri ) ) {
            s += '<a target="_blank" rel="noopener noreferrer" href="' + this.databaseEntry.uri + '">' + externalDatabaseLogo + '</a>';
         } else if ( this.databaseEntry.externalDatabase.uri !== null && isHttpUrl( this.databaseEntry.externalDatabase.uri ) ) {
            s += '<a target="_blank" rel="noopener noreferrer" href="' + this.databaseEntry.externalDatabase.uri + '">' + externalDatabaseLogo + '</a>';
         } else {
            // no link available
            s += externalDatabaseLogo;
         }
      } else {
         let externalDatabaseLinkHtml = htmlEncode( this.databaseEntry.externalDatabase.name ) + ' <i class="fa fa-external-link"></i>';
         if ( this.databaseEntry.uri !== null ) {
            if ( this.databaseEntry.label ) {
               s = htmlEncode( this.databaseEntry.label ) + ' (<a target="_blank" rel="noopener noreferrer" href="' + this.databaseEntry.uri + '">' + externalDatabaseLinkHtml + '</a>)';
            } else {
               s = '<a target="_blank" rel="noopener noreferrer" href="' + this.databaseEntry.uri + '">' + externalDatabaseLinkHtml + '</a>';
            }
         } else {
            // no link available
            if ( this.databaseEntry.label ) {
               s = htmlEncode( this.databaseEntry.label ) + ' (' + externalDatabaseLinkHtml + "')";
            } else {
               s = externalDatabaseLinkHtml;
            }
         }
      }
      return "<span>" + s + "</span>";
   }
}