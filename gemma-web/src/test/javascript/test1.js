describe("CoexVOUtil", function() {
   it("getEntityIds", function() {

      expect(Gemma.CoexVOUtil.getEntityIds([ {
         id : 1
      }, {
         id : 2
      }, {
         id : 3
      } ])).toEqual([ 1, 2, 3 ]);

   });

   it("getAllGeneIds", function() {

      expect(Gemma.CoexVOUtil.getAllGeneIds([ {
         id : 1,
         queryGene : {
            id : 10
         },
         foundGene : {
            id : 11
         }
      }, {
         id : 2,
         queryGene : {
            id : 10
         },
         foundGene : {
            id : 12
         }
      }, {
         id : 3,
         queryGene : {
            id : 14
         },
         foundGene : {
            id : 15
         }
      } ]).length).toEqual(5);

   });
});