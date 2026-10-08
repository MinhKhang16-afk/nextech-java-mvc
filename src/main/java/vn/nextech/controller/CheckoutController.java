package vn.nextech.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.nextech.model.CheckoutForm;
import vn.nextech.service.CartService;
import vn.nextech.service.OrderService;

@Controller
public class CheckoutController {
    private final CartService cart; private final OrderService orders;
    public CheckoutController(CartService cart,OrderService orders){this.cart=cart;this.orders=orders;}
    @GetMapping("/dat-hang")
    public String form(HttpSession session,Model model){
        if(cart.items(session).isEmpty())return "redirect:/gio-hang";
        model.addAttribute("checkoutForm",new CheckoutForm());
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
