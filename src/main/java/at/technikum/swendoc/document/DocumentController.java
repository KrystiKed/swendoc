package at.technikum.swendoc.document;

import at.technikum.swendoc.user.User;
import at.technikum.swendoc.user.UserService;
import at.technikum.swendoc.validation.Validator;
import at.technikum.swendoc.validation.ValidatorFactory;
import java.io.IOException;
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

/** The caller's own documents; every request needs "Authorization: Bearer {token}" from POST /api/session. */
@RestController
@RequestMapping("/api/docs")
public class DocumentController {

    private final DocumentService service;
    private final UserService users;
    private final Validator<UploadRequest> uploads;
    private final Validator<TitleRequest> titles;
    private final DocumentMapper mapper;

    public DocumentController(DocumentService service, UserService users, ValidatorFactory validators,
                              DocumentMapper mapper) {
        this.service = service;
        this.mapper = mapper;
        this.users = users;
        this.uploads = validators.forClass(UploadRequest.class);
        this.titles = validators.forClass(TitleRequest.class);
    }

    @GetMapping
    public List<DocumentResponse> list(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        return service.findAll(users.authenticate(authorization)).stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    public DocumentResponse get(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable UUID id) {
        return mapper.toResponse(service.find(users.authenticate(authorization), id));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentResponse> upload(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestParam(required = false) String title,
            @RequestPart(required = false) MultipartFile file) throws IOException {
        User owner = users.authenticate(authorization);
        // optional on purpose: a missing title or file is reported by the validator like any other rule
        UploadRequest request = uploads.check(new UploadRequest(title, file));
        Document saved;
        try (InputStream content = request.file().getInputStream()) {
            saved = service.upload(owner, request.title(), request.file().getOriginalFilename(),
                    request.file().getContentType(), request.file().getSize(), content);
        }
        return ResponseEntity.created(URI.create("/api/docs/" + saved.getId()))
                .body(mapper.toResponse(saved));
    }

    /** The bytes themselves; GET /api/docs/{id} stays metadata-only. */
    @GetMapping("/{id}/content")
    public ResponseEntity<InputStreamResource> content(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable UUID id) {
        Document document = service.find(users.authenticate(authorization), id);
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
    public DocumentResponse rename(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable UUID id, @RequestBody TitleRequest request) {
        User owner = users.authenticate(authorization);
        titles.check(request);
        return mapper.toResponse(service.rename(owner, id, request.title()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable UUID id) {
        service.delete(users.authenticate(authorization), id);
        return ResponseEntity.noContent().build();
    }
}
