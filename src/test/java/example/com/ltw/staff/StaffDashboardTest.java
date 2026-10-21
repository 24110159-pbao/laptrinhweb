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
    private StaffOrderController staffOrderController;

    @Autowired
    private AuthService authService;

    @Test
    public void testStaffLoginAndDashboardAccess() {
        MockHttpSession session = new MockHttpSession();
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("staff1@gamil.com");
        loginRequest.setPassword("123456");

        User user = authService.login(loginRequest, session);
        System.out.println("Logged in user: " + user.getEmail() + ", role: " + user.getRole() + ", branch: " + user.getBranch());

        Model model = new ExtendedModelMap();
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = staffOrderController.showDashboard(model, session, redirectAttributes);
        System.out.println("Returned view: " + view);
        System.out.println("Model attributes: " + model.asMap().keySet());
    }
}
