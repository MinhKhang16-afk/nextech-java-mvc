package vn.nextech.controller;

import jakarta.validation.Valid;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import vn.nextech.repository.OrderRepository;
import vn.nextech.service.ImageService;
import java.io.IOException;
import vn.nextech.model.CustomerAccount;
import vn.nextech.model.RegistrationForm;
import vn.nextech.service.CustomerAccountService;

import java.util.Objects;

@Controller
public class CustomerAccountController {
    private final CustomerAccountService customers;
    private final OrderRepository orders;
    private final ImageService images;

    public CustomerAccountController(CustomerAccountService customers,
                                     OrderRepository orders, ImageService images) {
        this.customers = customers;
        this.orders = orders;
        this.images = images;
    }

    @GetMapping("/tai-khoan/dang-ky")
    public String registerForm(Model model) {
        model.addAttribute("registrationForm", new RegistrationForm());
        return "customer/register";
    }

    @PostMapping("/tai-khoan/dang-ky")
    public String register(@Valid @ModelAttribute("registrationForm") RegistrationForm form,
                           BindingResult errors) {
        if (form.getPassword() != null && form.getConfirmPassword() != null &&
                !Objects.equals(form.getPassword(), form.getConfirmPassword())) {
            errors.rejectValue("confirmPassword", "mismatch", "Mật khẩu xác nhận không khớp");
        }
        if (errors.hasErrors()) return "customer/register";
        try {
            customers.register(form);
        } catch (IllegalArgumentException ex) {
            if (ex.getMessage().contains("Email"))
                errors.rejectValue("email", "duplicate", ex.getMessage());
            else
                errors.rejectValue("password", "invalid", ex.getMessage());
            return "customer/register";
        }
        return "redirect:/tai-khoan/dang-nhap?registered";
    }

    @GetMapping("/tai-khoan/dang-nhap")
    public String login(Authentication authentication) {
        if (isCustomer(authentication)) return "redirect:/tai-khoan";
        return "customer/login";
    }

    @GetMapping("/tai-khoan")
    public String profile(Authentication authentication, Model model) {
        if (!isCustomer(authentication)) return "redirect:/tai-khoan/dang-nhap";
        CustomerAccount account = customers.findByEmail(authentication.getName())
            .orElseThrow(() -> new IllegalStateException("Tài khoản không tồn tại"));
        model.addAttribute("fullName", account.fullName());
        model.addAttribute("email", account.email());
        model.addAttribute("phone", account.phone());
        model.addAttribute("birthDate", account.birthDate());
        model.addAttribute("createdAt", account.createdAt());
        model.addAttribute("avatarPath", account.avatarPath());
        model.addAttribute("orderCount", orders.findByAccount(account.id()).size());
        return "customer/profile";
    }

    @PostMapping("/tai-khoan/avatar")
    public String uploadAvatar(@RequestParam("file") MultipartFile file,
                               Authentication authentication, RedirectAttributes flash) {
        if (!isCustomer(authentication)) return "redirect:/tai-khoan/dang-nhap";
        try {
            String path = images.save(file);
            if (path == null) throw new IllegalArgumentException("Vui lòng chọn ảnh đại diện.");
            customers.updateAvatar(authentication.getName(), path);
            flash.addFlashAttribute("message", "Đã cập nhật ảnh đại diện.");
        } catch (IOException | IllegalArgumentException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/tai-khoan";
    }

    @GetMapping("/tai-khoan/don-hang")
    public String orderHistory(Authentication authentication, Model model) {
        if (!isCustomer(authentication)) return "redirect:/tai-khoan/dang-nhap";
        CustomerAccount account = customers.findByEmail(authentication.getName())
            .orElseThrow(() -> new IllegalStateException("Tài khoản không tồn tại"));
        model.addAttribute("orders", orders.findByAccount(account.id()));
        return "customer/orders";
    }

    @GetMapping("/tai-khoan/don-hang/{id}")
    public String orderDetail(@PathVariable long id, Authentication authentication, Model model) {
        if (!isCustomer(authentication)) return "redirect:/tai-khoan/dang-nhap";
        CustomerAccount account = customers.findByEmail(authentication.getName())
            .orElseThrow(() -> new IllegalStateException("Tài khoản không tồn tại"));
        var order = orders.findOwnedByAccount(id, account.id()).orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("order", order);
        model.addAttribute("lines", orders.lines(id));
        model.addAttribute("history", orders.statusHistory(id));
        return "customer/order-detail";
    }

    private boolean isCustomer(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated() &&
            !(authentication instanceof AnonymousAuthenticationToken) &&
            authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_CUSTOMER".equals(a.getAuthority()));
    }
}
