/**
 * @author poirigui
 */
export default class DatabaseEntryValueObject {
   id;
   accession;
   uri;
   label;
   /**
    * @type {ExternalDatabaseValueObject}
    */
   externalDatabase;

   constructor( {accession, uri, label, externalDatabase} ) {
      this.id = id;
      this.accession = accession;
      this.uri = uri;
      this.label = label;
      this.externalDatabase = externalDatabase;
   }
}