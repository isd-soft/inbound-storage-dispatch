package com.isd.wms.repository;

import com.isd.wms.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileRepository extends JpaRepository<User, Long> {
}
