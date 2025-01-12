package io.pivotal.jira;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RemoteLink {

    String url;
    String title;

    @JsonProperty("object")
    private void unpackInformationFromNestedObject(Map<String, Object> nestedObject) {
        url = (String) nestedObject.get("url");
        title = (String) nestedObject.get("title");
    }
}
