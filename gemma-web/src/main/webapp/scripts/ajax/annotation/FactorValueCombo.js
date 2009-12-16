/**
 * Combobox to show factor values for a given factor. The factor values can be loaded from the server or passed in
 * directly from JSON (JsonReader, not ArrayReader)
 * 
 * @class Gemma.FactorValueCombo
 * @extends Ext.form.ComboBox
 */
Gemma.FactorValueCombo = Ext.extend(Ext.form.ComboBox, {

			displayField : "factorValue",
			valueField : "id",
			editable : false,
			mode : "local",
			triggerAction : "all",
			listWidth : 200,

			record : Ext.data.Record.create([{
						name : "charId",
						type : "int"
					}, {
						name : "id",
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
						name : "factorValue",
						type : "string"
					}]),

			initComponent : function() {

				/*
				 * Option to pass in data directly from JSON (JsonReader, not ArrayReader)
				 */
				if (this.data) {

					this.store = new Ext.data.Store({
								proxy : new Ext.data.MemoryProxy(this.data),
								reader : new Ext.data.JsonReader({}, this.record)
							});
				} else {
					this.store = new Ext.data.Store({
								proxy : new Ext.data.DWRProxy(ExperimentalDesignController.getFactorValues),
								reader : new Ext.data.ListRangeReader({
											id : "id"
										}, this.record),
								remoteSort : false,
								sortInfo : {
									field : "id"
								}
							});
				}

				Gemma.FactorValueCombo.superclass.initComponent.call(this);

				// this.store.on("load", function() {
				// console.log(this.store);
				// }.createDelegate(this));

				if (this.efId) {
					this.store.load({
								params : [{
											id : this.efId,
											classDelegatingFor : "ExperimentalFactor"
										}]
							});
				} else {
					this.store.load();
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
