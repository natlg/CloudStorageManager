package com.nat.cloudman.service;

import com.nat.cloudman.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.nat.cloudman.repository.UserRepository;

@Service("userService")
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public User findUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public void saveUser(User user) {
        System.out.println("saveUser");
        user.setActive(1);
        userRepository.save(user);
    }

    @Override
    public String createAndSaveUser(String email, String firstName, String lastName, String password) {
        String result = "";
        User userExists = findUserByEmail(email);
        if (userExists != null) {
            result = "User already exists";
        } else {
            User user = new User();
            user.setEmail(email);
            user.setName(firstName);
            user.setLastName(lastName);
            user.setPassword(password);
            saveUser(user);
            result = "User was saved";
        }
        System.out.println("result: " + result);
        return result;
    }
}
