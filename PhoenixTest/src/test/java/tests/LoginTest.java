package tests;

import base.BaseTest;
import org.testng.annotations.Test;
import pageEvents.LoginPageEvents;

public class LoginTest extends BaseTest {

    @Test
    public void testAdminLogin() {
        LoginPageEvents loginPage = new LoginPageEvents(driver);
        loginPage.login("Admin", "admin123");
        // Here you can add assertion like:
        // Assert.assertTrue(driver.getCurrentUrl().contains("dashboard"), "Login failed!");
    }
}
