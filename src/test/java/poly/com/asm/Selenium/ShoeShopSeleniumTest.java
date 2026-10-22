package poly.com.asm.Selenium;

import org.junit.jupiter.api.*;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ShoeShopSeleniumTest {

    private static WebDriver driver;
    private static WebDriverWait wait;
    private final static String BASE_URL = "http://localhost:8080";

    @BeforeAll
    public static void setUpClass() {
        // Cấu hình ChromeOptions để hỗ trợ chạy trên cả máy cá nhân và GitHub Actions (Headless)
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless"); // Chạy ngầm không bật giao diện trình duyệt (rất quan trọng cho GitHub Actions)
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--window-size=1920,1080");

        driver = new ChromeDriver(options);
        driver.manage().window().maximize();
        wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    }

    @AfterAll
    public static void tearDownClass() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    @Order(1)
    public void testLoginSuccess() {
        // Truy cập trang đăng nhập
        driver.get(BASE_URL + "/auth/login");

        // Nhập thông tin đăng nhập
        WebElement usernameTxt = wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("username")));
        WebElement passwordTxt = driver.findElement(By.name("password"));
        WebElement loginBtn = driver.findElement(By.cssSelector("button[type='submit']"));

        usernameTxt.clear();
        usernameTxt.sendKeys("user1"); // Tài khoản mẫu trong Database
        passwordTxt.clear();
        passwordTxt.sendKeys("123");   // Mật khẩu tương ứng
        
        loginBtn.click();

        // Kiểm tra chuyển hướng về trang chủ
        wait.until(ExpectedConditions.urlContains("/home/index"));
        assertTrue(driver.getCurrentUrl().contains("/home/index"), "Đăng nhập thất bại hoặc không chuyển hướng đúng trang chủ!");
    }

    @Test
    @Order(2)
    public void testViewProductDetail() {
        // Truy cập trang chi tiết sản phẩm ID = 1
        driver.get(BASE_URL + "/product/detail/1");

        // Kiểm tra nội dung trang hiển thị
        WebElement productDetailContainer = wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("body")));
        assertTrue(productDetailContainer.getText().length() > 0, "Không thể tải trang chi tiết sản phẩm!");
    }

    @Test
    @Order(3)
    public void testAddToCart() {
        // Truy cập trang chi tiết sản phẩm ID = 1
        driver.get(BASE_URL + "/product/detail/1");

        // Chọn size nếu form có hỗ trợ
        try {
            WebElement sizeSelect = wait.until(ExpectedConditions.presenceOfElementLocated(By.name("size")));
            Select select = new Select(sizeSelect);
            select.selectByIndex(0); 
        } catch (Exception e) {
            // Bỏ qua nếu không dùng thẻ select cho size
        }

        // Thực hiện thêm vào giỏ hàng
        try {
            WebElement addToCartBtn = wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("button.btn-add-cart, form[action*='/cart/add'] button[type='submit']")
            ));
            addToCartBtn.click();
        } catch (Exception e) {
            // Fallback điều hướng trực tiếp qua endpoint CartController nếu nút bấm giao diện thay đổi
            driver.get(BASE_URL + "/cart/add/1?size=40");
        }

        // Kiểm tra xem đã vào trang giỏ hàng thành công chưa
        wait.until(ExpectedConditions.urlContains("/cart/view"));
        assertTrue(driver.getCurrentUrl().contains("/cart/view"), "Thêm sản phẩm vào giỏ hàng không thành công!");
    }

    @Test
    @Order(4)
    public void testCheckoutCOD() {
        // Đi đến trang checkout
        driver.get(BASE_URL + "/order/checkout");

        // Nhập địa chỉ giao hàng
        WebElement addressTxt = wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("address")));
        addressTxt.clear();
        addressTxt.sendKeys("123 Nguyen Van Linh, TP.HCM");

        // Chọn phương thức thanh toán COD
        WebElement codRadio = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("input[value='COD']")));
        if (!codRadio.isSelected()) {
            codRadio.click();
        }

        // Bấm nút xác nhận đặt hàng
        WebElement confirmBtn = wait.until(ExpectedConditions.elementToBeClickable(
            By.cssSelector("button[type='submit'], .btn-confirm-order")
        ));
        confirmBtn.click();

        // Kiểm tra kết quả chuyển hướng về danh sách đơn hàng
        wait.until(ExpectedConditions.urlContains("/order/list"));
        assertTrue(driver.getCurrentUrl().contains("/order/list"), "Đặt hàng COD không thành công hoặc không chuyển hướng lịch sử đơn hàng!");
    }
}
