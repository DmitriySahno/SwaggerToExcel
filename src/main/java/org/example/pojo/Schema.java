package org.example.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * Schema can be described as:
 * <ul>
 *     <li><i>{@link Schema#type}</i> - schema properties will be defined from {@link Schema#properties} for object type, or from other properties in this schema related to type</li>
 *     <li><i>{@link Schema#allOf}</i> - schema contains nested schemas in allOf-list</li>
 *     <li><i>{@link Schema#oneOf}</i> - schema contains nested schemas in oneOf-list</li>
 *     <li><i>{@link Schema#ref}</i> - schema reference stored in #/components/schemas/ structure</li>
 * </ul>
 **/
@Accessors(chain = true)
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Schema {
    private String name;
    @Builder.Default
    private List<Schema> allOf = new LinkedList<>();
    @Builder.Default
    private List<Schema> oneOf = new LinkedList<>();
    private String ref; //when schema described outside, get this out?

    @Builder.Default
    private String type = "";
    private String description;
    private List<String> eNum;

    private String format; //type: string
    private Integer maxLength; //type: string
    private Float multipleOf; //type: number
    private String defaultValue; //type: ?
    private List<Schema> properties; //type: object
    private List<Schema> items; //type: array

    public Schema(String name) { this.name = name; }

    public Schema addAllOf(List<Schema> schemas) {
        allOf.addAll(schemas);
        return this;
    }

    public Schema addOneOf(List<Schema> schemas) {
        oneOf.addAll(schemas);
        return this;
    }

    public void fillFrom(Schema schema) {
        name=Objects.isNull(name) ? schema.name : name;
        type= Objects.isNull(type) || type.isEmpty()
                ? schema.type
                : type.equals("array")
                    ? "array[" + schema.type + "]"
                    : type;
        description= Objects.nonNull(description) ? description : schema.description;
        ref=schema.ref;

        allOf.addAll(schema.allOf);
        oneOf.addAll(schema.oneOf);
        eNum=schema.eNum;
        format=schema.format;
        maxLength=schema.maxLength;
        multipleOf=schema.multipleOf;
        defaultValue=schema.defaultValue;
        properties=schema.properties;
        items=schema.items;
    }

    /**
     * common structure:
     * String name;
     * String type; ?
     * String description; ?
     * String example; ?
     * String enum; ?
     *
     * type: ARRAY:
     * Property[] items;
     *
     * type: STRING:
     * Integer maxLength;
     * String format;
     *
     * type: OBJECT:
     * Property[] properies;
     *
     * type: BOOLEAN:
     * --not specified
     *
     * type: NUMBER:
     * Decimal multipleOf; 0.001
     * String format; float
     *
     * type: INTEGER:
     * minimum: 0
     * maximum: 31
     *
     * $ref: '#/components/schemas/Remain'
     */

}
