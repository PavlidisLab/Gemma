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
        if (record.get('troubled')) {
            var trouble = record.get('troubleDetails') ? record.get('troubleDetails') : "Trouble details unspecified";
            return '<i class="red fa fa-exclamation-triangle fa-lg" ext:qtip="' + trouble + '"></i>';
        }
    },

    curationRenderer: function (value, metadata, record, rowIndex, colIndex, store) {
        if (record.get('needsAttention')) {
            var note = record.get('curationNote') ? record.get('curationNote') : "Curation note empty";
            return '<i class="gold fa fa-exclamation-circle fa-lg" ext:qtip="' + note + '"></i>';
        }
    }

};
