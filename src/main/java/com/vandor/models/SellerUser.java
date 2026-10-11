package com.vandor.models;

import jakarta.persistence.*;

@Entity
@Table(name = "seller_users")
public class SellerUser {
    @Id
    @Column(name = "seller_id")
    private Long sellerId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "seller_id")
    private User user;

    public SellerUser(User user) {
        this.user = user;
    }

    // For JPA
    protected SellerUser() {
    }
}
