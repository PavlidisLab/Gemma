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

			// http://extjs.com/forum/showthread.php?p=177623
			onRender : function(ct, position) {
				Gemma.FactorValueCombo.superclass.onRender.call(this, ct, position);

				this.wrap.setWidth = this.wrap.setWidth.createInterceptor(function(width) {
							if (width && width * 1 > 0) {
								return true;
							} else {
								return false;
							}
						});

			},

			// http://extjs.com/forum/showthread.php?p=177623
			onResize : function(w, h) {
				Gemma.FactorValueCombo.superclass.onResize.call(this, w, h);
				if (this.trigger.isDisplayed()) {
					var realWidth = this.trigger.getWidth() == 0 ? (w - 20) : w - this.trigger.getWidth();
				} else {
					var realWidth = this.trigger.getWidth() == 0 ? (w - 5) : w - this.trigger.getWidth();
				}
				if (typeof w == 'number') {
					this.el.setWidth(this.adjustWidth('input', realWidth));
				}
			},

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
