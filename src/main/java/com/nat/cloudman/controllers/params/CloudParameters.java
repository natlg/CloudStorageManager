package com.nat.cloudman.controllers.params;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY)
public class CloudParameters {
    public String cloudName = "";
    public String path = "";
    public String id = "";

    @Override
    public String toString() {
        return "CloudParameters cloudName: " + cloudName + ", path: " + path + ", id: " + id;
    }
}
