/**
 * 
 */
Gemma.ExperimentalFactorCombo = Ext.extend(Ext.form.ComboBox, {

			// fixme only show disc.
			// if it differs from
			// name.
			displayField : 'name',
			tpl : new Ext.XTemplate('<tpl for="."><div class="x-combo-list-item">{name}  ({description})</div></tpl>'),
			listWidth : 250,

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

			valueField : "id",
			editable : false,
			mode : "local",
			triggerAction : "all",

			// http://extjs.com/forum/showthread.php?p=177623
			onRender : function(ct, position) {
				Gemma.ExperimentalFactorCombo.superclass.onRender.call(this, ct, position);

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
				Gemma.ExperimentalFactorCombo.superclass.onResize.call(this, w, h);
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
