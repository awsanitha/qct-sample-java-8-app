package com.bhoruka.bloodbank.dao.repository;

import com.bhoruka.bloodbank.dao.entity.Patient;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface PatientRepository extends CrudRepository<Patient, String>, PagingAndSortingRepository<Patient, String> {
}
