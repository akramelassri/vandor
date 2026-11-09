package com.vandor.repository;

import com.vandor.models.CustomerUser;
import jakarta.persistence.EntityManager;

public class CustomerUserRepository extends Repository<CustomerUser, Long> {

  public CustomerUserRepository(EntityManager em) {
    super(em, CustomerUser.class);
  }
}
