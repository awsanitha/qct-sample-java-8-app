package com.bhoruka.bloodbank.dao.repository;

import com.bhoruka.bloodbank.dao.entity.Inventory;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface InventoryRepository extends CrudRepository<Inventory, String>, PagingAndSortingRepository<Inventory, String> {
}
