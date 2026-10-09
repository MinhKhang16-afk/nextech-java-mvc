package vn.nextech.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.nextech.model.CheckoutForm;
import vn.nextech.service.CartService;
import vn.nextech.service.CustomerAccountService;
import vn.nextech.service.OrderService;

@Controller
public class CheckoutController {
    private final CartService cart;
    private final OrderService orders;
    private final CustomerAccountService customers;
    public CheckoutController(CartService cart, OrderService orders, CustomerAccountService customers) {
        this.cart = cart;
        this.orders = orders;
        this.customers = customers;
    }
    @GetMapping("/dat-hang")
    public String form(HttpSession session, Model model, Authentication authentication){
        if(cart.items(session).isEmpty())return "redirect:/gio-hang";
        CheckoutForm checkout = new CheckoutForm();
        if (authentication != null && authentication.isAuthenticated() &&
            authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_CUSTOMER".equals(a.getAuthority()))) {
            customers.findByEmail(authentication.getName()).ifPresent(account -> {
                checkout.setFullName(account.fullName());
                checkout.setEmail(account.email());
                checkout.setPhone(account.phone());
            });
        }
        model.addAttribute("checkoutForm", checkout);
        populate(model,session); return "checkout";
    }
    private void populate(Model model,HttpSession session){
        model.addAttribute("items",cart.items(session));model.addAttribute("total",cart.total(session));
    }
    @PostMapping("/dat-hang")
    public String place(@Valid @ModelAttribute("checkoutForm") CheckoutForm form,BindingResult errors,
                        HttpSession session,Model model,RedirectAttributes flash){
        if(cart.items(session).isEmpty())return "redirect:/gio-hang";
        if(errors.hasErrors()){populate(model,session);return "checkout";}
        try {
            String code=orders.place(form,cart.values(session));
            cart.clear(session);
            flash.addFlashAttribute("orderCode",code);
            return "redirect:/dat-hang/thanh-cong";
        }catch(IllegalArgumentException | ArithmeticException ex){
            model.addAttribute("checkoutError",ex.getMessage());populate(model,session);return "checkout";
        }
    }
    @GetMapping("/dat-hang/thanh-cong")
    public String success(Model model){
        if(!model.containsAttribute("orderCode"))return "redirect:/";
        return "success";
    }
}
