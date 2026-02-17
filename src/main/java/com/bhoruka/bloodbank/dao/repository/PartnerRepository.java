package com.bhoruka.bloodbank.dao.repository;

import com.bhoruka.bloodbank.dao.entity.Partner;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface PartnerRepository extends CrudRepository<Partner, String>, PagingAndSortingRepository<Partner, String> {
}
