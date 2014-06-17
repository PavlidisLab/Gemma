/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 * 
 */

Ext.namespace( 'Gemma' );

/**
 * 
 * @param {}
 *           config
 */
Gemma.SessionGeneGroupStore = function( config ) {

   /*
    * Leave this here so copies of records can be constructed.
    */
   this.record = Ext.data.Record.create( [ {
      name : "id",
      type : "int"
   }, {
      name : "taxonId",
      type : "int"
   }, {
      name : "name",
      type : "string",
      convert : function( v, rec ) {
         if ( v.indexOf( "GO", 0 ) == 0 ) {
            return rec.description;
         }
         return v;
      }
   }, {
      name : "taxonName",
      type : "string"
   }, {
      name : "description",
      type : "string",
      convert : function( v, rec ) {
         if ( rec.name.indexOf( "GO", 0 ) == 0 ) {
            return rec.name;
         }
         return v;
      }

   }, {
      name : "isPublic",
      type : "boolean"
   }, {
      name : "size",
      type : "int"
   }, {
      name : "isShared",
      type : 'boolean'
   }, {
      name : "userCanWrite",
      type : 'boolean'
   }, {
      name : "session",
      type : 'boolean'
   }, {
      name : "geneIds"
   } ] );

   // todo replace with JsonReader.
   this.reader = new Ext.data.ListRangeReader( {
   // id : "id"
   }, this.record );

   Gemma.SessionGeneGroupStore.superclass.constructor.call( this, config );

};

/**
 * 
 * @class Gemma.GeneGroupStore
 * @extends Ext.data.Store
 */
Ext.extend( Gemma.SessionGeneGroupStore, Ext.data.Store, {

   autoLoad : true,
   autoSave : false,
   selected : null,
   name : "geneGroupSessionData-store",

   proxy : new Ext.data.DWRProxy( {
      apiActionToHandlerMap : {
         read : {
            dwrFunction : GeneSetController.getUserAndSessionGeneGroups,
            getDwrArgsFunction : function( request ) {
               if ( request.params.length > 0 ) {
                  return [ request.params[0], request.params[1] ];
               }
               return [ false, null ];
            }
         },
         create : {
            dwrFunction : GeneSetController.addSessionGroups
         },
         update : {
            dwrFunction : GeneSetController.updateSessionGroups
         },
         destroy : {
            dwrFunction : GeneSetController.removeSessionGroups
         }
      }
   } ),

   writer : new Ext.data.JsonWriter( {
      writeAllFields : true
   } ),

   /**
    * @memberOf Gemma.SessionGeneGroupStore
    */
   getSelected : function() {
      return this.selected;
   },

   setSelected : function( rec ) {
      this.previousSelection = this.getSelected();
      if ( rec ) {
         this.selected = rec;
      }
   },

   getPreviousSelection : function() {
      return this.previousSelection;
   },

   clearSelected : function() {
      this.selected = null;
      delete this.selected;
   },

   listeners : {
      write : function( store, action, result, res, rs ) {
         // Ext.Msg.show({
         // title : "Saved",
         // msg : "Changes were saved",
         // icon : Ext.MessageBox.INFO
         // });
      },
      exception : function( proxy, type, action, options, res, arg ) {
         // console.log(res);
         if ( type === 'remote' ) {
            Ext.Msg.show( {
               title : 'Error',
               msg : res,
               icon : Ext.MessageBox.ERROR
            } );
         } else {
            Ext.Msg.show( {
               title : 'Error',
               msg : arg,
               icon : Ext.MessageBox.ERROR
            } );
         }
      }

   }

} );
