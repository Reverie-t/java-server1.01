package cn.edu.sdu.java.server.repositorys;

import cn.edu.sdu.java.server.models.Notice;
import cn.edu.sdu.java.server.models.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NoticeRepository extends JpaRepository<Notice, Long> {
    Optional<Notice> findById(Integer personId);



    @Query(value = "from Student where ?1='' or person.num like %?1% or person.name like %?1% ")
    List<Notice> findNoticeListByTitle(String title);



}
