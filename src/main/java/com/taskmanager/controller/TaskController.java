package com.taskmanager.controller;

import com.taskmanager.entity.Task;
import com.taskmanager.service.TaskService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import jakarta.validation.Valid;

@Controller
public class TaskController {
    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping("/tasks")
    public String getAllTasks(Model model,
                              @RequestParam(required = false) String scrollPos,
                              @RequestParam(required = false) String sort) {

        List<Task> tasks;

        if ("oldest".equals(sort)) {
            tasks = taskService.getTasksOldestFirst();
        } else if ("priority".equals(sort)) {
            tasks = taskService.getTasksByPriorityOrder();
        } else if ("az".equals(sort)) {
            tasks = taskService.getTasksAlphabetically();
        } else if ("dueDate".equals(sort)) {
            tasks = taskService.getTasksByNearestDueDate();
        } else {
            tasks = taskService.getTasksNewestFirst();
        }

        List<Task> overdueTasks = tasks.stream()
                .filter(task -> task.getDueDate() != null)
                .filter(task -> task.getDueDate().isBefore(LocalDate.now()))
                .filter(task -> !task.isDone())
                .toList();

        List<Task> completedTasks = tasks.stream()
                .filter(Task::isDone)
                .toList();

        List<Task> activeTasks = tasks.stream()
                .filter(task -> !task.isDone())
                .filter(task -> !(task.getDueDate() != null
                        && task.getDueDate().isBefore(LocalDate.now())))
                .toList();

        model.addAttribute("tasks", activeTasks);
        model.addAttribute("overdueTasks", overdueTasks);
        model.addAttribute("overdueCount", overdueTasks.size());
        model.addAttribute("completedTasks", completedTasks);
        model.addAttribute("completedCount", completedTasks.size());

        model.addAttribute("task", new Task());
        model.addAttribute("showAddButton", true);
        model.addAttribute("showBackToTasks", false);
        model.addAttribute("backUrl", "/tasks");
        model.addAttribute("currentPriority", null);
        model.addAttribute("currentCategory", null);
        model.addAttribute("pageTitle", "All Tasks");
        model.addAttribute("scrollPos", scrollPos);
        model.addAttribute("currentSort", sort);

        return "tasks";
    }

    @GetMapping("/today")
    public String showTodayTasks(Model model,
                                 @RequestParam(required = false) String scrollPos) {

        Task task = new Task();
        task.setDueDate(LocalDate.now());

        model.addAttribute("tasks", taskService.getTodayTasks());
        model.addAttribute("task", task);
        model.addAttribute("showAddButton", true);
        model.addAttribute("showBackToTasks", false);
        model.addAttribute("backUrl", "/today");
        model.addAttribute("currentPriority", null);
        model.addAttribute("currentCategory", null);
        model.addAttribute("pageTitle", "Today");
        model.addAttribute("scrollPos", scrollPos);
        model.addAttribute("currentSource", "today");

        return "tasks";
    }
    @GetMapping("/priorities")
    public String showPrioritiesPage() {
        return "priorities";
    }

    @GetMapping("/tasks/priority/{priority}")
    public String getTasksByPriority(@PathVariable String priority,
                                     @RequestParam(required = false) String scrollPos,
                                     Model model) {
        model.addAttribute("tasks", taskService.getTasksByPriority(priority));
        model.addAttribute("task", new Task());
        model.addAttribute("showAddButton", false);
        model.addAttribute("showBackToTasks", true);
        model.addAttribute("backUrl", "/priorities");
        model.addAttribute("currentPriority", priority);
        model.addAttribute("currentCategory", null);
        model.addAttribute("pageTitle", "Priority - " + priority);
        model.addAttribute("scrollPos", scrollPos);
        model.addAttribute("currentSource", "priority");
        return "tasks";
    }

    @GetMapping("/tasks/new")
    public String showAddForm(@RequestParam(required = false) String dueDate,
                              @RequestParam(required = false) String source,
                              @RequestParam(required = false) String startDate,
                              @RequestParam(required = false) String targetDate,
                              @RequestParam(required = false) String sort,

                              Model model) {
        Task task = new Task();

        if (dueDate != null && !dueDate.isEmpty()) {
            task.setDueDate(LocalDate.parse(dueDate));
        }

        model.addAttribute("task", task);
        model.addAttribute("source", source);
        model.addAttribute("startDate", startDate);
        model.addAttribute("targetDate", targetDate);

        return "add-task";
    }

    @PostMapping("/tasks")
    public String saveTask(@Valid @ModelAttribute("task") Task task,
                           BindingResult result,
                           @RequestParam(required = false) String priorityFilter,
                           @RequestParam(required = false) String currentCategory,
                           @RequestParam(required = false) String source,
                           @RequestParam(required = false) String startDate,
                           @RequestParam(required = false) String targetDate,
                           @RequestParam(required = false) String sort,
                           Model model) {

        if (result.hasErrors()) {
            model.addAttribute("tasks", taskService.getAllTasks());
            model.addAttribute("task", task);
            model.addAttribute("showAddButton", true);
            model.addAttribute("showBackToTasks", false);
            model.addAttribute("backUrl", "/tasks");
            model.addAttribute("currentPriority", priorityFilter);
            model.addAttribute("currentCategory", currentCategory);
            model.addAttribute("currentSource", source);
            model.addAttribute("pageTitle", "All Tasks");
            model.addAttribute("startDate", startDate);
            model.addAttribute("targetDate", targetDate);
            return "tasks";
        }

        if (task.getPriority() == null || task.getPriority().isEmpty()) {
            task.setPriority("P4");
        }

        taskService.saveTask(task);

        if ("today".equals(source)) {
            return "redirect:/today";
        }

        if ("upcoming".equals(source)) {
            String redirectUrl = "redirect:/upcoming";

            if (startDate != null && !startDate.isEmpty()) {
                redirectUrl += "?startDate=" + startDate;
            }

            if (task.getDueDate() != null) {
                redirectUrl += "#day-" + task.getDueDate();
            } else if (targetDate != null && !targetDate.isEmpty()) {
                redirectUrl += "#day-" + targetDate;
            }

            return redirectUrl;
        }

        if (priorityFilter != null && !priorityFilter.isEmpty()) {
            return "redirect:/tasks/priority/" + priorityFilter;
        }

        if (currentCategory != null && !currentCategory.isEmpty()) {
            return "redirect:/tasks/category/" + currentCategory;
        }

        if (sort != null && !sort.isEmpty()) {
            return "redirect:/tasks?sort=" + sort;
        }

        return "redirect:/tasks";    }

    @GetMapping("/tasks/delete/{id}")
    public String deleteTask(@PathVariable Long id,
                             @RequestParam(required = false) String priority,
                             @RequestParam(required = false) String category,
                             @RequestParam(required = false) String source,
                             @RequestParam(required = false) String startDate,
                             @RequestParam(required = false) String targetDate,
                             @RequestParam(required = false) String sort) {

        taskService.deleteTask(id);

        if ("today".equals(source)) {
            return "redirect:/today";
        }

        if ("upcoming".equals(source)) {
            String redirectUrl = "redirect:/upcoming";

            if (startDate != null && !startDate.isEmpty()) {
                redirectUrl += "?startDate=" + startDate;
            }

            if (targetDate != null && !targetDate.isEmpty()) {
                redirectUrl += "#day-" + targetDate;
            }

            return redirectUrl;
        }

        if (category != null && !category.isEmpty()) {
            return "redirect:/tasks/category/" + category;
        }

        if (priority != null && !priority.isEmpty()) {
            return "redirect:/tasks/priority/" + priority;
        }

        if (sort != null && !sort.isEmpty()) {
            return "redirect:/tasks?sort=" + sort;
        }

        return "redirect:/tasks";    }

    @GetMapping("/tasks/edit/{id}")
    public String showEditForm(@PathVariable Long id,
                               @RequestParam(required = false) String priority,
                               @RequestParam(required = false) String category,
                               @RequestParam(required = false) String source,
                               @RequestParam(required = false) String startDate,
                               @RequestParam(required = false) String targetDate,
                               Model model) {

        Task task = taskService.getTaskById(id);

        model.addAttribute("task", task);
        model.addAttribute("currentPriority", priority);
        model.addAttribute("currentCategory", category);
        model.addAttribute("source", source);
        model.addAttribute("startDate", startDate);
        model.addAttribute("targetDate", targetDate);

        return "add-task";
    }

    @PostMapping("/tasks/update/{id}")
    public String updateTask(@PathVariable Long id,
                             @RequestParam String title,
                             @RequestParam(required = false) String description,
                             @RequestParam(required = false) String priority,
                             @RequestParam(required = false) String category,
                             @RequestParam(required = false)
                             @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDate,
                             @RequestParam(required = false) String priorityFilter,
                             @RequestParam(required = false) String currentCategory,
                             @RequestParam(required = false) String source,
                             @RequestParam(required = false) String startDate,
                             @RequestParam(required = false) String targetDate,
                             @RequestParam(required = false) String sort) {

        Task existingTask = taskService.getTaskById(id);

        existingTask.setTitle(title);
        existingTask.setDescription(description);
        existingTask.setDueDate(dueDate);
        existingTask.setCategory(category);

        if (priority == null || priority.isEmpty()) {
            existingTask.setPriority("P4");
        } else {
            existingTask.setPriority(priority);
        }

        taskService.saveTask(existingTask);

        if ("today".equals(source)) {
            return "redirect:/today";
        }

        if ("upcoming".equals(source)) {
            String redirectUrl = "redirect:/upcoming";

            if (startDate != null && !startDate.isEmpty()) {
                redirectUrl += "?startDate=" + startDate;
            }

            if (existingTask.getDueDate() != null) {
                redirectUrl += "#day-" + existingTask.getDueDate();
            } else if (targetDate != null && !targetDate.isEmpty()) {
                redirectUrl += "#day-" + targetDate;
            }

            return redirectUrl;
        }

        if (currentCategory != null && !currentCategory.isEmpty()) {
            return "redirect:/tasks/category/" + currentCategory;
        }

        if (priorityFilter != null && !priorityFilter.isEmpty()) {
            return "redirect:/tasks/priority/" + priorityFilter;
        }

        if (sort != null && !sort.isEmpty()) {
            return "redirect:/tasks?sort=" + sort;
        }

        return "redirect:/tasks";    }

    @PostMapping("/tasks/toggle/{id}")
    public String toggleTaskDone(@PathVariable Long id,
                                 @RequestParam(required = false) String priority,
                                 @RequestParam(required = false) String category,
                                 @RequestParam(required = false) String source,
                                 @RequestParam(required = false) String startDate,
                                 @RequestParam(required = false) String targetDate,
                                 @RequestParam(required = false) String sort) {

        taskService.toggleDone(id);

        if ("today".equals(source)) {
            return "redirect:/today";
        }

        if (category != null && !category.isEmpty()) {
            return "redirect:/tasks/category/" + category;
        }

        if (priority != null && !priority.isEmpty() && !priority.equals("Upcoming")) {
            return "redirect:/tasks/priority/" + priority;
        }

        if ("upcoming".equals(source)) {
            String redirectUrl = "redirect:/upcoming";

            if (startDate != null && !startDate.isEmpty()) {
                redirectUrl += "?startDate=" + startDate;
            }

            if (targetDate != null && !targetDate.isEmpty()) {
                redirectUrl += "#day-" + targetDate;
            }

            return redirectUrl;
        }

        if (sort != null && !sort.isEmpty()) {
            return "redirect:/tasks?sort=" + sort;
        }

        return "redirect:/tasks";
    }
    @GetMapping("/upcoming")
    public String showUpcoming(
            @RequestParam(value = "startDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            Model model) {

        LocalDate today = LocalDate.now();

        if (startDate == null || startDate.isBefore(today)) {
            startDate = today;
        }

        List<LocalDate> weekDates = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            weekDates.add(startDate.plusDays(i));
        }

        DateTimeFormatter monthFormatter =
                DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);

        LocalDate prevWeekStart = startDate.minusDays(7);
        if (prevWeekStart.isBefore(today)) {
            prevWeekStart = today;
        }

        LocalDate nextWeekStart = startDate.plusDays(7);

        model.addAttribute("groupedTasks", taskService.getUpcomingTasksGroupedByDay(startDate));
        model.addAttribute("weekDates", weekDates);
        model.addAttribute("monthLabel", startDate.format(monthFormatter));
        model.addAttribute("prevWeekStart", prevWeekStart);
        model.addAttribute("nextWeekStart", nextWeekStart);
        model.addAttribute("currentStartDate", startDate);
        model.addAttribute("today", today);
        model.addAttribute("task", new Task());
        return "upcoming";
    }

    @GetMapping("/categories")
    public String showCategoriesPage() {
        return "categories";
    }

    @GetMapping("/tasks/category/{category}")
    public String getTasksByCategory(@PathVariable String category,
                                     @RequestParam(required = false) String scrollPos,
                                     Model model) {
        model.addAttribute("tasks", taskService.getTasksByCategory(category));
        model.addAttribute("task", new Task());
        model.addAttribute("showAddButton", false);
        model.addAttribute("showBackToTasks", true);
        model.addAttribute("backUrl", "/categories");
        model.addAttribute("currentPriority", null);
        model.addAttribute("currentCategory", category);
        model.addAttribute("pageTitle", "Category - " + category);
        model.addAttribute("scrollPos", scrollPos);
        model.addAttribute("currentSource", "category");
        return "tasks";
    }

    @PostMapping("/tasks/reorder")
    @ResponseBody
    public void reorderTasks(@RequestBody List<Long> orderedIds) {
        taskService.reorderTasks(orderedIds);
    }

}