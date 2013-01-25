Ext.namespace('Gemma');

/**
 * Config options;
 *
 * @param taskId
 *            (required)
 * @param callback
 *            fired when finished, with the task payload as argument
 * @param showAllMessages
 *            when finished, show all the messages. Useful for complex jobs.
 * @class Gemma.ProgressWindow
 * @extends Ext.Window
 */
Gemma.ProgressWindow = Ext.extend(Ext.Window, {

    modal: true,
    closable: false,
    resizable: false,
    stateful: false,
    showAllMessages: false,
    collapsible: false,
    autoHeight: true,

    // autoWidth : true,
    width: 400,
    id: "progressWidget-window",

    initComponent: function () {

        this.progressBar = new Gemma.ProgressWidget({
            taskId: this.taskId
        });

        Ext.apply(this, {
            items: [this.progressBar]
        });

        Gemma.ProgressWindow.superclass.initComponent.call(this);

        this.addEvents('done', 'fail');

        this.on('show', this.start, this);

        this.relayEvents(this.progressBar, ['done', 'fail']);
    },

    start: function () {
        this.progressBar.on('done', function (payload) {
            if (this.callback) {
                this.callback(payload);
            }
            this.destroy();
        }, this);

        this.progressBar.on('fail', function (message) {
            if (this.errorHandler) {
                this.errorHandler(message);
            }
            //this.destroy();
        }, this);

        this.progressBar.on('cancel', function (successfullyCancelled) {
            if (this.errorHandler) {
                if (successfullyCancelled) {
                    this.errorHandler("Job was cancelled");
                } else {
                    this.errorHandler("Could not be cancelled?");
                }
            }
            //this.destroy();
        }, this);

        this.progressBar.startProgress();
    }

//    afterRender: function (a, b) {
//        Gemma.ProgressWindow.superclass.afterRender.call(this, a, b);
//
//        this.progressBar.on('done', function (payload) {
//            this.getEl().switchOff(
//                {
//                callback: function () {
//                    if (this.showAllMessages && this.progressBar.allMessages.length > 0) {
//                        this.progressBar.showAllMessages("Job completed normally");
//                    }
//                    this.destroy();
//                },
//                scope: this
//            }
//            );
//        }.createDelegate(this));
//
//        this.progressBar.on('fail', function (message) {
//            this.progressBar.allMessages = message + "<br/><br/>Other messages:<br/>" + this.progressBar.allMessages;
//            this.progressBar.showLogMessages("Job failed!");
//            this.destroy();
//        }.createDelegate(this));
//
//        this.progressBar.on('cancel', function (successfullyCancelled) {
//            this.destroy();
//        }.createDelegate(this));
//    }

});

/**
 * Config options:
 *
 * @param taskId
 *            (required)
 * @class Gemma.ProgressWidget
 * @extends Ext.Panel
 */
Gemma.ProgressWidget = Ext.extend(Ext.Panel, {
    BAR_UPDATE_INTERVAL: 2000,
    refreshTaskProgressIntervalID: null,

    waitingForReply: false,
    noEmailOption: false,

    allMessages: "",
    previousMessage: '',

    // layout : 'fit',
    resizable: false,
    bodyBorder: false,
    stateful: false,

    id: "progressWidget-panel",

    //TODO: probably shouldn't have side effects?
    showLogMessages: function () {
        if (!this.allMessages) {
            return;
        }

        // TODO don't use 1, make it more clear
        this.insert(1, new Ext.form.Label({
            id: "logsProgressWidget",
            html: this.allMessages
        }));

        this.doLayout();

        this.on('message-received', function () {
            Ext.DomHelper.overwrite('logsProgressWidget', {
                id: 'logsProgressWidget',
                html: this.allMessages
            });
        });
    },

    hideLogMessages: function() {
        this.remove(1, true);
    },

    initComponent: function () {
        this.progressBar = new Ext.ProgressBar({
            width: 400,
            text: "Initializing ..."
        });

        Ext.apply(this, {
            items: [this.progressBar],
            buttons: [
                {
                    text: "Logs",
                    id: "progresslogsbutton",
                    tooltip: "Show log messages",
                    enableToggle: true,
                    toggleHandler: function (button, enabled) {
                        if (enabled) {
                            this.showLogMessages();
                        } else {
                            this.hideLogMessages();
                        }
                    },
                    scope: this
                },
                {
                    text: "Cancel Job",
                    id: "progresscancelbutton",
                    tooltip: "Attempt to stop the job.",
                    handler: function () {
                        Ext.Msg.show({
                            title: 'Cancel?',
                            msg: 'Are you sure?',
                            buttons: Ext.Msg.YESNO,
                            fn: function (btn) {
                                if (btn === 'yes') {
                                    this.cancelJob();
                                }
                            }.createDelegate(this),
                            icon: Ext.MessageBox.QUESTION
                        });
                    },
                    scope: this
                },
                {
                    text: "Hide",
                    id: "progresshidebutton",
                    tooltip: "Remove the progress bar and return to the page",
                    handler: function () {
                        /*
                         * FIXME add link to job listing
                         */
                        Ext.Msg.show({
                            title: "Discontinuing monitoring",
                            msg: "The job will continue to run. You can get an email on completion. ",
                            buttons: {
                                ok: 'OK',
                                cancel: 'Email me'
                            },
                            fn: function (btn) {
                                if (btn === 'cancel') {
                                    ProgressStatusService.addEmailAlert(this.taskId);
                                }
                            },
                            scope: this
                        });

                        this.stopProgress();
                        if (this.ownerCt) { // ugly.
                            this.ownerCt.destroy();
                        } else {
                            this.destroy();
                        }

                    },
                    scope: this
                }
            ]
        });

        Gemma.ProgressWidget.superclass.initComponent.call(this);

        Ext.apply(this);

        this.addEvents('done', 'fail', 'cancel', 'message-received');
    },

    /**
     * Handle failure of the server to respond to a check.
     *
     * @param {}
     *            data
     * @param {}
     *            e
     */
    handleResponseFailure: function (data, e) {
        /*
         * keep going. Hopefully just a temporary network lapse.
         */
        this.waitingForReply = false;
        this.progressBar.updateText("Waiting...");
    },

    /**
     * Handle failure of a job.
     *
     * @param {}
     *            data
     * @param {}
     *            e
     */
    handleFailure: function (data, e) {
        this.stopProgress();
        var messageText = "";
        if (data.description) {
            messageText = data.description;
        } else if (!e) {
            messageText = data;
        } else if (e.message) {
            messageText = e.message;
        } else {
            messageText = e;
        }
        this.fireEvent("fail", messageText);
    },

    /**
     * Start the progressbar in motion.
     */
    startProgress: function () {
        /*
         * Don't start twice.
         */
        if (this.waitingForReply) {
            return;
        }
        this.progressBar.wait({
            text: "Starting ..."
        });

        if (!this.taskId) {

            var taskId = this.findTaskId();

            if (!taskId) {
                alert("no task id");
                return;
            }

            this.taskId = taskId;

        }

        var callParams = [];
        var callback = this.updateProgress.createDelegate(this);
        var errorHandler = this.handleResponseFailure.createDelegate(this);
        callParams.push(callback);
        callParams.push(errorHandler);

        /*
         * Start it running on a delay.
         */

        var refreshTaskProgressFn = this.refreshProgress.createDelegate( this, callParams, false );
        this.refreshTaskProgressIntervalID = window.setInterval( refreshTaskProgressFn, this.BAR_UPDATE_INTERVAL );
    },

    stopProgress: function () {
        window.clearInterval( this.refreshTaskProgressIntervalID );
        this.previousMessage = null;
        this.waitingForReply = false;
        this.progressBar.reset();
    },

    handleFinalResult: function (result) {
        this.stopProgress();
        this.fireEvent('done', result);
    },

    done: false,

    updateProgress: function (data) {

        // for testing: simulate a network failure.
        // if (Math.random() < 0.3 ) {
        // this.handleResponseFailure();
        // this.le = 0.0;
        // return;
        // }

        this.waitingForReply = false;
        var messages = "";
        var messagesToSave = '';

        while (data.length > 0) {
           var progressUpdateItem = data.shift();

            /*
             * Only show the most recent message in the progress bar.
             */
            messages = progressUpdateItem.description;

            /*
             * But put all messages in the logs.
             */
            messagesToSave = messagesToSave + progressUpdateItem.description + "<br/>";

            if (progressUpdateItem.failed) {
                return this.handleFailure(progressUpdateItem);
            } else if (progressUpdateItem.done && !this.done) {
                // try to ensure we only call this once.
                TaskCompletionController.checkResult(this.taskId, {
                    callback: this.handleFinalResult.createDelegate(this),
                    errorHandler: this.handleFailure.createDelegate(this)
                });
                this.done = true;
            }
        }

        if (this.previousMessage !== messages && messages.length > 0) {
            this.allMessages = this.allMessages + messagesToSave;

            // chop them if they are too long
            messages = Ext.util.Format.ellipsis(messages, 70);

            this.previousMessage = messages;

            this.progressBar.updateText(messages);

            this.fireEvent('message-received', messages);
        } else {
            /*
             * Just show dots to make it clear stuff is still happening.
             */
            this.progressBar.updateText(this.progressBar.text.replace('.......', ''));
            this.progressBar.updateText(this.progressBar.text + '.');
        }
    },

    /*
     * Private Send a cancel notification to the server.
     */
    cancelJob: function () {
        var f = this.cancelCallback.createDelegate(this);
        ProgressStatusService.cancelJob(this.taskId, f);
    },

    /*
     * Private Check for status from server.
     */
    refreshProgress: function (callback, errorHandler) {
        // only check for status if we aren't already waiting for a reply.
        if (!this.waitingForReply) {
            ProgressStatusService.getProgressStatus(this.taskId, {
                callback: callback,
                errorHandler: errorHandler
            });
            this.waitingForReply = true;
        }
    },

    /*
     * Private callback to handle cancellation @param {Object} data
     */
    cancelCallback: function (successfullyCancelled) {

        this.stopProgress();
        this.fireEvent('cancel', successfullyCancelled);

        if (!successfullyCancelled) {
            Ext.Msg.alert("Couldn't cancel",
                "Sorry, the job couldn't be cancelled; perhaps it finished or was cancelled already?");
        } else {
            this.showLogMessages("Job was cancelled");
        }
    },

    findTaskId: function () {
        // try to get from query string

        var queryStart = document.URL.indexOf("?");
        if (queryStart > -1) {
            var param = Ext.urlDecode(document.URL.substr(queryStart + 1));
            if (param.taskId) {
                return param.taskId;
            }
        }

        // try to get from hidden input field.
        return dwr.util.getValue("taskId");
    },

    hideLogsButton: function () {
        Ext.getCmp("progresslogsbutton").setVisible( false );
    },

    hideHideButton: function () {
        Ext.getCmp("progresshidebutton").setVisible( false );
    }

});