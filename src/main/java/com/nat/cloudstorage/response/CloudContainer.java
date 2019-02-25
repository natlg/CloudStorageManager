package com.nat.cloudstorage.response;

import java.util.Set;
import java.util.TreeSet;

public class CloudContainer {
    private Set<Cloud> clouds = new TreeSet<>();
    private String userEmail;
    private String userFirstName;
    private String userLastName;

    public CloudContainer(Set<Cloud> clouds, String userEmail, String userFirstName, String userLastName) {
        this.clouds = clouds;
        this.userEmail = userEmail;
        this.userFirstName = userFirstName;
        this.userLastName = userLastName;
    }

    public CloudContainer(String userEmail, String userFirstName, String userLastName) {
        this.userEmail = userEmail;
        this.userFirstName = userFirstName;
        this.userLastName = userLastName;
    }

    public void addCloud(String accountName, String service) {
        Cloud cloud = new Cloud(accountName, service);
        clouds.add(cloud);
    }

    public Set<Cloud> getClouds() {
        return clouds;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public String getUserFirstName() {
        return userFirstName;
    }

    public String getUserLastName() {
        return userLastName;
    }

    private class Cloud implements Comparable<Cloud> {
        public String accountName;
        public String service;

        public Cloud(String accountName, String service) {
            this.accountName = accountName;
            this.service = service;
        }

        @Override
        public int compareTo(Cloud o) {
            char ch1 = this.accountName.toLowerCase().charAt(0);
            char ch2 = o.accountName.toLowerCase().charAt(0);
            if (ch1 > ch2) {
                return 1;
            }
            return -1;
            //no 0 for equal so all clouds will be added anyway
        }
    }
}

