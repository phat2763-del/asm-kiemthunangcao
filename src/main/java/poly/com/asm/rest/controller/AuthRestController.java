package poly.com.asm.rest.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;
import poly.com.asm.dao.AccountDAO;
import poly.com.asm.entity.Account;
import poly.com.asm.model.LoginRequest;
import java.util.Map;
import java.util.HashMap;

@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
@RestController
@RequestMapping("/api/auth")
public class AuthRestController {

    @Autowired AccountDAO accountDao;
    @Autowired HttpSession session;

    // 1. API Đăng nhập: Cấp Session (JSESSIONID)
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        Account user = accountDao.findById(loginRequest.getUsername()).orElse(null);
        
        Map<String, Object> response = new HashMap<>();
        
        if (user != null && user.getPassword().equals(loginRequest.getPassword())) {
            session.setAttribute("user", user);
            
            response.put("success", true);
            response.put("message", "Đăng nhập thành công!");
            response.put("username", user.getUsername());
            response.put("isAdmin", user.getAdmin());
            return ResponseEntity.ok(response);
        }
        
        response.put("success", false);
        response.put("message", "Tài khoản hoặc mật khẩu không chính xác!");
        return ResponseEntity.status(401).body(response);
    }

    // 2. API Kiểm tra trạng thái (Dùng để hiện Profile hoặc Check Session)
    @GetMapping("/check")
    public ResponseEntity<?> check() {
        Account user = (Account) session.getAttribute("user");
        if (user != null) {
            Account freshUser = accountDao.findById(user.getUsername()).orElse(user);
            freshUser.setPassword("******");
            return ResponseEntity.ok(freshUser);
        }
        return ResponseEntity.status(401).body("{\"message\": \"Chưa đăng nhập\"}");
    }
    
    // 3. API Test Quyền Admin
    @GetMapping("/admin/test")
    public ResponseEntity<?> testAdmin() {
        Account user = (Account) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).body("{\"message\": \"Vui lòng đăng nhập!\"}");
        }
        if (!user.getAdmin()) {
            return ResponseEntity.status(403).body("{\"message\": \"Lỗi 403: Bạn không có quyền Admin!\"}");
        }
        return ResponseEntity.ok("Chào Admin " + user.getUsername());
    }

    // 4. API Đăng xuất: Hủy Session lập tức
    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        session.invalidate(); 
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Đăng xuất thành công!");
        return ResponseEntity.ok(response);
    }
    
    // 5. API Đăng ký tài khoản
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Account account) {
        Map<String, Object> response = new HashMap<>();
        
        if (accountDao.existsById(account.getUsername())) {
            response.put("success", false);
            response.put("message", "Tên đăng nhập đã tồn tại!");
            return ResponseEntity.status(400).body(response);
        }

        account.setAdmin(false);      
        account.setActivated(true);    
        if (account.getPhoto() == null || account.getPhoto().isEmpty()) {
            account.setPhoto("user.png"); 
        }
        
        try {
            accountDao.save(account);
            response.put("success", true);
            response.put("message", "Đăng ký tài khoản thành công!");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi hệ thống: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    // 6. API Cập nhật Hồ sơ cá nhân
    @PutMapping("/profile/update")
    public ResponseEntity<?> updateProfile(@RequestBody Account data) {
        Account sessionUser = (Account) session.getAttribute("user");
        if (sessionUser == null) return ResponseEntity.status(401).body("{\"message\": \"Chưa đăng nhập\"}");

        Account currentUser = accountDao.findById(sessionUser.getUsername()).orElse(null);
        if (currentUser != null) {
            currentUser.setFullname(data.getFullname());
            currentUser.setEmail(data.getEmail());
            
            accountDao.save(currentUser);
            session.setAttribute("user", currentUser);
            
            return ResponseEntity.ok("{\"success\": true, \"message\": \"Cập nhật thành công!\"}");
        }
        return ResponseEntity.status(404).body("{\"message\": \"Không tìm thấy tài khoản!\"}");
    }

    // 🟢 7. BỔ SUNG API ĐỔI MẬT KHẨU (Đang bị thiếu gây lỗi trước đó)
    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> body) {
        Account sessionUser = (Account) session.getAttribute("user");
        if (sessionUser == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Vui lòng đăng nhập!"));
        }

        String currentPassword = body.get("currentPassword");
        String newPassword = body.get("newPassword");

        Account currentUser = accountDao.findById(sessionUser.getUsername()).orElse(null);
        if (currentUser != null && currentUser.getPassword().equals(currentPassword)) {
            currentUser.setPassword(newPassword);
            accountDao.save(currentUser);
            session.setAttribute("user", currentUser);
            return ResponseEntity.ok(Map.of("success", true, "message", "Đổi mật khẩu thành công!"));
        }
        
        return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Mật khẩu hiện tại không chính xác!"));
    }
}