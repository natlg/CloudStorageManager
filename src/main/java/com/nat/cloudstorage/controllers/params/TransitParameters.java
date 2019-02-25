package com.nat.cloudstorage.controllers.params;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY)

public class TransitParameters {
    public String parentId = "";
    public String cloudSource = "";
    public String pathSource = "";
    public String idSource = "";
    public String downloadUrl = "";
    public String cloudDest = "";
    public String pathDest = "";
    public String idDest = "";
    public String fileName = "";


    public TransitParameters() {
    }

    public TransitParameters(String parentId, String cloudSource, String pathSource, String idSource, String downloadUrl,
                             String cloudDest, String pathDest, String idDest, String fileName) {
        this.parentId = parentId;
        this.cloudSource = cloudSource;
        this.pathSource = pathSource;
        this.idSource = idSource;
        this.downloadUrl = downloadUrl;
        this.cloudDest = cloudDest;
        this.pathDest = pathDest;
        this.idDest = idDest;
        this.fileName = fileName;
    }

    @Override
    public String toString() {
        return "TransitParameters, cloudSource: " + cloudSource + ", pathSource: " + pathSource + ", idSource: " + idSource +
                ", cloudDest: " + cloudDest + ", pathDest: " + pathDest + ", idDest: " + idDest + ", downloadUrl: " + downloadUrl + ", fileName: " + fileName + ", parentId: " + parentId;
    }
}
