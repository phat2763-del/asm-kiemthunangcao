package poly.com.asm.rest.controller;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession; // 👈 Nhớ import HttpSession
import poly.com.asm.entity.Account;       // 👈 Nhớ import Account entity
import poly.com.asm.model.CartItem;
import poly.com.asm.service.CartService;

@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true") // 👈 Đồng bộ allowCredentials
@RestController
@RequestMapping("/api/cart")
public class CartRestController {

    @Autowired
    CartService cartService;

    @Autowired
    HttpSession session; // 👈 Inject HttpSession vào đây

    @GetMapping
    public ResponseEntity<?> getCart() {
        // (Tùy chọn) Có thể check đăng nhập nếu giỏ hàng lưu theo DB của user
        Collection<CartItem> items = cartService.getItems();
        Map<String, Object> response = new HashMap<>();
        response.put("items", items);
        response.put("count", cartService.getCount());
        response.put("amount", cartService.getAmount());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getCartInfo() {
        Map<String, Object> response = new HashMap<>();
        response.put("count", cartService.getCount());
        response.put("amount", cartService.getAmount());
        return ResponseEntity.ok(response);
    }

    // 🟢 BỔ SUNG KIỂM TRA SESSION ĐĂNG NHẬP CHO AN TOÀN
    @PostMapping("/add/{id}")
    public ResponseEntity<?> addToCart(
            @PathVariable("id") Integer id,
            @RequestParam("size") Integer size) {
        
        // Kiểm tra xem user đã đăng nhập qua AuthRestController chưa
        Account currentUser = (Account) session.getAttribute("user");
        if (currentUser == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Vui lòng đăng nhập!"));
        }

        // Tiến hành thêm vào giỏ hàng
        cartService.add(id, size);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Thêm vào giỏ hàng thành công!");
        response.put("count", cartService.getCount());
        response.put("amount", cartService.getAmount());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateCart(
            @PathVariable("id") Integer id,
            @RequestParam("size") Integer size,
            @RequestParam("qty") Integer qty) {
        cartService.update(id, size, qty);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Cập nhật giỏ hàng thành công!");
        response.put("count", cartService.getCount());
        response.put("amount", cartService.getAmount());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/remove/{id}")
    public ResponseEntity<?> removeFromCart(
            @PathVariable("id") Integer id,
            @RequestParam("size") Integer size) {
        cartService.remove(id, size);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Xóa khỏi giỏ hàng thành công!");
        response.put("count", cartService.getCount());
        response.put("amount", cartService.getAmount());
        return ResponseEntity.ok(response);
    }
}