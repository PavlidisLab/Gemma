/**
 * @author poirigui
 */
export default class ExternalDatabaseValueObject {
   id;
   name;
   uri;

   constructor( {id, name, uri} ) {
      this.id = id;
      this.name = name;
      this.uri = uri;
   }
}