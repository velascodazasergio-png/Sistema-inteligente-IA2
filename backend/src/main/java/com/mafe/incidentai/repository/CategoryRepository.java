package com.mafe.incidentai.repository;

import com.mafe.incidentai.domain.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// Repositorio para consultar y administrar las categorias tecnicas
public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByActiveTrue();
}

