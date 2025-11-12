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

        <form:errors path="quantitationTypes" cssClass="error-feedback" />

        <table class="detail" style="width: 100%;">
            <tr>
                <th style="vertical-align: bottom;">ID</th>
                <th style="vertical-align: bottom;">Name</th>
                <th style="vertical-align: bottom;">Description</th>
                <th style="writing-mode: sideways-lr;">Preferred?</th>
                <th style="writing-mode: sideways-lr;">Recomputed?</th>
                <th style="writing-mode: sideways-lr;">Aggregated?</th>
                <th style="writing-mode: sideways-lr;">Batch Corrected?</th>
                <th style="writing-mode: sideways-lr;">Ratio</th>
                <th style="writing-mode: sideways-lr;">Background</th>
                <th style="writing-mode: sideways-lr;">Background Subtracted</th>
                <th style="writing-mode: sideways-lr;">Normalized</th>
                <th style="vertical-align: bottom;">General Type</th>
                <th style="vertical-align: bottom;">Type</th>
                <th style="vertical-align: bottom;">Scale</th>
                <th style="vertical-align: bottom;">Representation</th>
            </tr>

            <!-- vectors, grouped by vector type -->
            <c:forEach items="${expressionExperiment.quantitationTypesByVectorType.entrySet()}" var="e">
                <c:set var="vectorType" value="${e.key}" />
                <c:set var="quantitationTypes" value="${e.value}" />
                <tr>
                    <td colspan="16">
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
                                    ${qt.id}
                            </td>
                            <td>
                                <form:input path="name" size="20" cssErrorClass="error" />
                                <form:errors path="name" cssClass="error-feedback" />
                            </td>
                            <td style="width: 100%;"><form:input path="description"
                                    cssStyle="width: 100%;" /><form:errors
                                    path="description" /></td>
                            <td class="text-center">
                                <c:choose>
                                    <c:when test="${vectorType == null}">
                                        <!-- this happens when a QT does not have any associated vectors -->
                                    </c:when>
                                    <c:when test="${vectorType.simpleName == 'SingleCellExpressionDataVector'}">
                                        <form:checkbox path="isSingleCellPreferred" />
                                        <form:errors path="isSingleCellPreferred" cssClass="error-feedback" />
                                    </c:when>
                                    <c:when test="${vectorType.simpleName == 'RawExpressionDataVector'}">
                                        <form:checkbox path="isPreferred" />
                                        <form:errors path="isPreferred" cssClass="error-feedback" />
                                    </c:when>
                                    <c:when test="${vectorType.simpleName == 'ProcessedExpressionDataVector'}">
                                        <form:checkbox path="isMaskedPreferred" disabled="true"
                                                title="The preferred status for processed vectors cannot be modified." />
                                        <form:errors path="isMaskedPreferred" cssClass="error-feedback" />
                                    </c:when>
                                </c:choose>
                            </td>
                            <td class="text-center"><form:checkbox path="isRecomputedFromRawData" /></td>
                            <td class="text-center"><form:checkbox path="isAggregated" disabled="true" /></td>
                            <td class="text-center"><form:checkbox path="isBatchCorrected" /></td>
                            <td class="text-center"><form:checkbox path="isRatio" /></td>
                            <td class="text-center"><form:checkbox path="isBackground" /></td>
                            <td class="text-center"><form:checkbox path="isBackgroundSubtracted" /></td>
                            <td class="text-center"><form:checkbox path="isNormalized" /></td>
                            <td>
                                <form:select path="generalType">
                                    <form:options items="${generalQuantitationTypes}" />
                                </form:select>
                                <form:errors path="generalType" cssClass="error-feedback" />
                            </td>
                            <td>
                                <form:select path="type">
                                    <form:options items="${standardQuantitationTypes}" />
                                </form:select>
                                <form:errors path="type" cssClass="error-feedback" />
                            </td>
                            <td>
                                <form:select path="scale">
                                    <form:options items="${scaleTypes}" />
                                </form:select>
                                <form:errors path="scale" cssClass="error-feedback" />
                            </td>
                            <td>
                                <form:select path="representation" disabled="true"
                                        title="The representation cannot be changed without rewriting the vectors.">
                                    <form:options items="${representations}" />
                                </form:select>
                                <form:errors path="representation" cssClass="error-feedback" />
                            </td>
                            <td class="text-right">
                                <form:button
                                        name="deleteQuantitationType"
                                        value="${qt.id}"
                                        class="btn-unstyled"><i class="fa fa-remove red"></i></form:button>
                            </td>
                        </tr>
                    </spring:nestedPath>
                </c:forEach>
            </c:forEach>
        </table>

        <c:forEach items="${expressionExperiment.singleCellDimensions}" var="scd" varStatus="scdIndex">
            <hr class="normal">
            <h3>Single-Cell Dimension #${scd.id}</h3>
            <c:if test="${not empty scd.quantitationTypes}">
                Used by the following quantitation types:
                <c:forEach items="${scd.quantitationTypes}" var="qt">
                    ${fn:escapeXml(qt.name)} (#${qt.id})
                </c:forEach>
            </c:if>
            <spring:nestedPath path="singleCellDimensions[${scdIndex.index}]">
                <form:hidden path="id" />
                <c:if test="${!scd.cellTypeAssignments.isEmpty()}">
                    <h4>Cell Type Assignments</h4>
                    <form:errors path="cellTypeAssignments" cssClass="error-feedback" />
                    <table class="detail" style="width: 100%;">
                        <tr>
                            <th style="min-width: 50px;">ID</th>
                            <th>Name</th>
                            <th>Description</th>
                            <th>Protocol</th>
                            <th style="writing-mode: sideways-lr;">Preferred?</th>
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
                                        <form:errors path="name" cssClass="error-feedback" />
                                    </td>
                                    <td>
                                        <form:input path="description" size="35" cssErrorClass="error"
                                                disabled="true" />
                                        <form:errors path="description" cssClass="error-feedback" />
                                    </td>
                                    <td>
                                        <form:select path="protocolId" disabled="true">
                                            <form:options items="${cellTypeAssignmentProtocols}" itemValue="id"
                                                    itemLabel="name" />
                                        </form:select>
                                    </td>
                                    <td class="text-center">
                                        <form:checkbox path="isPreferred" cssErrorClass="error" />
                                        <form:errors path="isPreferred" cssClass="error-feedback" />
                                    </td>
                                    <td class="text-ellipsis" style="width: 100%; max-width: 0;">
                                        <c:forEach items="${cta.values}" var="value" varStatus="valueI">
                                            ${fn:escapeXml(value)}<c:if test="${!valueI.last}">, </c:if>
                                        </c:forEach>
                                    </td>
                                    <td class="text-nowrap text-right">
                                        <c:if test="${cta.id == expressionExperiment.preferredCellTypeAssignmentId}">
                                            <form:button
                                                    name="recreateCellTypeFactor" class="btn-unstyled mr-1"
                                                    title="Re-create the cell type factor.">
                                                <i class="fa fa-refresh orange"></i>
                                            </form:button>
                                        </c:if>
                                        <form:button
                                                name="deleteCellTypeAssignment"
                                                value="${cta.id}" class="btn-unstyled"><i
                                                class="fa fa-remove red"></i></form:button>
                                    </td>
                                </tr>
                            </spring:nestedPath>
                        </c:forEach>
                    </table>
                </c:if>

                <c:if test="${!scd.cellLevelCharacteristics.isEmpty()}">
                    <h4>Cell-level Characteristics</h4>
                    <table class="detail" style="width: 100%;">
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
                                        <form:errors path="name" cssClass="error-feedback" />
                                    </td>
                                    <td>
                                        <form:input path="description" size="35" cssErrorClass="error"
                                                disabled="true" />
                                        <form:errors path="description" cssClass="error-feedback" />
                                    </td>
                                    <td>
                                            ${fn:escapeXml(clc.category)}
                                    </td>
                                    <td style="width: 100%; max-width: 0; white-space: nowrap;  overflow: hidden; text-overflow: ellipsis;">
                                        <c:forEach items="${clc.values}" var="value" varStatus="valueI">
                                            ${fn:escapeXml(value)}<c:if test="${!valueI.last}">, </c:if>
                                        </c:forEach>
                                    </td>
                                    <td class="text-right">
                                        <form:button
                                                name="deleteCellLevelCharacteristics"
                                                value="${clc.id}" class="btn-unstyled"><i
                                                class="fa fa-remove red"></i></form:button>
                                    </td>
                                </tr>
                            </spring:nestedPath>
                        </c:forEach>
                    </table>
                </c:if>
            </spring:nestedPath>
        </c:forEach>

        <c:if test="${not expressionExperiment.preferredCellTypeAssignmentCompatibleWithCellTypeFactor}">
            <div class="error" style="padding: 1em; width: 800px;">
                <p>Values in the preferred cell type assignment are not compatible with the cell type factor:</p>
                <div class="flex justify-space-around">
                    <div>
                        <h4>Preferred Cell Type Assignment</h4>
                        <ul>
                            <c:forEach items="${expressionExperiment.preferredCellTypeAssignmentValues}" var="value">
                                <li class="${expressionExperiment.incompatibleCellTypeAssignmentValues.contains(value) ? 'error mb-1': ''}">${fn:escapeXml(value)}</li>
                            </c:forEach>
                        </ul>
                    </div>
                    <div>
                        <h4>Cell Type Factor</h4>
                        <ul>
                            <c:forEach items="${expressionExperiment.cellTypeFactorValues}" var="value"
                                    varStatus="valueI">
                                <li class="${expressionExperiment.unmatchedCellTypeFactorValues.contains(value) ? 'warning mb-1': ''}">${fn:escapeXml(value)}</li>
                            </c:forEach>
                        </ul>
                    </div>
                </div>
            </div>
        </c:if>

        <hr class="normal">

        <h3>Samples and Assays</h3>

        <form:errors path="assayToMaterialMap" cssClass="error-feedback" />

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

        <form:hidden path="confirmation" />

    </form:form>

</div>

<script>
function bindCheckboxes( selector ) {
   $( selector ).click( function() {
      $( selector ).not( this ).prop( 'checked', false );
   } );
}

bindCheckboxes( '[name^="quantitationTypes["][name$="].isPreferred"]' );
bindCheckboxes( '[name^="quantitationTypes["][name$="].isSingleCellPreferred"]' );
<c:forEach items="${expressionExperiment.singleCellDimensions}" var="scd" varStatus="scdIndex">
bindCheckboxes( '[name^="singleCellDimensions[${scdIndex.index}].cellTypeAssignments["][name$="].isPreferred"]' );
</c:forEach>

function addDeleteConfirmation( selector, what, messagePrefix ) {
   $( selector ).click( function() {
      const id = $( this ).val();
      const name = $( this ).closest( 'tr' ).children( 'td' ).eq( 1 ).children().eq( 0 ).val();
      if ( prompt( 'Enter "DELETE" to confirm deletion of ' + what + ' with ID ' + id + ' and name ' + name + ':' ) === 'DELETE' ) {
         $( '#confirmation' ).val( 'DELETE ' + messagePrefix + ' ' + $( this ).val() );
      } else {
         event.preventDefault();
      }
   } );
}

addDeleteConfirmation( '[name$="deleteQuantitationType"]', 'quantitation type', 'QT' )
addDeleteConfirmation( '[name$="deleteCellTypeAssignment"]', 'cell type assignment', 'CTA ' )
addDeleteConfirmation( '[name$="deleteCellLevelCharacteristics"]', 'cell-level characteristics', 'CLC' )

$( '[name="recreateCellTypeFactor"]' ).click( function() {
   const id = $( this ).closest( 'tr' ).children( 'td' ).eq( 0 ).children().eq( 0 ).val();
   const name = $( this ).closest( 'tr' ).children( 'td' ).eq( 1 ).children().eq( 0 ).val();
   if ( prompt( 'Enter "RECREATE" to confirm re-creation of the cell type factor from ' + name + ':' ) === 'RECREATE' ) {
      $( '#confirmation' ).val( 'RECREATE CTF FROM CTA ' + id );
   } else {
      event.preventDefault();
   }
} );
</script>