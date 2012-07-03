Ext.namespace('Gemma');
/**
 * Holds the list of available ExpressionExperiment groups. This is a separate class so it can be more easily shared
 * between components. It also knows which record is selected, unlike the default store.
 * 
 * @class Gemma.DatasetGroupStore
 * @extends Ext.data.Store
 * @see DatasetGroupCombo
 */
Gemma.DatasetGroupStore = function(config) {

	/*
	 * Leave this here so copies of records can be constructed.
	 */
	this.record = Ext.data.Record.create([{
				name : "id",
				type : "int"
			}, {
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
				name : "currentUserHasWritePermission",
				type : 'boolean'
			}, {
				name : "currentUserIsOwner",
				type : 'boolean'
			}]);

	// todo replace with JsonReader.
	this.reader = new Ext.data.ListRangeReader({
				id : "id"
			}, this.record);

	Gemma.DatasetGroupStore.superclass.constructor.call(this, config);

};

/**
 * 
 * @class Gemma.DatasetGroupStore
 * @extends Ext.data.Store
 */
Ext.extend(Gemma.DatasetGroupStore, Ext.data.Store, {

			autoLoad : true,
			autoSave : false,
			selected : null,

			proxy : new Ext.data.DWRProxy({
						apiActionToHandlerMap : {
							read : {
								dwrFunction : ExpressionExperimentSetController.loadAll
							},
							create : {
								dwrFunction : ExpressionExperimentSetController.create
							},
							update : {
								dwrFunction : ExpressionExperimentSetController.update
							},
							destroy : {
								dwrFunction : ExpressionExperimentSetController.remove
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
