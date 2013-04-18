

Ext.namespace('Gemma');

Gemma.UserSessionGeneGroupStore = function(config) {

   /*
    * Leave this here so copies of records can be constructed.
    */
   this.record = Ext.data.Record.create([{
         name : "id",
         type : "int"
      }, {
         name : "taxonId",
         type : "int"
      }, {
         name : "name",
         type : "string",
         convert : function(v, rec) {
            if (v.startsWith("GO")) {
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
         convert : function(v, rec) {
            if (rec.name.startsWith("GO")) {
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
      }]);

   // todo replace with JsonReader.
   this.reader = new Ext.data.ListRangeReader({}, this.record);

   Gemma.UserSessionGeneGroupStore.superclass.constructor.call(this, config);

};

/**
 * 
 * @class Gemma.UserSessionGeneGroupStore
 * @extends Ext.data.Store
 */
Ext.extend(Gemma.UserSessionGeneGroupStore, Ext.data.Store, {

      autoLoad : true,
      autoSave : false,
      selected : null,
      name : "geneGroupData-store",

      proxy : new Ext.data.DWRProxy({
            apiActionToHandlerMap : {
               read : {
                  dwrFunction : GeneSetController.getUserAndSessionGeneGroups,
                  getDwrArgsFunction : function(request) {
                     if (request.params.length > 0) {
                        return [request.params[0], request.params[1]];
                     }
                     return [false, null];
                  }
               },
               create : {
                  dwrFunction : GeneSetController.addUserAndSessionGroups
               },
               update : {
                  dwrFunction : GeneSetController.updateUserAndSessionGroups
               },
               destroy : {
                  dwrFunction : GeneSetController.removeUserAndSessionGroups
               }
            }
         }),

      writer : new Ext.data.JsonWriter({
            writeAllFields : true
         }),

      getSelected : function() {
         return this.selected;
      },

      setSelected : function(rec) {
         this.previousSelection = this.getSelected();
         if (rec) {
            this.selected = rec;
         }
      },

      getPreviousSelection : function() {
         return this.previousSelection;
      },

      clearSelected : function() {
         this.selected = null;
         delete this.selected;
      }

   });
