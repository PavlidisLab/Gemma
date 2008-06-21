/**
 * 
 */
Gemma.ExperimentalFactorCombo = Ext.extend(Ext.form.ComboBox, {

	record : Ext.data.Record.create([{
		name : "id",
		type : "int"
	}, {
		name : "name",
		type : "string"
	}, {
		name : "description",
		type : "string"
	}, {
		name : "category",
		type : "string"
	}, {
		name : "categoryUri",
		type : "string"
	}]),

	displayField : "name",
	valueField : "id",
	editable : "false",
	mode : "local",
	triggerAction : "all",
	initComponent : function() {

		this.experimentalDesign = {
			id : this.edId,
			classDelegatingFor : "ExperimentalDesign"
		};

		Ext.apply(this, {
			store : new Ext.data.Store({
				proxy : new Ext.data.DWRProxy(ExperimentalDesignController.getExperimentalFactors),
				reader : new Ext.data.ListRangeReader({
					id : "id"
				}, this.record),
				remoteSort : false,
				sortInfo : {
					field : "name"
				}
			})
		});

		Gemma.ExperimentalFactorCombo.superclass.initComponent.call(this);

		this.store.load({
			params : [this.experimentalDesign]
		});
	}

});
