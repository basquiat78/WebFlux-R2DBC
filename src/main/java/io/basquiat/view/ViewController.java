package io.basquiat.view;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 *
 * View Controller
 *
 * created by basquiat
 *
 */
@Controller
public class ViewController {

    /**
     * index html render controller
     * @param model
     * @return String
     */
    @GetMapping("/view/{street}")
    public String index(@PathVariable("street") String street, final Model model) {
        model.addAttribute("street", street);
        return "index";
    }

}
