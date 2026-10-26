package com.lizhenqi.controller;

import com.lizhenqi.entity.LizhenqiShangpin;
import com.lizhenqi.service.LizhenqiShangpinService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/lizhenqi/shangpin")
public class LizhenqiShangpinController {

    private final LizhenqiShangpinService lizhenqiShangpinService;

    @Autowired
    public LizhenqiShangpinController(LizhenqiShangpinService lizhenqiShangpinService) {
        this.lizhenqiShangpinService = lizhenqiShangpinService;
    }

    @GetMapping
    public String list(Model model, @RequestParam(value = "keyword", required = false) String keyword) {
        List<LizhenqiShangpin> lizhenqiShangpinList;
        if (keyword != null && !keyword.isEmpty()) {
            lizhenqiShangpinList = lizhenqiShangpinService.searchByMingcheng(keyword);
        } else {
            lizhenqiShangpinList = lizhenqiShangpinService.findAll();
        }
        model.addAttribute("lizhenqiShangpinList", lizhenqiShangpinList);
        model.addAttribute("keyword", keyword);
        return "lizhenqi/shangpin/list";
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("lizhenqiShangpin", new LizhenqiShangpin());
        return "lizhenqi/shangpin/add";
    }

    @PostMapping("/add")
    public String add(@ModelAttribute LizhenqiShangpin lizhenqiShangpin) {
        lizhenqiShangpinService.save(lizhenqiShangpin);
        return "redirect:/lizhenqi/shangpin";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        LizhenqiShangpin lizhenqiShangpin = lizhenqiShangpinService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid product id: " + id));
        model.addAttribute("lizhenqiShangpin", lizhenqiShangpin);
        return "lizhenqi/shangpin/edit";
    }

    @PostMapping("/edit/{id}")
    public String edit(@PathVariable Long id, @ModelAttribute LizhenqiShangpin lizhenqiShangpin) {
        lizhenqiShangpin.setId(id);
        lizhenqiShangpinService.save(lizhenqiShangpin);
        return "redirect:/lizhenqi/shangpin";
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Long id) {
        lizhenqiShangpinService.deleteById(id);
        return "redirect:/lizhenqi/shangpin";
    }

    @GetMapping("/view/{id}")
    public String view(@PathVariable Long id, Model model) {
        LizhenqiShangpin lizhenqiShangpin = lizhenqiShangpinService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid product id: " + id));
        model.addAttribute("lizhenqiShangpin", lizhenqiShangpin);
        return "lizhenqi/shangpin/view";
    }
}