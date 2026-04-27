package com.taskmanager.controller;

import com.taskmanager.entity.Task;
import com.taskmanager.service.TaskService;
import jakarta.validation.Valid;
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

@Controller
public class TaskController {
    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    private void prepareTasksPage(Model model, List<Task> sourceTasks, String sort) {
        List<Task> overdueTasks = sourceTasks.stream()
                .filter(task -> task.getDueDate() != null)
                .filter(task -> task.getDueDate().isBefore(LocalDate.now()))
                .filter(task -> !task.isDone())
                .toList();

        List<Task> completedTasks = sourceTasks.stream()
                .filter(Task::isDone)
                .toList();

        List<Task> activeTasks = sourceTasks.stream()
                .filter(task -> !task.isDone())
                .filter(task -> !(task.getDueDate() != null
                        && task.getDueDate().isBefore(LocalDate.now())))
                .toList();

        boolean overdueOpen = "overdue".equals(sort);
        boolean completedOpen = false;
        boolean showOverdueFirst = "overdue".equals(sort);

        model.addAttribute("tasks", activeTasks);
        model.addAttribute("overdueTasks", overdueTasks);
        model.addAttribute("overdueCount", overdueTasks.size());
        model.addAttribute("overdueOpen", overdueOpen);
        model.addAttribute("showOverdueFirst", showOverdueFirst);

        model.addAttribute("completedTasks", completedTasks);
        model.addAttribute("completedCount", completedTasks.size());
        model.addAttribute("completedOpen", completedOpen);
        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.minusDays(today.getDayOfWeek().getValue() - 1);

        List<Integer> weeklyStats = new ArrayList<>();

        for (int i = 0; i < 7; i++) {
            LocalDate day = startOfWeek.plusDays(i);

            int count = (int) sourceTasks.stream()
                    .filter(Task::isDone)
                    .filter(task -> task.getDueDate() != null)
                    .filter(task -> task.getDueDate().isEqual(day))
                    .count();

            weeklyStats.add(count);
        }

        model.addAttribute("weeklyStats", weeklyStats);
        model.addAttribute("todayIndex", today.getDayOfWeek().getValue() - 1);

        model.addAttribute("currentSort", sort);
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

        prepareTasksPage(model, tasks, sort);

        model.addAttribute("task", new Task());
        model.addAttribute("showAddButton", true);
        model.addAttribute("showBackToTasks", false);
        model.addAttribute("backUrl", "/tasks");
        model.addAttribute("currentPriority", null);
        model.addAttribute("currentCategory", null);
        model.addAttribute("pageTitle", "All Tasks");
        model.addAttribute("scrollPos", scrollPos);
        model.addAttribute("currentSource", "all");

        return "tasks";
    }

    @GetMapping("/today")
    public String showTodayTasks(Model model,
                                 @RequestParam(required = false) String scrollPos,
                                 @RequestParam(required = false) String sort) {

        Task task = new Task();
        task.setDueDate(LocalDate.now());

        List<Task> todayTasks;

        if ("oldest".equals(sort)) {
            todayTasks = taskService.getTodayTasks().stream()
                    .sorted((t1, t2) -> t1.getId().compareTo(t2.getId()))
                    .toList();
        } else if ("priority".equals(sort)) {
            todayTasks = taskService.getTodayTasks().stream()
                    .sorted((t1, t2) -> {
                        String p1 = t1.getPriority() != null ? t1.getPriority() : "P4";
                        String p2 = t2.getPriority() != null ? t2.getPriority() : "P4";
                        return p1.compareTo(p2);
                    })
                    .toList();
        } else if ("az".equals(sort)) {
            todayTasks = taskService.getTodayTasks().stream()
                    .sorted((t1, t2) -> t1.getTitle().compareToIgnoreCase(t2.getTitle()))
                    .toList();
        } else if ("dueDate".equals(sort)) {
            todayTasks = taskService.getTodayTasks().stream()
                    .sorted((t1, t2) -> {
                        if (t1.getDueDate() == null && t2.getDueDate() == null) return 0;
                        if (t1.getDueDate() == null) return 1;
                        if (t2.getDueDate() == null) return -1;
                        return t1.getDueDate().compareTo(t2.getDueDate());
                    })
                    .toList();
        } else {
            todayTasks = taskService.getTodayTasks().stream()
                    .sorted((t1, t2) -> t2.getId().compareTo(t1.getId()))
                    .toList();
        }

        prepareTasksPage(model, todayTasks, sort);

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
                                     @RequestParam(required = false) String sort,
                                     Model model) {

        List<Task> priorityTasks = taskService.getTasksByPriority(priority);

        if ("oldest".equals(sort)) {
            priorityTasks = priorityTasks.stream()
                    .sorted((t1, t2) -> t1.getId().compareTo(t2.getId()))
                    .toList();
        } else if ("priority".equals(sort)) {
            priorityTasks = priorityTasks.stream()
                    .sorted((t1, t2) -> {
                        String p1 = t1.getPriority() != null ? t1.getPriority() : "P4";
                        String p2 = t2.getPriority() != null ? t2.getPriority() : "P4";
                        return p1.compareTo(p2);
                    })
                    .toList();
        } else if ("az".equals(sort)) {
            priorityTasks = priorityTasks.stream()
                    .sorted((t1, t2) -> t1.getTitle().compareToIgnoreCase(t2.getTitle()))
                    .toList();
        } else if ("dueDate".equals(sort)) {
            priorityTasks = priorityTasks.stream()
                    .sorted((t1, t2) -> {
                        if (t1.getDueDate() == null && t2.getDueDate() == null) return 0;
                        if (t1.getDueDate() == null) return 1;
                        if (t2.getDueDate() == null) return -1;
                        return t1.getDueDate().compareTo(t2.getDueDate());
                    })
                    .toList();
        } else {
            priorityTasks = priorityTasks.stream()
                    .sorted((t1, t2) -> t2.getId().compareTo(t1.getId()))
                    .toList();
        }

        prepareTasksPage(model, priorityTasks, sort);
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
        model.addAttribute("sort", sort);

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
                           @RequestParam(required = false) String scrollPos,
                           @RequestParam(required = false) String keyword,
                           Model model) {

        if (result.hasErrors()) {
            prepareTasksPage(model, taskService.getAllTasks(), null);
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
            String redirect = "redirect:/today";
            boolean hasParam = false;

            if (sort != null && !sort.isEmpty()) {
                redirect += "?sort=" + sort;
                hasParam = true;
            }

            if (keyword != null && !keyword.isEmpty()) {
                redirect += (hasParam ? "&" : "?") + "keyword=" + keyword;
                hasParam = true;
            }

            if (scrollPos != null && !scrollPos.isEmpty()) {
                redirect += (hasParam ? "&" : "?") + "scrollPos=" + scrollPos;
            }

            return redirect;
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

        if ("search".equals(source)) {
            if (sort != null && !sort.isEmpty()) {
                return "redirect:/tasks/search?keyword=" + keyword + "&sort=" + sort;
            }
            return "redirect:/tasks/search?keyword=" + keyword;
        }
        if (priorityFilter != null && !priorityFilter.isEmpty()) {
            return "redirect:/tasks/priority/" + priorityFilter;
        }

        if (currentCategory != null && !currentCategory.isEmpty()) {
            return "redirect:/tasks/category/" + currentCategory;
        }

        String redirect = "redirect:/tasks";
        boolean hasParam = false;

        if (sort != null && !sort.isEmpty()) {
            redirect += "?sort=" + sort;
            hasParam = true;
        }

        if (keyword != null && !keyword.isEmpty()) {
            redirect += (hasParam ? "&" : "?") + "keyword=" + keyword;
            hasParam = true;
        }

        if (scrollPos != null && !scrollPos.isEmpty()) {
            redirect += (hasParam ? "&" : "?") + "scrollPos=" + scrollPos;
        }

        return redirect;

    }

    @GetMapping("/tasks/delete/{id}")
    public String deleteTask(@PathVariable Long id,
                             @RequestParam(required = false) String priority,
                             @RequestParam(required = false) String category,
                             @RequestParam(required = false) String source,
                             @RequestParam(required = false) String startDate,
                             @RequestParam(required = false) String targetDate,
                             @RequestParam(required = false) String sort,
                             @RequestParam(required = false) String scrollPos,
                             @RequestParam(required = false) String keyword)
                            {

        taskService.deleteTask(id);

                                if ("today".equals(source)) {
                                    String redirect = "redirect:/today";
                                    boolean hasParam = false;

                                    if (sort != null && !sort.isEmpty()) {
                                        redirect += "?sort=" + sort;
                                        hasParam = true;
                                    }

                                    if (keyword != null && !keyword.isEmpty()) {
                                        redirect += (hasParam ? "&" : "?") + "keyword=" + keyword;
                                        hasParam = true;
                                    }

                                    if (scrollPos != null && !scrollPos.isEmpty()) {
                                        redirect += (hasParam ? "&" : "?") + "scrollPos=" + scrollPos;
                                    }

                                    return redirect;
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
        if ("search".equals(source)) {
            if (sort != null && !sort.isEmpty()) {
                return "redirect:/tasks/search?keyword=" + keyword + "&sort=" + sort;
            }
            return "redirect:/tasks/search?keyword=" + keyword;
        }
        if (category != null && !category.isEmpty()) {
            return "redirect:/tasks/category/" + category;
        }

        if (priority != null && !priority.isEmpty()) {
            return "redirect:/tasks/priority/" + priority;
        }

                                String redirect = "redirect:/tasks";
                                boolean hasParam = false;

                                if (sort != null && !sort.isEmpty()) {
                                    redirect += "?sort=" + sort;
                                    hasParam = true;
                                }

                                if (keyword != null && !keyword.isEmpty()) {
                                    redirect += (hasParam ? "&" : "?") + "keyword=" + keyword;
                                    hasParam = true;
                                }

                                if (scrollPos != null && !scrollPos.isEmpty()) {
                                    redirect += (hasParam ? "&" : "?") + "scrollPos=" + scrollPos;
                                }

                                return redirect;


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
                             @RequestParam(required = false) String sort,
                             @RequestParam(required = false) String scrollPos,
                             @RequestParam(required = false) String keyword){

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
            String redirect = "redirect:/today";
            boolean hasParam = false;

            if (sort != null && !sort.isEmpty()) {
                redirect += "?sort=" + sort;
                hasParam = true;
            }

            if (keyword != null && !keyword.isEmpty()) {
                redirect += (hasParam ? "&" : "?") + "keyword=" + keyword;
                hasParam = true;
            }

            if (scrollPos != null && !scrollPos.isEmpty()) {
                redirect += (hasParam ? "&" : "?") + "scrollPos=" + scrollPos;
            }

            return redirect;
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

        if ("search".equals(source)) {
            if (sort != null && !sort.isEmpty()) {
                return "redirect:/tasks/search?keyword=" + keyword + "&sort=" + sort;
            }
            return "redirect:/tasks/search?keyword=" + keyword;
        }
        if (currentCategory != null && !currentCategory.isEmpty()) {
            return "redirect:/tasks/category/" + currentCategory;
        }


        if (priorityFilter != null && !priorityFilter.isEmpty()) {
            return "redirect:/tasks/priority/" + priorityFilter;
        }
        String redirect = "redirect:/tasks";
        boolean hasParam = false;

        if (sort != null && !sort.isEmpty()) {
            redirect += "?sort=" + sort;
            hasParam = true;
        }

        if (keyword != null && !keyword.isEmpty()) {
            redirect += (hasParam ? "&" : "?") + "keyword=" + keyword;
            hasParam = true;
        }

        if (scrollPos != null && !scrollPos.isEmpty()) {
            redirect += (hasParam ? "&" : "?") + "scrollPos=" + scrollPos;
        }

        return redirect;

    }

    @PostMapping("/tasks/toggle/{id}")
    public String toggleTaskDone(@PathVariable Long id,
                                 @RequestParam(required = false) String priority,
                                 @RequestParam(required = false) String category,
                                 @RequestParam(required = false) String source,
                                 @RequestParam(required = false) String startDate,
                                 @RequestParam(required = false) String targetDate,
                                 @RequestParam(required = false) String sort,
                                 @RequestParam(required = false) String scrollPos,

                                 @RequestParam(required = false) String keyword){

        taskService.toggleDone(id);

        if ("today".equals(source)) {
            String redirect = "redirect:/today";
            boolean hasParam = false;

            if (sort != null && !sort.isEmpty()) {
                redirect += "?sort=" + sort;
                hasParam = true;
            }

            if (keyword != null && !keyword.isEmpty()) {
                redirect += (hasParam ? "&" : "?") + "keyword=" + keyword;
                hasParam = true;
            }

            if (scrollPos != null && !scrollPos.isEmpty()) {
                redirect += (hasParam ? "&" : "?") + "scrollPos=" + scrollPos;
            }

            return redirect;
        }
        if ("search".equals(source)) {
            if (sort != null && !sort.isEmpty()) {
                return "redirect:/tasks/search?keyword=" + keyword + "&sort=" + sort;
            }
            return "redirect:/tasks/search?keyword=" + keyword;
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

        String redirect = "redirect:/tasks";
        boolean hasParam = false;

        if (sort != null && !sort.isEmpty()) {
            redirect += "?sort=" + sort;
            hasParam = true;
        }

        if (keyword != null && !keyword.isEmpty()) {
            redirect += (hasParam ? "&" : "?") + "keyword=" + keyword;
            hasParam = true;
        }

        if (scrollPos != null && !scrollPos.isEmpty()) {
            redirect += (hasParam ? "&" : "?") + "scrollPos=" + scrollPos;
        }

        return redirect;
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
                                     @RequestParam(required = false) String sort,
                                     Model model) {

        List<Task> categoryTasks = taskService.getTasksByCategory(category);

        if ("oldest".equals(sort)) {
            categoryTasks = categoryTasks.stream()
                    .sorted((t1, t2) -> t1.getId().compareTo(t2.getId()))
                    .toList();
        } else if ("priority".equals(sort)) {
            categoryTasks = categoryTasks.stream()
                    .sorted((t1, t2) -> {
                        String p1 = t1.getPriority() != null ? t1.getPriority() : "P4";
                        String p2 = t2.getPriority() != null ? t2.getPriority() : "P4";
                        return p1.compareTo(p2);
                    })
                    .toList();
        } else if ("az".equals(sort)) {
            categoryTasks = categoryTasks.stream()
                    .sorted((t1, t2) -> t1.getTitle().compareToIgnoreCase(t2.getTitle()))
                    .toList();
        } else if ("dueDate".equals(sort)) {
            categoryTasks = categoryTasks.stream()
                    .sorted((t1, t2) -> {
                        if (t1.getDueDate() == null && t2.getDueDate() == null) return 0;
                        if (t1.getDueDate() == null) return 1;
                        if (t2.getDueDate() == null) return -1;
                        return t1.getDueDate().compareTo(t2.getDueDate());
                    })
                    .toList();
        } else {
            categoryTasks = categoryTasks.stream()
                    .sorted((t1, t2) -> t2.getId().compareTo(t1.getId()))
                    .toList();
        }

        prepareTasksPage(model, categoryTasks, sort);

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

    @GetMapping("/search")
    @ResponseBody
    public List<Task> search(@RequestParam String keyword) {
        return taskService.searchTasks(keyword);
    }

    @GetMapping("/tasks/search")
    public String searchTasks(@RequestParam String keyword,
                              @RequestParam(required = false) String sort,
                              Model model) {

        List<Task> tasks = taskService.searchTasks(keyword);

        if ("oldest".equals(sort)) {
            tasks = tasks.stream()
                    .sorted((t1, t2) -> t1.getId().compareTo(t2.getId()))
                    .toList();
        } else if ("priority".equals(sort)) {
            tasks = tasks.stream()
                    .sorted((t1, t2) -> {
                        String p1 = t1.getPriority() != null ? t1.getPriority() : "P4";
                        String p2 = t2.getPriority() != null ? t2.getPriority() : "P4";
                        return p1.compareTo(p2);
                    })
                    .toList();
        } else if ("az".equals(sort)) {
            tasks = tasks.stream()
                    .sorted((t1, t2) -> t1.getTitle().compareToIgnoreCase(t2.getTitle()))
                    .toList();
        } else if ("dueDate".equals(sort)) {
            tasks = tasks.stream()
                    .sorted((t1, t2) -> {
                        if (t1.getDueDate() == null && t2.getDueDate() == null) return 0;
                        if (t1.getDueDate() == null) return 1;
                        if (t2.getDueDate() == null) return -1;
                        return t1.getDueDate().compareTo(t2.getDueDate());
                    })
                    .toList();
        } else {
            tasks = tasks.stream()
                    .sorted((t1, t2) -> t2.getId().compareTo(t1.getId()))
                    .toList();
        }

        prepareTasksPage(model, tasks, sort);

        model.addAttribute("task", new Task());
        model.addAttribute("showAddButton", false);
        model.addAttribute("showBackToTasks", true);
        model.addAttribute("backUrl", "/tasks");
        model.addAttribute("pageTitle", "Search results for: " + keyword);
        model.addAttribute("currentSource", "search");
        model.addAttribute("searchKeyword", keyword);

        return "tasks";
    }
}
