package com.nat.cloudman.service;

import com.nat.cloudman.model.User;

public interface UserService {
    public User findUserByEmail(String email);

    public void saveUser(User user);

    public String createAndSaveUser(String email, String firstName, String lastName, String password);
}
