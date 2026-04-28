package com.taskmanager.controller;

import com.taskmanager.entity.Category;
import com.taskmanager.service.ShoppingService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/shopping")
public class ShoppingController {

    private final ShoppingService service;

    public ShoppingController(ShoppingService service) {
        this.service = service;
    }

    @GetMapping("/categories")
    public List<Category> getCategories() {
        return service.getAllCategories();
    }

    @GetMapping("/category")
    public Category addCategory(@RequestParam String name) {
        return service.addCategory(name);
    }

    @GetMapping("/item")
    public void addItem(@RequestParam Long categoryId,
                        @RequestParam String name) {
        service.addItem(categoryId, name);
    }
    @GetMapping("/buy")
    public void markAsBought(@RequestParam Long itemId) {
        service.markAsBought(itemId);
    }
    @GetMapping("/deleteItem")
    public void deleteItem(@RequestParam Long id) {
        service.deleteItem(id);
    }
    @GetMapping("/deleteCategory")
    public void deleteCategory(@RequestParam Long id) {
        service.deleteCategory(id);
    }
}
