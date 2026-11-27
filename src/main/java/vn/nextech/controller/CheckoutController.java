package vn.nextech.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import org.springframework.validation.BindingResult;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import vn.nextech.model.CheckoutForm;
import vn.nextech.model.OrderReceipt;

import vn.nextech.service.CartService;
import vn.nextech.service.CustomerAccountService;
import vn.nextech.service.OrderService;
import vn.nextech.service.VietQrService;

@Controller
public class CheckoutController {

    private final CartService cart;
    private final OrderService orders;
    private final CustomerAccountService customers;
    private final VietQrService vietQr;

    public CheckoutController(
            CartService cart,
            OrderService orders,
            CustomerAccountService customers,
            VietQrService vietQr) {

        this.cart = cart;
        this.orders = orders;
        this.customers = customers;
        this.vietQr = vietQr;
    }

    @GetMapping("/dat-hang")
    public String form(
            HttpSession session,
            Model model,
            Authentication authentication) {

        if (cart.items(session).isEmpty()) {
            return "redirect:/gio-hang";
        }

        CheckoutForm checkout = new CheckoutForm();

        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getAuthorities().stream()
                    .anyMatch(a ->
                        "ROLE_CUSTOMER".equals(a.getAuthority()))) {

            customers.findByEmail(authentication.getName())
                .ifPresent(account -> {
                    checkout.setFullName(account.fullName());
                    checkout.setEmail(account.email());
                    checkout.setPhone(account.phone());
                });
        }

        model.addAttribute("checkoutForm", checkout);
        populate(model, session);

        return "checkout";
    }

    private void populate(Model model, HttpSession session) {
        model.addAttribute("items", cart.items(session));
        model.addAttribute("total", cart.total(session));
    }

    @PostMapping("/dat-hang")
    public String place(
            @Valid @ModelAttribute("checkoutForm") CheckoutForm form,
            BindingResult errors,
            HttpSession session,
            Model model,
            RedirectAttributes flash,
            Authentication authentication) {

        if (cart.items(session).isEmpty()) {
            return "redirect:/gio-hang";
        }

        if (errors.hasErrors()) {
            populate(model, session);
            return "checkout";
        }

        String method = form.getPaymentMethod();

        if (!"COD".equals(method)
                && !"CHUYEN_KHOAN".equals(method)) {

            model.addAttribute(
                "checkoutError",
                "Phương thức thanh toán không hợp lệ"
            );

            populate(model, session);
            return "checkout";
        }

        if ("CHUYEN_KHOAN".equals(method)
                && !vietQr.isConfigured()) {

            model.addAttribute(
                "checkoutError",
                "Thông tin Vietcombank chưa được cấu hình đầy đủ"
            );

            populate(model, session);
            return "checkout";
        }

        try {

            Long accountId = null;
            if (authentication != null && authentication.isAuthenticated()
                    && authentication.getAuthorities().stream()
                        .anyMatch(a -> "ROLE_CUSTOMER".equals(a.getAuthority()))) {
                accountId = customers.findByEmail(authentication.getName())
                    .map(vn.nextech.model.CustomerAccount::id).orElse(null);
            }
            OrderReceipt receipt = orders.place(
                form,
                cart.values(session),
                accountId
            );

            cart.clear(session);

            flash.addFlashAttribute(
                "orderCode",
                receipt.code()
            );

            flash.addFlashAttribute(
                "orderTotal",
                receipt.total()
            );

            flash.addFlashAttribute(
                "paymentMethod",
                receipt.paymentMethod()
            );

            if ("CHUYEN_KHOAN".equals(receipt.paymentMethod())) {

                flash.addFlashAttribute(
                    "qrUrl",
                    vietQr.createQrUrl(
                        receipt.total(),
                        receipt.code()
                    )
                );

                flash.addFlashAttribute(
                    "bankAccount",
                    vietQr.getBankAccount()
                );

                flash.addFlashAttribute(
                    "bankAccountName",
                    vietQr.getBankAccountName()
                );
            }

            return "redirect:/dat-hang/thanh-cong";

        } catch (IllegalArgumentException | ArithmeticException ex) {

            model.addAttribute(
                "checkoutError",
                ex.getMessage()
            );

            populate(model, session);

            return "checkout";
        }
    }

    @GetMapping("/dat-hang/thanh-cong")
    public String success(Model model) {

        if (!model.containsAttribute("orderCode")) {
            return "redirect:/";
        }

        return "success";
    }
}