package poly.com.asm.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import jakarta.servlet.http.HttpSession;
import poly.com.asm.entity.Account;
import poly.com.asm.service.AccountService;
import poly.com.asm.service.CookieService;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AccountService accountService;

    @Mock
    private CookieService cookieService;

    @Mock
    private HttpSession session; // Mock session để Controller gọi setAttribute/removeAttribute không bị lỗi

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("classpath:/templates/");
        viewResolver.setSuffix(".html");

        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setViewResolvers(viewResolver)
                .build();
    }

    @Test
    @DisplayName("TC_AUTH_01: GET /auth/login - Tải giao diện đăng nhập thành công")
    void testLoginPage() throws Exception {
        when(cookieService.getValue("user")).thenReturn("testUser");

        mockMvc.perform(get("/auth/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("layout/index"))
                .andExpect(model().attribute("username", "testUser"))
                .andExpect(model().attribute("view", "account/login.html"));
    }

    @Test
    @DisplayName("TC_AUTH_02: POST /auth/login - Đăng nhập thành công (Không chọn remember)")
    void testLoginSuccessWithoutRemember() throws Exception {
        Account mockAccount = new Account();
        mockAccount.setUsername("user01");
        mockAccount.setPassword("123456");
        mockAccount.setFullname("Nguyen Van A");

        when(accountService.findById("user01")).thenReturn(mockAccount);

        mockMvc.perform(post("/auth/login")
                        .param("username", "user01")
                        .param("password", "123456")
                        .param("remember", "false"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home/index"));

        // Kiểm tra cookie remove được gọi khi remember = false
        verify(cookieService, times(1)).remove("user");
    }

    @Test
    @DisplayName("TC_AUTH_02_B: POST /auth/login - Đăng nhập thành công (Có chọn remember)")
    void testLoginSuccessWithRemember() throws Exception {
        Account mockAccount = new Account();
        mockAccount.setUsername("user01");
        mockAccount.setPassword("123456");
        mockAccount.setFullname("Nguyen Van A");

        when(accountService.findById("user01")).thenReturn(mockAccount);

        mockMvc.perform(post("/auth/login")
                        .param("username", "user01")
                        .param("password", "123456")
                        .param("remember", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home/index"));

        // Kiểm tra cookie add được gọi khi remember = true
        verify(cookieService, times(1)).add(eq("user"), eq("user01"), anyInt());
    }

    @Test
    @DisplayName("TC_AUTH_03: POST /auth/login - Đăng nhập thất bại do sai mật khẩu")
    void testLoginWrongPassword() throws Exception {
        Account mockAccount = new Account();
        mockAccount.setUsername("user01");
        mockAccount.setPassword("123456");

        when(accountService.findById("user01")).thenReturn(mockAccount);

        mockMvc.perform(post("/auth/login")
                        .param("username", "user01")
                        .param("password", "wrongpass"))
                .andExpect(status().isOk())
                .andExpect(view().name("layout/index"))
                .andExpect(model().attribute("message", "Sai tên đăng nhập hoặc mật khẩu!"))
                .andExpect(model().attribute("view", "account/login.html"));
    }

    @Test
    @DisplayName("TC_AUTH_04: POST /auth/login - Đăng nhập thất bại do tài khoản không tồn tại")
    void testLoginUserNotFound() throws Exception {
        when(accountService.findById("unknown")).thenReturn(null);

        mockMvc.perform(post("/auth/login")
                        .param("username", "unknown")
                        .param("password", "123"))
                .andExpect(status().isOk())
                .andExpect(view().name("layout/index"))
                .andExpect(model().attribute("message", "Sai tên đăng nhập hoặc mật khẩu!"))
                .andExpect(model().attribute("view", "account/login.html"));
    }

    @Test
    @DisplayName("TC_AUTH_05: GET /auth/logoff - Đăng xuất hệ thống thành công")
    void testLogoff() throws Exception {
        mockMvc.perform(get("/auth/logoff"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home/index"));
    }
}