package at.technikum.swendoc.document;

import at.technikum.swendoc.user.User;
import at.technikum.swendoc.user.UserService;
import at.technikum.swendoc.validation.Validator;
import at.technikum.swendoc.validation.ValidatorFactory;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/docs")
public class DocumentController {

    private final DocumentService service;
    private final UserService users;
    private final Validator<UploadRequest> uploads;
    private final Validator<TitleRequest> titles;

    public DocumentController(DocumentService service, UserService users, ValidatorFactory validators) {
        this.service = service;
        this.users = users;
        this.uploads = validators.forClass(UploadRequest.class);
        this.titles = validators.forClass(TitleRequest.class);
    }

    @GetMapping
    public List<DocumentResponse> list() {
        return service.findAll().stream().map(DocumentResponse::from).toList();
    }

    @GetMapping("/{id}")
    public DocumentResponse get(@PathVariable UUID id) {
        return DocumentResponse.from(service.find(id));
    }

    /** With a session token the uploader becomes the owner and can put the document into groups. */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentResponse> upload(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestParam(required = false) String title,
            @RequestPart(required = false) MultipartFile file) throws Exception {
        // optional on purpose: a missing title or file is reported by the validator like any other rule
        UploadRequest request = uploads.check(new UploadRequest(title, file));
        User owner = authorization != null ? users.authenticate(authorization) : null;
        Document saved = service.upload(request.title(), request.file(), owner);
        return ResponseEntity.created(URI.create("/docs/" + saved.getId()))
                .body(DocumentResponse.from(saved));
    }

    /** The bytes themselves; GET /docs/{id} stays metadata-only. */
    @GetMapping("/{id}/content")
    public ResponseEntity<InputStreamResource> content(@PathVariable UUID id) throws Exception {
        Document document = service.find(id);
        InputStream stream = service.download(document);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + document.getFilename() + "\"")
                .contentType(MediaType.parseMediaType(
                        document.getContentType() != null
                                ? document.getContentType()
                                : MediaType.APPLICATION_OCTET_STREAM_VALUE))
                .body(new InputStreamResource(stream));
    }

    @PutMapping("/{id}")
    public DocumentResponse rename(@PathVariable UUID id, @RequestBody TitleRequest request) {
        titles.check(request);
        return DocumentResponse.from(service.rename(id, request.title()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) throws Exception {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
