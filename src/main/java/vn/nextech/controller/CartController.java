package vn.nextech.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.nextech.service.CartService;

@Controller
public class CartController {
    private final CartService cart;
    public CartController(CartService cart){this.cart=cart;}
    @GetMapping("/gio-hang")
    public String show(Model m,HttpSession session){
        m.addAttribute("items",cart.items(session));m.addAttribute("total",cart.total(session));return "cart";
    }
    @PostMapping("/gio-hang/them")
    public String add(@RequestParam int id,@RequestParam(defaultValue="1") int quantity,HttpSession session,RedirectAttributes flash){
        try{cart.add(session,id,quantity);flash.addFlashAttribute("message","Đã thêm vào giỏ hàng");}
        catch(IllegalArgumentException ex){flash.addFlashAttribute("error",ex.getMessage());}
        return "redirect:/gio-hang";
    }
    @PostMapping("/gio-hang/cap-nhat")
    public String update(@RequestParam int id,@RequestParam int quantity,HttpSession session,RedirectAttributes flash){
        try{cart.change(session,id,quantity);flash.addFlashAttribute("message","Đã cập nhật giỏ hàng");}
        catch(IllegalArgumentException ex){flash.addFlashAttribute("error",ex.getMessage());}
        return "redirect:/gio-hang";
    }
    @PostMapping("/gio-hang/xoa")
    public String delete(@RequestParam int id,HttpSession session){cart.change(session,id,0);return "redirect:/gio-hang";}
}
