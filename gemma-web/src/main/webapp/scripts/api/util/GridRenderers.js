Ext.namespace('Gemma');

Gemma.GridRenderers = {

    dateTimeRenderer: function (value, metadata, record, rowIndex, colIndex, store) {
        var time = Date.parse(value);
        return new Date(time).toLocaleString();
    },

    dateRenderer: function (value, metadata, record, rowIndex, colIndex, store) {
        var time = Date.parse(value);
        return new Date(time).toLocaleDateString();
    },

    troubleRenderer: function (value, metadata, record, rowIndex, colIndex, store) {
        var id = record.get('id');
        var result = '';

        if (record.get('troubled')) {
            result = '<i class="red fa fa-exclamation-triangle fa-lg" ext:qtip="trouble: '
                + record.get('troubleDetails') + '"/>';
        }

        return result;
    },

    curationRenderer: function (value, metadata, record, rowIndex, colIndex, store) {
        var id = record.get('id');
        var result = '';

        if (record.get('needsAttention')) {
            result = '<i class="gold fa fa-exclamation-circle fa-lg" ext:qtip="'
                + record.get('curationNote') + '"/>';
        }

        return result;
    }

};
