Ext.namespace("Gemma");

/**
 * Dropdown menu of categories to use in annotations (previously known as MGEDCombo)
 *
 * @class Gemma.CategoryCombo
 * @extends Ext.form.ComboBox
 */
Gemma.CategoryCombo = Ext.extend(Ext.form.ComboBox, {

    editable: true,
    mode: 'local',
    selectOnFocus: true,
    triggerAction: 'all',
    typeAhead: true,
    forceSelection: true,
    displayField: 'term',

    record: Ext.data.Record.create([{
        name: "uri"
    }, {
        name: "term"
    }, {
        name: "comment"
    }]),

    getTerm: function () {
        return this.selectedTerm;
    },

    initComponent: function () {

        this.dwrMethod = AnnotationController.getCategoryTerms;
        this.dwrParams = [];

        this.store = new Ext.data.Store({
            proxy: new Ext.data.DWRProxy(this.dwrMethod),
            reader: new Ext.data.ListRangeReader({
                id: "uri"
            }, this.record),
            remoteSort: false,
            sortInfo: {
                field: "term"
            }
        });

        Gemma.CategoryCombo.superclass.initComponent.call(this);

        this.on("change", function (combo) {
            if(combo.value){
                this.selectedTerm = combo.store.data.items[combo.selectedIndex].data;
                combo.setValue(this.selectedTerm.term + "\t");
            }else{
                this.selectedTerm = undefined;
            }
        });

        // Otherwise the combo is only firing this event after losing focus
        this.on("select", function (combo, record, index) {
            this.fireEvent("change", combo);
        });

        this.store.load({
            params: this.dwrParams
        });
    }
});
