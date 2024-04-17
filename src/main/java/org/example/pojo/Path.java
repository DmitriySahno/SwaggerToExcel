package org.example.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashSet;
import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Path {

    private String uri;
    private LinkedHashSet<RequestMethod> methods;

    @Builder
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RequestMethod {
        private String name;
        private String tags;
        private String summary;
        private String description;
        private String operationId;
        private LinkedHashSet<Parameter> parameters;
        private RequestBody requestBody;
        private LinkedHashSet<Response> responses;
        private Boolean deprecated;
    }

    @Builder
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private String code;
        private String description;
        private Content content;
        private Boolean required;
    }

    @Builder
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RequestBody {
        private String description;
        private Path.Content content;
        private Boolean required;
    }

    @Builder
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Content {
        private String contentType;
        private Schema schema; //may be oneOf
    }

    @Builder
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Parameter {
        private String name;
    }

}
