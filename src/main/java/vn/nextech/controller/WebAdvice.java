package vn.nextech.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import vn.nextech.repository.ShopRepository;
import vn.nextech.service.CartService;
import org.springframework.ui.Model;

@ControllerAdvice
public class WebAdvice {
    private final ShopRepository shop;
    private final CartService cart;
    public WebAdvice(ShopRepository shop,CartService cart){this.shop=shop;this.cart=cart;}
    @ModelAttribute
    public void global(Model model,HttpSession session){
        model.addAttribute("categories",shop.categories());
        model.addAttribute("cartCount",cart.count(session));
    }
}
