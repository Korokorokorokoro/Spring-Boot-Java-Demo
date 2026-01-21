package com.springbootjavatest.demo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.springbootjavatest.demo.controller.OrderController;
import com.springbootjavatest.demo.model.Order;
import com.springbootjavatest.demo.model.OrderState;
import com.springbootjavatest.demo.repository.OrderRepository;
import com.springbootjavatest.demo.service.EmailService;
import com.springbootjavatest.demo.service.SmsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class OrderControllerMockMvcTests {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();
    private OrderRepository repo;
    private EmailService emailService;
    private SmsService smsService;

    @BeforeEach
    public void setup() {
        repo = mock(OrderRepository.class);
        emailService = mock(EmailService.class);
        smsService = mock(SmsService.class);

        OrderController controller = new OrderController(repo, emailService, smsService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    public void createOrder_returnsCreated() throws Exception {
        Order order = new Order("MockMvc order");
        when(repo.save(any(Order.class))).thenAnswer(inv -> {
            Order e = inv.getArgument(0);
            e.setId(1L);
            if (e.getState() == null) e.setState(OrderState.CREATED);
            return e;
        });

        mockMvc.perform(post("/order")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(order)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.details").value("MockMvc order"))
                .andExpect(jsonPath("$.state").value("CREATED"));
    }

    @Test
    public void getOrderById_returnsOrder() throws Exception {
        Order saved = new Order("Fetch me MockMvc"); 
        saved.setId(1L); 
        saved.setState(OrderState.CREATED);
        when(repo.findById(1L)).thenReturn(java.util.Optional.of(saved));

        mockMvc.perform(get("/order/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.details").value("Fetch me MockMvc"));
    }

    @Test
    public void searchByDetails_returnsMatches() throws Exception {
        Order a = new Order("apple item"); 
        a.setId(1L);
        when(repo.findByDetailsContainingIgnoreCase("apple")).thenReturn(List.of(a));

        mockMvc.perform(get("/order/search").param("details", "apple"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].details").value(containsString("apple")));
    }

    @Test
    public void updateOrder_changesState_and_sendsNotifications() throws Exception {
        Order saved = new Order("to update MockMvc"); 
        saved.setId(1L); 
        saved.setState(OrderState.CREATED);
        when(repo.findById(1L)).thenReturn(java.util.Optional.of(saved));
        when(repo.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order updated = new Order();
        updated.setId(1L);
        updated.setDetails("updated details");
        updated.setState(OrderState.COMPLETED);

        mockMvc.perform(put("/order/1")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("COMPLETED"));

        verify(emailService, times(1)).sendNotification(anyString(), anyString());
        verify(smsService, times(1)).sendNotification(anyString(), anyString());
    }
}
