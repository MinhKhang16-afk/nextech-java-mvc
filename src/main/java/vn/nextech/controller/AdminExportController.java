package vn.nextech.controller;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import vn.nextech.model.AdminCustomerRow;
import vn.nextech.model.Product;
import vn.nextech.model.ShopOrder;
import vn.nextech.repository.AdminAnalyticsRepository;
import vn.nextech.repository.AdminCustomerRepository;
import vn.nextech.repository.OrderRepository;
import vn.nextech.repository.ShopRepository;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

@Controller
@RequestMapping("/quan-tri/xuat-excel")
public class AdminExportController {
    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private final OrderRepository orders;
    private final AdminCustomerRepository customers;
    private final ShopRepository products;
    private final AdminAnalyticsRepository analytics;

    public AdminExportController(OrderRepository orders, AdminCustomerRepository customers,
                                 ShopRepository products, AdminAnalyticsRepository analytics) {
        this.orders = orders;
        this.customers = customers;
        this.products = products;
        this.analytics = analytics;
    }

    @GetMapping("/don-hang")
    public ResponseEntity<byte[]> exportOrders(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "all") String status,
            @RequestParam(defaultValue = "all") String paymentMethod,
            @RequestParam(defaultValue = "all") String paymentStatus,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        if (q.length() > 100) q = q.substring(0, 100);
        if (!List.of("all","CHO_XAC_NHAN","DA_XAC_NHAN","DANG_GIAO","HOAN_TAT","DA_HUY").contains(status)) status = "all";
        if (!List.of("all","COD","CHUYEN_KHOAN").contains(paymentMethod)) paymentMethod = "all";
        if (!List.of("all","CHUA_THANH_TOAN","CHO_KIEM_TRA","DA_THANH_TOAN").contains(paymentStatus)) paymentStatus = "all";
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            LocalDate swap = fromDate; fromDate = toDate; toDate = swap;
        }
        LocalDateTime from = fromDate == null ? null : fromDate.atStartOfDay();
        LocalDateTime to = toDate == null ? null : toDate.plusDays(1).atStartOfDay();
        List<ShopOrder> rows = orders.searchAdmin(q,
            "all".equals(status) ? null : status,
            "all".equals(paymentMethod) ? null : paymentMethod,
            "all".equals(paymentStatus) ? null : paymentStatus,
            from, to, 50000, 0);
        return workbook("nextech-don-hang.xlsx", wb -> {
            Sheet sheet = wb.createSheet("Don hang");
            header(wb, sheet, "Mã đơn","Ngày đặt","Người nhận","Điện thoại","Địa chỉ","Tổng tiền (VND)","Thanh toán","TT thanh toán","Trạng thái");
            int i=1;
            for (ShopOrder o : rows) {
                Row r=sheet.createRow(i++);
                text(r,0,o.code()); text(r,1,o.createdAt().format(DATE_TIME));
                text(r,2,o.customerName()); text(r,3,o.phone()); text(r,4,o.address());
                number(r,5,o.total()); text(r,6,o.paymentMethod()); text(r,7,o.paymentStatus()); text(r,8,o.status());
            }
            widths(sheet,9);
        });
    }

    @GetMapping("/khach-hang")
    public ResponseEntity<byte[]> exportCustomers(@RequestParam(defaultValue = "") String q,
                                                    @RequestParam(defaultValue = "all") String status) {
        if (q.length() > 100) q = q.substring(0,100);
        Boolean active = "active".equals(status) ? Boolean.TRUE :
            "locked".equals(status) ? Boolean.FALSE : null;
        List<AdminCustomerRow> rows = customers.search(q, active, 50000, 0);
        return workbook("nextech-khach-hang.xlsx", wb -> {
            Sheet sheet=wb.createSheet("Khach hang");
            header(wb,sheet,"ID","Họ tên","Email","Điện thoại","Ngày sinh","Ngày đăng ký","Trạng thái");
            int i=1;
            for(AdminCustomerRow a:rows) {
                Row r=sheet.createRow(i++);
                number(r,0,a.id()); text(r,1,a.fullName()); text(r,2,a.email()); text(r,3,a.phone());
                text(r,4,a.birthDate()==null?"":a.birthDate().toString());
                text(r,5,a.createdAt().format(DATE_TIME)); text(r,6,a.active()?"Hoạt động":"Đã khóa");
            }
            widths(sheet,7);
        });
    }

    @GetMapping("/san-pham")
    public ResponseEntity<byte[]> exportProducts() {
        List<Product> rows=products.allAdmin();
        return workbook("nextech-san-pham-ton-kho.xlsx", wb -> {
            Sheet sheet=wb.createSheet("San pham");
            header(wb,sheet,"ID","Mã sản phẩm","Tên sản phẩm","Danh mục","Giá (VND)","Tồn kho","Nổi bật","Kinh doanh");
            int i=1;
            for(Product p:rows) {
                Row r=sheet.createRow(i++);
                number(r,0,p.id()); text(r,1,p.sku()); text(r,2,p.name()); text(r,3,p.categoryName());
                number(r,4,p.price()); number(r,5,p.stock()); text(r,6,p.featured()?"Có":"Không");
                text(r,7,p.active()?"Đang bán":"Đã ẩn");
            }
            widths(sheet,8);
        });
    }

    @GetMapping("/doanh-thu")
    public ResponseEntity<byte[]> exportRevenue(
            @RequestParam(defaultValue = "MONTH") String period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Integer categoryId) {
        if (!List.of("DAY","WEEK","MONTH","YEAR").contains(period)) period="MONTH";
        if (date==null) date=LocalDate.now();
        LocalDate start=switch(period) {
            case "DAY" -> date;
            case "WEEK" -> date.with(DayOfWeek.MONDAY);
            case "YEAR" -> date.withDayOfYear(1);
            default -> date.withDayOfMonth(1);
        };
        LocalDate end=switch(period) {
            case "DAY" -> start.plusDays(1);
            case "WEEK" -> start.plusWeeks(1);
            case "YEAR" -> start.plusYears(1);
            default -> start.plusMonths(1);
        };
        LocalDateTime from=start.atStartOfDay(), to=end.atStartOfDay();
        final Integer category=(categoryId!=null && categoryId>0)?categoryId:null;
        final LocalDate selectedDate=date;
        final String selectedPeriod=period;
        return workbook("nextech-doanh-thu.xlsx", wb -> {
            Sheet summary=wb.createSheet("Tong quan");
            header(wb,summary,"Chỉ tiêu","Giá trị");
            Row a=summary.createRow(1); text(a,0,"Kỳ"); text(a,1,periodLabel(selectedPeriod,selectedDate));
            Row b=summary.createRow(2); text(b,0,"Doanh thu đơn hoàn tất (VND)"); number(b,1,analytics.revenue(from,to,category));
            Row d=summary.createRow(3); text(d,0,"Tổng đơn hàng"); number(d,1,analytics.totalOrders(from,to,category));
            Row e=summary.createRow(4); text(e,0,"Đơn đang giao"); number(e,1,analytics.ordersByStatus(from,to,category,"DANG_GIAO"));
            Row f=summary.createRow(5); text(f,0,"Đơn hoàn tất"); number(f,1,analytics.ordersByStatus(from,to,category,"HOAN_TAT"));
            Row g=summary.createRow(6); text(g,0,"Đơn đã hủy"); number(g,1,analytics.ordersByStatus(from,to,category,"DA_HUY"));
            widths(summary,2);
            Sheet productSheet=wb.createSheet("San pham ban chay");
            header(wb,productSheet,"Sản phẩm","Danh mục","Số lượng bán","Doanh thu hoàn tất (VND)");
            int i=1;
            for(var p:analytics.bestSellingProducts(from,to,category)) {
                Row r=productSheet.createRow(i++);
                text(r,0,p.name()); text(r,1,p.category()); number(r,2,p.quantity()); number(r,3,p.revenue());
            }
            widths(productSheet,4);
        });
    }

    private static String periodLabel(String period, LocalDate date) {
        return switch(period) {
            case "DAY" -> "Ngày " + date;
            case "WEEK" -> "Tuần chứa ngày " + date;
            case "YEAR" -> "Năm " + date.getYear();
            default -> "Tháng " + date.getMonthValue() + "/" + date.getYear();
        };
    }

    private ResponseEntity<byte[]> workbook(String filename, Consumer<Workbook> writer) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            writer.accept(workbook);
            workbook.write(out);
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(XLSX))
                .body(out.toByteArray());
        } catch (IOException ex) {
            throw new IllegalStateException("Không thể tạo tệp Excel.", ex);
        }
    }

    private static void header(Workbook wb, Sheet sheet, String... labels) {
        Row row=sheet.createRow(0);
        CellStyle style=wb.createCellStyle();
        Font font=wb.createFont(); font.setBold(true); style.setFont(font);
        for(int i=0;i<labels.length;i++) {
            Cell cell=row.createCell(i); cell.setCellValue(labels[i]); cell.setCellStyle(style);
        }
        sheet.createFreezePane(0,1);
        sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0,0,0,labels.length-1));
    }
    private static void text(Row row,int column,String value) {
        row.createCell(column).setCellValue(value==null?"":value);
    }
    private static void number(Row row,int column,long value) {
        row.createCell(column).setCellValue(value);
    }
    private static void widths(Sheet sheet,int columns) {
        for(int i=0;i<columns;i++) sheet.autoSizeColumn(i);
    }
}
