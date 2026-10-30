package example.com.ltw.staff;

import example.com.ltw.auth.dto.LoginRequest;
import example.com.ltw.auth.service.AuthService;
import example.com.ltw.staff.controller.StaffOrderController;
import example.com.ltw.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

@SpringBootTest
public class StaffDashboardTest {

    @Autowired
    private org.springframework.web.context.WebApplicationContext webApplicationContext;

    @Autowired
    private AuthService authService;

    @Test
    public void testStaffLoginAndDashboardAccess() throws Exception {
        MockHttpSession session = new MockHttpSession();
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("staff1@gmail.com");
        loginRequest.setPassword("123456");

        User user = authService.login(loginRequest, session);
        System.out.println("Logged in user: " + user.getEmail() + ", role: " + user.getRole() + ", branch: " + user.getBranch());

        org.springframework.test.web.servlet.MockMvc mockMvc = 
                org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/staff/dashboard")
                .session(session))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/staff/orders")
                .session(session))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/staff/pending-orders")
                .session(session))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/staff/returns")
                .session(session))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/staff/chat")
                .session(session))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
    }
}
