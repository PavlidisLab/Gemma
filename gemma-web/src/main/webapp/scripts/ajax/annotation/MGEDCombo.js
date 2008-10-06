Ext.namespace("Gemma");

/**
 * 
 * @class Gemma.MGEDCombo
 * @extends Ext.form.ComboBox
 */
Gemma.MGEDCombo = Ext.extend(Ext.form.ComboBox, {

			editable : true,
			mode : 'local',
			selectOnFocus : true,
			triggerAction : 'all',
			typeAhead : true,
			forceSelection : true,
			displayField : 'term',

			record : Ext.data.Record.create([{
						name : "uri"
					}, {
						name : "term"
					}, {
						name : "comment"
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
										id : "uri"
									}, this.record),
							remoteSort : false,
							sortInfo : {
								field : "term"
							}
						});

				Gemma.MGEDCombo.superclass.initComponent.call(this);

				// this.tpl = new Ext.XTemplate('<tpl for="."><div ext:qtip="{comment}<br/>{uri}"
				// class="x-combo-list-item">{term}</div></tpl>');
				// this.tpl.compile();

				this.on("select", function(combo, record, index) {
							this.selectedTerm = record.data;
						});

				this.store.load({
							params : this.dwrParams
						});
			}
		});
