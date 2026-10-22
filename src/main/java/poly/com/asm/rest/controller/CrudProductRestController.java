package poly.com.asm.rest.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import poly.com.asm.entity.Category;
import poly.com.asm.entity.Product;
import poly.com.asm.service.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
@RequestMapping("/api/admin/products")
public class CrudProductRestController {

    @Autowired ProductService productService;
    @Autowired CategoryService categoryService;
    @Autowired ParamService paramService;

    @GetMapping
    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok(productService.findAll());
    }

    @PostMapping
    public ResponseEntity<?> create(
            @ModelAttribute Product product, 
            @RequestParam(value = "photo_file", required = false) MultipartFile file,
            @RequestParam(value = "categoryId", required = false) String categoryId) {
        try {
            if (file != null && !file.isEmpty()) {
                paramService.save(file, "images");
                product.setImage(file.getOriginalFilename());
            } else {
                product.setImage("default.png");
            }

            if (categoryId != null && !categoryId.isEmpty()) {
                Category cat = categoryService.findById(categoryId);
                if (cat != null) product.setCategory(cat);
            }

            if (product.getCreateDate() == null) {
                product.setCreateDate(new Date());
            }

            Product saved = productService.create(product);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("message", "Lỗi thêm sản phẩm: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @PathVariable("id") Integer id,
            @ModelAttribute Product product,
            @RequestParam(value = "photo_file", required = false) MultipartFile file,
            @RequestParam(value = "categoryId", required = false) String categoryId) {
        
        Product oldProduct = productService.findById(id);
        if (oldProduct == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Không tìm thấy sản phẩm ID: " + id));
        }

        try {
            if (file != null && !file.isEmpty()) {
                paramService.save(file, "images");
                product.setImage(file.getOriginalFilename());
            } else {
                product.setImage(oldProduct.getImage());
            }

            if (categoryId != null && !categoryId.isEmpty()) {
                Category cat = categoryService.findById(categoryId);
                if (cat != null) product.setCategory(cat);
            } else {
                product.setCategory(oldProduct.getCategory());
            }

            if (product.getCreateDate() == null) {
                product.setCreateDate(oldProduct.getCreateDate());
            }
            
            product.setId(id);
            Product updated = productService.update(product);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("message", "Lỗi cập nhật sản phẩm: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable("id") Integer id) {
        try {
            productService.delete(id);
            return ResponseEntity.ok(Map.of("message", "Xóa thành công!"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("message", "Không thể xóa sản phẩm do có ràng buộc dữ liệu!"));
        }
    }
}