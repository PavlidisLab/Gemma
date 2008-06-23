Gemma.FactorValueCombo = Ext.extend(Ext.form.ComboBox, {

	displayField : "factorValueString",
	valueField : "factorValueId",
	editable : false,
	mode : "local",
	triggerAction : "all",

record : Ext.data.Record.create([{
		name : "charId",
		type : "int"
	}, {
		name : "factorValueId",
		type : "int"
	}, {
		name : "category",
		type : "string"
	}, {
		name : "categoryUri",
		type : "string"
	}, {
		name : "value",
		type : "string"
	}, {
		name : "measurement",
		type : "bool"
	}, {
		name : "valueUri",
		type : "string"
	}, {
		name : "factorValueString",
		type : "string"
	}]),
	
	initComponent : function() {
 
		this.store = new Ext.data.Store({
			proxy : new Ext.data.DWRProxy(ExperimentalDesignController.getFactorValues),
			reader : new Ext.data.ListRangeReader({
				id : "factorValueId"
			}, this.record),
			remoteSort : false,
			sortInfo : {
				field : "factorValueId"
			}
		});

		Gemma.FactorValueCombo.superclass.initComponent.call(this);

		if (this.experimentalFactor) {
			this.store.load({
				params : [{
					id : this.experimentalFactor.id,
					classDelegatingFor : "ExperimentalFactor"
				}]
			});
		}

		this.on("select", function(combo, record, index) {
			this.selectedValue = record.data;
		});

	},

	setExperimentalFactor : function(efId, callback) {
		var options = {
			params : [{
				id : efId,
				classDelegatingFor : "ExperimentalFactor"
			}]
		};
		if (callback) {
			options.callback = callback;
		}
		this.store.load(options);
	},

	getFactorValue : function() {
		return this.selectedValue;
	}
});
