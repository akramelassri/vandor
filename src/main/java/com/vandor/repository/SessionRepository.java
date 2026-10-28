package com.vandor.repository;

import com.vandor.models.Session;
import jakarta.persistence.EntityManager;

public class SessionRepository extends Repository<Session, Long> {

    public SessionRepository(EntityManager em) {
        super(em, Session.class);
    }
}
