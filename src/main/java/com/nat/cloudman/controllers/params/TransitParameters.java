package com.nat.cloudman.controllers.params;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY)

public class TransitParameters {
    public String cloudSource = "";
    public String pathSource = "";
    public String idSource = "";
    public String downloadUrl = "";
    public String cloudDest = "";
    public String pathDest = "";
    public String idDest = "";
    public String fileName = "";

    @Override
    public String toString() {
        return "TransitParameters, cloudSource: " + cloudSource + ", pathSource: " + pathSource + ", idSource: " + idSource +
                ", cloudDest: " + cloudDest + ", pathDest: " + pathDest + ", idDest: " + idDest + ", downloadUrl: " + downloadUrl + ", fileName: " + fileName;
    }
}