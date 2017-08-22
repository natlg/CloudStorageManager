package com.nat.cloudman.service;

import com.nat.cloudman.model.Cloud;
import com.nat.cloudman.model.Role;
import com.nat.cloudman.repository.CloudRepository;
import com.nat.cloudman.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;

@Service("cloudService")
public class CloudServiceImpl implements CloudService {

    @Autowired
    private CloudRepository cloudRepository;

    @Override
    public void saveCloud(Cloud cloud) {
        System.out.println("saveCloud");
        cloudRepository.save(cloud);
    }
}
