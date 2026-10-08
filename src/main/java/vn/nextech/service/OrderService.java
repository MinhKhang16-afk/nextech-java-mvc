package vn.nextech.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.nextech.model.CartItem;
import java.util.ArrayList;
import vn.nextech.model.CheckoutForm;
import vn.nextech.model.Product;
import vn.nextech.repository.OrderRepository;
import vn.nextech.repository.ShopRepository;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class OrderService {
    private final OrderRepository orders;
    private final ShopRepository products;
    public OrderService(OrderRepository orders,ShopRepository products){this.orders=orders;this.products=products;}

    @Transactional
    public String place(CheckoutForm form, Map<Integer,Integer> cart){
        if(cart==null || cart.isEmpty())throw new IllegalArgumentException("Giỏ hàng trống");
        if(cart.size()>100)throw new IllegalArgumentException("Giỏ hàng có quá nhiều dòng");
        long total=0;
        List<CartItem> snapshot = new ArrayList<>();
        // Tất cả giá và tồn kho đều đọc tại server: không tin dữ liệu giá từ trình duyệt.
        for(Map.Entry<Integer,Integer> line:cart.entrySet()){
            int qty=line.getValue();
            if(qty<1 || qty>999)throw new IllegalArgumentException("Số lượng không hợp lệ");
            Product p=products.activeById(line.getKey()).orElseThrow(()->new IllegalArgumentException("Có sản phẩm đã ngừng bán"));
            if(p.price()<0 || p.stock()<qty)throw new IllegalArgumentException("Không đủ hàng: "+p.name());
            total=Math.addExact(total,Math.multiplyExact(p.price(),qty));
            snapshot.add(new CartItem(p,qty));
        }
        long customer=orders.insertCustomer(form);
        String code="DH"+LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMddHHmmss"))+"-"+UUID.randomUUID().toString().substring(0,8).toUpperCase();
        long order=orders.insertOrder(customer,code,form,total);
        for(CartItem line:snapshot){
            Product p=line.product();
            int qty=line.quantity();
            if(!orders.decrementStock(p.id(),qty))throw new IllegalArgumentException("Hết hàng hoặc số lượng vừa thay đổi: "+p.name());
            orders.insertLine(order,p.id(),p.name(),qty,p.price(),line.subtotal());
        }
        return code;
    }
    @Transactional
    public void changeStatus(long orderId,String requested){
        String current=orders.lockStatus(orderId);
        List<String> allowed=switch(current){
            case "CHO_XAC_NHAN" -> List.of("DA_XAC_NHAN","DA_HUY");
            case "DA_XAC_NHAN" -> List.of("DANG_GIAO","DA_HUY");
            case "DANG_GIAO" -> List.of("HOAN_TAT");
            default -> List.of();
        };
        if(!allowed.contains(requested))throw new IllegalArgumentException("Không thể chuyển từ "+current+" sang "+requested);
        if(requested.equals("DA_HUY"))orders.restock(orderId);
        orders.updateStatus(orderId,requested);
    }
}
