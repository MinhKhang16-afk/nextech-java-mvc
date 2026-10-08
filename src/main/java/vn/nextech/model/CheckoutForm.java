package vn.nextech.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CheckoutForm {
    @NotBlank(message="Hãy nhập họ tên") @Size(max=120, message="Họ tên tối đa 120 ký tự")
    private String fullName;
    @NotBlank(message="Hãy nhập số điện thoại")
    @Pattern(regexp="[0-9+ .()\\-]{9,20}", message="Số điện thoại không hợp lệ")
    private String phone;
    @Email(message="Email không hợp lệ") @Size(max=100, message="Email tối đa 100 ký tự")
    private String email;
    @NotBlank(message="Hãy nhập địa chỉ giao hàng") @Size(max=255, message="Địa chỉ quá dài")
    private String address;
    @Size(max=500, message="Ghi chú tối đa 500 ký tự")
    private String note;
    public String getFullName(){return fullName;} public void setFullName(String v){fullName=v;}
    public String getPhone(){return phone;} public void setPhone(String v){phone=v;}
    public String getEmail(){return email;} public void setEmail(String v){email=v;}
    public String getAddress(){return address;} public void setAddress(String v){address=v;}
    public String getNote(){return note;} public void setNote(String v){note=v;}
}
