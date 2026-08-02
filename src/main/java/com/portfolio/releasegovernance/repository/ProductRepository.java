package com.portfolio.releasegovernance.repository;

import com.portfolio.releasegovernance.domain.Product;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProductRepository extends JpaRepository<Product, UUID> {
    Optional<Product> findByNameIgnoreCase(String name);

    @Query("""
            select p.id from Product p
            where p.active = true
              and p.sourceType <> com.portfolio.releasegovernance.domain.DomainEnums.SourceType.MANUAL
              and (p.nextCheckAfter is null or p.nextCheckAfter <= :now)
            order by p.name
            """)
    List<UUID> findProductsDueForCheck(Instant now);
}
