package vn.nextech.controller;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import vn.nextech.repository.ShopRepository;

@Controller
public class ShopController {
    private final ShopRepository shop;
    public ShopController(ShopRepository shop){this.shop=shop;}
    @GetMapping("/")
    public String home(Model m){m.addAttribute("featured",shop.featured());return "home";}
    @GetMapping("/san-pham")
    public String products(@RequestParam(required=false,name="danh_muc") Integer category,
            @RequestParam(required=false,name="q") String q,
            @RequestParam(required=false,name="sap_xep") String sort,Model m){
        if(q!=null && q.length()>100)q=q.substring(0,100);
        m.addAttribute("products",shop.search(category,q,sort));
        m.addAttribute("selectedCategory",category);m.addAttribute("query",q==null?"":q);
        m.addAttribute("sort",sort==null?"":sort);
        return "products";
    }
    @GetMapping("/san-pham/{id}")
    public String detail(@PathVariable int id,Model m){
        m.addAttribute("product",shop.activeById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND)));
        return "detail";
    }
    @GetMapping("/dang-nhap")
    public String login(){return "login";}
}
