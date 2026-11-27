package vn.nextech.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import vn.nextech.repository.ShopRepository;
import vn.nextech.service.CartService;
import vn.nextech.service.CustomerAccountService;

@ControllerAdvice
public class WebAdvice {
    private final ShopRepository shop;
    private final CartService cart;
    private final CustomerAccountService customers;
    public WebAdvice(ShopRepository shop, CartService cart, CustomerAccountService customers) {
        this.shop = shop;
        this.cart = cart;
        this.customers = customers;
    }

    @ModelAttribute
    public void global(Model model, HttpSession session) {
        model.addAttribute("categories", shop.categories());
        model.addAttribute("cartCount", cart.count(session));
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean loggedIn = auth != null && auth.isAuthenticated() &&
            auth.getAuthorities().stream().anyMatch(a -> "ROLE_CUSTOMER".equals(a.getAuthority()));
        model.addAttribute("customerLoggedIn", loggedIn);
        if (loggedIn) {
            customers.findByEmail(auth.getName()).ifPresent(account -> {
                model.addAttribute("customerAvatar", account.avatarPath());
                model.addAttribute("customerName", account.fullName());
                String[] parts = account.fullName().trim().split("\\s+");
                String initials = parts.length == 0 ? "N" :
                    (parts.length == 1 ? parts[0].substring(0, 1) :
                    parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1));
                model.addAttribute("customerInitials", initials.toUpperCase(java.util.Locale.ROOT));
            });
        }
    }
}
