package com.lizhenqi.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LizhenqiHomeController {

    @GetMapping("/")
    public String home() {
        return "lizhenqi/index";
    }
}