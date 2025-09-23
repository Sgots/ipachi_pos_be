package com.ipachi.pos.repo;


import com.ipachi.pos.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByUserId(Long userId);
    Optional<Transaction> findByIdAndUserId(Long id, Long userId);

    long countByBusinessIdAndCreatedAtBetween(Long businessId, OffsetDateTime start, OffsetDateTime end);
    @Query(value = """
select 
  case 
    when u.role in ('ADMIN','ROLE_ADMIN') and nullif(up.physical_address,'') is not null 
      then up.physical_address
    when ls.name is not null 
      then ls.name
    else 'Unknown'
  end                           as location,
  coalesce(sum(t.total), 0)     as total
from tx_head t
join users u
  on u.user_id = t.created_by_user_id
left join user_profiles up
  on up.user_id = u.user_id
left join staff_members sm
  on sm.user_id = u.user_id
 and sm.business_id = t.business_id
 and sm.active = 1
left join staff_locations ls          -- << if your table is different, change this name
  on ls.id = sm.location_id
where t.business_id = :biz
  and t.created_at between :start and :end
group by location
order by total desc
""", nativeQuery = true)
    List<Object[]> salesByLocation(
            @Param("biz") Long businessId,
            @Param("start") OffsetDateTime start,
            @Param("end") OffsetDateTime end
    );

    @Query(value = """
        select date_format(t.created_at, '%Y-%m') as period, coalesce(sum(t.total),0) as total
        from tx_head t
        where t.business_id = :biz and t.created_at between :start and :end
        group by date_format(t.created_at, '%Y-%m')
        order by period
    """, nativeQuery = true)
    List<Object[]> monthlySales(
            @Param("biz") Long businessId,
            @Param("start") OffsetDateTime start,
            @Param("end") OffsetDateTime end
    );
    @Query("""
      select coalesce(sum(t.total), 0)
      from Transaction t
      where t.businessId = :biz and t.createdAt between :start and :end
    """)
    BigDecimal sumTotal(Long biz, OffsetDateTime start, OffsetDateTime end);
}
