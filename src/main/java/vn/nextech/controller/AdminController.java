package vn.nextech.controller;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.nextech.model.ProductForm;
import vn.nextech.repository.OrderRepository;
import vn.nextech.repository.ShopRepository;
import vn.nextech.service.ImageService;
import vn.nextech.service.OrderService;
import java.io.IOException;

@Controller
@RequestMapping("/quan-tri")
public class AdminController {
    private final OrderRepository orders; private final OrderService orderService;
    private final ShopRepository products; private final ImageService images;
    public AdminController(OrderRepository orders,OrderService orderService,ShopRepository products,ImageService images){
        this.orders=orders;this.orderService=orderService;this.products=products;this.images=images;
    }
    @GetMapping({"","/"})
    public String dashboard(Model m){
        m.addAttribute("orders",orders.all());m.addAttribute("orderCount",orders.countOrders());
        m.addAttribute("revenue",orders.totalRevenue());return "admin/dashboard";
    }
    @GetMapping("/don-hang/{id}")
    public String detail(@PathVariable long id,Model m){
        m.addAttribute("order",orders.get(id));m.addAttribute("lines",orders.lines(id));return "admin/order";
    }
    @PostMapping("/don-hang/{id}/trang-thai")
    public String status(@PathVariable long id,@RequestParam String status,RedirectAttributes flash){
        try{orderService.changeStatus(id,status);flash.addFlashAttribute("message","Đã cập nhật trạng thái đơn hàng");}
        catch(IllegalArgumentException ex){flash.addFlashAttribute("error",ex.getMessage());}
        return "redirect:/quan-tri/don-hang/"+id;
    }
    @GetMapping("/san-pham")
    public String products(Model m){m.addAttribute("products",products.allAdmin());return "admin/products";}
    @GetMapping("/san-pham/them")
    public String create(Model m){m.addAttribute("productForm",new ProductForm());m.addAttribute("editId",null);return "admin/product-form";}
    @GetMapping("/san-pham/{id}/sua")
    public String edit(@PathVariable int id,Model m){
        var p=products.findById(id).orElseThrow(()->new IllegalArgumentException("Không tìm thấy sản phẩm"));
        m.addAttribute("productForm",ProductForm.from(p));m.addAttribute("editId",id);
        m.addAttribute("currentImage",p.image());return "admin/product-form";
    }
    @PostMapping("/san-pham/luu")
    public String save(@RequestParam(required=false) Integer id,ProductForm productForm,
                       @RequestParam(name="file",required=false) MultipartFile file,
                       Model model,RedirectAttributes flash){
        String warning=validate(productForm);
        if(warning!=null)return invalid(model,id,productForm,warning);
        try{
            String oldImage=id==null?"assets/img/products/default.svg":products.findById(id)
                  .orElseThrow(()->new IllegalArgumentException("Không tìm thấy sản phẩm")).image();
            String uploaded=images.save(file);
            String image=uploaded==null?oldImage:uploaded;
            if(id==null)products.create(productForm,image);else products.update(id,productForm,image);
            flash.addFlashAttribute("message","Đã lưu sản phẩm");return "redirect:/quan-tri/san-pham";
        }catch(IOException|IllegalArgumentException|DataIntegrityViolationException ex){
            return invalid(model,id,productForm,"Không thể lưu: "+ex.getMessage());
        }
    }
    private String invalid(Model m,Integer id,ProductForm f,String message){
        m.addAttribute("productForm",f);m.addAttribute("editId",id);m.addAttribute("error",message);
        if(id!=null)products.findById(id).ifPresent(p->m.addAttribute("currentImage",p.image()));
        return "admin/product-form";
    }
    private String validate(ProductForm f){
        if(f.getSku()==null||!f.getSku().matches("[A-Za-z0-9_-]{2,30}"))return "Mã sản phẩm 2–30 ký tự chữ/số";
        if(f.getName()==null||f.getName().isBlank()||f.getName().length()>150)return "Tên sản phẩm không hợp lệ";
        if(f.getCategoryId()<1||products.categories().stream().noneMatch(c->c.id()==f.getCategoryId()))return "Danh mục không hợp lệ";
        if(f.getPrice()<0||f.getPrice()>999_999_999_999L)return "Giá sản phẩm không hợp lệ";
        if(f.getStock()<0||f.getStock()>1_000_000)return "Số lượng tồn không hợp lệ";
        if(f.getDescription()!=null&&f.getDescription().length()>10000)return "Mô tả quá dài";
        return null;
    }
}
