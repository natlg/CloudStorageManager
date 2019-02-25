package com.nat.cloudstorage.service;

import com.nat.cloudstorage.model.User;

public interface UserService {
    public User findUserByEmail(String email);

    public void saveUser(User user);

    public String createAndSaveUser(String email, String firstName, String lastName, String password);
}
