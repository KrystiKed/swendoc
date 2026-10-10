package at.technikum.swendoc.DocumentGroup;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import at.technikum.swendoc.user.User;
import at.technikum.swendoc.user.UserService;
import at.technikum.swendoc.validation.Validator;
import at.technikum.swendoc.validation.ValidatorFactory;

/** The caller's own groups; every request needs "Authorization: Bearer {token}" from POST /session. */
@RestController
@RequestMapping("/groups")
public class DocumentGroupController {

    private final DocumentGroupService service;
    private final UserService users;
    private final Validator<GroupNameRequest> names;

    public DocumentGroupController(DocumentGroupService service, UserService users, ValidatorFactory validators) {
        this.service = service;
        this.users = users;
        this.names = validators.forClass(GroupNameRequest.class);
    }

    @GetMapping
    public List<DocumentGroupResponse> list(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        return service.findAll(users.authenticate(authorization)).stream()
                .map(DocumentGroupResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public DocumentGroupResponse get(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
                                     @PathVariable UUID id) {
        return DocumentGroupResponse.from(service.find(users.authenticate(authorization), id));
    }

    @PostMapping
    public ResponseEntity<DocumentGroupResponse> create(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @RequestBody GroupNameRequest request) {
        User owner = users.authenticate(authorization);
        names.check(request);
        DocumentGroup group = service.create(owner, request.name());
        return ResponseEntity.created(URI.create("/groups/" + group.getId()))
                .body(DocumentGroupResponse.from(group));
    }

    @PutMapping("/{id}")
    public DocumentGroupResponse rename(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
                                        @PathVariable UUID id,
                                        @RequestBody GroupNameRequest request) {
        User owner = users.authenticate(authorization);
        names.check(request);
        return DocumentGroupResponse.from(service.rename(owner, id, request.name()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
                                       @PathVariable UUID id) {
        service.delete(users.authenticate(authorization), id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/documents/{documentId}")
    public DocumentGroupResponse addDocument(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
                                             @PathVariable UUID id, @PathVariable UUID documentId) {
        User owner = users.authenticate(authorization);
        return DocumentGroupResponse.from(service.addDocument(owner, id, documentId));
    }

    @DeleteMapping("/{id}/documents/{documentId}")
    public DocumentGroupResponse removeDocument(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
                                                @PathVariable UUID id, @PathVariable UUID documentId) {
        User owner = users.authenticate(authorization);
        return DocumentGroupResponse.from(service.removeDocument(owner, id, documentId));
    }
}
