package at.technikum.swendoc;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Deep links into the Angular app (reload on /documents) get the SPA shell, not a 404. */
@Controller
public class SpaController {

    // ponytail: explicit list of client routes; add new ones here when the router grows
    @GetMapping({"/login", "/documents", "/my-groups"})
    public String index() {
        return "forward:/index.html";
    }
}
