package com.nat.cloudstorage.service;

import com.nat.cloudstorage.cloud.UserManager;
import com.nat.cloudstorage.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.nat.cloudstorage.repository.UserRepository;

@Service("userService")
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;
    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    @Override
    public User findUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public void saveUser(User user) {
        logger.debug("saveUser");
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
        logger.debug("result: " + result);
        return result;
    }
}
