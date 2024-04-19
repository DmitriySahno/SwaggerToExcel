package org.example.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.NonNull;
import org.example.pojo.API;
import org.example.pojo.Info;
import org.example.pojo.Path;
import org.example.pojo.Path.*;
import org.example.pojo.Schema;

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
            getSchemaFromNode(api, schema.getValue())
                    .forEach(node -> api.getSchemasMap().put("#/components/schemas/" + schema.getKey(), node.setName(schema.getKey())));
        }
    }

    /**
     * Getting {@link Schema} from {@link JsonNode}, when node definitely has type
     * @param api {@link API} value
     * @param schemaNode structure to parse
     * @return {@link Schema} or null if type isn`t defined
     */
    private static Schema getSchemaByType(API api, JsonNode schemaNode) {
        JsonNode type = schemaNode.get("type");
        if (Objects.isNull(type)) return null;

        return switch (type.asText()) {
            case ("object") -> Schema.builder()
                        .type(type.asText())
                        .properties(getSchemaFromNode(api, schemaNode.get("properties")))
                        .description(schemaNode.has("description") ? schemaNode.get("description").asText() : null)
                        .build();
            case ("array") -> Schema.builder()
                        .type(type.asText())
                        .items(getSchemaFromNode(api, schemaNode.get("items")))
                        .build();
            default -> {
                List<String> eNum = null;
                if (schemaNode.has("enum")) {
                    eNum = new LinkedList<>();
                    JsonNode enumArrayNode = schemaNode.withArray("enum");
                    for (int i = 0; i < enumArrayNode.size(); i++) {
                        eNum.add(enumArrayNode.get(i).asText());
                    }
                }
                yield  Schema.builder()
                        .type(type.asText())
                        .description(schemaNode.has("description") ? schemaNode.get("description").asText() : null)
                        .eNum(eNum)
                        .maxLength(schemaNode.has("maxLength") ? schemaNode.get("maxLength").asInt() : null)
                        .format(schemaNode.has("format") ? schemaNode.get("format").asText() : null)
                        .multipleOf(schemaNode.has("multipleOf") ? schemaNode.get("multipleOf").floatValue() : null)
                        .defaultValue(schemaNode.has("default") ? schemaNode.get("default").asText() : null)
                        .properties(schemaNode.has("properties") ? getSchemaFromArrayNode(api, schemaNode.withArray("properties")) : null)
                        .build();
            }
        };
    }

    /** Base reading node for schema
     * For detail information about structure look at {@link Schema}
     **/
    private static List<Schema> getSchemaFromNode(API api, JsonNode schemaNode) {
        List<Schema> schemas = new LinkedList<>();

        if (schemaNode.has("$ref")) {
            schemas.add(new Schema().setRef(schemaNode.get("$ref").asText()));
        }
        if (schemaNode.has("allOf")) {
            schemas.add(Schema.builder()
                    .name("allOf")
                    .type("allOf")
                    .build()
                    .addAllOf(getSchemaFromArrayNode(api, schemaNode.withArray("allOf"))));
        }
        if (schemaNode.has("oneOf")) {
            schemas.add(Schema.builder()
                    .name("oneOf")
                    .type("oneOf")
                    .build()
                    .addOneOf(getSchemaFromArrayNode(api, schemaNode.withArray("oneOf"))));
        }
        if (schemaNode.has("type")) {
            schemas.add(getSchemaByType(api, schemaNode));
        }
        if (schemas.isEmpty()) {
            Iterator<Entry<String, JsonNode>> fields = schemaNode.fields();
            while (fields.hasNext()) {
                Entry<String, JsonNode> field = fields.next();
                String key = field.getKey();
                Schema schema = getSchemaFromNode(api, field.getValue()).get(0);
                schemas.add(schema.setName(key));
            }
        }
        return schemas;
    }

    private static List<Schema> getSchemaFromArrayNode(API api, ArrayNode array) {
        List<Schema> schemas = new LinkedList<>();
        for (int i = 0; i < array.size(); i++) {
            schemas.addAll(getSchemaFromNode(api, array.get(i)));
        }
        return schemas;
    }

    private static void fillPaths(API api, JsonNode pathsNode) {
        Iterator<Entry<String, JsonNode>> fields = pathsNode.fields();
        while (fields.hasNext()) {
            LinkedHashSet<RequestMethod> methods = new LinkedHashSet<>();
            Entry<String, JsonNode> field = fields.next();
            Iterator<Entry<String, JsonNode>> methodsFields = field.getValue().fields();
            while (methodsFields.hasNext()) {
                //request building
                Entry<String, JsonNode> methodNodeMap = methodsFields.next();
                JsonNode methodNode = methodNodeMap.getValue();
                JsonNode requestBodyNode = methodNode.get("requestBody");
                RequestBody requestBody = null;
                if (requestBodyNode != null) {
                    Entry<String, JsonNode> requestBodyContent = requestBodyNode.get("content").fields().next();
                    requestBody = RequestBody.builder()
                            .content(Content.builder()
                                    .contentType(requestBodyContent.getKey())
                                    .schema(getSchemaFromNode(api, requestBodyContent.getValue().get("schema")).get(0)) //TODO: allow multi schemas
                                    .build())
                            .description(Objects.nonNull(requestBodyNode.get("description")) ? requestBodyNode.get("description").asText() : null)
                            .required(Objects.nonNull(requestBodyNode.get("required")) ? requestBodyNode.get("required").asBoolean() : null)
                            .build();
                }
                //response building
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
                                    .schema(getSchemaFromNode(api, contentNode.getValue().get("schema")).get(0))
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

    private static void fillInfo(API api, JsonNode node) {
        api.setInfo(Info.builder()
                .title(node.get("title").asText())
                .description(Objects.nonNull(node.get("description")) ? node.get("description").asText() : null)
                .contact(Objects.nonNull(node.get("contact")) ? node.get("contact").asText() : null)
                .version(Objects.nonNull(node.get("version")) ? node.get("version").asText() : null)
                .build());
    }

    public static Schema getSchemaByRef(@NonNull API api,
                                        @NonNull String schemaRef) {
        Schema schema = api.getSchemasMap().getOrDefault(schemaRef, null);
        if (schema == null) {
            throw new IllegalStateException("Schema is null by ref %s".formatted(schemaRef));
        }
//        if (Objects.nonNull(schema.getProperties())) {
//            schema.getProperties().forEach(s -> {
//                if (s.getRef() != null) {
//                    s = getSchemaByRef(api, s.getRef());
//                }
//            });
//        }
        return schema;
    }

}
