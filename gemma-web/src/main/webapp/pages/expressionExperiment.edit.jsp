<%@ include file="/common/taglibs.jsp" %>

<head>
<title>Edit ${fn:escapeXml(expressionExperiment.shortName)} - ${fn:escapeXml(expressionExperiment.name)}</title>
<meta name="description" content="${fn:escapeXml(expressionExperiment.description)}">
<meta name="keywords" content="${fn:escapeXml(keywords)}" />
<Gemma:script src='/scripts/app/bioassay.draganddrop.js' />
</head>

<title><fmt:message key="expressionExperiment.title" /> ${expressionExperiment.shortName}</title>

<div class="padded">
    <form:form modelAttribute="expressionExperiment" autocomplete="off">
        <h2>
            Editing:
            <a href="<c:url value="/expressionExperiment/showExpressionExperiment.html?id=${expressionExperiment.id}" />">
                    ${expressionExperiment.shortName}
            </a>
        </h2>

        <h3>Quantitation Types</h3>

        <c:if test='${expressionExperiment.quantitationTypes.isEmpty()}'>
            <p>No quantitation types! Data may be corrupted (likely data import error)</p>
        </c:if>

        <form:errors path="quantitationTypes" cssClass="error" />

        <table class="detail">
            <tr>
                <th>Name</th>
                <th>Description</th>
                <th>Preferred</th>
                <th>Recomputed</th>
                <th>Batch Corrected?</th>
                <th>Ratio</th>
                <th>Background</th>
                <th>Background Subtracted</th>
                <th>Normalized</th>
                <th>General Type</th>
                <th>Type</th>
                <th>Scale</th>
                <th>Representation</th>
            </tr>

            <!-- vectors, grouped by vector type -->
            <c:forEach items="${expressionExperiment.quantitationTypesByVectorType.entrySet()}" var="e">
                <c:set var="vectorType" value="${e.key}" />
                <c:set var="quantitationTypes" value="${e.value}" />
                <tr>
                    <td colspan="13">
                        <c:choose>
                            <c:when test="${vectorType != null}">
                                <h4><spring:message code="${vectorType.simpleName}.title"
                                        text="??${vectorType.simpleName}.title??" /></h4>
                            </c:when>
                            <c:otherwise>
                                <h4>Unused quantitation types</h4>
                                <p>These quantitation types are not associated to any vectors.</p>
                            </c:otherwise>
                        </c:choose>
                    </td>
                </tr>
                <c:forEach items="${quantitationTypes}" var="qt">
                    <spring:nestedPath path="quantitationTypes[${expressionExperiment.quantitationTypes.indexOf(qt)}]">
                        <tr>
                            <td>
                                <form:hidden path="id" />
                                <form:input path="name" size="20" cssErrorClass="error" />
                                <form:errors path="name" cssClass="error" />
                            </td>
                            <td><form:input path="description" size="35" /><form:errors path="description" /></td>
                            <td class="text-center">
                                <c:choose>
                                    <c:when test="${vectorType == null}">
                                        <!-- this happens when a QT does not have any associated vectors -->
                                    </c:when>
                                    <c:when test="${vectorType.simpleName == 'SingleCellExpressionDataVector'}">
                                        <form:checkbox path="isSingleCellPreferred" />
                                        <form:errors path="isSingleCellPreferred" />
                                    </c:when>
                                    <c:when test="${vectorType.simpleName == 'RawExpressionDataVector'}">
                                        <form:checkbox path="isPreferred" />
                                        <form:errors path="isPreferred" />
                                    </c:when>
                                    <c:when test="${vectorType.simpleName == 'ProcessedExpressionDataVector'}">
                                        <form:checkbox path="isMaskedPreferred" disabled="true"
                                                title="The preferred status for processed vectors cannot be modified." />
                                        <form:errors path="isMaskedPreferred" />
                                    </c:when>
                                </c:choose>
                            </td>
                            <td class="text-center"><form:checkbox path="isRecomputedFromRawData" /></td>
                            <td class="text-center"><form:checkbox path="isBatchCorrected" /></td>
                            <td class="text-center"><form:checkbox path="isRatio" /></td>
                            <td class="text-center"><form:checkbox path="isBackground" /></td>
                            <td class="text-center"><form:checkbox path="isBackgroundSubtracted" /></td>
                            <td class="text-center"><form:checkbox path="isNormalized" /></td>
                            <td>
                                <form:select path="generalType">
                                    <form:options items="${generalQuantitationTypes}" />
                                </form:select>
                                <form:errors path="generalType" cssClass="error" />
                            </td>
                            <td>
                                <form:select path="type">
                                    <form:options items="${standardQuantitationTypes}" />
                                </form:select>
                                <form:errors path="type" cssClass="error" />
                            </td>
                            <td>
                                <form:select path="scale">
                                    <form:options items="${scaleTypes}" />
                                </form:select>
                                <form:errors path="scale" cssClass="error" />
                            </td>
                            <td>
                                <form:select path="representation" disabled="true"
                                        title="The representation cannot be changed without rewriting the vectors.">
                                    <form:options items="${representations}" />
                                </form:select>
                                <form:errors path="representation" cssClass="error" />
                            </td>
                        </tr>
                    </spring:nestedPath>
                </c:forEach>
            </c:forEach>
        </table>

        <c:if test="${!expressionExperiment.singleCellDimensions.isEmpty()}">
        <hr class="normal">
        <h3>Single-Cell Metadata</h3>
            <p>The values here cannot be modified for now.</p>
        </c:if>

        <c:forEach items="${expressionExperiment.singleCellDimensions}" var="scd" varStatus="scdIndex">
            <spring:nestedPath path="singleCellDimensions[${scdIndex.index}]">
                <form:hidden path="id" />
                <c:if test="${!scd.cellTypeAssignments.isEmpty()}">
                    <h4>Cell Type Assignments</h4>
                    <table>
                        <tr>
                            <th style="min-width: 50px;">ID</th>
                            <th>Name</th>
                            <th>Description</th>
                            <th>Protocol</th>
                            <th class="text-center">Preferred?</th>
                            <th>Values</th>
                        </tr>
                        <c:forEach items="${scd.cellTypeAssignments}" var="cta" varStatus="ctaIndex">
                            <spring:nestedPath
                                    path="cellTypeAssignments[${ctaIndex.index}]">
                                <tr>
                                    <td>
                                            ${cta.id}
                                        <form:hidden path="id" />
                                    </td>
                                    <td>
                                        <form:input path="name" size="20" cssErrorClass="error" disabled="true" />
                                        <form:errors path="name" cssClass="error" />
                                    </td>
                                    <td>
                                        <form:input path="description" size="35" cssErrorClass="error"
                                                disabled="true" />
                                        <form:errors path="description" cssClass="error" />
                                    </td>
                                    <td>
                                        <form:select path="protocolId" disabled="true">
                                            <form:options items="${cellTypeAssignmentProtocols}" itemValue="id"
                                                    itemLabel="name" />
                                        </form:select>
                                    </td>
                                    <td class="text-center">
                                        <form:checkbox path="isPreferred" cssErrorClass="error" disabled="true" />
                                        <form:errors path="isPreferred" cssClass="error" />
                                    </td>
                                    <td class="text-ellipsis" style="width: 100%; max-width: 0;">
                                        <c:forEach items="${cta.values}" var="value" varStatus="valueI">
                                            ${fn:escapeXml(value)}<c:if test="${!valueI.last}">, </c:if>
                                        </c:forEach>
                                    </td>
                                </tr>
                            </spring:nestedPath>
                        </c:forEach>
                    </table>
                </c:if>

                <c:if test="${!scd.cellLevelCharacteristics.isEmpty()}">
                    <h4>Cell-level Characteristics</h4>
                    <table>
                        <tr>
                            <th style="min-width: 50px">ID</th>
                            <th>Name</th>
                            <th>Description</th>
                            <th>Category</th>
                            <th>Values</th>
                        </tr>
                        <c:forEach items="${scd.cellLevelCharacteristics}" var="clc" varStatus="clcIndex">
                            <spring:nestedPath
                                    path="cellLevelCharacteristics[${clcIndex.index}]">
                                <tr>
                                    <td>
                                            ${clc.id}
                                        <form:hidden path="id" />
                                    </td>
                                    <td>
                                        <form:input path="name" size="20" cssErrorClass="error" disabled="true" />
                                        <form:errors path="name" cssClass="error" />
                                    </td>
                                    <td>
                                        <form:input path="description" size="20" cssErrorClass="error"
                                                disabled="true" />
                                        <form:errors path="description" size="35" cssClass="error" />
                                    </td>
                                    <td>
                                            ${fn:escapeXml(clc.category)}
                                    </td>
                                    <td style="width: 100%; max-width: 0; white-space: nowrap;  overflow: hidden; text-overflow: ellipsis;">
                                        <c:forEach items="${clc.values}" var="value" varStatus="valueI">
                                            ${fn:escapeXml(value)}<c:if test="${!valueI.last}">, </c:if>
                                        </c:forEach>
                                    </td>
                                </tr>
                            </spring:nestedPath>
                        </c:forEach>
                    </table>
                </c:if>
            </spring:nestedPath>
        </c:forEach>

        <hr class="normal">

        <h3>Samples and Assays</h3>

        <form:errors path="assayToMaterialMap" cssClass="error" />

        <div class="v-padded">
            <a
                    href="<c:url value="/expressionExperiment/showBioAssaysFromExpressionExperiment.html?id=${expressionExperiment.id}"/>">
                Click for details and QC</a>
        </div>

        <div class="v-padded">
            <input type="button" onClick="Ext.getCmp('eemanager').unmatchBioAssays(${expressionExperiment.id})"
                    value="Unmatch all assays" />
        </div>

        <Gemma:assayView bioAssays="${expressionExperiment.bioAssays}"
                expressionExperimentId="${expressionExperiment.id}" edit="true" />

        <div class="v-padded">
            <input type="submit" class="button" value="<fmt:message key="button.save" />" />
            <input type="reset" class="button" name="cancel" value="<fmt:message key="button.cancel"/>" />
        </div>

    </form:form>

</div>