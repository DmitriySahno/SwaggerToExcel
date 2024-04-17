package org.example.pojo;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

/**
 * POJO class, that describes swagger api scheme
 */
@ToString
@Data
@NoArgsConstructor
public class API {
    private String openapi;
    private Info info;
    private final LinkedHashSet<Server> servers = new LinkedHashSet<>();
    /**
     * array of http urls in paths tag
     */
    private final LinkedHashSet<Path> paths = new LinkedHashSet<>();
    /**
     * map of schemas, placed for instance in components/schemas
     */
    private final Map<String, Schema> schemasMap = new HashMap<>();
}
