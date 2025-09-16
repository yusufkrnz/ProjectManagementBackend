package com.yusufkurnaz.ProjectManagementBackend.Integration.GitHub.Controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/github/webhook")
public class WebhookController {

    @PostMapping
    public void receiveWebhook() {
        // stub
    }
}


