package site.protoa.api.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 루트 경로를 /docs로 리다이렉트
 */
@Controller
public class RootController {

    @GetMapping("/")
    public String root() {
        return "redirect:/docs";
    }
}
