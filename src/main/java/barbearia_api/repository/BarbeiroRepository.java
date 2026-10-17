package barbearia_api.repository;

import barbearia_api.entity.Barbeiro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;

@Repository
public interface BarbeiroRepository extends JpaRepository<Barbeiro, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Barbeiro b where b.id = :id")
    java.util.Optional<Barbeiro> findByIdForUpdate(Long id);

}
