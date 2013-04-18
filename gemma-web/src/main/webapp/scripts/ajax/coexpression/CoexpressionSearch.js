Ext.namespace("Gemma");

Gemma.CoexpressionSearch = {

    /**
     * @param searchCommand
     *
     * @return {ObservableCoexpressionSearchResults}
     */
    search: function (searchCommand) {
        var observableSearchResults;

        ExtCoexpressionSearchController.doBackgroundCoexSearch( searchCommand,
            {
                callback: function (taskId) {
                    var task = new Gemma.ObservableSubmittedTask({'taskId': taskId});
                    task.showTaskProgressWindow({});
                    Ext.getBody().unmask();
                    task.on('task-completed', this.onCoexpressionSearchCompletion, this);
                    task.on('task-failed', this.onCoexpressionSearchFailure, this);
                    task.on('task-cancelling', this.onCoexpressionSearchFailure, this);
                }.createDelegate(this),
                errorHandler: this.timeoutFromCoexSearch.createDelegate(this) // sometimes got triggered without timeout
            }
        );
    }
};
