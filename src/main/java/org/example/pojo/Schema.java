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
    private String title;
    private String type;
    private List<String> eNum;
    private LinkedHashSet<Property> properties;
    private String description;

    @Accessors(chain = true)
    @Builder
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Property {
        private String name;
        private Integer maxLength;
        private String type;
        private String description;
        private List<String> eNum;
        private String format;
        private Float multipleOf;
        private String defaultValue;
        private Schema schema;
        private String schemaRef;

        public Property(String name) { this.name = name; }
    }

}
