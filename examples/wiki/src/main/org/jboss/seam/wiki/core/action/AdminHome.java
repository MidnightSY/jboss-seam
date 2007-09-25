package org.jboss.seam.wiki.core.action;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.TermEnum;
import org.hibernate.Session;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.util.ContextHelper;
import org.hibernate.search.FullTextSession;
import org.hibernate.validator.ClassValidator;
import org.hibernate.validator.InvalidValue;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.*;
import org.jboss.seam.annotations.remoting.WebRemote;
import org.jboss.seam.annotations.datamodel.DataModel;
import org.jboss.seam.annotations.datamodel.DataModelSelection;
import org.jboss.seam.annotations.security.Restrict;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.core.Validators;
import org.jboss.seam.log.Log;
import org.jboss.seam.security.AuthorizationException;
import org.jboss.seam.security.Identity;
import org.jboss.seam.wiki.core.model.LinkProtocol;
import org.jboss.seam.wiki.core.model.User;
import org.jboss.seam.wiki.core.search.IndexManager;
import org.jboss.seam.wiki.core.search.metamodel.SearchRegistry;
import org.jboss.seam.wiki.core.search.metamodel.SearchableEntity;
import org.jboss.seam.wiki.preferences.PreferenceComponent;
import org.jboss.seam.wiki.preferences.PreferenceVisibility;
import org.jboss.seam.wiki.util.Progress;

import javax.faces.application.FacesMessage;
import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.Serializable;

@Name("adminHome")
@Scope(ScopeType.CONVERSATION)
public class AdminHome implements Serializable {

    @Logger
    static Log log;

    @In
    private FacesMessages facesMessages;

    @In
    EntityManager entityManager;

    PreferenceEditor preferenceEditor;

    @Create
    public void create() {
        if (!Identity.instance().hasPermission("User", "isAdmin", (User)Component.getInstance("currentUser") ) ) {
            throw new AuthorizationException("You don't have permission for this operation");
        }
    }

    @Restrict("#{s:hasPermission('User', 'isAdmin', currentUser)}")
    public String update() {

        // Preferences
        if (preferenceEditor != null) {
            String editorFailed = preferenceEditor.save();
            if (editorFailed != null) return null;
        }

        entityManager.flush(); // Flush everything (maybe again if prefEditor.save() already flushed)

        facesMessages.addFromResourceBundleOrDefault(
            FacesMessage.SEVERITY_INFO,
            "systemSettingsUpdated",
            "System settings updated"
        );

        return null;
    }

    // ####################### PREFERENCES ##################################

    @DataModel(value = "systemPreferenceComponents")
    private List<PreferenceComponent> systemPreferenceComponents;

    @Factory("systemPreferenceComponents")
    public void loadUserPreferenceComponents() {
        preferenceEditor = (PreferenceEditor)Component.getInstance("prefEditor");
        preferenceEditor.setPreferenceVisibility(PreferenceVisibility.SYSTEM);
        systemPreferenceComponents = preferenceEditor.loadPreferenceComponents();
        Contexts.getConversationContext().set("preferenceEditor", preferenceEditor);
    }


    // ####################### LINK PROTOCOLS ##################################

    @DataModel(value = "linkProtocols")
    private List<LinkProtocol> linkProtocols;

    @DataModelSelection(value = "linkProtocols")
    private LinkProtocol selectedLinkProtocol;
    LinkProtocol linkProtocol = new LinkProtocol();
    LinkProtocol newLinkProtocol;

    @Factory("linkProtocols")
    public void loadLinkProtocols() {
        //noinspection unchecked
        Map<String, LinkProtocol> linkProtocolMap = (Map<String, LinkProtocol>)Component.getInstance("linkProtocolMap");
        linkProtocols = new ArrayList<LinkProtocol>(linkProtocolMap.values());
    }

    public LinkProtocol getLinkProtocol() {
        return linkProtocol;
    }

    public void addLinkProtocol() {

        // TODO: http://jira.jboss.com/jira/browse/JBSEAM-1297
        ClassValidator<LinkProtocol> validator = Validators.instance().getValidator(LinkProtocol.class);
        InvalidValue[] ivs = validator.getInvalidValues(linkProtocol);
        if (ivs.length>0) {
            for (InvalidValue iv : ivs) {
                facesMessages.addToControl(iv.getPropertyName(), FacesMessage.SEVERITY_INFO, iv.getMessage());
            }
            return;
        }

        newLinkProtocol = linkProtocol;
        linkProtocols.add(newLinkProtocol);
        entityManager.persist(newLinkProtocol);
        linkProtocol = new LinkProtocol();
    }

    public void removeLinkProtocol() {
        entityManager.remove(selectedLinkProtocol);
        linkProtocols.remove(selectedLinkProtocol);
    }

    // ####################### INDEXING ##################################

    @DataModel(value = "indexedEntities")
    private List<SearchableEntity> indexedEntities;

    @DataModelSelection(value = "indexedEntities")
    private SearchableEntity selectedIndexedEntity;

    @Factory("indexedEntities")
    public void loadIndexedEntities() throws Exception {

        SearchRegistry registry = (SearchRegistry)Component.getInstance("searchRegistry");
        indexedEntities = registry.getSearchableEntities();

        EntityManager em = (EntityManager) Component.getInstance("entityManager");

        for (SearchableEntity indexedEntity : indexedEntities) {
            DirectoryProvider dirProvider = ((FullTextSession)em.getDelegate()).getSearchFactory().getDirectoryProviders(indexedEntity.getClazz())[0];
            IndexReader reader = IndexReader.open(dirProvider.getDirectory());

            indexedEntity.setNumOfIndexedDocuments(reader.numDocs());

            TermEnum te = reader.terms();
            long numTerms = 0;
            while (te.next()) numTerms++;
            indexedEntity.setNumOfIndexedTerms(numTerms);

            long size = 0;
            String [] fileNames = dirProvider.getDirectory().list();
            for (String fileName : fileNames) {
                size += dirProvider.getDirectory().fileLength(fileName);
            }
            indexedEntity.setIndexSizeInBytes(size);

            reader.close();
        }
    }

    @In(required = false) @Out(required = false, scope = ScopeType.SESSION)
    public Map<String, Progress> indexingProgressMonitors;

    public void resetSearchIndex() throws Exception {

        IndexManager indexMgr = (IndexManager)Component.getInstance("indexManager");
        Progress progress = new Progress(selectedIndexedEntity.getClazz().getName());
        indexMgr.rebuildIndex(selectedIndexedEntity.getClazz(), progress);

        if (indexingProgressMonitors == null) indexingProgressMonitors = new HashMap<String, Progress>();
        indexingProgressMonitors.put(selectedIndexedEntity.getClazz().getName(), progress);
    }

    @WebRemote
    public Progress getIndexingProgress(String className) {
        return indexingProgressMonitors != null ? indexingProgressMonitors.get(className) : null;
    }

    @WebRemote
    public void resetIndexingProgress(String className) {
        if (indexingProgressMonitors != null) indexingProgressMonitors.remove(className);
    }

}
