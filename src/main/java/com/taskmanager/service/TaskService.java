package com.taskmanager.service;

import com.taskmanager.entity.Task;
import com.taskmanager.repository.TaskRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class TaskService {

    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public List<Task> getAllTasks() {
        return taskRepository.findAllByOrderByDoneAscIdDesc();
    }

    public Task saveTask(Task task) {
        return taskRepository.save(task);
    }

    public void deleteTask(Long id) {
        taskRepository.deleteById(id);
    }

    public Task getTaskById(Long id) {
        return taskRepository.findById(id).orElse(null);
    }

    public void toggleDone(Long id) {
        Task task = taskRepository.findById(id).orElse(null);
        if (task != null) {
            task.setDone(!task.isDone());
            taskRepository.save(task);
        }
    }

    public List<Task> getTasksByPriority(String priority) {
        return taskRepository.findByPriorityOrderByDoneAsc(priority);
    }

    public Map<LocalDate, List<Task>> getUpcomingTasksGroupedByDate() {
        LocalDate today = LocalDate.now();
        LocalDate nextWeek = today.plusDays(7);

        List<Task> tasks = taskRepository.findByDueDateBetweenAndDoneFalseOrderByDueDateAsc(today, nextWeek);
        Map<LocalDate, List<Task>> groupedTasks = new LinkedHashMap<>();

        for (Task task : tasks) {
            LocalDate dueDate = task.getDueDate();

            if (!groupedTasks.containsKey(dueDate)) {
                groupedTasks.put(dueDate, new ArrayList<>());
            }

            groupedTasks.get(dueDate).add(task);
        }

        return groupedTasks;
    }

    public Map<String, List<Task>> getUpcomingTasksGroupedByDay(LocalDate startDate) {

        LocalDate endDate = startDate.plusDays(6);

        List<Task> tasks = taskRepository
                .findByDueDateBetweenAndDoneFalseOrderByDueDateAsc(startDate, endDate);

        Map<String, List<Task>> grouped = new LinkedHashMap<>();

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("d MMMM", Locale.ENGLISH);
        DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("EEEE", Locale.ENGLISH);

        for (int i = 0; i < 7; i++) {
            LocalDate date = startDate.plusDays(i);

            String label;
            if (date.equals(LocalDate.now())) {
                label = "Today";
            } else if (date.equals(LocalDate.now().plusDays(1))) {
                label = "Tomorrow";
            } else {
                label = date.format(dayFormatter) + ", " + date.format(dateFormatter);
            }

            LocalDate finalDate = date;

            List<Task> dayTasks = tasks.stream()
                    .filter(task -> finalDate.equals(task.getDueDate()))
                    .toList();

            grouped.put(label, dayTasks);
        }

        return grouped;
    }

    public List<Task> getTasksByCategory(String category) {
        return taskRepository.findByCategoryOrderByDoneAsc(category);
    }

}