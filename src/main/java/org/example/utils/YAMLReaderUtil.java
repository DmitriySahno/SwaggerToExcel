package org.example.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.example.pojo.API;
import org.example.pojo.Info;
import org.example.pojo.Path;
import org.example.pojo.Path.*;
import org.example.pojo.Schema;
import org.example.pojo.Schema.Property;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

public class YAMLReaderUtil {

    public static API read(String fileName) throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        JsonNode root = mapper.readTree(new File(fileName));
        API api = new API();
        api.setOpenapi(root.get("openapi").asText());
        fillInfo(api, root.get("info"));
        fillSchemasMap(api, root.get("components").get("schemas"));
        fillPaths(api, root.get("paths"));
        return api;
    }

    private static void fillSchemasMap(API api, JsonNode schemasNode) {
        Iterator<Entry<String, JsonNode>> fields = schemasNode.fields();
        while (fields.hasNext()) {
            Entry<String, JsonNode> schema = fields.next();
            getSchemasByNode(schema.getValue()).forEach(node -> api.getSchemasMap().put("#/components/schemas/" + schema.getKey(), node));
        }
    }


    private static Schema getSchemaByType(API api, JsonNode schemaNode) {
        JsonNode type = schemaNode.get("type");
        if (Objects.isNull(type)) return null;

        switch (type.asText()) {
            case "object":
                return Schema.builder()
                        .type(type.asText())
                        .properties(getSchemaProperties(schemaNode.get("properties")))
                        .description(Objects.nonNull(schemaNode.get("description")) ? schemaNode.get("description").asText() : null)
                        .summary(Objects.nonNull(schemaNode.get("summary")) ? schemaNode.get("summary").asText() : null)
                        .build();
            case "array":
                getSchemaByNode(api, schemaNode.get("items"));
                break;
            default:
                return null;
        }
    }

    private static List<Schema> getSchemaByNode(API api, JsonNode schemaNode) {
        List<Schema> schemas = new ArrayList<>();
        Iterator<Entry<String, JsonNode>> schemaFields = schemaNode.fields();
        Entry<String, JsonNode> firstSchemaNode = schemaFields.next();
        //TODO the problem of all of that is the ref can be placed after link or not?...
        switch (firstSchemaNode.getKey()) {
            case "$ref":
                String schemaRef = Objects.nonNull(schemaNode.get("$ref")) ? schemaNode.get("$ref").asText() : null;
                schemas.add(getSchemaByRef(api, schemaRef));
                break;
            case "type":
                schemas.add(getSchemaByType(api, schemaNode));
                break;
            case "allOf":
                while (schemaFields.hasNext()) {
                    Entry<String, JsonNode> field = schemaFields.next();
                    schemas.addAll(getSchemaByNode(api, field.getValue()));
                }
                break;
            case "oneOf":
                while (schemaFields.hasNext()) {
                    Entry<String, JsonNode> field = schemaFields.next();
                    schemas.addAll(getSchemaByNode(api, field.getValue()));
                }
                break;
            case "array":

                break;
        }

        Iterator<Entry<String, JsonNode>> fields = schemaNode.fields();
        while (fields.hasNext()) {
            Entry<String, JsonNode> field = fields.next();
            String key = field.getKey();
            JsonNode value = field.getValue();
            ArrayNode enumNode = (ArrayNode) value.get("enum");
            List<String> enumValues;
            if (enumNode != null) {
                enumValues = new ArrayList<>();
                enumNode.forEach(n -> enumValues.add(n.asText()));
            } else {
                enumValues = null;
            }
            schemas.add(Schema.builder()
                    .name(key)
                    .title(Objects.nonNull(value.get("title")) ? value.get("title").asText() : null)
                    .type(Objects.nonNull(value.get("type")) ? value.get("type").asText() : null)
                    .eNum(enumValues)
                    .properties(getSchemaProperties(value.get("properties")))
                    .description(Objects.nonNull(value.get("description")) ? value.get("description").asText() : null)
                    .build());
        }
        return schemas;
    }

    private static LinkedHashSet<Property> getSchemaProperties(JsonNode propertiesNode) {
        LinkedHashSet<Property> properties = new LinkedHashSet<>();
        if (propertiesNode != null) {
            Iterator<Entry<String, JsonNode>> propertiesFields = propertiesNode.fields();
            while (propertiesFields.hasNext()) {
                Entry<String, JsonNode> propertyNodeMap = propertiesFields.next();
                Property property = new Property(propertyNodeMap.getKey());
                JsonNode propertyNode = propertyNodeMap.getValue();
                JsonNode allOf = propertyNode.get("allOf"); //TODO check the properties structure and make universal logic
                if (allOf != null) {
                    Iterator<JsonNode> allOfFields = allOf.elements();
                    while (allOfFields.hasNext()) {
                        Entry<String, JsonNode> allOfField = allOfFields.next().fields().next();
                        switch (allOfField.getKey()) {
                            case "$ref":
                                property.setSchemaRef(allOfField.getValue().asText());
                                break;
                            case "maxLength":
                                property.setMaxLength(allOfField.getValue().asInt());
                                break;
                            case "description":
                                property.setDescription(allOfField.getValue().asText());
                                break;
                        }
                    }
                } else {
                    ArrayNode enumNode = (ArrayNode) propertyNode.get("enum");
                    List<String> enumValues;
                    if (enumNode != null) {
                        enumValues = new ArrayList<>();
                        enumNode.forEach(n -> enumValues.add(n.asText()));
                    } else {
                        enumValues = null;
                    }

                    property.setType(propertyNode.get("type") == null ? null : propertyNode.get("type").asText())
                            .setDescription(propertyNode.get("description") == null ? null : propertyNode.get("description").asText())
                            .setDefaultValue(propertyNode.get("default") == null ? null : propertyNode.get("default").asText())
                            .setENum(enumValues)
                            .setMultipleOf(propertyNode.get("multipleOf") == null ? null : propertyNode.get("multipleOf").floatValue())
                            .setMaxLength(propertyNode.get("maxLength") == null ? null : propertyNode.get("maxLength").intValue())
                            .setFormat(propertyNode.get("format") == null ? null : propertyNode.get("format").asText())
                            .setSchema(propertyNode.get("schema") != null ? getSchemasByNode(propertyNode).get(0) : null)
                            .setProperties(propertyNode.get("properties") != null ? getSchemaProperties(propertyNode.get("properties")) : null)
                            .setSchemaRef(propertyNode.get("$ref") == null ?
                                    propertyNode.get("items") != null && propertyNode.get("items").get("$ref") != null ? propertyNode.get("items").get("$ref").asText() : null :
                                    propertyNode.get("$ref").asText());
                }
                properties.add(property);
            }
        }
        return properties;
    }

    private static void fillPaths(API api, JsonNode pathsNode) {
        Iterator<Entry<String, JsonNode>> fields = pathsNode.fields();
        while (fields.hasNext()) {
            LinkedHashSet<RequestMethod> methods = new LinkedHashSet<>();
            Entry<String, JsonNode> field = fields.next();
            Iterator<Entry<String, JsonNode>> methodsFields = field.getValue().fields();
            while (methodsFields.hasNext()) {
                Entry<String, JsonNode> methodNodeMap = methodsFields.next();
                JsonNode methodNode = methodNodeMap.getValue();
                JsonNode requestBodyNode = methodNode.get("requestBody");
                RequestBody requestBody = null;
                if (requestBodyNode != null) {
                    Entry<String, JsonNode> requestBodyContent = requestBodyNode.get("content").fields().next();
                    requestBody = RequestBody.builder()
                            .content(Content.builder()
                                    .contentType(requestBodyContent.getKey())
                                    .schema(getSchema(api, requestBodyContent.getValue().get("schema")))
                                    .build())
                            .description(Objects.nonNull(requestBodyNode.get("description")) ? requestBodyNode.get("description").asText() : null)
                            .required(Objects.nonNull(requestBodyNode.get("required")) ? requestBodyNode.get("required").asBoolean() : null)
                            .build();
                }
                LinkedHashSet<Parameter> parameters = new LinkedHashSet<>();
                LinkedHashSet<Response> responses = new LinkedHashSet<>();
                Iterator<Entry<String, JsonNode>> responsesNodeMap = methodNode.get("responses").fields();
                while (responsesNodeMap.hasNext()) {
                    Entry<String, JsonNode> responsesNode = responsesNodeMap.next();
                    String key = responsesNode.getKey();
                    JsonNode value = responsesNode.getValue();
                    Entry<String, JsonNode> contentNode = value.get("content").fields().next();
                    responses.add(Response.builder()
                            .code(key)
                            .content(Content.builder()
                                    .contentType(contentNode.getKey())
                                    .schema(getSchema(api, contentNode.getValue().get("schema")))
                                    .build())
                            .description(Objects.nonNull(value.get("description")) ? value.get("description").asText() : null)
                            .required(Objects.nonNull(value.get("required")) ? value.get("required").booleanValue() : null)
                            .build());
                }

                methods.add(RequestMethod.builder()
                        .name(methodNodeMap.getKey())
                        .tags(Objects.nonNull(methodNode.get("tags")) ? methodNode.get("tags").asText() : null)
                        .summary(Objects.nonNull(methodNode.get("summary")) ? methodNode.get("summary").asText() : null)
                        .description(Objects.nonNull(methodNode.get("description")) ? methodNode.get("description").asText() : null)
                        .operationId(Objects.nonNull(methodNode.get("operationId")) ? methodNode.get("operationId").asText() : null)
                        .parameters(parameters)
                        .requestBody(requestBody)
                        .responses(responses)
                        .deprecated(Objects.nonNull(methodNode.get("deprecated")) ? methodNode.get("deprecated").booleanValue() : null)
                        .build());
            }
            api.getPaths().add(Path.builder()
                    .uri(field.getKey())
                    .methods(methods)
                    .build());
        }
    }

//    private static Schema getSchema(API api, JsonNode schemaNode) {
//        List<Schema> schemas = new ArrayList<>();
//        String schemaRef;
//        Entry<String, JsonNode> firstSchemaNode = schemaNode.fields().next();
//        switch (firstSchemaNode.getKey()) {
//            case "$ref":
//                schemaRef = Objects.nonNull(schemaNode.get("$ref")) ? schemaNode.get("$ref").asText() : null;
//                schemas.add(getSchemaByRef(api, schemaRef));
//                break;
//            case "type":
//                schemas.addAll(getSchemasByNode(schemaNode));
//                break;
//            case "allOf":
//            case "oneOf":
//            case "array":
//        }
//        return schemas.get(0);
//    }

    private static void fillInfo(API api, JsonNode node) {
        api.setInfo(Info.builder()
                .title(node.get("title").asText())
                .description(Objects.nonNull(node.get("description")) ? node.get("description").asText() : null)
                .contact(Objects.nonNull(node.get("contact")) ? node.get("contact").asText() : null)
                .version(Objects.nonNull(node.get("version")) ? node.get("version").asText() : null)
                .build());
    }

    public static Schema getSchemaByRef(API api, String schemaRef) {
        Schema schema = Objects.nonNull(schemaRef) ? api.getSchemasMap().get(schemaRef) : null;
        if (schema == null) return null;

        schema.getProperties().forEach(s -> {
            if (s.getSchema() == null && s.getSchemaRef() != null) {
                s.setSchema(getSchemaByRef(api, s.getSchemaRef()));
            }
        });

        return schema;
    }

}
