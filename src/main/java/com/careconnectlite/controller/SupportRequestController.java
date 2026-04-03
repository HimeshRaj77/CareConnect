package com.careconnectlite.controller;

import com.careconnectlite.model.SupportRequest;
import com.careconnectlite.service.SupportRequestService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/requests")
@CrossOrigin(origins = "*")
public class SupportRequestController {

    private final SupportRequestService supportRequestService;

    public SupportRequestController(SupportRequestService supportRequestService) {
        this.supportRequestService = supportRequestService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SupportRequest createRequest(@RequestBody SupportRequest supportRequest) {
        supportRequest.setId(null);
        return supportRequestService.processAndSaveRequest(supportRequest);
    }
}
