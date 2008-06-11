/**
 * 
 */
Gemma.ExperimentalFactorCombo = Ext.extend(Ext.form.ComboBox, {

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
				}, Gemma.ExperimentalFactorGrid.getRecord()),
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
