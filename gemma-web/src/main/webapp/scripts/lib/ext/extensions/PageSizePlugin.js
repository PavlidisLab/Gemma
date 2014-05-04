/**
 * A plugin for PagingToolbar that allows you to change the pageSize property using a combo box 
 * with some predefined options. This was developed using Ext 2.0a
 * 
 * http://www.sencha.com/forum/showthread.php?14426-PageSize-plugin-for-PagingToolbar&s=d267c316d10692a95c6ed95a6dec6e29
 */

Ext.namespace('Ext.ux');

Ext.ux.PageSizePlugin = function() {
    Ext.ux.PageSizePlugin.superclass.constructor.call(this, {
        store: new Ext.data.SimpleStore({
            fields: ['text', 'value'],
            data: [['10', 10], ['20', 20], ['30', 30], ['50', 50], ['100', 100]]
        }),
        mode: 'local',
        displayField: 'text',
        valueField: 'value',
        editable: false,
        allowBlank: false,
        triggerAction: 'all',
        width: 40
    });
};

Ext.extend(Ext.ux.PageSizePlugin, Ext.form.ComboBox, {
    init: function(paging) {
        paging.on('render', this.onInitView, this);
    },
    
    onInitView: function(paging) {
        paging.add('-',
            this,
            'Items per page'
        );
        this.setValue(paging.pageSize);
        this.on('select', this.onPageSizeChanged, paging);
    },
    
    onPageSizeChanged: function(combo) {
        this.pageSize = parseInt(combo.getValue());
        this.doLoad(0);
    }
});