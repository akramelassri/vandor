package com.vandor.repository;

import com.vandor.models.User;
import jakarta.persistence.EntityManager;

public class UserRepository extends Repository<User, Long> {

    public UserRepository(EntityManager em) {
        super(em, User.class);
    }
}
