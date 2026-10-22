package poly.com.asm.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import poly.com.asm.entity.Account;
import poly.com.asm.entity.Order;
import poly.com.asm.entity.Product;
import poly.com.asm.model.CartItem;
import poly.com.asm.service.*;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class OrderControllerTest {

    private MockMvc mockMvc;

    @Mock private CartService cartService;
    @Mock private OrderService orderService;
    @Mock private ProductService productService;
    @Mock private VNPayService vnpayService;

    @InjectMocks
    private OrderController orderController;

    private MockHttpSession mockSession;
    private Account mockUser;

    @BeforeEach
    void setUp() {
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("classpath:/templates/");
        viewResolver.setSuffix(".html");

        mockMvc = MockMvcBuilders.standaloneSetup(orderController)
                .setViewResolvers(viewResolver)
                .build();

        mockSession = new MockHttpSession();
        mockUser = new Account();
        mockUser.setUsername("user01");
    }

    @Test
    @DisplayName("TC_ORDER_01: GET /order/checkout - Chuyển hướng /cart/view khi giỏ hàng rỗng")
    void testCheckoutEmptyCart() throws Exception {
        when(cartService.getCount()).thenReturn(0);

        mockMvc.perform(get("/order/checkout"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart/view"));
    }

    @Test
    @DisplayName("TC_ORDER_02: GET /order/checkout - Chuyển hướng /auth/login khi chưa đăng nhập")
    void testCheckoutUnauthenticated() throws Exception {
        when(cartService.getCount()).thenReturn(2);

        mockMvc.perform(get("/order/checkout"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login"));
    }

    @Test
    @DisplayName("TC_ORDER_03: GET /order/checkout - Tải trang Checkout thành công")
    void testCheckoutSuccess() throws Exception {
        when(cartService.getCount()).thenReturn(2);
        mockSession.setAttribute("user", mockUser);

        mockMvc.perform(get("/order/checkout").session(mockSession))
                .andExpect(status().isOk())
                .andExpect(view().name("layout/index"))
                .andExpect(model().attribute("user", mockUser))
                .andExpect(model().attribute("view", "order/check-out.html"));
    }

    @Test
    @DisplayName("TC_ORDER_04: POST /order/confirm - Đặt hàng thành công phương thức COD")
    void testConfirmCOD() throws Exception {
        mockSession.setAttribute("user", mockUser);

        CartItem item = new CartItem();
        item.setId(1);
        item.setPrice(100.0);
        item.setQty(2);
        item.setSize("L");

        when(cartService.getItems()).thenReturn(List.of(item));
        when(productService.findById(1)).thenReturn(new Product());

        mockMvc.perform(post("/order/confirm")
                        .session(mockSession)
                        .param("address", "120 Ton That Thuyet")
                        .param("paymentMethod", "COD"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/order/list"));

        verify(orderService, times(1)).create(any(Order.class));
        verify(cartService, times(1)).clear();
    }

    @Test
    @DisplayName("TC_ORDER_05: POST /order/confirm - Đặt hàng phương thức VNPay (Chuyển hướng VNPay URL)")
    void testConfirmVNPay() throws Exception {
        mockSession.setAttribute("user", mockUser);

        Order savedOrder = new Order();
        savedOrder.setId(100L);

        when(cartService.getItems()).thenReturn(Collections.emptyList());
        when(orderService.create(any(Order.class))).thenReturn(savedOrder);
        when(cartService.getAmount()).thenReturn(500000.0);
        when(vnpayService.createPaymentUrl(anyLong(), anyString(), anyString()))
                .thenReturn("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html");

        mockMvc.perform(post("/order/confirm")
                        .session(mockSession)
                        .param("address", "120 Ton That Thuyet")
                        .param("paymentMethod", "VNPAY"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html"));
    }

    @Test
    @DisplayName("TC_ORDER_06: GET /order/vnpay-return - Thanh toán VNPay thành công (vnp_ResponseCode = 00)")
    void testVnPayReturnSuccess() throws Exception {
        mockSession.setAttribute("pendingOrderId", 100L);
        Order mockOrder = new Order();
        mockOrder.setId(100L);

        when(orderService.findById(100L)).thenReturn(mockOrder);

        mockMvc.perform(get("/order/vnpay-return")
                        .session(mockSession)
                        .param("vnp_ResponseCode", "00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/order/detail/100"));

        verify(orderService, times(1)).update(mockOrder);
        verify(cartService, times(1)).clear();
    }

    @Test
    @DisplayName("TC_ORDER_07: GET /order/vnpay-return - Thanh toán VNPay thất bại (vnp_ResponseCode != 00)")
    void testVnPayReturnFailed() throws Exception {
        mockSession.setAttribute("pendingOrderId", 100L);

        mockMvc.perform(get("/order/vnpay-return")
                        .session(mockSession)
                        .param("vnp_ResponseCode", "24"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart/view"));
    }

    @Test
    @DisplayName("TC_ORDER_08: GET /order/pay-confirm/{id} - Admin xác nhận đã thanh toán")
    void testPayConfirm() throws Exception {
        Order mockOrder = new Order();
        mockOrder.setId(100L);

        when(orderService.findById(100L)).thenReturn(mockOrder);

        mockMvc.perform(get("/order/pay-confirm/100"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/order/detail/100"));

        verify(orderService, times(1)).update(mockOrder);
    }
}