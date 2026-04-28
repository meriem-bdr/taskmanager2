package com.taskmanager.service;

import com.taskmanager.entity.Category;
import com.taskmanager.entity.Item;
import com.taskmanager.repository.CategoryRepository;
import com.taskmanager.repository.ItemRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ShoppingService {

    private final CategoryRepository categoryRepo;
    private final ItemRepository itemRepo;

    public ShoppingService(CategoryRepository categoryRepo, ItemRepository itemRepo) {
        this.categoryRepo = categoryRepo;
        this.itemRepo = itemRepo;
    }

    public List<Category> getAllCategories() {
        return categoryRepo.findAll();
    }

    public Category addCategory(String name) {
        Category c = new Category();
        c.setName(name);
        return categoryRepo.save(c);
    }

    public Item addItem(Long categoryId, String name) {
        Category c = categoryRepo.findById(categoryId).orElseThrow();

        Item item = new Item();
        item.setName(name);
        item.setCategory(c);

        return itemRepo.save(item);
    }
    public void markAsBought(Long itemId) {
        Item item = itemRepo.findById(itemId).orElseThrow();
        item.setBought(true);
        itemRepo.save(item);
    }
    public void deleteItem(Long id) {
        itemRepo.deleteById(id);
    }
    public void deleteCategory(Long id) {
        categoryRepo.deleteById(id);
    }
}
