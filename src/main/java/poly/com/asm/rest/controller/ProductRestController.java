package poly.com.asm.rest.controller;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import poly.com.asm.dao.ProductDAO;
import poly.com.asm.entity.Product;

@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
@RestController
@RequestMapping("/api/products")
public class ProductRestController {

    @Autowired
    ProductDAO productDao;

    /**
     * API Lấy danh sách sản phẩm tổng hợp:
     * Hỗ trợ Phân trang, Lọc theo Danh mục (cid) và Tìm kiếm theo từ khóa (keywords).
     */
    @GetMapping
    public ResponseEntity<?> getAllProducts(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "12") int size, // 🟢 Tăng lên 12 sản phẩm/trang
            @RequestParam(name = "cid", required = false) String cid,
            @RequestParam(name = "keywords", required = false) String keywords) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<Product> data;

        if (cid != null && !cid.trim().isEmpty() && !cid.equalsIgnoreCase("null")) {
            data = productDao.findByCategoryId(cid, pageable);
        } else if (keywords != null && !keywords.trim().isEmpty()) {
            data = productDao.findByKeywords(keywords, pageable);
        } else {
            data = productDao.findAll(pageable);
        }

        return ResponseEntity.ok(data);
    }
    /**
     * API Lấy chi tiết 1 sản phẩm theo ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getProductById(@PathVariable("id") Integer id) {
        Optional<Product> product = productDao.findById(id);
        if (product.isPresent()) {
            return ResponseEntity.ok(product.get());
        }
        return ResponseEntity.status(404).body("{\"message\": \"Không tìm thấy sản phẩm!\"}");
    }

    /**
     * API Lọc sản phẩm theo Danh mục riêng biệt
     */
    @GetMapping("/category/{cid}")
    public ResponseEntity<?> getProductsByCategory(
            @PathVariable("cid") String cid,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "9") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<Product> data = productDao.findByCategoryId(cid, pageable);
        return ResponseEntity.ok(data);
    }

    /**
     * API Tìm kiếm sản phẩm theo từ khóa
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchProducts(
            @RequestParam("keywords") String keywords,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "9") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<Product> data = productDao.findByKeywords(keywords, pageable);
        return ResponseEntity.ok(data);
    }
}