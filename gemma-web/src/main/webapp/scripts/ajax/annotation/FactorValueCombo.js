Gemma.FactorValueCombo = Ext.extend(Ext.form.ComboBox, {

	displayField : "factorValueString",
	valueField : "factorValueId",
	editable : false,
	mode : "local",
	triggerAction : "all",

	initComponent : function() {

		var record = this.record || Gemma.FactorValueGrid.getRecord();

		this.store = new Ext.data.Store({
			proxy : new Ext.data.DWRProxy(ExperimentalDesignController.getFactorValues),
			reader : new Ext.data.ListRangeReader({
				id : "factorValueId"
			}, record),
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
