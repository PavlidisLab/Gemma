Ext.namespace('Gemma');
/**
 * 
 * Creates a new session-bound experiment group
 * 
 * This store should only be used for creation, use UserSessionDatasetGroupStore for the rest of CRUD
 * 
 * Though this should only be used for creation, EXT requires all CRUD operations to be defined:
 * read: retrieves all session-bound groups
 * update: uses same method as UserSessionDatasetGroupStore
 * destroy: uses same method as UserSessionDatasetGroupStore
 * 
 * @class Gemma.SessionDatasetGroupStore
 * @extends Ext.data.Store
 * @see DatasetGroupCombo, UserSessionDatasetGroupStore
 */
Gemma.SessionDatasetGroupStore = function(config) {

	/*
	 * Leave this here so copies of records can be constructed.
	 */
	this.record = Ext.data.Record.create([{
				name : "id",
				type : "int"
			},{
				name : "name",
				type : "string"
			}, {
				name : "description",
				type : "string"
			}, {
				name : "numExperiments",
				type : "int"
			}, {
				name : "expressionExperimentIds"
			}, {
				name : "taxonId",
				type : "int"
			}, {
				name : "taxonName"
			}, {
				name : "modifiable",
				type : 'boolean'
			}, {
				name : "publik",
				type : 'boolean'
			}, {
				name : "shared",
				type : 'boolean'
			}, {
				name : "writeableByUser",
				type : 'boolean'
			}, {
				name : "session",
				type : 'boolean'
			}]);

	// todo replace with JsonReader.
	this.reader = new Ext.data.ListRangeReader({
			}, this.record);

	Gemma.SessionDatasetGroupStore.superclass.constructor.call(this, config);

};

/**
 * 
 * @class Gemma.DatasetGroupStore
 * @extends Ext.data.Store
 */
Ext.extend(Gemma.SessionDatasetGroupStore, Ext.data.Store, {

			autoLoad : true,
			autoSave : false,
			selected : null,

			proxy : new Ext.data.DWRProxy({
						apiActionToHandlerMap: {
							read: {
								dwrFunction: ExpressionExperimentSetController.loadAllSessionGroups
							},
							create: {
								dwrFunction: ExpressionExperimentSetController.addSessionGroups
							},
							update: {
								dwrFunction: ExpressionExperimentSetController.updateUserAndSessionGroups
							},
							destroy: {
								dwrFunction: ExpressionExperimentSetController.removeUserAndSessionGroups
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
			},

			listeners : {
				exception : function(proxy, type, action, options, res, arg) {
					if (type === 'remote') {
						Ext.Msg.show({
									title : 'Error',
									msg : res.message,
									icon : Ext.MessageBox.ERROR
								});
					} else {
						Ext.Msg.show({
									title : 'Error',
									msg : arg.message ? arg.message : arg,
									icon : Ext.MessageBox.ERROR
								});
					}
				}

			}

		});
