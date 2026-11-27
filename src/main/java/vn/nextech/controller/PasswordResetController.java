package vn.nextech.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.nextech.service.PasswordResetService;

@Controller
public class PasswordResetController {
    private final PasswordResetService resets;

    public PasswordResetController(PasswordResetService resets) {
        this.resets = resets;
    }

    @GetMapping("/tai-khoan/quen-mat-khau")
    public String forgotForm(Model model) {
        model.addAttribute("mailEnabled", resets.isMailEnabled());
        return "customer/forgot-password";
    }

    @PostMapping("/tai-khoan/quen-mat-khau")
    public String requestReset(@RequestParam String email, HttpServletRequest request,
                               RedirectAttributes flash) {
        try {
            String base = request.getRequestURL().toString()
                .replace(request.getRequestURI(), request.getContextPath());
            resets.requestReset(email, base + "/tai-khoan/dat-lai-mat-khau");
            flash.addFlashAttribute("message",
                "Nếu email thuộc tài khoản đang hoạt động, hướng dẫn đặt lại mật khẩu sẽ được gửi đến hộp thư.");
        } catch (IllegalStateException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
            return "redirect:/tai-khoan/quen-mat-khau";
        }
        return "redirect:/tai-khoan/quen-mat-khau?sent";
    }

    @GetMapping("/tai-khoan/dat-lai-mat-khau")
    public String resetForm(@RequestParam(required = false) String token, Model model) {
        if (token == null || token.isBlank()) {
            model.addAttribute("error", "Liên kết đặt lại mật khẩu không hợp lệ.");
        }
        model.addAttribute("token", token == null ? "" : token);
        return "customer/reset-password";
    }

    @PostMapping("/tai-khoan/dat-lai-mat-khau")
    public String reset(@RequestParam String token, @RequestParam String password,
                        @RequestParam String confirmPassword, RedirectAttributes flash) {
        if (!password.equals(confirmPassword)) {
            flash.addFlashAttribute("error", "Mật khẩu xác nhận không khớp.");
            return "redirect:/tai-khoan/dat-lai-mat-khau?token=" + token;
        }
        try {
            resets.resetPassword(token, password);
            flash.addFlashAttribute("message", "Đặt lại mật khẩu thành công. Bạn có thể đăng nhập.");
            return "redirect:/tai-khoan/dang-nhap?reset";
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
            return "redirect:/tai-khoan/dat-lai-mat-khau?token=" + token;
        }
    }
}
