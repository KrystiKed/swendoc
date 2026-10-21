package at.technikum.swendoc.DocumentGroup;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import at.technikum.swendoc.document.Document;
import at.technikum.swendoc.document.DocumentNotFoundException;
import at.technikum.swendoc.document.DocumentRepository;
import at.technikum.swendoc.user.User;

@Service
public class DocumentGroupServiceImpl implements DocumentGroupService {

    private final DocumentGroupRepository groups;
    private final DocumentRepository documents;

    public DocumentGroupServiceImpl(DocumentGroupRepository groups, DocumentRepository documents) {
        this.groups = groups;
        this.documents = documents;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentGroup> findAll(User owner) {
        List<DocumentGroup> result = groups.findByOwner(owner);
        result.forEach(group -> group.getDocuments().size()); // load the lazy set inside the transaction
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentGroup find(User owner, UUID id) {
        DocumentGroup group = groups.findById(id)
                .filter(candidate -> candidate.getOwner().getId().equals(owner.getId()))
                .orElseThrow(() -> new DocumentGroupNotFoundException(id));
        group.getDocuments().size();
        return group;
    }

    @Override
    @Transactional
    public DocumentGroup create(User owner, String name) {
        return groups.save(new DocumentGroup(name, owner));
    }

    @Override
    @Transactional
    public DocumentGroup rename(User owner, UUID id, String name) {
        DocumentGroup group = find(owner, id);
        group.setName(name);
        return groups.save(group);
    }

    @Override
    @Transactional
    public void delete(User owner, UUID id) {
        groups.delete(find(owner, id));
    }

    @Override
    @Transactional
    public DocumentGroup addDocument(User owner, UUID groupId, UUID documentId) {
        DocumentGroup group = find(owner, groupId);
        group.addDocument(ownDocument(owner, documentId));
        return groups.save(group);
    }

    @Override
    @Transactional
    public DocumentGroup removeDocument(User owner, UUID groupId, UUID documentId) {
        DocumentGroup group = find(owner, groupId);
        group.removeDocument(ownDocument(owner, documentId));
        return groups.save(group);
    }

    private Document ownDocument(User owner, UUID documentId) {
        return documents.findById(documentId)
                .filter(document -> document.getOwner() != null
                        && document.getOwner().getId().equals(owner.getId()))
                .orElseThrow(() -> new DocumentNotFoundException(documentId));
    }
}
