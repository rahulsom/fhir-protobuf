package fhir.protobuf.bld

import groovy.transform.TupleConstructor
import groovy.util.logging.Slf4j
import org.codehaus.jackson.map.ObjectMapper

@TupleConstructor
@Slf4j
class FhirJsonToProto {

    File schemaFile
    File protoFile
    private Writer fileWriter = new PrintWriter(new FileWriter(protoFile))
    private def baos = new ByteArrayOutputStream()
    private def output = new PrintWriter(baos)

    public static final String NAMESPACE = 'org.fhir.stu3'
    private Schema document
    private String currRecord = null
    private Set<String> currRecordFields = []

    void convert() {
        document = new ObjectMapper().readValue(schemaFile, Schema)
        fileWriter.println "/**"
        fileWriter.println " * Schema: ${document.$schema}"
        fileWriter.println " * Description: $document.description"
        fileWriter.println " */"
        fileWriter.println 'syntax = "proto3";'
        fileWriter.println 'package org.fhir.stu3;'
        fileWriter.println 'option java_multiple_files = true;'
        fileWriter.println ""
        document.definitions.each { k, v -> createRecord(k, v) }
        fileWriter.flush()
        fileWriter.close()
    }

    def createRecord(String recordName, Definition definition) {
        currRecordFields = []
        def description = definition.allOf ? definition.allOf.find {
            it.description
        }.description : ""
        currRecord = recordName
        output.println "/**"
        output.println " * ${description.replace('\n', ' ')}"
        output.println " */"
        output.println "message $recordName {"
        renderDefinition(definition)
        output.println "}"
        output.println ""
        output.flush()
        output.close()
        if (buffer.toString()) {
            fileWriter.println buffer
            buffer = new StringBuilder()
        }
        fileWriter.write(new String(baos.toByteArray()))
        baos = new ByteArrayOutputStream()
        output = new PrintWriter(baos)
        fieldNumber = 1
    }

    private StringBuilder buffer = new StringBuilder()
    int fieldNumber = 1

    private void renderDefinition(Definition definition) {
        assert definition.allOf == null ^ definition.oneOf == null

        if (definition.allOf) {
            definition.allOf.each { partial ->
                if (partial.$ref) {
                    output.println "    // begin $partial.ref"
                    renderDefinition(document.definitions.find { it.key == partial.ref }.value)
                    output.println "    // end $partial.ref"
                    output.println ""
                } else {
                    partial.properties.each { fieldName, fieldDef ->
                        if (!currRecordFields.contains(fieldName)) {
                            currRecordFields << fieldName
                            String fieldDefinition = getFieldDefinitionString(fieldDef, fieldName, partial) + ' //'
                            def isArray = fieldDef.type == 'array'
                            def isRequired = partial.required?.contains(fieldName)
                            def isOptional = !isArray && !isRequired

                            String followUp
                            if (isRequired) {
                                followUp = 'REQUIRED'
                            } else if (isOptional) {
                                followUp = 'OPTIONAL'
                            } else {
                                followUp = '        '
                            }
                            output.print "    ${fieldDefinition.padRight(50)} |"
                            output.println " ${followUp}  -  ${fieldDef.description.replace('\n', ' ').replace('\r', ' ')}"
                        }
                    }
                }
            }
        } else if (definition.oneOf) {
            def possibilities = definition.oneOf*.ref.unique()
            possibilities.each {
                output.println "    ${it} ${it[0].toLowerCase()}${it[1..-1]} = ${fieldNumber++};"
            }
            output.println "    ${currRecord}ValueType valueType = ${fieldNumber++};"
            buffer.append("enum ${currRecord}ValueType {\n")
            possibilities.eachWithIndex { it, idx ->
                buffer.append("    ${currRecord}ValueType_${it} = $idx;\n")
            }
            buffer.append("}\n\n")

        }
    }

    private String getFieldDefinitionString(FieldDef fieldDef, fieldName, PartialDefinition partial) {
        if (fieldDef.type == 'array') {
            return "repeated ${fieldDef.items.typeOrRef} ${transformFieldName fieldName} = ${fieldNumber++};"
        } else {
            def ref = fieldDef.typeOrRef
            if (fieldDef.enumChoices) {
                provisionEnum(fieldName, fieldDef)
                ref = "${currRecord}_${fieldName}"
            }
            return "${ref} ${transformFieldName fieldName} = ${fieldNumber++};"
        }
    }

    private void provisionEnum(fieldName, FieldDef fieldDef) {
        buffer.append("enum ${currRecord}_${fieldName} {\n")
        fieldDef.enumChoices.eachWithIndex { it, idx ->
            buffer.append("    ${currRecord}_${fieldName}_${transformEnumConstant(it)} = $idx;\n")
        }
        buffer.append("}\n\n")
    }

    private String transformEnumConstant(String it) {
        def retval = [
                '<' : 'LessThan',
                '>' : 'GreaterThan',
                '<=': 'LessThanOrEqualTo',
                '>=': 'GreaterThanOrEqualTo',
                '=' : 'Equals',
        ][it] ?: it.replace('-', '_')
        if (retval.isNumber()) {
            "N_$retval"
        } else if (retval == 'option') {
            'OPTION'
        } else if (retval == 'reserved') {
            'RESERVED'
        } else if (retval == 'in') {
            'IN'
        } else {
            retval
        }
    }

    private String transformFieldName(String fieldName) {
        if (fieldName.startsWith('_')) {
            fieldName[1..-1] + "ExtensionElement"
        } else {
            fieldName
        }
    }
}
