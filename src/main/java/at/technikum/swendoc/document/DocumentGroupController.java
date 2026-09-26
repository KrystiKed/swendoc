package at.technikum.swendoc.document;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/documents/group")
public class DocumentGroupController {

    private final DocumentService service;

    public DocumentGroupController(DocumentService service) {
        this.service = service;
    }

    @GetMapping
    public List<DocumentType> types() {
        return List.of(DocumentType.values());
    }

    /** All documents of one type: /documents/group/word, /pdf or /excel. */
    @GetMapping("/{id}")
    public List<DocumentResponse> documents(@PathVariable String id) {
        return service.findByType(DocumentType.fromPath(id)).stream()
                .map(DocumentResponse::from)
                .toList();
    }
}
