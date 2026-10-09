package vn.nextech.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import vn.nextech.repository.ShopRepository;
import vn.nextech.service.CartService;

@ControllerAdvice
public class WebAdvice {
    private final ShopRepository shop;
    private final CartService cart;
    public WebAdvice(ShopRepository shop, CartService cart) {
        this.shop = shop;
        this.cart = cart;
    }

    @ModelAttribute
    public void global(Model model, HttpSession session) {
        model.addAttribute("categories", shop.categories());
        model.addAttribute("cartCount", cart.count(session));
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        model.addAttribute("customerLoggedIn", auth != null && auth.isAuthenticated() &&
            auth.getAuthorities().stream().anyMatch(a -> "ROLE_CUSTOMER".equals(a.getAuthority())));
    }
}
