package com.his.repositories;

import com.his.models.Visit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface VisitRepository extends JpaRepository<Visit, UUID> {
}

