package org.example.pojo;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

@ToString
@Data
@NoArgsConstructor
public class API {
    private String openapi;
    private Info info;
    private final LinkedHashSet<Server> servers = new LinkedHashSet<>();
    private final LinkedHashSet<Path> paths = new LinkedHashSet<>();
    private final Map<String, Schema> schemasMap = new HashMap<>();
}
