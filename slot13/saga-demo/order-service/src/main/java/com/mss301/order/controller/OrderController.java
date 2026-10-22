package com.mss301.order.controller;

import com.mss301.order.dto.CreateOrderRequest;
import com.mss301.order.entity.*;
import com.mss301.order.repository.*;
import com.mss301.order.saga.OrderSagaOrchestrator;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderSagaOrchestrator orchestrator;
    private final OrderRepository orderRepository;
    private final SagaLogRepository sagaLogRepository;

    @PostMapping
    public Order placeOrder(@Valid @RequestBody CreateOrderRequest req) {
        return orchestrator.placeOrder(req);
    }

    @GetMapping
    public List<Order> findAll() {
        return orderRepository.findAll();
    }

    @GetMapping("/{id}/saga-logs")
    public List<SagaLog> sagaLogs(@PathVariable Long id) {
        if (!orderRepository.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy order " + id);
        return sagaLogRepository.findByOrderIdOrderByIdAsc(id);
    }
}
