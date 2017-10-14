package com.nat.cloudman.model;

import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotEmpty;

import javax.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "cloud")
public class Cloud {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "cloud_id")
    private long id;

    @Column(name = "account_name")
    @NotEmpty(message = "*Please provide account name")
    private String accountName;

    @Column(name = "access_token", length = 1300)
    @NotEmpty(message = "*Please provide access token")
    private String accessToken;

    @Column(name = "refresh_token", length = 600)
    @NotEmpty(message = "*Please provide refresh token")
    private String refreshToken;

    @Column(name = "account_email")
    private String accountEmail;

    @Column(name = "cloud_service")
    @NotEmpty(message = "*Please provide Cloud Service")
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

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, accountName, accessToken, refreshToken, accountEmail, cloudService);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof Cloud)) {
            return false;
        }
        Cloud cloud = (Cloud) o;
        return id == cloud.id &&
                Objects.equals(accountName, cloud.accountName) &&
                Objects.equals(accessToken, cloud.accessToken) &&
                Objects.equals(refreshToken, cloud.refreshToken) &&
                Objects.equals(accountEmail, cloud.accountEmail) &&
                Objects.equals(cloudService, cloud.cloudService);
    }

}
