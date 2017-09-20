package com.nat.cloudman.response;

public class DownloadedFileContainer {

    private byte[] byteArray;
    private String name;

    public byte[] getByteArray() {
        return byteArray;
    }

    public String getName() {
        return name;
    }

    public void setByteArray(byte[] byteArray) {
        this.byteArray = byteArray;
    }

    public void setName(String name) {
        this.name = name;
    }

    public DownloadedFileContainer(String name, byte[] byteArray) {
        this.name = name;
        this.byteArray = byteArray;
    }
}
