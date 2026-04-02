package com.vieterp.otb.domain.repository;

import com.vieterp.otb.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    @Query("SELECT p FROM Product p WHERE p.subCategoryId = :subCategoryId")
    List<Product> findBySubCategoryId(@Param("subCategoryId") Long subCategoryId);

    @Query("SELECT p FROM Product p WHERE p.subCategoryId = :subCategoryId AND p.brandId = :brandId AND p.isActive = true")
    List<Product> findActiveBySubCategoryIdAndBrandId(@Param("subCategoryId") Long subCategoryId, @Param("brandId") Long brandId);
}
