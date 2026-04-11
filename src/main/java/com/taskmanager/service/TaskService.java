package com.taskmanager.service;

import com.taskmanager.entity.Task;
import com.taskmanager.repository.TaskRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
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
        initializeDisplayOrderIfNeeded();
        return taskRepository.findAllByOrderByDoneAscDisplayOrderAscIdAsc();
    }

    public Task saveTask(Task task) {
        if (task.getDisplayOrder() == null) {
            Integer maxOrder = taskRepository.findAll()
                    .stream()
                    .map(Task::getDisplayOrder)
                    .filter(order -> order != null)
                    .max(Integer::compareTo)
                    .orElse(0);

            task.setDisplayOrder(maxOrder + 1);
        }

        return taskRepository.save(task);
    }

    public void deleteTask(Long id) {
        taskRepository.deleteById(id);
        normalizeDisplayOrders();
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
        initializeDisplayOrderIfNeeded();
        return taskRepository.findByPriorityOrderByDoneAscDisplayOrderAscIdAsc(priority);
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
        initializeDisplayOrderIfNeeded();
        return taskRepository.findByCategoryOrderByDoneAscDisplayOrderAscIdAsc(category);
    }

    public List<Task> getTodayTasks() {
        LocalDate today = LocalDate.now();
        return getAllTasks()
                .stream()
                .filter(task -> task.getDueDate() != null && task.getDueDate().equals(today))
                .toList();
    }

    public void reorderTasks(List<Long> orderedIds) {
        List<Task> allTasks = taskRepository.findAll();

        Map<Long, Task> taskMap = new LinkedHashMap<>();
        for (Task task : allTasks) {
            taskMap.put(task.getId(), task);
        }

        int order = 1;
        for (Long id : orderedIds) {
            Task task = taskMap.get(id);
            if (task != null) {
                task.setDisplayOrder(order++);
            }
        }

        taskRepository.saveAll(allTasks);
    }

    private void initializeDisplayOrderIfNeeded() {
        List<Task> tasks = taskRepository.findAll();

        boolean needsInit = tasks.stream().anyMatch(task -> task.getDisplayOrder() == null);

        if (needsInit) {
            List<Task> sorted = tasks.stream()
                    .sorted(Comparator.comparing(Task::isDone)
                            .thenComparing(Task::getId))
                    .toList();

            int order = 1;
            for (Task task : sorted) {
                task.setDisplayOrder(order++);
            }

            taskRepository.saveAll(sorted);
        }
    }

    private void normalizeDisplayOrders() {
        List<Task> tasks = taskRepository.findAllByOrderByDoneAscDisplayOrderAscIdAsc();

        int order = 1;
        for (Task task : tasks) {
            task.setDisplayOrder(order++);
        }

        taskRepository.saveAll(tasks);
    }

    public List<Task> getTasksNewestFirst() {
        List<Task> allTasks = taskRepository.findAllByOrderByIdDesc();
        return sortUndoneAndKeepDoneLast(allTasks);
    }

    public List<Task> getTasksOldestFirst() {
        List<Task> allTasks = taskRepository.findAllByOrderByIdAsc();
        return sortUndoneAndKeepDoneLast(allTasks);
    }

    public List<Task> getTasksAlphabetically() {
        List<Task> allTasks = taskRepository.findAllByOrderByTitleAsc();
        return sortUndoneAndKeepDoneLast(allTasks);
    }

    public List<Task> getTasksByNearestDueDate() {
        List<Task> allTasks = taskRepository.findAll();
        LocalDate today = LocalDate.now();

        List<Task> undoneUpcomingWithDate = allTasks.stream()
                .filter(task -> !task.isDone())
                .filter(task -> task.getDueDate() != null)
                .filter(task -> !task.getDueDate().isBefore(today))
                .sorted(Comparator.comparing(Task::getDueDate))
                .toList();

        List<Task> undoneWithoutDate = allTasks.stream()
                .filter(task -> !task.isDone())
                .filter(task -> task.getDueDate() == null)
                .toList();

        List<Task> doneTasks = allTasks.stream()
                .filter(Task::isDone)
                .toList();

        List<Task> result = new ArrayList<>();
        result.addAll(undoneUpcomingWithDate);
        result.addAll(undoneWithoutDate);
        result.addAll(doneTasks);

        return result;
    }
    public List<Task> getTasksByPriorityOrder() {
        List<Task> allTasks = taskRepository.findAll();

        List<Task> undoneTasks = allTasks.stream()
                .filter(task -> !task.isDone())
                .sorted(Comparator.comparingInt(task -> priorityRank(task.getPriority())))
                .toList();

        List<Task> doneTasks = allTasks.stream()
                .filter(Task::isDone)
                .toList();

        List<Task> result = new ArrayList<>();
        result.addAll(undoneTasks);
        result.addAll(doneTasks);

        return result;
    }

    private List<Task> sortUndoneAndKeepDoneLast(List<Task> allTasks) {
        List<Task> undoneTasks = allTasks.stream()
                .filter(task -> !task.isDone())
                .toList();

        List<Task> doneTasks = allTasks.stream()
                .filter(Task::isDone)
                .toList();

        List<Task> result = new ArrayList<>();
        result.addAll(undoneTasks);
        result.addAll(doneTasks);

        return result;
    }
    private int priorityRank(String priority) {
        if (priority == null) return 4;

        return switch (priority) {
            case "P1" -> 1;
            case "P2" -> 2;
            case "P3" -> 3;
            default -> 4;
        };
    }
    public List<Task> getOverdueTasks() {
        LocalDate today = LocalDate.now();

        List<Task> overdueTasks = taskRepository.findAll().stream()
                .filter(task -> !task.isDone())
                .filter(task -> task.getDueDate() != null)
                .filter(task -> task.getDueDate().isBefore(today))
                .sorted(Comparator.comparing(Task::getDueDate))
                .toList();

        List<Task> doneTasks = taskRepository.findAll().stream()
                .filter(Task::isDone)
                .toList();

        List<Task> result = new ArrayList<>();
        result.addAll(overdueTasks);
        result.addAll(doneTasks);

        return result;
    }
}