package example.com.ltw;

import example.com.ltw.shipper.controller.ShipperController;
import example.com.ltw.user.entity.User;
import example.com.ltw.user.repository.UserRepository;
import jakarta.servlet.ServletContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ConcurrentModel;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;
import org.thymeleaf.web.servlet.IServletWebExchange;
import org.thymeleaf.context.WebContext;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class LtwckApplicationTests {

	@Autowired
	private ShipperController shipperController;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private SpringTemplateEngine templateEngine;

	@Autowired
	private ServletContext servletContext;

	@Test
	void contextLoads() {
	}

	@Test
	void testRenderShipperTasks() {
		User shipper = userRepository.findById(7L).orElse(null);
		if (shipper != null) {
			MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
			MockHttpServletResponse response = new MockHttpServletResponse();
			MockHttpSession session = new MockHttpSession();
			session.setAttribute("currentUser", shipper);
			session.setAttribute("user", shipper);
			request.setSession(session);

			ConcurrentModel model = new ConcurrentModel();
			String view = shipperController.listTasks(
					null, null, "today", null, null, 0, 10,
					model, session, new RedirectAttributesModelMap()
			);

			System.out.println("VIEW: " + view);
			JakartaServletWebApplication app = JakartaServletWebApplication.buildApplication(servletContext);
			IServletWebExchange webExchange = app.buildExchange(request, response);
			WebContext context = new WebContext(webExchange, request.getLocale(), model);

			String html = templateEngine.process(view, context);
			System.out.println("RENDERED HTML LENGTH: " + html.length());
			assertNotNull(html);

			// Test Dashboard
			ConcurrentModel dashModel = new ConcurrentModel();
			String dashView = shipperController.showDashboard("today", null, null, dashModel, session, new RedirectAttributesModelMap());
			WebContext dashContext = new WebContext(webExchange, request.getLocale(), dashModel);
			String dashHtml = templateEngine.process(dashView, dashContext);
			System.out.println("RENDERED DASHBOARD HTML LENGTH: " + dashHtml.length());
			assertNotNull(dashHtml);

			// Test Task Detail (id=10)
			ConcurrentModel detailModel = new ConcurrentModel();
			String detailView = shipperController.showTaskDetail(10L, detailModel, session, new RedirectAttributesModelMap());
			WebContext detailContext = new WebContext(webExchange, request.getLocale(), detailModel);
			String detailHtml = templateEngine.process(detailView, detailContext);
			System.out.println("RENDERED DETAIL HTML LENGTH: " + detailHtml.length());
			assertNotNull(detailHtml);
		}
	}
}
