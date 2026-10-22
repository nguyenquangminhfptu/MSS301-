package com.mss301.payment.repository;

import com.mss301.payment.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from Wallet w where w.customerId = :id")
    Optional<Wallet> findForUpdate(@Param("id") String id);
}
