describe("ObservableCoexpressionSearchResults tests", function() {
    var searchResults;

    beforeEach(function(){
        searchResults = new Gemma.ObservableCoexpressionSearchResults();
    });

    describe("ObservableCoexpressionSearchResults test suite", function() {

        it("call search", function() {
            expect(searchResults.getCoexpressionPairs()).not.toBeNull();
        });

        it("searchResults object should be created", function() {
            expect(searchResults).not.toBeNull();
        });

    });

});