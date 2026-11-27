package vn.nextech.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.nextech.model.AdminCustomerForm;
import vn.nextech.repository.AdminCustomerRepository;

import java.time.LocalDate;

@Controller
@RequestMapping("/quan-tri/tai-khoan")
public class AdminCustomerController {
    private static final int PAGE_SIZE = 20;
    private final AdminCustomerRepository customers;

    public AdminCustomerController(AdminCustomerRepository customers) {
        this.customers = customers;
    }

    @GetMapping
    public String list(
            @RequestParam(name = "q", defaultValue = "") String query,
            @RequestParam(name = "status", defaultValue = "all") String status,
            @RequestParam(name = "page", defaultValue = "0") int requestedPage,
            Model model) {

        String q = query.trim();
        if (q.length() > 100) q = q.substring(0, 100);
        if (!"active".equals(status) && !"locked".equals(status)) status = "all";
        Boolean active = "active".equals(status) ? Boolean.TRUE
            : "locked".equals(status) ? Boolean.FALSE : null;

        long total = customers.countSearch(q, active);
        int totalPages = Math.max(1, (int) Math.ceil(total / (double) PAGE_SIZE));
        int page = Math.min(Math.max(0, requestedPage), totalPages - 1);

        model.addAttribute("q", q);
        model.addAttribute("status", status);
        model.addAttribute("accounts", customers.search(q, active, PAGE_SIZE, page * PAGE_SIZE));
        model.addAttribute("totalAccounts", customers.countAll());
        model.addAttribute("activeAccounts", customers.countActive());
        model.addAttribute("resultCount", total);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("pageSize", PAGE_SIZE);
        model.addAttribute("startNumber", total == 0 ? 0 : page * PAGE_SIZE + 1);
        model.addAttribute("endNumber", Math.min((page + 1) * PAGE_SIZE, total));
        return "admin/customers";
    }

    @GetMapping("/{id}/sua")
    public String edit(@PathVariable long id, Model model) {
        var customer = customers.findById(id).orElseThrow(
            () -> new IllegalArgumentException("Không tìm thấy tài khoản khách hàng.")
        );
        AdminCustomerForm form = new AdminCustomerForm();
        form.setFullName(customer.fullName());
        form.setPhone(customer.phone());
        form.setBirthDate(customer.birthDate());
        model.addAttribute("customer", customer);
        model.addAttribute("customerForm", form);
        return "admin/customer-form";
    }

    @PostMapping("/{id}/sua")
    public String update(
            @PathVariable long id,
            AdminCustomerForm form,
            Model model,
            RedirectAttributes flash) {

        var customer = customers.findById(id).orElseThrow(
            () -> new IllegalArgumentException("Không tìm thấy tài khoản khách hàng.")
        );
        String error = validate(form);
        if (error != null) {
            model.addAttribute("customer", customer);
            model.addAttribute("customerForm", form);
            model.addAttribute("error", error);
            return "admin/customer-form";
        }

        int updated = customers.updateProfile(
            id,
            form.getFullName().trim(),
            form.getPhone() == null ? "" : form.getPhone().trim(),
            form.getBirthDate()
        );
        if (updated != 1) {
            flash.addFlashAttribute("error", "Không thể cập nhật thông tin khách hàng.");
        } else {
            flash.addFlashAttribute("message", "Đã cập nhật thông tin khách hàng.");
        }
        return "redirect:/quan-tri/tai-khoan";
    }

    @PostMapping("/{id}/trang-thai")
    public String changeStatus(@PathVariable long id,
                               @RequestParam("active") boolean active,
                               RedirectAttributes flash) {
        if (id <= 0 || !customers.setActive(id, active)) {
            flash.addFlashAttribute("error", "Không tìm thấy tài khoản khách hàng.");
        } else {
            flash.addFlashAttribute(
                "message",
                active ? "Đã mở khóa tài khoản." : "Đã khóa tài khoản."
            );
        }
        return "redirect:/quan-tri/tai-khoan";
    }

    private String validate(AdminCustomerForm form) {
        if (form.getFullName() == null || form.getFullName().isBlank()
                || form.getFullName().trim().length() > 120) {
            return "Họ tên không được để trống và tối đa 120 ký tự.";
        }
        String phone = form.getPhone() == null ? "" : form.getPhone().trim();
        if (!phone.isEmpty() && !phone.matches("[0-9+ .()\\-]{9,20}")) {
            return "Số điện thoại không hợp lệ (9–20 ký tự).";
        }
        if (form.getBirthDate() != null && form.getBirthDate().isAfter(LocalDate.now())) {
            return "Ngày sinh không được nằm trong tương lai.";
        }
        return null;
    }
}
