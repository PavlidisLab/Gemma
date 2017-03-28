Ext.namespace('Gemma');

Gemma.Renderers = {

    dateTimeRenderer: function (value, metadata, record, rowIndex, colIndex, store) {
        var time = Date.parse(value);
        var options = {
            day: 'numeric',
            month: 'short',
            year: 'numeric',
            hour: 'numeric',
            minute: 'numeric',
            second: 'numeric'
        };
        return new Date(time).toLocaleDateString(undefined, options);
    },

    dateRenderer: function (value, metadata, record, rowIndex, colIndex, store) {
        var time = Date.parse(value);
        var options = {
            day: 'numeric',
            month: 'short',
            year: 'numeric'
        };
        return new Date(time).toLocaleDateString(undefined, options);
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
    },

    curationNoteStubRenderer: function (value, metadata, record, rowIndex, colIndex, store) {
        if (record.get('needsAttention')) {
            var note = record.get('curationNote') ? record.get('curationNote') : "";
            if (note.length > 50) {
                note = note.substring(0, 49) + "...";
            }
            return note;
        }
    }

};
