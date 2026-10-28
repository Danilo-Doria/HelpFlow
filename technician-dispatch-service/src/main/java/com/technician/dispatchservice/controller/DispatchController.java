package com.technician.dispatchservice.controller;

import com.technician.dispatchservice.dto.DispatchRequest;
import com.technician.dispatchservice.dto.DispatchResponse;
import com.technician.dispatchservice.service.DispatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/technician-dispatch")
@RequiredArgsConstructor
public class DispatchController {

    private final DispatchService service;

    @PostMapping
    public DispatchResponse despachar(@Valid @RequestBody DispatchRequest request) {
        return service.calcular(request);
    }
}
