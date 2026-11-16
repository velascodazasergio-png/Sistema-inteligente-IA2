package com.mafe.incidentai.repository;

import com.mafe.incidentai.domain.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// Repository to read and manage the technical categories
public interface CategoryRepository extends JpaRepository<Category, Long> {
    // Only the active categories
    List<Category> findByActiveTrue();
}

