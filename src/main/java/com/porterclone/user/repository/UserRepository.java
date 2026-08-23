package com.porterclone.user.repository;

import com.porterclone.user.entity.Role;
import com.porterclone.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByPhone(String phone);
    boolean existsByPhone(String phone);

    Optional<List<User>> findByRole(Role role);
}
