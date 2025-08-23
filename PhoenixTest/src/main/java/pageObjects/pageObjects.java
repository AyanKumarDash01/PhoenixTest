package pageObjects;

import org.openqa.selenium.By;

public class LoginPage {
    // Locators
    public static final By usernameInput = By.name("username");
    public static final By passwordInput = By.name("password");
    public static final By loginButton = By.xpath("//button[@type='submit']");
}
