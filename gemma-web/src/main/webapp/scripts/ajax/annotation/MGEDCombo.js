Ext.namespace("Gemma");

/**
 * 
 */
Gemma.MGEDCombo = Ext.extend(Ext.form.ComboBox, {

	editable : false,
	mode : 'local',
	selectOnFocus : true,
	triggerAction : 'all',
	typeAhead : true,

	record : Ext.data.Record.create([{
		name : "id",
		type : "int"
	}, {
		name : "uri",
		type : "string"
	}, {
		name : "term",
		type : "string"
	}]),

	getTerm : function() {
		return this.selectedTerm;
	},

	initComponent : function() {
		if (this.termKey) {
			this.dwrMethod = MgedOntologyService.getMgedTermsByKey;
			this.dwrParams = [this.termKey];
		} else {
			this.dwrMethod = MgedOntologyService.getUsefulMgedTerms;
			this.dwrParams = [];
		}

		this.store = new Ext.data.Store({
			proxy : new Ext.data.DWRProxy(this.dwrMethod),
			reader : new Ext.data.ListRangeReader({
				id : "id"
			}, this.record),
			remoteSort : false,
			sortInfo : {
				field : "term"
			}
		});

		Gemma.MGEDCombo.superclass.initComponent.call(this);

		this.store.load({
			params : this.dwrParams
		});

		this.on("select", function(combo, record, index) {
			this.selectedTerm = record.data;
		});
	}
});
