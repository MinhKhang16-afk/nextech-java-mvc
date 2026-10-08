package vn.nextech.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import vn.nextech.model.CheckoutForm;
import vn.nextech.model.OrderLine;
import vn.nextech.model.ShopOrder;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Objects;

@Repository
public class OrderRepository {
    private final JdbcTemplate jdbc;
    public OrderRepository(JdbcTemplate jdbc){this.jdbc=jdbc;}
    private static final RowMapper<ShopOrder> ORDER=(rs,n)->new ShopOrder(
        rs.getLong("don_hang_id"),rs.getString("ma_don_hang"),rs.getString("ten_nguoi_nhan"),
        rs.getString("sdt_nguoi_nhan"),rs.getString("dia_chi_giao_hang"),rs.getString("ghi_chu"),
        rs.getLong("tong_tien"),rs.getString("trang_thai"),rs.getTimestamp("thoi_gian_dat").toLocalDateTime());
    private long generated(String sql, Object... values){
        KeyHolder holder=new GeneratedKeyHolder();
        jdbc.update(conn->{
            PreparedStatement ps=conn.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS);
            for(int i=0;i<values.length;i++)ps.setObject(i+1,values[i]);
            return ps;
        },holder);
        return Objects.requireNonNull(holder.getKey()).longValue();
    }
    public long insertCustomer(CheckoutForm f){
        return generated("INSERT INTO khach_hang(ho_ten,so_dien_thoai,email,dia_chi) VALUES(?,?,?,?)",
            f.getFullName().trim(),f.getPhone().trim(),f.getEmail(),f.getAddress().trim());
    }
    public long insertOrder(long customerId,String code, CheckoutForm f, long total){
        return generated("INSERT INTO don_hang(ma_don_hang,khach_hang_id,ten_nguoi_nhan,sdt_nguoi_nhan,dia_chi_giao_hang,ghi_chu,tam_tinh,phi_giao_hang,tong_tien,phuong_thuc_thanh_toan,trang_thai) VALUES(?,?,?,?,?,?,?,?,?,'COD','CHO_XAC_NHAN')",
            code,customerId,f.getFullName().trim(),f.getPhone().trim(),f.getAddress().trim(),f.getNote(),total,0,total);
    }
    public void insertLine(long orderId, int productId, String name, int qty, long price, long subtotal){
        jdbc.update("INSERT INTO chi_tiet_don_hang(don_hang_id,san_pham_id,ten_san_pham,so_luong,don_gia,thanh_tien) VALUES(?,?,?,?,?,?)",
            orderId,productId,name,qty,price,subtotal);
    }
    public boolean decrementStock(int productId, int amount){
        return jdbc.update("UPDATE san_pham SET so_luong_ton=so_luong_ton-? WHERE san_pham_id=? AND so_luong_ton>=? AND trang_thai=1",amount,productId,amount)==1;
    }
    public List<ShopOrder> all(){
        return jdbc.query("SELECT * FROM don_hang ORDER BY don_hang_id DESC LIMIT 200",ORDER);
    }
    public ShopOrder get(long id){
        return jdbc.query("SELECT * FROM don_hang WHERE don_hang_id=?",ORDER,id).stream()
            .findFirst().orElseThrow(()->new IllegalArgumentException("Không tìm thấy đơn hàng"));
    }
    public String lockStatus(long id){
        return jdbc.query("SELECT trang_thai FROM don_hang WHERE don_hang_id=? FOR UPDATE",(rs,n)->rs.getString(1),id).stream()
            .findFirst().orElseThrow(()->new IllegalArgumentException("Không tìm thấy đơn hàng"));
    }
    public List<OrderLine> lines(long orderId){
        return jdbc.query("SELECT * FROM chi_tiet_don_hang WHERE don_hang_id=?",(rs,n)->new OrderLine(
            rs.getInt("san_pham_id"),rs.getString("ten_san_pham"),rs.getInt("so_luong"),
            rs.getLong("don_gia"),rs.getLong("thanh_tien")),orderId);
    }
    public void restock(long orderId){
        jdbc.update("UPDATE san_pham p JOIN chi_tiet_don_hang ct ON ct.san_pham_id=p.san_pham_id SET p.so_luong_ton=p.so_luong_ton+ct.so_luong WHERE ct.don_hang_id=?",orderId);
    }
    public void updateStatus(long id,String status){
        jdbc.update("UPDATE don_hang SET trang_thai=? WHERE don_hang_id=?",status,id);
    }
    public long countOrders(){return jdbc.queryForObject("SELECT COUNT(*) FROM don_hang",Long.class);}
    public long totalRevenue(){return jdbc.queryForObject("SELECT COALESCE(SUM(tong_tien),0) FROM don_hang WHERE trang_thai='HOAN_TAT'",Long.class);}
}
