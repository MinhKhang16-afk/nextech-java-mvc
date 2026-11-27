package vn.nextech.controller;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.format.annotation.DateTimeFormat;
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
import vn.nextech.repository.AdminAnalyticsRepository;
import vn.nextech.repository.OrderRepository;
import vn.nextech.repository.ShopRepository;
import vn.nextech.service.ImageService;
import vn.nextech.service.OrderService;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Controller
@RequestMapping("/quan-tri")
public class AdminController {
    private static final Set<String> PERIODS = Set.of("DAY", "WEEK", "MONTH", "YEAR");

    private final OrderRepository orders;
    private final OrderService orderService;
    private final ShopRepository products;
    private final ImageService images;
    private final AdminAnalyticsRepository analytics;

    public AdminController(OrderRepository orders, OrderService orderService,
                           ShopRepository products, ImageService images,
                           AdminAnalyticsRepository analytics) {
        this.orders = orders;
        this.orderService = orderService;
        this.products = products;
        this.images = images;
        this.analytics = analytics;
    }

    @GetMapping({"", "/"})
    public String dashboard(
            @RequestParam(defaultValue = "MONTH") String period,
            @RequestParam(required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Integer categoryId,
            Model m) {

        if (!PERIODS.contains(period)) period = "MONTH";
        if (date == null) date = LocalDate.now();
        if (categoryId != null) {
            int requestedCategoryId = categoryId;
            boolean categoryExists = products.categories().stream()
                .anyMatch(c -> c.id() == requestedCategoryId);
            if (!categoryExists) categoryId = null;
        }

        LocalDate start = switch (period) {
            case "DAY" -> date;
            case "WEEK" -> date.with(DayOfWeek.MONDAY);
            case "YEAR" -> date.withDayOfYear(1);
            default -> date.withDayOfMonth(1);
        };
        LocalDate end = switch (period) {
            case "DAY" -> start.plusDays(1);
            case "WEEK" -> start.plusWeeks(1);
            case "YEAR" -> start.plusYears(1);
            default -> start.plusMonths(1);
        };

        LocalDateTime from = start.atStartOfDay();
        LocalDateTime to = end.atStartOfDay();

        m.addAttribute("period", period);
        m.addAttribute("filterDate", date);
        m.addAttribute("categoryId", categoryId);
        m.addAttribute("categories", products.categories());
        m.addAttribute("rangeStart", start);
        m.addAttribute("rangeEnd", end.minusDays(1));

        m.addAttribute("revenue", analytics.revenue(from, to, categoryId));
        m.addAttribute("orderCount", analytics.totalOrders(from, to, categoryId));
        m.addAttribute("waitingOrders", analytics.ordersByStatus(from, to, categoryId, "CHO_XAC_NHAN"));
        m.addAttribute("confirmedOrders", analytics.ordersByStatus(from, to, categoryId, "DA_XAC_NHAN"));
        m.addAttribute("shippingOrders", analytics.ordersByStatus(from, to, categoryId, "DANG_GIAO"));
        m.addAttribute("completedOrders", analytics.ordersByStatus(from, to, categoryId, "HOAN_TAT"));
        m.addAttribute("cancelledOrders", analytics.ordersByStatus(from, to, categoryId, "DA_HUY"));

        m.addAttribute("customerCount", analytics.totalCustomers());
        m.addAttribute("customerAccountCount", analytics.customerAccounts());
        m.addAttribute("newCustomers", analytics.newCustomers(
            date.withDayOfMonth(1), date.withDayOfMonth(1).plusMonths(1)
        ));
        m.addAttribute("birthdays", analytics.birthdaysInMonth(date.getMonthValue()));

        m.addAttribute("productCount", analytics.activeProducts(categoryId));
        m.addAttribute("sellingProductCount", analytics.sellingProducts(categoryId));
        m.addAttribute("lowStockCount", analytics.lowStockProducts(categoryId));
        m.addAttribute("outOfStockCount", analytics.outOfStockProducts(categoryId));

        m.addAttribute("revenueChart", analytics.revenueChart(period, from, to, categoryId));
        m.addAttribute("topProducts", analytics.bestSellingProducts(from, to, categoryId));
        m.addAttribute("topCustomers", analytics.topCustomers(from, to, categoryId));
        m.addAttribute("orders", orders.all().stream().limit(10).toList());

        return "admin/dashboard";
    }

    @GetMapping("/don-hang/{id}")
    public String detail(@PathVariable long id, Model m) {
        m.addAttribute("order", orders.get(id));
        m.addAttribute("lines", orders.lines(id));
        return "admin/order";
    }

    @PostMapping("/don-hang/{id}/trang-thai")
    public String status(@PathVariable long id, @RequestParam String status,
                         RedirectAttributes flash) {
        try {
            orderService.changeStatus(id, status);
            flash.addFlashAttribute("message", "Đã cập nhật trạng thái đơn hàng");
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/quan-tri/don-hang/" + id;
    }

    @PostMapping("/don-hang/{id}/xac-nhan-thanh-toan")
    public String confirmTransferPayment(
            @PathVariable long id,
            @RequestParam(name = "transactionCode", required = false) String transactionCode,
            RedirectAttributes flash) {
        try {
            orderService.confirmTransferPayment(id, transactionCode);
            flash.addFlashAttribute("message", "Đã xác nhận thanh toán chuyển khoản thành công");
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/quan-tri/don-hang/" + id;
    }

    @GetMapping("/san-pham")
    public String products(Model m) {
        m.addAttribute("products", products.allAdmin());
        return "admin/products";
    }

    @GetMapping("/san-pham/them")
    public String create(Model m) {
        m.addAttribute("productForm", new ProductForm());
        m.addAttribute("editId", null);
        return "admin/product-form";
    }

    @GetMapping("/san-pham/{id}/sua")
    public String edit(@PathVariable int id, Model m) {
        var p = products.findById(id).orElseThrow(
            () -> new IllegalArgumentException("Không tìm thấy sản phẩm")
        );
        m.addAttribute("productForm", ProductForm.from(p));
        m.addAttribute("editId", id);
        m.addAttribute("currentImage", p.image());
        return "admin/product-form";
    }

    @PostMapping("/san-pham/luu")
    public String save(@RequestParam(required = false) Integer id,
                       ProductForm productForm,
                       @RequestParam(name = "file", required = false) MultipartFile file,
                       Model model, RedirectAttributes flash) {
        String warning = validate(productForm);
        if (warning != null) return invalid(model, id, productForm, warning);
        try {
            String oldImage = id == null
                ? "assets/img/products/default.svg"
                : products.findById(id).orElseThrow(
                    () -> new IllegalArgumentException("Không tìm thấy sản phẩm")
                ).image();
            String uploaded = images.save(file);
            String image = uploaded == null ? oldImage : uploaded;
            if (id == null) products.create(productForm, image);
            else products.update(id, productForm, image);
            flash.addFlashAttribute("message", "Đã lưu sản phẩm");
            return "redirect:/quan-tri/san-pham";
        } catch (IOException | IllegalArgumentException | DataIntegrityViolationException ex) {
            return invalid(model, id, productForm, "Không thể lưu: " + ex.getMessage());
        }
    }

    private String invalid(Model m, Integer id, ProductForm f, String message) {
        m.addAttribute("productForm", f);
        m.addAttribute("editId", id);
        m.addAttribute("error", message);
        if (id != null) {
            products.findById(id).ifPresent(p -> m.addAttribute("currentImage", p.image()));
        }
        return "admin/product-form";
    }

    private String validate(ProductForm f) {
        if (f.getSku() == null || !f.getSku().matches("[A-Za-z0-9_-]{2,30}"))
            return "Mã sản phẩm 2–30 ký tự chữ/số";
        if (f.getName() == null || f.getName().isBlank() || f.getName().length() > 150)
            return "Tên sản phẩm không hợp lệ";
        if (f.getCategoryId() < 1 ||
                products.categories().stream().noneMatch(c -> c.id() == f.getCategoryId()))
            return "Danh mục không hợp lệ";
        if (f.getPrice() < 0 || f.getPrice() > 999_999_999_999L)
            return "Giá sản phẩm không hợp lệ";
        if (f.getStock() < 0 || f.getStock() > 1_000_000)
            return "Số lượng tồn không hợp lệ";
        if (f.getDescription() != null && f.getDescription().length() > 10000)
            return "Mô tả quá dài";
        return null;
    }
}
