package com.mafe.incidentai.repository;

import com.mafe.incidentai.domain.entity.UserPreferences;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// Repository for the interface preferences of each user (id = user id)
@Repository
public interface UserPreferencesRepository extends JpaRepository<UserPreferences, Long> {
}
