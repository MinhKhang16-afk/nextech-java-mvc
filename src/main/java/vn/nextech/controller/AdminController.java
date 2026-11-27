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
import vn.nextech.model.RevenueAiSnapshot;
import vn.nextech.service.AdminAiReportService;
import vn.nextech.repository.OrderRepository;
import vn.nextech.repository.ShopRepository;
import vn.nextech.service.ImageService;
import vn.nextech.service.OrderService;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.DateTimeException;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
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
    private final AdminAiReportService aiReports;

    public AdminController(OrderRepository orders, OrderService orderService,
                           ShopRepository products, ImageService images,
                           AdminAnalyticsRepository analytics,
                           AdminAiReportService aiReports) {
        this.orders = orders;
        this.orderService = orderService;
        this.products = products;
        this.images = images;
        this.analytics = analytics;
        this.aiReports = aiReports;
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
        m.addAttribute("aiEnabled", aiReports.isEnabled());

        return "admin/dashboard";
    }



    @PostMapping("/ai/bao-cao")
    public String generateAiReport(
            @RequestParam(defaultValue = "MONTH") String period,
            @RequestParam(required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Integer categoryId,
            Model model,
            RedirectAttributes flash) {

        if (!aiReports.isEnabled()) {
            flash.addFlashAttribute("error",
                "AI chưa được cấu hình. Hãy thêm OPENAI_API_KEY vào file .env rồi khởi động lại web.");
            return "redirect:/quan-tri";
        }

        if (!PERIODS.contains(period)) period = "MONTH";
        if (date == null) date = LocalDate.now();
        if (categoryId != null) {
            int requestedCategory = categoryId;
            boolean exists = products.categories().stream().anyMatch(c -> c.id() == requestedCategory);
            if (!exists) categoryId = null;
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
        LocalDate previousStart = switch (period) {
            case "DAY" -> start.minusDays(1);
            case "WEEK" -> start.minusWeeks(1);
            case "YEAR" -> start.minusYears(1);
            default -> start.minusMonths(1);
        };

        LocalDateTime from = start.atStartOfDay();
        LocalDateTime to = end.atStartOfDay();
        LocalDateTime previousFrom = previousStart.atStartOfDay();
        LocalDateTime previousTo = start.atStartOfDay();

        String categoryLabel = "Tất cả danh mục";
        if (categoryId != null) {
            for (var category : products.categories()) {
                if (category.id() == categoryId) {
                    categoryLabel = category.name();
                    break;
                }
            }
        }

        long currentRevenue = analytics.revenue(from, to, categoryId);
        long previousRevenue = analytics.revenue(previousFrom, previousTo, categoryId);
        long currentOrders = analytics.totalOrders(from, to, categoryId);
        long previousOrders = analytics.totalOrders(previousFrom, previousTo, categoryId);
        long currentCompleted = analytics.ordersByStatus(from, to, categoryId, "HOAN_TAT");
        long previousCompleted = analytics.ordersByStatus(previousFrom, previousTo, categoryId, "HOAN_TAT");

        List<Map<String, Object>> currentProducts = analytics.bestSellingProducts(from, to, categoryId)
            .stream().map(p -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("name", p.name());
                item.put("category", p.category());
                item.put("quantity", p.quantity());
                item.put("completedRevenueVnd", p.revenue());
                return item;
            }).toList();
        List<Map<String, Object>> previousProducts = analytics.bestSellingProducts(previousFrom, previousTo, categoryId)
            .stream().map(p -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("name", p.name());
                item.put("category", p.category());
                item.put("quantity", p.quantity());
                item.put("completedRevenueVnd", p.revenue());
                return item;
            }).toList();

        String periodLabel = switch (period) {
            case "DAY" -> "Ngày " + start.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            case "WEEK" -> "Tuần " + start.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                + " – " + end.minusDays(1).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            case "YEAR" -> "Năm " + start.getYear();
            default -> "Tháng " + start.format(DateTimeFormatter.ofPattern("MM/yyyy"));
        };

        RevenueAiSnapshot snapshot = new RevenueAiSnapshot(
            periodLabel, categoryLabel,
            currentRevenue, previousRevenue,
            currentOrders, previousOrders,
            currentCompleted, previousCompleted,
            analytics.ordersByStatus(from, to, categoryId, "CHO_XAC_NHAN"),
            analytics.ordersByStatus(from, to, categoryId, "DANG_GIAO"),
            analytics.ordersByStatus(from, to, categoryId, "DA_HUY"),
            analytics.activeProducts(categoryId),
            analytics.lowStockProducts(categoryId),
            analytics.outOfStockProducts(categoryId),
            currentProducts, previousProducts
        );

        try {
            model.addAttribute("report", aiReports.generateRevenueReport(snapshot));
            model.addAttribute("periodLabel", periodLabel);
            model.addAttribute("categoryLabel", categoryLabel);
            model.addAttribute("modelName", aiReports.model());
            model.addAttribute("generatedAt", ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
            return "admin/ai-report";
        } catch (IllegalStateException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
            return "redirect:/quan-tri";
        }
    }

    @GetMapping("/don-hang")
    public String orderList(
            @RequestParam(name = "q", defaultValue = "") String query,
            @RequestParam(defaultValue = "all") String status,
            @RequestParam(defaultValue = "all") String paymentMethod,
            @RequestParam(defaultValue = "all") String paymentStatus,
            @RequestParam(required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "0") int requestedPage,
            Model m) {

        String q = query.trim();
        if (q.length() > 100) q = q.substring(0, 100);
        if (!List.of("all", "CHO_XAC_NHAN", "DA_XAC_NHAN", "DANG_GIAO", "HOAN_TAT", "DA_HUY").contains(status))
            status = "all";
        if (!List.of("all", "COD", "CHUYEN_KHOAN").contains(paymentMethod))
            paymentMethod = "all";
        if (!List.of("all", "CHUA_THANH_TOAN", "CHO_KIEM_TRA", "DA_THANH_TOAN").contains(paymentStatus))
            paymentStatus = "all";

        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            LocalDate swap = fromDate;
            fromDate = toDate;
            toDate = swap;
            m.addAttribute("filterNotice", "Khoảng ngày đã được tự động sắp xếp lại.");
        }

        LocalDateTime from = fromDate == null ? null : fromDate.atStartOfDay();
        LocalDateTime to = toDate == null ? null : toDate.plusDays(1).atStartOfDay();

        long total = orders.countAdmin(q, "all".equals(status) ? null : status,
            "all".equals(paymentMethod) ? null : paymentMethod,
            "all".equals(paymentStatus) ? null : paymentStatus, from, to);
        int pageSize = 20;
        int totalPages = Math.max(1, (int) Math.ceil(total / (double) pageSize));
        int page = Math.min(Math.max(0, requestedPage), totalPages - 1);

        m.addAttribute("q", q);
        m.addAttribute("status", status);
        m.addAttribute("paymentMethod", paymentMethod);
        m.addAttribute("paymentStatus", paymentStatus);
        m.addAttribute("fromDate", fromDate);
        m.addAttribute("toDate", toDate);
        m.addAttribute("orders", orders.searchAdmin(q,
            "all".equals(status) ? null : status,
            "all".equals(paymentMethod) ? null : paymentMethod,
            "all".equals(paymentStatus) ? null : paymentStatus,
            from, to, pageSize, page * pageSize));
        m.addAttribute("allOrderCount", orders.countOrders());
        m.addAttribute("resultCount", total);
        m.addAttribute("currentPage", page);
        m.addAttribute("totalPages", totalPages);
        m.addAttribute("startNumber", total == 0 ? 0 : page * pageSize + 1);
        m.addAttribute("endNumber", Math.min((page + 1) * pageSize, total));
        return "admin/orders";
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
