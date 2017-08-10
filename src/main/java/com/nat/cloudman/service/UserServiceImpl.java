package com.nat.cloudman.service;


import java.util.Arrays;
import java.util.HashSet;

import com.nat.cloudman.model.Role;
import com.nat.cloudman.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.nat.cloudman.repository.RoleRepository;
import com.nat.cloudman.repository.UserRepository;

@Service("userService")
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;

    @Override
    public User findUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public void saveUser(User user) {
        System.out.println("saveUser");
        user.setActive(1);
        Role userRole = roleRepository.findByRole("ADMIN");
        user.setRoles(new HashSet<Role>(Arrays.asList(userRole)));
        userRepository.save(user);
    }
}
