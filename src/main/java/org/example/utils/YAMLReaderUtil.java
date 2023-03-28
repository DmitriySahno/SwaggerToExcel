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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;

public class YAMLReaderUtil {

    public static API read(String fileName) throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        JsonNode root = mapper.readTree(new File(fileName));
        API api = new API();
        api.setOpenapi(root.get("openapi").asText());
        fillInfo(api, root.get("info"));
        fillSchemas(api, root.get("components").get("schemas"));
        fillPaths(api, root.get("paths"));
        return api;
    }

    private static void fillSchemas(API api, JsonNode schemasNode) {
        Iterator<Entry<String, JsonNode>> fields = schemasNode.fields();
        while (fields.hasNext()) {
            Entry<String, JsonNode> field = fields.next();
            JsonNode propertiesNode = field.getValue().get("properties");
            LinkedHashSet<Property> properties = new LinkedHashSet<>();
            if ("TypeOperation1".equals(field.getKey()))
                System.out.println("TypeOperation1");
            if (propertiesNode != null) {
                Iterator<Entry<String, JsonNode>> propertiesFields = propertiesNode.fields();
                while (propertiesFields.hasNext()) {
                    Entry<String, JsonNode> propertyNodeMap = propertiesFields.next();
                    JsonNode propertyNode = propertyNodeMap.getValue();
                    JsonNode allOf = propertyNode.get("allOf");
                    Property property = new Property(propertyNodeMap.getKey());
                    if (allOf != null) {
                        Iterator<JsonNode> allOfFields = allOf.elements();
                        while (allOfFields.hasNext()) {
                            Entry<String, JsonNode> allOfField = allOfFields.next().fields().next();
                            switch (allOfField.getKey()) {
                                case "$ref" -> property.setSchemaRef(allOfField.getValue().asText());
                                case "maxLength" -> property.setMaxLength(allOfField.getValue().asInt());
                                case "description" -> property.setDescription(allOfField.getValue().asText());
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
                                .setSchemaRef(propertyNode.get("$ref") == null ?
                                        propertyNode.get("items") == null ? null : propertyNode.get("items").get("$ref").asText() :
                                        propertyNode.get("$ref").asText());
                    }
                    properties.add(property);
                }
            }
            ArrayNode enumNode = (ArrayNode) field.getValue().get("enum");
            List<String> enumValues;
            if (enumNode != null) {
                enumValues = new ArrayList<>();
                enumNode.forEach(n -> enumValues.add(n.asText()));
            } else {
                enumValues = null;
            }
            api.getSchemasMap().put("#/components/schemas/" + field.getKey(), Schema.builder()
                    .name(field.getKey())
                    .title(field.getValue().get("title").asText())
                    .type(field.getValue().get("type").asText())
                    .eNum(enumValues)
                    .properties(properties)
                    .description(field.getValue().get("description") == null ? null : field.getValue().get("description").asText())
                    .build());
        }
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
                    JsonNode schemaRef = requestBodyContent.getValue().get("schema").get("$ref");
                    requestBody = RequestBody.builder()
                            .content(Content.builder()
                                    .schemaRef(schemaRef == null ? null : schemaRef.asText())
                                    .contentType(requestBodyContent.getKey())
                                    .build())
                            .description(requestBodyNode.get("description").asText())
                            .required(requestBodyNode.get("required").asBoolean())
                            .build();
                }
                LinkedHashSet<Parameter> parameters = new LinkedHashSet<>();
                LinkedHashSet<Response> responses = new LinkedHashSet<>();
                Iterator<Entry<String, JsonNode>> responsesNodeMap = methodNode.get("responses").fields();
                while (responsesNodeMap.hasNext()) {
                    Entry<String, JsonNode> responsesNode = responsesNodeMap.next();
                    Entry<String, JsonNode> contentNode = responsesNode.getValue().get("content").fields().next();
                    JsonNode schemaRef = contentNode.getValue().get("schema").get("$ref");
                    responses.add(Response.builder()
                            .code(responsesNode.getKey())
                            .description(responsesNode.getValue().get("description").asText())
                            .content(Content.builder()
                                    .schemaRef(schemaRef == null ? null : schemaRef.asText())
                                    .contentType(contentNode.getKey())
                                    .build())
                            .required(responsesNode.getValue().get("required") == null ? null : responsesNode.getValue().get("required").booleanValue())
                            .build());
                }

                methods.add(RequestMethod.builder()
                        .name(methodNodeMap.getKey())
                        .tags(methodNode.get("tags").asText())
                        .summary(methodNode.get("summary").asText())
                        .description(methodNode.get("description").asText())
                        .operationId(methodNode.get("operationId").asText())
                        .parameters(parameters)
                        .requestBody(requestBody)
                        .responses(responses)
                        .deprecated(methodNode.get("deprecated").booleanValue())
                        .build());
            }
            api.getPaths().add(Path.builder()
                    .uri(field.getKey())
                    .methods(methods)
                    .build());
        }
    }

    private static void fillInfo(API api, JsonNode node) {
        api.setInfo(Info.builder()
                .title(node.get("title").asText())
                .description(node.get("description").asText())
                .contact(node.get("contact").asText())
                .version(node.get("version").asText())
                .build());
    }

    public static Schema getFullSchema(API api, String schemaRef) {
        Schema schema = api.getSchemasMap().get(schemaRef);
        if (schema == null) return null;

        schema.getProperties().forEach(s -> {
            if (s.getSchema() == null && s.getSchemaRef() != null) {
                s.setSchema(getFullSchema(api, s.getSchemaRef()));
            }
        });

        return schema;
    }

}
