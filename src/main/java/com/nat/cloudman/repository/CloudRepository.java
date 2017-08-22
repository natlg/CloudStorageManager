package com.nat.cloudman.repository;

import com.nat.cloudman.model.Cloud;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository("cloudRepository")
public interface CloudRepository extends JpaRepository<Cloud, Long> {

}