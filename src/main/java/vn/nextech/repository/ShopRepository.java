package vn.nextech.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import vn.nextech.model.Category;
import vn.nextech.model.Product;
import vn.nextech.model.ProductForm;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;

@Repository
public class ShopRepository {
    private final JdbcTemplate jdbc;
    public ShopRepository(JdbcTemplate jdbc){this.jdbc=jdbc;}
    private static final RowMapper<Product> PRODUCT=(rs,n)-> new Product(
        rs.getInt("san_pham_id"), rs.getString("ma_san_pham"), rs.getString("ten_san_pham"),
        rs.getInt("danh_muc_id"), rs.getString("ten_danh_muc"), rs.getString("mo_ta"),
        rs.getString("thong_so"), rs.getString("hinh_anh"), rs.getLong("gia_ban"),
        rs.getInt("so_luong_ton"), rs.getBoolean("noi_bat"), rs.getBoolean("trang_thai"));
    private static final String COLUMNS = "SELECT p.*, d.ten_danh_muc FROM san_pham p JOIN danh_muc d ON d.danh_muc_id=p.danh_muc_id ";
    public List<Category> categories(){
        return jdbc.query("SELECT * FROM danh_muc WHERE trang_thai=1 ORDER BY thu_tu",(rs,n)->
            new Category(rs.getInt("danh_muc_id"),rs.getString("ma_danh_muc"),
                rs.getString("ten_danh_muc"),rs.getString("mo_ta")));
    }
    public Optional<Product> findById(int id){
        return jdbc.query(COLUMNS+"WHERE p.san_pham_id=?",PRODUCT,id).stream().findFirst();
    }
    public Optional<Product> activeById(int id){
        return jdbc.query(COLUMNS+"WHERE p.san_pham_id=? AND p.trang_thai=1 AND d.trang_thai=1",PRODUCT,id).stream().findFirst();
    }
    public List<Product> featured(){
        return jdbc.query(COLUMNS+"WHERE p.trang_thai=1 AND d.trang_thai=1 AND p.noi_bat=1 ORDER BY p.san_pham_id LIMIT 8",PRODUCT);
    }
    public List<Product> search(Integer categoryId, String term, String sort){
        StringBuilder query=new StringBuilder(COLUMNS+"WHERE p.trang_thai=1 AND d.trang_thai=1");
        List<Object> args=new ArrayList<>();
        if(categoryId!=null && categoryId>0){query.append(" AND p.danh_muc_id=?"); args.add(categoryId);}
        if(term!=null && !term.isBlank()){
            query.append(" AND (p.ten_san_pham LIKE ? OR p.mo_ta LIKE ?)");
            String like="%"+term.trim().replace("%","\\%").replace("_","\\_")+"%";
            args.add(like); args.add(like);
        }
        String order=switch(sort==null?"":sort){
            case "gia-tang" -> "p.gia_ban ASC";
            case "gia-giam" -> "p.gia_ban DESC";
            case "ten" -> "p.ten_san_pham ASC";
            default -> "p.noi_bat DESC, p.san_pham_id DESC";
        };
        query.append(" ORDER BY ").append(order);
        return jdbc.query(query.toString(),PRODUCT,args.toArray());
    }
    public List<Product> allAdmin(){
        return jdbc.query(COLUMNS+"ORDER BY p.san_pham_id DESC",PRODUCT);
    }
    public void create(ProductForm f,String image){
        jdbc.update("INSERT INTO san_pham(ma_san_pham,ten_san_pham,danh_muc_id,mo_ta,thong_so,hinh_anh,gia_ban,so_luong_ton,noi_bat,trang_thai) VALUES(?,?,?,?,?,?,?,?,?,?)",
            f.getSku(),f.getName(),f.getCategoryId(),f.getDescription(),f.getSpecifications(),image,
            f.getPrice(),f.getStock(),f.isFeatured(),f.isActive());
    }
    public void update(int id, ProductForm f,String image){
        jdbc.update("UPDATE san_pham SET ma_san_pham=?,ten_san_pham=?,danh_muc_id=?,mo_ta=?,thong_so=?,hinh_anh=?,gia_ban=?,so_luong_ton=?,noi_bat=?,trang_thai=? WHERE san_pham_id=?",
            f.getSku(),f.getName(),f.getCategoryId(),f.getDescription(),f.getSpecifications(),image,
            f.getPrice(),f.getStock(),f.isFeatured(),f.isActive(),id);
    }
}
