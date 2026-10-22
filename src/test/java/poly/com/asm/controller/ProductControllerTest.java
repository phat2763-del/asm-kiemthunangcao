package poly.com.asm.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import poly.com.asm.entity.Product;
import poly.com.asm.service.CategoryService;
import poly.com.asm.service.ProductService;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class ProductControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ProductService productService;

    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private ProductController productController;

    @BeforeEach
    void setUp() {
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("classpath:/templates/");
        viewResolver.setSuffix(".html");

        mockMvc = MockMvcBuilders.standaloneSetup(productController)
                .setViewResolvers(viewResolver)
                .build();
    }

    @Test
    @DisplayName("TC_PROD_01: GET /product/detail/{id} - Trang truyền thống (Direct Load)")
    void testDetailDirectLoad() throws Exception {
        Product mockProduct = new Product();
        mockProduct.setId(1);

        when(productService.findById(1)).thenReturn(mockProduct);

        mockMvc.perform(get("/product/detail/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("layout/index"))
                .andExpect(model().attribute("item", mockProduct))
                .andExpect(model().attribute("view", "product/product-detail.html"));
    }

    @Test
    @DisplayName("TC_PROD_02: GET /product/detail/{id} - Yêu cầu AJAX (XMLHttpRequest)")
    void testDetailAjax() throws Exception {
        Product mockProduct = new Product();
        mockProduct.setId(1);

        when(productService.findById(1)).thenReturn(mockProduct);

        mockMvc.perform(get("/product/detail/1")
                        .header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isOk())
                .andExpect(view().name("product/product-detail"))
                .andExpect(model().attribute("item", mockProduct));
    }

    @Test
    @DisplayName("TC_PROD_03: GET /product/list-by-category/{cid} - Trang truyền thống")
    void testListByCategoryDirectLoad() throws Exception {
        Pageable pageable = PageRequest.of(0, 8);
        Page<Product> mockPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

        when(productService.findByCategoryId(eq("C01"), any(Pageable.class))).thenReturn(mockPage);
        when(categoryService.findAll()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/product/list-by-category/C01").param("p", "0"))
                .andExpect(status().isOk())
                .andExpect(view().name("layout/index"))
                .andExpect(model().attribute("page", mockPage))
                .andExpect(model().attribute("categories", Collections.emptyList()))
                .andExpect(model().attribute("view", "product/product-list.html"));
    }

    @Test
    @DisplayName("TC_PROD_04: GET /product/list-by-category/{cid} - Yêu cầu AJAX")
    void testListByCategoryAjax() throws Exception {
        Pageable pageable = PageRequest.of(0, 8);
        Page<Product> mockPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

        when(productService.findByCategoryId(eq("C01"), any(Pageable.class))).thenReturn(mockPage);
        when(categoryService.findAll()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/product/list-by-category/C01")
                        .header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isOk())
                .andExpect(view().name("product/product-list"))
                .andExpect(model().attribute("page", mockPage));
    }
}