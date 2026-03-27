package com.haui.tech_shop.exception;

import com.haui.tech_shop.services.Impl.ErrorNotificationService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import java.util.Set;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final ErrorNotificationService errorNotificationService;

    public GlobalExceptionHandler(ErrorNotificationService errorNotificationService) {
        this.errorNotificationService = errorNotificationService;
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public String handleUsernameNotFoundException(UsernameNotFoundException ex,                                        RedirectAttributes redirect) {
        redirect.addFlashAttribute("error", ex.getMessage());
        return "redirect:/login?usernameNotFound=true";
    }

    @ExceptionHandler(DisabledException.class)
    public String handleDisabledException(DisabledException ex, RedirectAttributes redirect) {
        redirect.addFlashAttribute("message", ex.getMessage());
        return "redirect:/login?disabled=true";
    }

    /**
     * Bắt tất cả exception còn lại → trả về trang 500
     * + Gửi thông báo Slack + Log lên Seq
     */
    @ExceptionHandler(Exception.class)
    public ModelAndView handleInternalServerError(HttpServletRequest request, Exception ex) {
        String uri = request.getRequestURI();

        if (shouldNotifyError(uri, ex)) {
            errorNotificationService.notifyError500(request, ex);
        } else {
            log.warn("[SKIP-NOTIFY] {} {} | {}: {}",
                    request.getMethod(), uri,
                    ex.getClass().getSimpleName(), ex.getMessage());
        }

        ModelAndView mav = new ModelAndView();
        mav.setViewName("error/500");
        mav.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        mav.addObject("errorMessage", "Đã xảy ra lỗi hệ thống. Vui lòng thử lại sau.");
        return mav;
    }

    private boolean shouldNotifyError(String uri, Exception ex) {
        // Bỏ qua static resource
        if (uri.matches(".*\\.(map|css|js|ico|png|jpg|gif|svg|woff|woff2|ttf|eot)$")) {
            return false;
        }

        // Bỏ qua exception không nghiêm trọng
        Set<Class<? extends Exception>> ignoredExceptions = Set.of(
                org.springframework.web.servlet.resource.NoResourceFoundException.class,
                org.springframework.web.servlet.NoHandlerFoundException.class,
                org.springframework.web.HttpRequestMethodNotSupportedException.class,
                org.springframework.web.bind.MissingServletRequestParameterException.class
        );

        for (Class<? extends Exception> ignored : ignoredExceptions) {
            if (ignored.isInstance(ex)) {
                return false;
            }
        }

        // Bỏ qua theo message
        String msg = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";
        if (msg.contains("no static resource") || msg.contains("favicon.ico")) {
            return false;
        }

        return true;
    }
}
