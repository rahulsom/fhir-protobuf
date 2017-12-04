package com.github.rahulsom.fhirprotobuf;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonMap;

public class Converter {

    public static String fromFhirJson(String fhirJson) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        Map<String, Object> map = gson.fromJson(fhirJson, Map.class);
        List<Map> entries = (List<Map>) map.get("entry");

        entries.forEach(entry -> {
            Map resource = (Map) entry.get("resource");

            String entryResourceType = (String) resource.get("resourceType");
            makeMapProtobufCompliant(resource, entryResourceType);
            if (entryResourceType != null) {
                entry.put("resource",
                        singletonMap(transformResourceTypeToProto(entryResourceType), resource));
            }
        });

        String resourceType = (String) map.get("resourceType");
        makeMapProtobufCompliant(map, resourceType);

        return gson.toJson(map);
    }

    private static void makeMapFhirCompliant(Map<String, Object> map, String resourceType) {
        map.keySet().forEach(key -> {
            if (key.endsWith("ExtensionElement")) {
                Object value = map.remove(key);
                String newKey = "_" + key.substring(0, key.length() - "ExtensionElement".length());
                map.put(newKey, value);
            }
        });

        EnumsHelper.Holder.INSTANCE.getEnums().stream().
                filter(strings -> strings[0].equals("TYPE") && strings[1].equals(resourceType))
                .forEach(strings -> {
                    String fieldName = strings[2];
                    String fieldType = strings[3];

                    if (map.containsKey(fieldName)) {
                        Object field = map.get(fieldName);
                        if (field instanceof Map) {
                            makeMapFhirCompliant((Map) field, fieldType);
                        }
                        if (field instanceof List && !fieldName.equals("ResourceList")) {
                            ((List) field).forEach(it -> makeMapFhirCompliant((Map) it, fieldType));
                        }
                    }
                });

        EnumsHelper.Holder.INSTANCE.getEnums().stream()
                .filter(strings -> strings[0].equals("ENUM") && strings[1].equals(resourceType))
                .map(strings -> strings[2])
                .forEach(key -> {
                    if (map.containsKey(key)) {
                        map.put(key, map.get(key).toString()
                                .substring((resourceType + "_" + key + "_" ).length()));
                    }
                });

    }
    private static void makeMapProtobufCompliant(Map<String, Object> map, String resourceType) {
        EnumsHelper.Holder.INSTANCE.getEnums().stream()
                .filter(strings -> strings[0].equals("ENUM") && strings[1].equals(resourceType))
                .map(strings -> strings[2])
                .forEach(key -> {
                    if (map.containsKey(key)) {
                        map.put(key, resourceType + "_" + key + "_" + map.get(key));
                    }
                });

        EnumsHelper.Holder.INSTANCE.getEnums().stream()
                .filter(strings -> strings[0].equals("TYPE") && strings[1].equals(resourceType))
                .forEach(strings -> {
                    String fieldName = strings[2];
                    String fieldType = strings[3];

                    if (map.containsKey(fieldName)) {
                        Object field = map.get(fieldName);
                        if (field instanceof Map) {
                            makeMapProtobufCompliant((Map) field, fieldType);
                        }
                        if (field instanceof List && !fieldName.equals("ResourceList")) {
                            ((List) field).forEach(it ->
                                    makeMapProtobufCompliant((Map) it, fieldType));
                        }
                    }
                });

        map.keySet().forEach(key -> {
            if (key.startsWith("_")) {
                Object value = map.remove(key);
                String newKey = key.substring(1) + "ExtensionElement";
                map.put(newKey, value);
            }
        });

        if (map.containsKey("resourceType")) {
            map.remove("resourceType");
        }
    }

    private static String transformResourceTypeToProto(String resourceType) {
        return resourceType.substring(0, 1).toLowerCase() + resourceType.substring(1);
    }

    private static String transformResourceTypeToFhir(String resourceType) {
        return resourceType.substring(0, 1).toUpperCase() + resourceType.substring(1);
    }

    @SuppressWarnings("WeakerAccess")
    public static String toFhirJson(String protoJson) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Map<String, Object> map = gson.fromJson(protoJson, Map.class);

        makeMapFhirCompliant(map, "Bundle");
        map.put("resourceType", "Bundle");

        List<Map> entries = (List<Map>) map.get("entry");

        entries.forEach(entry -> {
            Map<String, Object> resource = (Map<String, Object>) entry.get("resource");
            String protoResourceKey = (String) resource.keySet().toArray()[0];
            String fhirResourceType = transformResourceTypeToFhir(protoResourceKey);
            Map<String, Object> fhirResource = (Map<String, Object>) resource.get(protoResourceKey);
            fhirResource.put("resourceType", fhirResourceType);
            entry.put("resource", fhirResource);

            makeMapFhirCompliant(fhirResource, fhirResourceType);
        });

        return gson.toJson(map);
    }
}
