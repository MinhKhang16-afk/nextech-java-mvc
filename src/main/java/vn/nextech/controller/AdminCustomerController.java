package vn.nextech.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.nextech.repository.AdminCustomerRepository;

@Controller
@RequestMapping("/quan-tri/tai-khoan")
public class AdminCustomerController {
    private final AdminCustomerRepository customers;

    public AdminCustomerController(AdminCustomerRepository customers) {
        this.customers = customers;
    }

    @GetMapping
    public String list(@RequestParam(name = "q", defaultValue = "") String query, Model model) {
        String q = query.trim();
        if (q.length() > 100) q = q.substring(0, 100);
        model.addAttribute("q", q);
        model.addAttribute("accounts", customers.search(q));
        model.addAttribute("totalAccounts", customers.countAll());
        model.addAttribute("activeAccounts", customers.countActive());
        return "admin/customers";
    }

    @PostMapping("/{id}/trang-thai")
    public String changeStatus(@PathVariable long id,
                               @RequestParam("active") boolean active,
                               RedirectAttributes flash) {
        if (id <= 0 || !customers.setActive(id, active)) {
            flash.addFlashAttribute("error", "Không tìm thấy tài khoản khách hàng.");
        } else {
            flash.addFlashAttribute("message", active ? "Đã mở khóa tài khoản." : "Đã khóa tài khoản.");
        }
        return "redirect:/quan-tri/tai-khoan";
    }
}
