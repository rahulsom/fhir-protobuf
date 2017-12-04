package fhir.protobuf.bld

import groovy.transform.TupleConstructor
import groovy.util.logging.Slf4j
import org.codehaus.jackson.map.ObjectMapper

@TupleConstructor
@Slf4j
class FhirJsonToProto {

    File schemaFile
    File protoFile
    File enumsFile

    private Writer protoFileWriter = new PrintWriter(new FileWriter(protoFile))
    private Writer enumsFileWriter = new PrintWriter(new FileWriter(enumsFile))
    private def baos = new ByteArrayOutputStream()
    private def output = new PrintWriter(baos)

    private Schema document
    private String currRecord = null
    private Set<String> currRecordFields = []

    void convert() {
        document = new ObjectMapper().readValue(schemaFile, Schema)
        protoFileWriter.println "/*"
        protoFileWriter.println " * Schema: ${document.$schema}"
        protoFileWriter.println " * Description: $document.description"
        protoFileWriter.println " */"
        protoFileWriter.println 'syntax = "proto3";'
        protoFileWriter.println 'package org.fhir.stu3;'
        protoFileWriter.println 'option java_multiple_files = true;'
        protoFileWriter.println ""
        document.definitions.each { k, v -> createRecord(k, v) }
        protoFileWriter.flush()
        protoFileWriter.close()

        enumsFileWriter.flush()
        enumsFileWriter.close()
    }

    def createRecord(String recordName, Definition definition) {
        currRecordFields = []
        def description = definition.allOf ? definition.allOf.find {
            it.description
        }.description : ""
        currRecord = recordName
        output.println "/*"
        output.println " * ${description.replace('\n', ' ')}"
        output.println " */"
        output.println "message $recordName {"
        renderDefinition(definition)
        output.println "}"
        output.println ""
        output.flush()
        output.close()
        if (buffer.toString()) {
            protoFileWriter.println buffer
            buffer = new StringBuilder()
        }
        protoFileWriter.write(new String(baos.toByteArray()))
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
                        if (fieldName != 'resourceType')
                            if (!currRecordFields.contains(fieldName)) {
                                currRecordFields << fieldName
                                if (fieldDef.ref) {
                                    enumsFileWriter.println("TYPE,$currRecord,$fieldName,$fieldDef.ref")
                                } else if (fieldDef.type == 'array' && fieldDef.items.ref) {
                                    enumsFileWriter.println("TYPE,$currRecord,$fieldName,$fieldDef.items.ref")
                                }
                                String fieldDefinition = getFieldDefinitionString(fieldDef, fieldName).padRight(65) + ' //'

                                output.println "    ${fieldDefinition} ${fieldDef.description.replace('\n', ' ').replace('\r', ' ')}"
                            }
                    }
                }
            }
        } else if (definition.oneOf) {
            def possibilities = definition.oneOf*.ref.unique()
            possibilities.each {
                output.println "    ${it} ${it[0].toLowerCase()}${it[1..-1]} = ${fieldNumber++};"
            }
        }
    }

    private String getFieldDefinitionString(FieldDef fieldDef, fieldName) {
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
        enumsFileWriter.println "ENUM,${currRecord},$fieldName"
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
