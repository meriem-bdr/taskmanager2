package com.taskmanager.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ShoppingPageController {

    @GetMapping("/shopping")
    public String shoppingPage() {
        return "shopping";
    }
}
