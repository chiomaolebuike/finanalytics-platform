package com.finanalytics.finanalytics_platform.repository;

import com.finanalytics.finanalytics_platform.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByCategory(String category);

    @Query("""
            SELECT p FROM Product p
            WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%'))
            """)
    Page<Product> search(@Param("q") String query, Pageable pageable);

    List<Product> findByStockGreaterThan(int minStock);
}
