package com.taskmanager.entity;

import jakarta.persistence.Column;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import jakarta.validation.constraints.NotBlank;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.format.annotation.DateTimeFormat;



@Entity
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Title is required")
    private String title;
    @Size(max = 300)
    @Column(columnDefinition = "TEXT")

    private String description;

    private boolean done;

    private String priority;
    private String category;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate dueDate;


    public Task() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public boolean isDone() { return done; }
    public void setDone(boolean done) { this.done = done; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public String getFormattedDueDate() {
        if (dueDate == null) {
            return "";
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH);
        return dueDate.format(formatter);
    }
}