package com.example.DrugSecure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;


@Repository
public interface FileRepository extends JpaRepository<DrugFile, UUID> {

    @Query("SELECT d FROM DrugFile d WHERE d.userId = ?1")
    List<DrugFile> findFilesByUserId(UUID userId);

}
