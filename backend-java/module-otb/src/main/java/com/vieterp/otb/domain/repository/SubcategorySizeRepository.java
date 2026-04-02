package com.vieterp.otb.domain.repository;

import com.vieterp.otb.domain.SubcategorySize;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubcategorySizeRepository extends JpaRepository<SubcategorySize, Long> {

    @Query("SELECT s FROM SubcategorySize s WHERE s.subCategoryId = :subCategoryId ORDER BY s.name ASC")
    List<SubcategorySize> findBySubCategoryId(@Param("subCategoryId") Long subCategoryId);
}
