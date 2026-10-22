package com.mss301.order.saga;

import com.mss301.order.client.*;
import com.mss301.order.dto.CreateOrderRequest;
import com.mss301.order.entity.*;
import com.mss301.order.service.OrderService;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientException;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class OrderSagaOrchestratorTest {
    OrderService orders = mock(OrderService.class);
    InventoryClient inventory = mock(InventoryClient.class);
    PaymentClient payment = mock(PaymentClient.class);
    OrderSagaOrchestrator saga = new OrderSagaOrchestrator(orders, inventory, payment);
    CreateOrderRequest request = new CreateOrderRequest("C1", "P1", 1, new BigDecimal("1000"));
    @BeforeEach void setup() {
        Order o = new Order(); o.setId(1L);
        when(orders.createPendingOrder(request)).thenReturn(o);
        when(orders.findById(1L)).thenReturn(o);
    }
    @Test void successConfirmsWithoutCompensation() {
        saga.placeOrder(request);
        var sequence = inOrder(inventory, payment, orders);
        sequence.verify(inventory).reserve(1L, "P1", 1);
        sequence.verify(payment).pay(1L, "C1", request.amount());
        sequence.verify(orders).confirmOrder(1L);
        verify(inventory, never()).release(anyLong());
        verify(payment, never()).refund(anyLong());
    }
    @Test void inventoryFailureCancelsWithoutCallingPayment() {
        doThrow(new RestClientException("409 stock")).when(inventory).reserve(1L, "P1", 1);
        saga.placeOrder(request);
        verifyNoInteractions(payment);
        verify(inventory, never()).release(anyLong());
        verify(orders).cancelOrder(1L);
    }
    @Test void paymentFailureReleasesBeforeCancelling() {
        doThrow(new RestClientException("402 balance")).when(payment).pay(1L, "C1", request.amount());
        saga.placeOrder(request);
        var sequence = inOrder(inventory, orders);
        sequence.verify(inventory).release(1L);
        sequence.verify(orders).cancelOrder(1L);
        verify(payment, never()).refund(anyLong());
    }
    @Test void localConfirmationFailureRefundsThenReleasesThenCancels() {
        doThrow(new IllegalStateException("MySQL commit failed")).when(orders).confirmOrder(1L);
        saga.placeOrder(request);
        var sequence = inOrder(payment, inventory, orders);
        sequence.verify(payment).refund(1L);
        sequence.verify(inventory).release(1L);
        sequence.verify(orders).cancelOrder(1L);
    }
    @Test void compensationRetriesThenSucceeds() {
        doThrow(new RestClientException("offline")).when(payment).pay(1L, "C1", request.amount());
        doThrow(new RestClientException("temporary")).doNothing().when(inventory).release(1L);
        saga.placeOrder(request);
        verify(inventory, times(2)).release(1L);
        verify(orders).cancelOrder(1L);
    }
    @Test void exhaustedCompensationStaysCompensatingAfterThreeAttempts() {
        doThrow(new RestClientException("offline")).when(payment).pay(1L, "C1", request.amount());
        doThrow(new RestClientException("offline")).when(inventory).release(1L);
        saga.placeOrder(request);
        verify(inventory, times(3)).release(1L);
        verify(orders).updateSagaStatus(1L, SagaStatus.COMPENSATING);
        verify(orders, never()).cancelOrder(anyLong());
        verify(orders, times(3)).log(eq(1L), eq("COMPENSATION"), eq("FAILED"), anyString());
    }
    @Test void failureLoggingReservationStillReleasesCommittedStock() {
        doThrow(new IllegalStateException("log unavailable"))
            .when(orders).log(eq(1L), eq("RESERVE_INVENTORY"), eq("SUCCESS"), anyString());
        saga.placeOrder(request);
        verify(inventory).release(1L);
        verify(orders).cancelOrder(1L);
        verifyNoInteractions(payment);
    }
}
