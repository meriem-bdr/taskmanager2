package com.taskmanager.repository;

import com.taskmanager.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.time.LocalDate;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findAllByOrderByDoneAscIdDesc();

    List<Task> findAllByOrderByDoneAsc();

    List<Task> findByPriorityOrderByDoneAsc(String priority);

    List<Task> findByDueDateBetweenOrderByDueDateAsc(LocalDate startDate, LocalDate endDate);

    List<Task> findByDueDateBetweenAndDoneFalseOrderByDueDateAsc(LocalDate startDate, LocalDate endDate);

    List<Task> findByCategoryOrderByDoneAsc(String category);

    List<Task> findAllByOrderByDoneAscDisplayOrderAscIdAsc();

    List<Task> findByPriorityOrderByDoneAscDisplayOrderAscIdAsc(String priority);

    List<Task> findByCategoryOrderByDoneAscDisplayOrderAscIdAsc(String category);
}
