package fhir.protobuf.bld

import groovy.transform.ToString
import org.codehaus.jackson.annotate.JsonProperty

@ToString
class Schema {
    String $schema
    String description
    Map<String, Definition> definitions
}

@ToString
class Definition {
    List<PartialDefinition> allOf
    List<PartialDefinition> oneOf
}

@ToString
class PartialDefinition {
    String $ref
    String description
    List<String> required
    Map<String, FieldDef> properties

    String getRef() { $ref ? $ref.split('/')[-1] : null }

}

@ToString
class FieldDef {
    String description
    String type
    String $ref
    String pattern
    @JsonProperty('enum') Set<String> enumChoices
    FieldDef items

    String getRef() { $ref ? $ref.split('/')[-1] : null }

    String getTypeOrRef() {
        switch (type) {
            case 'number': return pattern.contains('.') ? 'double' : 'int64'
            case 'boolean': return 'bool'
            default: return type ?: ref
        }
    }
}


