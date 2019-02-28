package com.nat.cloudstorage.model;


import java.util.Set;

import javax.persistence.*;

import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.annotation.Transient;

@Entity
@Table(name = "user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "user_id")
    private long id;

    @Column(name = "email")
    @Email(message = "*Please provide a valid Email")
    @NotEmpty(message = "*Please provide an email")
    private String email;

    @Column(name = "password_hash")
    @Length(min = 5, message = "*Your password_hash must have at least 5 characters")
    @NotEmpty(message = "*Please provide your password_hash")
    @Transient
    private String passwordHash;

    @Column(name = "name")
    @NotEmpty(message = "*Please provide your name")
    private String name;

    @Column(name = "last_name")
    @NotEmpty(message = "*Please provide your last name")
    private String lastName;

    @Column(name = "active")
    private int active;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinTable(name = "user_cloud", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "cloud_id"))
    private Set<Cloud> clouds;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public int getActive() {
        return active;
    }

    public void setActive(int active) {
        this.active = active;
    }

    public Set<Cloud> getClouds() {
        return clouds;
    }

    public void setClouds(Set<Cloud> clouds) {
        this.clouds = clouds;
    }

    public void addCloud(Cloud cloud) {
        clouds.add(cloud);
    }

    public void deleteCloud(Cloud cloud) {
        boolean removed = clouds.remove(cloud);
    }
}
