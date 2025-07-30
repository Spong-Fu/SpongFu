package com.spongout.spongout.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageRoutingController {

    @GetMapping("/")
    public String routeUser(HttpServletRequest request) {
        String query = request.getQueryString();
        return (query == null || query.isBlank())
            ? "forward:/nameForm.html"
            : "forward:/index.html?" + query;
    }
}
