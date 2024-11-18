/* this code is generated, see generate-dwr-client.sh for details */

if (typeof SimpleTreeValueObject != "function") {
  function SimpleTreeValueObject() {
    this._parent = null;
    this.children = null;
    this.dbPhenotype = false;
    this.publicGeneCount = 0;
    this.valueUri = null;
    this._id = null;
    this._is_leaf = false;
    this.urlId = null;
    this.privateGeneCount = 0;
    this.value = null;
  }
}

if (typeof ArrayDesignValueObjectExt != "function") {
  function ArrayDesignValueObjectExt() {
    this.hasSequenceAssociations = null;
    this.isMergee = null;
    this.isMerged = null;
    this.technologyType = null;
    this.numGenes = null;
    this.lastUpdated = null;
    this.noParentsAnnotationLink = null;
    this.allParentsAnnotationLink = null;
    this.hasBlatAssociations = null;
    this.id = null;
    this.isSubsumer = null;
    this.externalReferences = null;
    this.dateCached = null;
    this.curationNote = null;
    this.lastNeedsAttentionEvent = null;
    this.mergees = null;
    this.lastSequenceUpdate = null;
    this.name = null;
    this.isAffymetrixAltCdf = null;
    this.lastGeneMapping = null;
    this.shortName = null;
    this.needsAttention = null;
    this.subsumees = null;
    this.color = null;
    this.description = null;
    this.lastRepeatMask = null;
    this.lastTroubledEvent = null;
    this.taxonObject = null;
    this.expressionExperimentCount = null;
    this.troubled = null;
    this.lastNoteUpdateEvent = null;
    this.subsumer = null;
    this.colorString = null;
    this.createDate = null;
    this.bioProcessAnnotationLink = null;
    this.releaseVersion = null;
    this.alternative = null;
    this.isSubsumed = null;
    this.numProbeAlignments = null;
    this.blackListed = null;
    this.hasGeneAssociations = null;
    this.switchedExpressionExperimentCount = null;
    this.numProbeSequences = null;
    this.alternateNames = null;
    this.designElementCount = null;
    this.lastSequenceAnalysis = null;
    this.numProbesToGenes = null;
    this.releaseUrl = null;
    this.merger = null;
  }
}

if (typeof PhenotypeAssPubValueObject != "function") {
  function PhenotypeAssPubValueObject() {
    this.citationValueObject = null;
    this.type = null;
  }
}

if (typeof GeneValueObject != "function") {
  function GeneValueObject() {
    this.associatedExperimentCount = null;
    this.aliases = null;
    this.homologues = null;
    this.nodeDegreeNegRanks = [];
    this.description = null;
    this.nodeDegreePosRanks = [];
    this.officialName = null;
    this.numGoTerms = null;
    this.score = null;
    this.platformCount = null;
    this.taxon = null;
    this.id = null;
    this.geneSets = null;
    this.multifunctionalityRank = null;
    this.ncbiId = null;
    this.ensemblId = null;
    this.includeTaxon = false;
    this.isQuery = null;
    this.compositeSequenceCount = null;
    this.nodeDegreesNeg = [];
    this.phenotypes = null;
    this.nodeDegreesPos = [];
    this.accessions = null;
    this.name = null;
    this.officialSymbol = null;
  }
}

if (typeof SessionBoundGeneSetValueObject != "function") {
  function SessionBoundGeneSetValueObject() {
    this.userCanWrite = false;
    this.userOwned = false;
    this.name = null;
    this.description = null;
    this.isPublic = false;
    this.modified = false;
    this.taxon = null;
    this.geneIds = null;
    this.id = null;
    this.isShared = false;
  }
}

if (typeof GeeqValueObject != "function") {
  function GeeqValueObject() {
    this.batchCorrected = false;
    this.corrMatIssues = 0;
    this.noVectors = false;
    this.id = null;
    this.publicQualityScore = 0;
    this.publicSuitabilityScore = 0;
    this.replicatesIssues = 0;
  }
}

if (typeof DiffExpressionEvidenceValueObject != "function") {
  function DiffExpressionEvidenceValueObject() {
    this.geneOfficialSymbol = null;
    this.geneDifferentialExpressionMetaAnalysisId = null;
    this.geneNCBI = null;
    this.description = null;
    this.className = null;
    this.evidenceSource = null;
    this.selectionThreshold = null;
    this.phenotypeMapping = null;
    this.originalPhenotype = null;
    this.phenotypeAssPubVO = null;
    this.lastUpdated = null;
    this.geneId = null;
    this.taxonCommonName = null;
    this.geneDifferentialExpressionMetaAnalysisSummaryValueObject = null;
    this.upperTail = null;
    this.evidenceCode = null;
    this.id = null;
    this.meanLogFoldChange = null;
    this.metaPvalue = null;
    this.relationship = null;
    this.homologueEvidence = false;
    this.metaPvalueRank = null;
    this.evidenceSecurityValueObject = null;
    this.isNegativeEvidence = false;
    this.metaQvalue = null;
    this.geneOfficialName = null;
    this.phenotypes = null;
    this.geneDifferentialExpressionMetaAnalysisResultId = null;
    this.numEvidenceFromSameMetaAnalysis = null;
    this.scoreValueObject = null;
    this.containQueryPhenotype = false;
  }
}

if (typeof ExpressionExperimentSetValueObject != "function") {
  function ExpressionExperimentSetValueObject() {
    this.userCanWrite = false;
    this.userOwned = false;
    this.taxonName = null;
    this.description = null;
    this.numWithCoexpressionAnalysis = null;
    this.expressionExperimentIds = null;
    this.taxonId = null;
    this.numWithDifferentialExpressionAnalysis = null;
    this.size = null;
    this.name = null;
    this.isPublic = false;
    this.id = null;
    this.modifiable = false;
    this.isShared = false;
  }
}

if (typeof GeeqAdminValueObject != "function") {
  function GeeqAdminValueObject() {
    this.manualHasStrongBatchEffect = false;
    this.batchCorrected = false;
    this.manualHasNoBatchEffect = false;
    this.manualBatchConfoundActive = false;
    this.QScoreBatchConfound = 0;
    this.detectedSuitabilityScore = 0;
    this.noVectors = false;
    this.publicQualityScore = 0;
    this.publicSuitabilityScore = 0;
    this.manualQualityScore = 0;
    this.manualSuitabilityScore = 0;
    this.QScoreBatchEffect = 0;
    this.manualHasBatchConfound = false;
    this.corrMatIssues = 0;
    this.id = null;
    this.manualBatchEffectActive = false;
    this.manualSuitabilityOverride = false;
    this.replicatesIssues = 0;
    this.otherIssues = null;
    this.manualQualityOverride = false;
    this.detectedQualityScore = 0;
  }
}

if (typeof LiteratureEvidenceValueObject != "function") {
  function LiteratureEvidenceValueObject() {
    this.geneOfficialSymbol = null;
    this.homologueEvidence = false;
    this.geneNCBI = null;
    this.description = null;
    this.evidenceSecurityValueObject = null;
    this.className = null;
    this.evidenceSource = null;
    this.isNegativeEvidence = false;
    this.phenotypeMapping = null;
    this.geneOfficialName = null;
    this.originalPhenotype = null;
    this.phenotypeAssPubVO = null;
    this.lastUpdated = null;
    this.phenotypes = null;
    this.geneId = null;
    this.taxonCommonName = null;
    this.evidenceCode = null;
    this.id = null;
    this.scoreValueObject = null;
    this.relationship = null;
    this.containQueryPhenotype = false;
  }
}

if (typeof PhenotypeGroupValueObject != "function") {
  function PhenotypeGroupValueObject() {
    this.userCanWrite = false;
    this.userOwned = false;
    this.description = null;
    this.phenotypeCategory = null;
    this.phenotypeName = null;
    this.searchTerm = null;
    this.name = null;
    this.isPublic = false;
    this.modified = false;
    this.taxon = null;
    this.geneIds = null;
    this.id = null;
    this.isShared = false;
  }
}

if (typeof ExpressionExperimentValueObject != "function") {
  function ExpressionExperimentValueObject() {
    this.needsAttention = null;
    this.metadata = null;
    this.experimentalDesign = null;
    this.numberOfBioAssays = null;
    this.userOwned = false;
    this.technologyType = null;
    this.description = null;
    this.accession = null;
    this.externalDatabase = null;
    this.source = null;
    this.batchConfound = null;
    this.processedExpressionVectorCount = null;
    this.currentUserIsOwner = null;
    this.lastUpdated = null;
    this.bioMaterialCount = null;
    this.lastTroubledEvent = null;
    this.taxonObject = null;
    this.isPublic = false;
    this.troubled = null;
    this.id = null;
    this.currentUserHasWritePermission = null;
    this.lastNoteUpdateEvent = null;
    this.suitableForDEA = null;
    this.batchEffect = null;
    this.characteristics = null;
    this.externalUri = null;
    this.userCanWrite = false;
    this.curationNote = null;
    this.externalDatabaseUri = null;
    this.lastNeedsAttentionEvent = null;
    this.arrayDesignCount = null;
    this.name = null;
    this.geeq = null;
    this.shortName = null;
    this.batchEffectStatistics = null;
    this.isShared = false;
  }
}

if (typeof DatabaseBackedGeneSetValueObject != "function") {
  function DatabaseBackedGeneSetValueObject() {
    this.userCanWrite = false;
    this.userOwned = false;
    this.name = null;
    this.description = null;
    this.isPublic = false;
    this.taxon = null;
    this.geneIds = null;
    this.id = null;
    this.isShared = false;
  }
}

if (typeof BlacklistedValueObject != "function") {
  function BlacklistedValueObject() {
    this.reason = null;
    this.name = null;
    this.accession = null;
    this.externalDatabase = null;
    this.id = null;
    this.shortName = null;
    this.type = null;
  }
}

if (typeof SessionBoundExpressionExperimentSetValueObject != "function") {
  function SessionBoundExpressionExperimentSetValueObject() {
    this.userCanWrite = false;
    this.userOwned = false;
    this.taxonName = null;
    this.description = null;
    this.numWithCoexpressionAnalysis = null;
    this.expressionExperimentIds = null;
    this.taxonId = null;
    this.numWithDifferentialExpressionAnalysis = null;
    this.size = null;
    this.name = null;
    this.isPublic = false;
    this.modified = false;
    this.id = null;
    this.modifiable = false;
    this.isShared = false;
  }
}

if (typeof FreeTextExpressionExperimentResultsValueObject != "function") {
  function FreeTextExpressionExperimentResultsValueObject() {
    this.userCanWrite = false;
    this.userOwned = false;
    this.taxonName = null;
    this.description = null;
    this.numWithCoexpressionAnalysis = null;
    this.expressionExperimentIds = null;
    this.queryString = null;
    this.taxonId = null;
    this.numWithDifferentialExpressionAnalysis = null;
    this.size = null;
    this.name = null;
    this.isPublic = false;
    this.modified = false;
    this.id = null;
    this.modifiable = false;
    this.isShared = false;
  }
}

if (typeof ScoreValueObject != "function") {
  function ScoreValueObject() {
    this.strength = null;
    this.scoreName = null;
    this.scoreValue = null;
  }
}

if (typeof PhenotypeValueObject != "function") {
  function PhenotypeValueObject() {
    this.valueUri = null;
    this.value = null;
  }
}

if (typeof ExternalDatabaseStatisticsValueObject != "function") {
  function ExternalDatabaseStatisticsValueObject() {
    this.pathToDownloadFile = null;
    this.webUri = null;
    this.lastUpdateDate = null;
    this.numPhenotypes = null;
    this.name = null;
    this.description = null;
    this.numGenes = null;
    this.numEvidence = null;
    this.numPublications = null;
  }
}

if (typeof GeneSetValueObject != "function") {
  function GeneSetValueObject() {
    this.userCanWrite = false;
    this.userOwned = false;
    this.name = null;
    this.description = null;
    this.isPublic = false;
    this.taxon = null;
    this.geneIds = null;
    this.id = null;
    this.isShared = false;
  }
}

if (typeof EvidenceValueObject != "function") {
  function EvidenceValueObject() {
    this.geneOfficialSymbol = null;
    this.homologueEvidence = false;
    this.geneNCBI = null;
    this.description = null;
    this.evidenceSecurityValueObject = null;
    this.className = null;
    this.evidenceSource = null;
    this.isNegativeEvidence = false;
    this.phenotypeMapping = null;
    this.geneOfficialName = null;
    this.originalPhenotype = null;
    this.phenotypeAssPubVO = null;
    this.lastUpdated = null;
    this.phenotypes = null;
    this.geneId = null;
    this.taxonCommonName = null;
    this.evidenceCode = null;
    this.id = null;
    this.scoreValueObject = null;
    this.relationship = null;
    this.containQueryPhenotype = false;
  }
}

if (typeof CitationValueObject != "function") {
  function CitationValueObject() {
    this.retracted = false;
    this.citation = null;
    this.pubmedAccession = null;
    this.id = null;
    this.pubmedURL = null;
  }
}

if (typeof ExperimentalEvidenceValueObject != "function") {
  function ExperimentalEvidenceValueObject() {
    this.experimentCharacteristics = null;
    this.geneOfficialSymbol = null;
    this.homologueEvidence = false;
    this.geneNCBI = null;
    this.description = null;
    this.evidenceSecurityValueObject = null;
    this.className = null;
    this.evidenceSource = null;
    this.isNegativeEvidence = false;
    this.phenotypeMapping = null;
    this.geneOfficialName = null;
    this.originalPhenotype = null;
    this.phenotypeAssPubVO = null;
    this.lastUpdated = null;
    this.phenotypes = null;
    this.geneId = null;
    this.taxonCommonName = null;
    this.evidenceCode = null;
    this.id = null;
    this.scoreValueObject = null;
    this.relationship = null;
    this.containQueryPhenotype = false;
  }
}

if (typeof ArrayDesignValueObject != "function") {
  function ArrayDesignValueObject() {
    this.needsAttention = null;
    this.color = null;
    this.hasSequenceAssociations = null;
    this.isMergee = null;
    this.isMerged = null;
    this.description = null;
    this.numGenes = null;
    this.lastRepeatMask = null;
    this.lastUpdated = null;
    this.hasBlatAssociations = null;
    this.lastTroubledEvent = null;
    this.taxonObject = null;
    this.expressionExperimentCount = null;
    this.troubled = null;
    this.id = null;
    this.isSubsumer = null;
    this.lastNoteUpdateEvent = null;
    this.createDate = null;
    this.externalReferences = null;
    this.dateCached = null;
    this.releaseVersion = null;
    this.curationNote = null;
    this.isSubsumed = null;
    this.numProbeAlignments = null;
    this.blackListed = null;
    this.hasGeneAssociations = null;
    this.lastNeedsAttentionEvent = null;
    this.switchedExpressionExperimentCount = null;
    this.numProbeSequences = null;
    this.designElementCount = null;
    this.lastSequenceAnalysis = null;
    this.lastSequenceUpdate = null;
    this.numProbesToGenes = null;
    this.name = null;
    this.releaseUrl = null;
    this.isAffymetrixAltCdf = null;
    this.lastGeneMapping = null;
    this.shortName = null;
  }
}

if (typeof CharacteristicValueObject != "function") {
  function CharacteristicValueObject() {
    this.alreadyPresentOnGene = false;
    this.valueId = null;
    this.categoryUri = null;
    this.originalValue = null;
    this.valueUri = null;
    this.ontologyUsed = null;
    this.urlId = null;
    this.numTimesUsed = 0;
    this.publicGeneCount = 0;
    this.root = false;
    this.taxon = null;
    this.id = null;
    this.category = null;
    this.alreadyPresentInDatabase = false;
    this.privateGeneCount = 0;
    this.value = null;
    this.valueDefinition = null;
    this.child = false;
  }
}

if (typeof FreeTextGeneResultsValueObject != "function") {
  function FreeTextGeneResultsValueObject() {
    this.userCanWrite = false;
    this.userOwned = false;
    this.name = null;
    this.description = null;
    this.isPublic = false;
    this.modified = false;
    this.taxon = null;
    this.geneIds = null;
    this.id = null;
    this.queryString = null;
    this.isShared = false;
  }
}

if (typeof GOGroupValueObject != "function") {
  function GOGroupValueObject() {
    this.goId = null;
    this.searchTerm = null;
    this.userCanWrite = false;
    this.userOwned = false;
    this.name = null;
    this.description = null;
    this.isPublic = false;
    this.modified = false;
    this.taxon = null;
    this.geneIds = null;
    this.id = null;
    this.isShared = false;
  }
}

window.SimpleTreeValueObject = SimpleTreeValueObject
window.ArrayDesignValueObjectExt = ArrayDesignValueObjectExt
window.PhenotypeAssPubValueObject = PhenotypeAssPubValueObject
window.GeneValueObject = GeneValueObject
window.SessionBoundGeneSetValueObject = SessionBoundGeneSetValueObject
window.GeeqValueObject = GeeqValueObject
window.DiffExpressionEvidenceValueObject = DiffExpressionEvidenceValueObject
window.ExpressionExperimentSetValueObject = ExpressionExperimentSetValueObject
window.GeeqAdminValueObject = GeeqAdminValueObject
window.LiteratureEvidenceValueObject = LiteratureEvidenceValueObject
window.PhenotypeGroupValueObject = PhenotypeGroupValueObject
window.ExpressionExperimentValueObject = ExpressionExperimentValueObject
window.DatabaseBackedGeneSetValueObject = DatabaseBackedGeneSetValueObject
window.BlacklistedValueObject = BlacklistedValueObject
window.SessionBoundExpressionExperimentSetValueObject = SessionBoundExpressionExperimentSetValueObject
window.FreeTextExpressionExperimentResultsValueObject = FreeTextExpressionExperimentResultsValueObject
window.ScoreValueObject = ScoreValueObject
window.PhenotypeValueObject = PhenotypeValueObject
window.ExternalDatabaseStatisticsValueObject = ExternalDatabaseStatisticsValueObject
window.GeneSetValueObject = GeneSetValueObject
window.EvidenceValueObject = EvidenceValueObject
window.CitationValueObject = CitationValueObject
window.ExperimentalEvidenceValueObject = ExperimentalEvidenceValueObject
window.ArrayDesignValueObject = ArrayDesignValueObject
window.CharacteristicValueObject = CharacteristicValueObject
window.FreeTextGeneResultsValueObject = FreeTextGeneResultsValueObject
window.GOGroupValueObject = GOGroupValueObject
