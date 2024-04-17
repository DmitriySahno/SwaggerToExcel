package org.example.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.LinkedHashSet;
import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Schema {
    private String name;
    private String type;
    private String description;
    private List<String> eNum;

    //    private String title;
    private LinkedHashSet<Property> properties;
    private String summary;

    @Accessors(chain = true)
    @Builder
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Property {
        private String name;
        private String type;
        private String description;
        private String example;
        private List<String> eNum;

        private Integer maxLength;
        private String format;
        private Float multipleOf;
        private String defaultValue;
        private Schema schema;
        private String schemaRef;
        private LinkedHashSet<Property> properties;

        public Property(String name) { this.name = name; }

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
         *
         *
         *
         *
         */

    }

}
