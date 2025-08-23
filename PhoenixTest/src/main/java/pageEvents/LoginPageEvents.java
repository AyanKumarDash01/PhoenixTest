package pageEvents;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import pageObjects.LoginPage;

public class LoginPageEvents {
    WebDriver driver;

    public LoginPageEvents(WebDriver driver) {
        this.driver = driver;
    }

    public void login(String username, String password) {
        WebElement user = driver.findElement(LoginPage.usernameInput);
        WebElement pass = driver.findElement(LoginPage.passwordInput);
        WebElement loginBtn = driver.findElement(LoginPage.loginButton);

        user.sendKeys(username);
        pass.sendKeys(password);
        loginBtn.click();
    }
}
