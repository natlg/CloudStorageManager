package com.nat.cloudman.model;

import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotEmpty;

import javax.persistence.*;

@Entity
@Table(name = "cloud")
public class Cloud {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "cloud_id")
    private long id;


    @Column(name = "account_name")
    private String accountName;

    @Column(name = "access_token")
    private String token;


    @Column(name = "account_email")
    private String accountEmail;

    @Column(name = "cloud_service")
    @NotEmpty(message = "*Please provide your Cloud Service")
    private String cloudService;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getAccountEmail() {
        return accountEmail;
    }

    public void setAccountEmail(String accountEmail) {
        this.accountEmail = accountEmail;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getCloudService() {
        return cloudService;
    }

    public void setCloudService(String cloudService) {
        this.cloudService = cloudService;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
