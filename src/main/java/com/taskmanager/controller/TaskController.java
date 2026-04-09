package com.taskmanager.controller;

import com.taskmanager.entity.Task;
import com.taskmanager.service.TaskService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
                              @RequestParam(required = false) String scrollPos) {
        model.addAttribute("tasks", taskService.getAllTasks());
        model.addAttribute("task", new Task());
        model.addAttribute("showAddButton", true);
        model.addAttribute("showBackToTasks", false);
        model.addAttribute("backUrl", "/tasks");
        model.addAttribute("currentPriority", null);
        model.addAttribute("currentCategory", null);
        model.addAttribute("pageTitle", "All Tasks");
        model.addAttribute("scrollPos", scrollPos);
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
        model.addAttribute("pageTitle", "Priority- " + priority);
        model.addAttribute("scrollPos", scrollPos);
        return "tasks";
    }

    @GetMapping("/tasks/new")
    public String showAddForm(@RequestParam(required = false) String dueDate,
                              @RequestParam(required = false) String source,
                              @RequestParam(required = false) String startDate,
                              @RequestParam(required = false) String targetDate,
                              Model model) {
        Task task = new Task();

        if (dueDate != null && !dueDate.isEmpty()) {
            task.setDueDate(java.time.LocalDate.parse(dueDate));
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
                           Model model) {

        if (result.hasErrors()) {
            model.addAttribute("currentPriority", priorityFilter);
            model.addAttribute("currentCategory", currentCategory);
            model.addAttribute("source", source);
            model.addAttribute("startDate", startDate);
            model.addAttribute("targetDate", targetDate);
            return "add-task";
        }

        if (task.getPriority() == null || task.getPriority().isEmpty()) {
            task.setPriority("P4");
        }

        taskService.saveTask(task);

        // مهم: upcoming قبل priority
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

        return "redirect:/tasks";
    }

    @GetMapping("/tasks/delete/{id}")
    public String deleteTask(@PathVariable Long id,
                             @RequestParam(required = false) String priority) {

        taskService.deleteTask(id);

        if (priority != null && !priority.isEmpty()) {
            return "redirect:/tasks/priority/" + priority;
        }

        return "redirect:/tasks";
    }

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

    @PostMapping("/tasks/toggle/{id}")
    public String toggleTaskDone(@PathVariable Long id,
                                 @RequestParam(required = false) String priority,
                                 @RequestParam(required = false) String category,
                                 @RequestParam(required = false) String source,
                                 @RequestParam(required = false) String startDate,
                                 @RequestParam(required = false) String targetDate) {

        taskService.toggleDone(id);

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
        model.addAttribute("pageTitle", "Category- " + category);
        model.addAttribute("scrollPos", scrollPos);
        return "tasks";
    }

}