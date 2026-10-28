package com.vandor.repository;

import com.vandor.models.SellerUser;
import jakarta.persistence.EntityManager;

public class SellerUserRepository extends Repository<SellerUser, Long> {

    public SellerUserRepository(EntityManager em) {
        super(em, SellerUser.class);
    }
}
