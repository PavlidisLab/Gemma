Ext.namespace('Gemma');

Gemma.Renderers = {

    dateTimeRenderer: function (value, metadata, record, rowIndex, colIndex, store) {
        if (value === '' || value === null) return "";
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
        if (value === '' || value === null) return "";
        try {
            var time = Date.parse(value);
            var options = {
                day: 'numeric',
                month: 'short',
                year: 'numeric'
            };
            var date = new Date(time);
            if (isNaN(date.getTime())) {  // d.valueOf() could also work
                //noinspection ExceptionCaughtLocallyJS
                throw "Trying to create a date from invalid string: " + value;
            }
            return date.toLocaleDateString(undefined, options);
        } catch (err) {
            //console.error(err);
            return "";
        }
    },

    troubleRenderer: function (value, metadata, record, rowIndex, colIndex, store) {
        if (record.get('troubled')) {
            var trouble = record.get('troubleDetails') ? record.get('troubleDetails') : "Trouble details unspecified";
            return '<i class="red fa fa-exclamation-triangle fa-lg" ext:qtip="' + trouble + '"></i>';
        }
    },

    curationRenderer: function (value, metadata, record, rowIndex, colIndex, store) {
        if (record.get('needsAttention')) {
            var note = record.get('curationNote') ? record.get('curationNote') : "Undergoing curation"; /* default message */
            return '<i class="gold fa fa-exclamation-circle fa-lg" ext:qtip="' + note + '"></i>';
        }
    },

    qualityRenderer: function (value, metadata, record, rowIndex, colIndex, store) {
        if (record.get('geeq')) {
            var geeq = record.get('geeq');
            return Gemma.GEEQ.getGeeqIconColored(geeq.publicQualityScore);
        }
    },

    suitabilityRenderer: function (value, metadata, record, rowIndex, colIndex, store) {
        if (record.get('geeq')) {
            var geeq = record.get('geeq');
            return Gemma.GEEQ.getGeeqIconColored(geeq.publicSuitabilityScore);
        }
    },

    curationNoteStubRenderer: function (value, metadata, record, rowIndex, colIndex, store) {
      //  if (record.get('needsAttention')) {
            var orig_note = record.get('curationNote') ? record.get('curationNote') : "";
            var note = orig_note;
            if (note.length > 50) {
                note = note.substring(0, 49) + "...";
            }
            return "<i ext:qtip='" + orig_note + "'>" + note + "</i>";
      //  }
    },

};