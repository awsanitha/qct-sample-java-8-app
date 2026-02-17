package com.bhoruka.bloodbank.dao.repository;

import com.bhoruka.bloodbank.dao.entity.BloodComponent;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface BloodComponentRepository extends CrudRepository<BloodComponent, String>, PagingAndSortingRepository<BloodComponent, String> {
}
