package com.bhoruka.bloodbank.dao.repository;

import com.bhoruka.bloodbank.dao.entity.Sample;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface SampleRepository extends CrudRepository<Sample, String>, PagingAndSortingRepository<Sample, String> {
}
