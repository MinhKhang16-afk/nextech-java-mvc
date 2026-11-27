package vn.nextech.controller;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.nextech.model.CategoryForm;
import vn.nextech.repository.AdminCategoryRepository;

@Controller
@RequestMapping("/quan-tri/danh-muc")
public class AdminCategoryController {
    private final AdminCategoryRepository categories;

    public AdminCategoryController(AdminCategoryRepository categories) {
        this.categories = categories;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("categories", categories.findAll());
        return "admin/categories";
    }

    @GetMapping("/them")
    public String createForm(Model model) {
        CategoryForm form = new CategoryForm();
        form.setActive(true);
        model.addAttribute("categoryForm", form);
        model.addAttribute("editId", null);
        return "admin/category-form";
    }

    @GetMapping("/{id}/sua")
    public String editForm(@PathVariable int id, Model model) {
        var category = categories.findById(id).orElseThrow(
            () -> new IllegalArgumentException("Không tìm thấy danh mục")
        );
        model.addAttribute("categoryForm", CategoryForm.from(category));
        model.addAttribute("editId", id);
        model.addAttribute("productCount", category.productCount());
        return "admin/category-form";
    }

    @PostMapping("/luu")
    public String save(@RequestParam(required = false) Integer id,
                       CategoryForm form, Model model, RedirectAttributes flash) {
        String error = validate(form, id == null ? 0 : id);
        if (error == null && id != null && !form.isActive()
                && categories.countProducts(id) > 0) {
            error = "Danh mục đang được sản phẩm sử dụng. Hãy chuyển sản phẩm sang danh mục khác trước khi ẩn.";
        }
        if (error != null) {
            model.addAttribute("categoryForm", form);
            model.addAttribute("editId", id);
            if (id != null) model.addAttribute("productCount", categories.countProducts(id));
            model.addAttribute("error", error);
            return "admin/category-form";
        }

        try {
            if (id == null) {
                categories.create(form);
            } else if (categories.update(id, form) != 1) {
                throw new IllegalArgumentException("Không tìm thấy danh mục cần cập nhật");
            }
            flash.addFlashAttribute("message", "Đã lưu danh mục thành công.");
            return "redirect:/quan-tri/danh-muc";
        } catch (DataIntegrityViolationException ex) {
            model.addAttribute("categoryForm", form);
            model.addAttribute("editId", id);
            model.addAttribute("error", "Mã danh mục đã tồn tại hoặc dữ liệu không hợp lệ.");
            if (id != null) model.addAttribute("productCount", categories.countProducts(id));
            return "admin/category-form";
        }
    }

    @PostMapping("/{id}/trang-thai")
    public String changeStatus(@PathVariable int id, @RequestParam boolean active,
                               RedirectAttributes flash) {
        if (id <= 0 || categories.findById(id).isEmpty()) {
            flash.addFlashAttribute("error", "Không tìm thấy danh mục.");
            return "redirect:/quan-tri/danh-muc";
        }
        long productCount = categories.countProducts(id);
        if (!active && productCount > 0) {
            flash.addFlashAttribute("error",
                "Không thể ẩn danh mục đang có " + productCount +
                " sản phẩm. Hãy chuyển các sản phẩm sang danh mục khác trước.");
            return "redirect:/quan-tri/danh-muc";
        }
        categories.setActive(id, active);
        flash.addFlashAttribute("message",
            active ? "Đã kích hoạt danh mục." : "Đã ẩn danh mục. Dữ liệu vẫn được giữ lại.");
        return "redirect:/quan-tri/danh-muc";
    }

    private String validate(CategoryForm form, int exceptId) {
        if (form.getCode() == null ||
                !form.getCode().trim().matches("[A-Za-z0-9_-]{2,30}")) {
            return "Mã danh mục cần có 2–30 ký tự chữ, số, dấu gạch ngang hoặc gạch dưới.";
        }
        if (form.getName() == null || form.getName().isBlank()
                || form.getName().trim().length() > 100) {
            return "Tên danh mục không được để trống và tối đa 100 ký tự.";
        }
        if (form.getDescription() != null && form.getDescription().length() > 255) {
            return "Mô tả danh mục tối đa 255 ký tự.";
        }
        if (form.getSortOrder() < 0 || form.getSortOrder() > 100000) {
            return "Thứ tự hiển thị phải nằm trong khoảng 0–100000.";
        }
        String normalizedCode = form.getCode().trim().toUpperCase(java.util.Locale.ROOT);
        if (categories.codeExists(normalizedCode, exceptId)) {
            return "Mã danh mục đã tồn tại. Hãy chọn mã khác.";
        }
        return null;
    }
}
